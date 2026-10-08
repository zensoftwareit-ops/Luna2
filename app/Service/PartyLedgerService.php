<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;

final class PartyLedgerService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId) {}

    /**
     * Rende ogni anagrafica un sottoconto e collega in modo idempotente le
     * scritture gia presenti. Non modifica conti, importi o quadrature.
     */
    public function synchronize(): array
    {
        $counts = ['codes' => 0, 'open_items' => 0, 'document_items' => 0, 'documents' => 0, 'payments' => 0, 'historical' => 0];
        foreach ([['customers', 'CLI-'], ['suppliers', 'FOR-']] as [$table, $prefix]) {
            $statement = $this->db->prepare(
                "UPDATE {$table} SET code = CONCAT(?, LPAD(id, 8, '0')), updated_at = NOW()
                 WHERE organization_id = ? AND (code IS NULL OR TRIM(code) = '')"
            );
            $statement->execute([$prefix, $this->organizationId]);
            $counts['codes'] += $statement->rowCount();
        }

        $statement = $this->db->prepare(
            "UPDATE accounting_open_items i
             JOIN documents d ON d.id = i.document_id AND d.organization_id = i.organization_id
             JOIN accounting_account_mappings m ON m.organization_id = i.organization_id
               AND m.mapping_key = CASE WHEN d.counterparty_type = 'SUPPLIER' THEN 'TRADE_PAYABLES' ELSE 'TRADE_RECEIVABLES' END
             SET i.party_type = d.counterparty_type, i.party_id = d.counterparty_id,
                 i.party_name = d.counterparty_name, i.account_id = m.account_id,
                 i.direction = CASE
                   WHEN (d.document_type = 'CREDIT_NOTE' OR UPPER(COALESCE(d.fatturapa_type, '')) IN ('TD04','TD08'))
                     THEN CASE WHEN d.counterparty_type = 'SUPPLIER' THEN 'RECEIVABLE' ELSE 'PAYABLE' END
                   ELSE CASE WHEN d.counterparty_type = 'SUPPLIER' THEN 'PAYABLE' ELSE 'RECEIVABLE' END
                 END,
                 i.updated_at = NOW()
             WHERE i.organization_id = ? AND d.counterparty_id IS NOT NULL"
        );
        $statement->execute([$this->organizationId]);
        $counts['document_items'] += $statement->rowCount();

        foreach ([
            ['suppliers', 'SUPPLIER', 'PAYABLE'],
            ['customers', 'CUSTOMER', 'RECEIVABLE'],
        ] as [$table, $type, $direction]) {
            $statement = $this->db->prepare(
                "UPDATE accounting_open_items i
                 JOIN {$table} p ON p.organization_id = i.organization_id
                   AND LOWER(TRIM(p.business_name)) = LOWER(TRIM(i.party_name))
                 SET i.party_type = ?, i.party_id = p.id, i.updated_at = NOW()
                 WHERE i.organization_id = ? AND i.direction = ? AND i.party_id IS NULL
                   AND (SELECT COUNT(*) FROM {$table} unique_party
                        WHERE unique_party.organization_id = i.organization_id
                          AND LOWER(TRIM(unique_party.business_name)) = LOWER(TRIM(i.party_name))) = 1"
            );
            $statement->execute([$type, $this->organizationId, $direction]);
            $counts['open_items'] += $statement->rowCount();
        }

        foreach ([['SUPPLIER', 'supplier_id'], ['CUSTOMER', 'customer_id']] as [$type, $column]) {
            $statement = $this->db->prepare(
                "UPDATE journal_entry_lines l
                 JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 JOIN documents d ON d.id = e.source_id AND d.organization_id = e.organization_id
                 SET l.{$column} = d.counterparty_id
                 WHERE l.organization_id = ? AND e.source_type = 'DOCUMENT' AND d.counterparty_type = ?
                   AND d.counterparty_id IS NOT NULL AND l.{$column} IS NULL
                   AND EXISTS (SELECT 1 FROM accounting_open_items i
                               WHERE i.organization_id = l.organization_id AND i.document_id = d.id
                                 AND i.account_id = l.account_id)"
            );
            $statement->execute([$this->organizationId, $type]);
            $counts['documents'] += $statement->rowCount();
        }

        foreach ([['SUPPLIER', 'supplier_id'], ['CUSTOMER', 'customer_id']] as [$type, $column]) {
            $statement = $this->db->prepare(
                "UPDATE journal_entry_lines l
                 JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 JOIN payments p ON p.id = e.source_id AND p.organization_id = e.organization_id
                 JOIN payment_allocations allocation ON allocation.payment_id = p.id AND allocation.organization_id = p.organization_id
                 JOIN accounting_open_items i ON i.id = allocation.open_item_id AND i.organization_id = allocation.organization_id
                 SET l.{$column} = i.party_id
                 WHERE l.organization_id = ? AND e.source_type IN ('PAYMENT','PAYMENT_REVERSAL')
                   AND i.party_type = ? AND i.party_id IS NOT NULL AND l.{$column} IS NULL
                   AND l.account_id = i.account_id"
            );
            $statement->execute([$this->organizationId, $type]);
            $counts['payments'] += $statement->rowCount();
        }

        foreach ([
            ['suppliers', 'supplier_id', 'TRADE_PAYABLES'],
            ['customers', 'customer_id', 'TRADE_RECEIVABLES'],
        ] as [$table, $column, $mapping]) {
            $statement = $this->db->prepare(
                "UPDATE journal_entry_lines l
                 JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
                   AND m.mapping_key = ? AND m.account_id = l.account_id
                 JOIN {$table} p ON p.organization_id = e.organization_id
                   AND LOWER(TRIM(p.business_name)) = LOWER(TRIM(e.counterparty))
                 SET l.{$column} = p.id
                 WHERE l.organization_id = ? AND l.{$column} IS NULL AND COALESCE(e.counterparty, '') <> ''
                   AND (SELECT COUNT(*) FROM {$table} unique_party
                        WHERE unique_party.organization_id = e.organization_id
                          AND LOWER(TRIM(unique_party.business_name)) = LOWER(TRIM(e.counterparty))) = 1"
            );
            $statement->execute([$mapping, $this->organizationId]);
            $counts['historical'] += $statement->rowCount();
        }
        $counts['open_items'] += $this->matchOpenItemsByCanonicalName();
        $counts['historical'] += $this->matchHistoricalLines();
        return $counts;
    }

    /** @return array<int,array<string,mixed>> */
    public function unassigned(string $type, int $limit = 100): array
    {
        [, $idColumn, , ] = $this->typeConfig($type);
        $mapping = $type === 'SUPPLIER' ? 'TRADE_PAYABLES' : 'TRADE_RECEIVABLES';
        $limit = max(1, min(500, $limit));
        $statement = $this->db->prepare(
            "SELECT l.id AS line_id, e.id AS entry_id, e.entry_date, e.protocol_number, e.source_protocol,
                    e.document_number, e.counterparty, e.description AS entry_description,
                    a.code AS account_code, a.name AS account_name, l.description, l.debit, l.credit
             FROM journal_entry_lines l
             JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = l.organization_id
             JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
               AND m.mapping_key = ? AND m.account_id = l.account_id
             WHERE l.organization_id = ? AND l.{$idColumn} IS NULL AND e.status = 'POSTED'
             ORDER BY e.entry_date DESC, e.id DESC, l.id DESC LIMIT {$limit}"
        );
        $statement->execute([$mapping, $this->organizationId]);
        return $statement->fetchAll();
    }

    public function unassignedCount(string $type): int
    {
        [, $idColumn, , ] = $this->typeConfig($type);
        $mapping = $type === 'SUPPLIER' ? 'TRADE_PAYABLES' : 'TRADE_RECEIVABLES';
        $statement = $this->db->prepare(
            "SELECT COUNT(*) FROM journal_entry_lines l
             JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
               AND m.mapping_key = ? AND m.account_id = l.account_id
             WHERE l.organization_id = ? AND l.{$idColumn} IS NULL AND e.status = 'POSTED'"
        );
        $statement->execute([$mapping, $this->organizationId]);
        return (int) $statement->fetchColumn();
    }

    /** @return array<int,array{id:int,code:string,business_name:string}> */
    public function partyChoices(string $type): array
    {
        [$table] = $this->typeConfig($type);
        $statement = $this->db->prepare(
            "SELECT id, code, business_name FROM {$table} WHERE organization_id = ? AND active = 1 ORDER BY business_name, code LIMIT 5000"
        );
        $statement->execute([$this->organizationId]);
        return $statement->fetchAll();
    }

    /** @return array<string,mixed> */
    public function assignLine(string $type, int $lineId, int $partyId): array
    {
        [$table, $idColumn] = $this->typeConfig($type);
        $mapping = $type === 'SUPPLIER' ? 'TRADE_PAYABLES' : 'TRADE_RECEIVABLES';
        $party = $this->db->prepare("SELECT id, code, business_name FROM {$table} WHERE id = ? AND organization_id = ? AND active = 1");
        $party->execute([$partyId, $this->organizationId]);
        $partyRow = $party->fetch();
        if (!$partyRow) {
            throw new InvalidArgumentException('Cliente o fornitore selezionato non valido.');
        }
        $otherColumn = $idColumn === 'supplier_id' ? 'customer_id' : 'supplier_id';
        $statement = $this->db->prepare(
            "UPDATE journal_entry_lines l
             JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
               AND m.mapping_key = ? AND m.account_id = l.account_id
             SET l.{$idColumn} = ?, l.{$otherColumn} = NULL
             WHERE l.id = ? AND l.organization_id = ?"
        );
        $statement->execute([$mapping, $partyId, $lineId, $this->organizationId]);
        if ($statement->rowCount() !== 1) {
            throw new InvalidArgumentException('Riga non trovata oppure non appartenente al conto collettivo selezionato.');
        }
        return ['line_id' => $lineId, 'party_id' => $partyId, 'party_code' => $partyRow['code'], 'party_name' => $partyRow['business_name']];
    }

    public function overview(string $type, string $from, string $to, string $search = ''): array
    {
        [$table, $idColumn, , $sign] = $this->typeConfig($type);
        $this->assertDates($from, $to);
        $search = trim($search);
        $where = '';
        if ($search !== '') {
            $where = ' AND (p.code LIKE ? OR p.business_name LIKE ? OR p.vat_number LIKE ? OR p.tax_code LIKE ?)';
        }
        $balanceExpression = $sign === 1 ? 'COALESCE(m.debit,0) - COALESCE(m.credit,0)' : 'COALESCE(m.credit,0) - COALESCE(m.debit,0)';
        $openingExpression = $sign === 1 ? 'COALESCE(m.opening_debit,0) - COALESCE(m.opening_credit,0)' : 'COALESCE(m.opening_credit,0) - COALESCE(m.opening_debit,0)';
        $sql = "SELECT p.id, p.code, p.business_name, p.vat_number, p.tax_code, p.active,
                       COALESCE(m.debit,0) AS period_debit, COALESCE(m.credit,0) AS period_credit,
                       {$openingExpression} AS opening_balance, {$balanceExpression} AS period_balance,
                       {$openingExpression} + {$balanceExpression} AS closing_balance,
                       COALESCE(o.outstanding,0) AS outstanding, COALESCE(o.open_count,0) AS open_count
                FROM {$table} p
                LEFT JOIN (
                    SELECT l.{$idColumn} AS party_id,
                           SUM(CASE WHEN e.entry_date < ? THEN l.debit ELSE 0 END) AS opening_debit,
                           SUM(CASE WHEN e.entry_date < ? THEN l.credit ELSE 0 END) AS opening_credit,
                           SUM(CASE WHEN e.entry_date BETWEEN ? AND ? THEN l.debit ELSE 0 END) AS debit,
                           SUM(CASE WHEN e.entry_date BETWEEN ? AND ? THEN l.credit ELSE 0 END) AS credit
                    FROM journal_entry_lines l JOIN journal_entries e
                      ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                    WHERE l.organization_id = ? AND l.{$idColumn} IS NOT NULL AND e.status = 'POSTED' AND e.entry_date <= ?
                    GROUP BY l.{$idColumn}
                ) m ON m.party_id = p.id
                LEFT JOIN (
                    SELECT party_id,
                           SUM(CASE WHEN direction = ? THEN GREATEST(original_amount - settled_amount, 0)
                                    ELSE -GREATEST(original_amount - settled_amount, 0) END) AS outstanding,
                           SUM(status IN ('OPEN','PARTIAL','OVERDUE','DISPUTED')) AS open_count
                    FROM accounting_open_items
                    WHERE organization_id = ? AND party_type = ? AND party_id IS NOT NULL AND status <> 'CANCELLED'
                    GROUP BY party_id
                ) o ON o.party_id = p.id
                WHERE p.organization_id = ?{$where}
                ORDER BY p.business_name, p.code LIMIT 2000";

        // The movement subquery needs: from, from, from, to, from, to, org, to.
        $expectedDirection = $type === 'SUPPLIER' ? 'PAYABLE' : 'RECEIVABLE';
        $params = [$from, $from, $from, $to, $from, $to, $this->organizationId, $to,
            $expectedDirection, $this->organizationId, $type, $this->organizationId];
        if ($search !== '') {
            $like = '%' . $search . '%';
            array_push($params, $like, $like, $like, $like);
        }
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetchAll();
    }

    public function detail(string $type, int $partyId, string $from, string $to): array
    {
        [$table, $idColumn, $expectedDirection, $sign] = $this->typeConfig($type);
        $this->assertDates($from, $to);
        $partyStatement = $this->db->prepare("SELECT * FROM {$table} WHERE id = ? AND organization_id = ?");
        $partyStatement->execute([$partyId, $this->organizationId]);
        $party = $partyStatement->fetch();
        if (!$party) {
            throw new InvalidArgumentException('Sottoconto cliente/fornitore non trovato.');
        }
        $statement = $this->db->prepare(
            "SELECT e.id AS entry_id, e.entry_date, e.protocol_number, e.source_protocol, e.entry_type,
                    e.document_number, e.description AS entry_description, l.description, l.debit, l.credit
             FROM journal_entry_lines l JOIN journal_entries e
               ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             WHERE l.organization_id = ? AND l.{$idColumn} = ? AND e.status = 'POSTED' AND e.entry_date <= ?
             ORDER BY e.entry_date, e.id, l.line_number, l.id"
        );
        $statement->execute([$this->organizationId, $partyId, $to]);
        $opening = $balance = $debit = $credit = 0;
        $rows = [];
        foreach ($statement->fetchAll() as $row) {
            $movement = $sign * ((float) $row['debit'] - (float) $row['credit']);
            if ((string) $row['entry_date'] < $from) {
                $opening += $movement;
                continue;
            }
            $balance += $movement;
            $debit += (float) $row['debit'];
            $credit += (float) $row['credit'];
            $row['running_balance'] = round($opening + $balance, 2);
            $rows[] = $row;
        }
        $items = $this->db->prepare(
            "SELECT i.*, GREATEST(i.original_amount - i.settled_amount, 0) AS outstanding
             FROM accounting_open_items i WHERE i.organization_id = ? AND i.party_type = ? AND i.party_id = ?
             ORDER BY i.issue_date, i.due_date, i.id"
        );
        $items->execute([$this->organizationId, $type, $partyId]);
        $openItems = $items->fetchAll();
        $outstanding = array_sum(array_map(
            static fn (array $item): float => $item['status'] === 'CANCELLED'
                ? 0.0
                : ($item['direction'] === $expectedDirection ? 1.0 : -1.0) * (float) $item['outstanding'],
            $openItems
        ));
        foreach ($openItems as &$item) {
            $item['signed_outstanding'] = ($item['direction'] === $expectedDirection ? 1.0 : -1.0) * (float) $item['outstanding'];
        }
        unset($item);
        return [
            'party' => $party, 'rows' => $rows, 'open_items' => $openItems,
            'totals' => ['opening' => round($opening, 2), 'debit' => round($debit, 2), 'credit' => round($credit, 2),
                'closing' => round($opening + $balance, 2), 'outstanding' => round($outstanding, 2)],
        ];
    }

    private function typeConfig(string $type): array
    {
        return match (strtoupper($type)) {
            'CUSTOMER' => ['customers', 'customer_id', 'RECEIVABLE', 1],
            'SUPPLIER' => ['suppliers', 'supplier_id', 'PAYABLE', -1],
            default => throw new InvalidArgumentException('Tipo di partitario non valido.'),
        };
    }

    private function matchOpenItemsByCanonicalName(): int
    {
        $updated = 0;
        foreach ([['SUPPLIER', 'suppliers'], ['CUSTOMER', 'customers']] as [$type, $table]) {
            $parties = $this->partyMatchIndex($table);
            $statement = $this->db->prepare(
                "SELECT id, party_name FROM accounting_open_items
                 WHERE organization_id = ? AND party_id IS NULL AND party_name IS NOT NULL AND TRIM(party_name) <> ''"
            );
            $statement->execute([$this->organizationId]);
            $save = $this->db->prepare(
                'UPDATE accounting_open_items SET party_type = ?, party_id = ?, updated_at = NOW() WHERE id = ? AND organization_id = ? AND party_id IS NULL'
            );
            foreach ($statement->fetchAll() as $item) {
                $ids = $parties['names'][$this->canonical((string) $item['party_name'])] ?? [];
                if (count($ids) !== 1) {
                    continue;
                }
                $save->execute([$type, $ids[0], $item['id'], $this->organizationId]);
                $updated += $save->rowCount();
            }
        }
        return $updated;
    }

    private function matchHistoricalLines(): int
    {
        $updated = 0;
        foreach ([
            ['SUPPLIER', 'suppliers', 'supplier_id', 'TRADE_PAYABLES'],
            ['CUSTOMER', 'customers', 'customer_id', 'TRADE_RECEIVABLES'],
        ] as [$type, $table, $idColumn, $mapping]) {
            $parties = $this->partyMatchIndex($table);
            $documentMap = $this->documentPartyMap($type);
            $statement = $this->db->prepare(
                "SELECT l.id AS line_id, e.id AS entry_id, e.entry_date, e.document_number, e.counterparty,
                        e.description AS entry_description,
                        GROUP_CONCAT(COALESCE(all_lines.description, '') SEPARATOR ' ') AS line_descriptions
                 FROM journal_entry_lines l
                 JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
                   AND m.mapping_key = ? AND m.account_id = l.account_id
                 LEFT JOIN journal_entry_lines all_lines ON all_lines.journal_entry_id = e.id
                   AND all_lines.organization_id = e.organization_id
                 WHERE l.organization_id = ? AND l.{$idColumn} IS NULL AND e.status = 'POSTED'
                 GROUP BY l.id, e.id, e.entry_date, e.document_number, e.counterparty, e.description
                 ORDER BY e.entry_date, e.id, l.id LIMIT 10000"
            );
            $statement->execute([$mapping, $this->organizationId]);
            $save = $this->db->prepare("UPDATE journal_entry_lines SET {$idColumn} = ? WHERE id = ? AND organization_id = ? AND {$idColumn} IS NULL");
            foreach ($statement->fetchAll() as $line) {
                $partyId = $this->candidateForLine($line, $parties, $documentMap);
                if (!$partyId) {
                    continue;
                }
                $save->execute([$partyId, $line['line_id'], $this->organizationId]);
                $updated += $save->rowCount();
            }
        }
        return $updated;
    }

    /** @return array{names:array<string,array<int,int>>,parties:array<int,array<string,mixed>>} */
    private function partyMatchIndex(string $table): array
    {
        $statement = $this->db->prepare("SELECT id, business_name, vat_number, tax_code FROM {$table} WHERE organization_id = ? AND active = 1");
        $statement->execute([$this->organizationId]);
        $names = [];
        $parties = [];
        foreach ($statement->fetchAll() as $party) {
            $party['canonical_name'] = $this->canonical((string) $party['business_name']);
            $party['vat_digits'] = preg_replace('/\D+/', '', (string) ($party['vat_number'] ?? '')) ?: '';
            $party['tax_key'] = $this->canonical((string) ($party['tax_code'] ?? ''));
            if ($party['canonical_name'] !== '') {
                $names[$party['canonical_name']][] = (int) $party['id'];
            }
            $parties[] = $party;
        }
        return compact('names', 'parties');
    }

    /** @return array<string,array<int,int>> */
    private function documentPartyMap(string $type): array
    {
        $statement = $this->db->prepare(
            'SELECT counterparty_id, number, document_date FROM documents
             WHERE organization_id = ? AND counterparty_type = ? AND counterparty_id IS NOT NULL'
        );
        $statement->execute([$this->organizationId, $type]);
        $map = [];
        foreach ($statement->fetchAll() as $document) {
            $number = $this->canonicalDocument((string) $document['number']);
            if ($number === '') {
                continue;
            }
            $year = substr((string) $document['document_date'], 0, 4);
            $map[$year . '|' . $number][] = (int) $document['counterparty_id'];
        }
        foreach ($map as &$ids) {
            $ids = array_values(array_unique($ids));
        }
        unset($ids);
        return $map;
    }

    /** @param array<string,mixed> $line @param array<string,mixed> $parties @param array<string,array<int,int>> $documentMap */
    private function candidateForLine(array $line, array $parties, array $documentMap): ?int
    {
        $counterpartyKey = $this->canonical((string) ($line['counterparty'] ?? ''));
        $exact = $parties['names'][$counterpartyKey] ?? [];
        if (count($exact) === 1) {
            return $exact[0];
        }
        $documentKey = substr((string) $line['entry_date'], 0, 4) . '|'
            . $this->canonicalDocument((string) ($line['document_number'] ?? ''));
        $documentIds = $documentMap[$documentKey] ?? [];
        if (count($documentIds) === 1) {
            return $documentIds[0];
        }
        $rawHaystack = implode(' ', array_filter([
            $line['counterparty'] ?? null, $line['entry_description'] ?? null, $line['line_descriptions'] ?? null,
        ]));
        $canonicalHaystack = $this->canonical($rawHaystack);
        $digitHaystack = preg_replace('/\D+/', '', $rawHaystack) ?: '';
        $matches = [];
        foreach ($parties['parties'] as $party) {
            if ($party['vat_digits'] !== '' && strlen($party['vat_digits']) >= 8 && str_contains($digitHaystack, $party['vat_digits'])) {
                $matches[] = (int) $party['id'];
                continue;
            }
            if ($party['tax_key'] !== '' && strlen($party['tax_key']) >= 8 && str_contains($canonicalHaystack, $party['tax_key'])) {
                $matches[] = (int) $party['id'];
                continue;
            }
            if ($party['canonical_name'] !== '' && strlen($party['canonical_name']) >= 5
                && str_contains($canonicalHaystack, $party['canonical_name'])) {
                $matches[] = (int) $party['id'];
            }
        }
        $matches = array_values(array_unique($matches));
        return count($matches) === 1 ? $matches[0] : null;
    }

    private function canonical(string $value): string
    {
        $value = mb_strtoupper(trim($value));
        if (function_exists('iconv')) {
            $transliterated = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $value);
            if ($transliterated !== false) {
                $value = $transliterated;
            }
        }
        $value = preg_replace('/\b(SRL|SRLS|SPA|SAPA|SAS|SNC|SCARL|SOC COOP|SOCIETA A RESPONSABILITA LIMITATA)\b/', ' ', $value) ?? $value;
        return trim(preg_replace('/[^A-Z0-9]+/', ' ', $value) ?? '');
    }

    private function canonicalDocument(string $value): string
    {
        return preg_replace('/[^A-Z0-9]+/', '', mb_strtoupper(trim($value))) ?? '';
    }

    private function assertDates(string $from, string $to): void
    {
        foreach ([$from, $to] as $value) {
            $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
            if (!$date || $date->format('Y-m-d') !== $value) {
                throw new InvalidArgumentException('Intervallo del partitario non valido.');
            }
        }
        if ($from > $to) {
            throw new InvalidArgumentException('La data iniziale non può essere successiva a quella finale.');
        }
    }
}
