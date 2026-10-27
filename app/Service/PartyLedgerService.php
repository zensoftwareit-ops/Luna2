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
        $counts = ['codes' => 0, 'open_items' => 0, 'document_items' => 0, 'documents' => 0, 'payments' => 0, 'historical' => 0, 'datev_rows' => 0];
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
        // I CSV di prima nota DATEV usano il nome del sottoconto nella colonna
        // account_name, mentre la testata puo contenere una causale diversa.
        // Le righe sorgente vengono conservate integralmente in import_rows:
        // usarle evita abbinamenti euristici e recupera anche tutto lo storico.
        $repairAssigned = $this->datevPartyRepairRequired();
        $counts['datev_rows'] += $this->matchHistoricalLinesFromDatevRows($repairAssigned);
        if ($repairAssigned) {
            $this->markDatevPartyRepairCompleted();
        }
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

    /** @return array{total_debit:float,total_credit:float,assigned_debit:float,assigned_credit:float,unassigned_count:int,is_balanced:bool} */
    public function integrity(string $type): array
    {
        [, $idColumn, , ] = $this->typeConfig($type);
        $mapping = strtoupper($type) === 'SUPPLIER' ? 'TRADE_PAYABLES' : 'TRADE_RECEIVABLES';
        $statement = $this->db->prepare(
            "SELECT COALESCE(SUM(l.debit),0) AS total_debit, COALESCE(SUM(l.credit),0) AS total_credit,
                    COALESCE(SUM(CASE WHEN l.{$idColumn} IS NOT NULL THEN l.debit ELSE 0 END),0) AS assigned_debit,
                    COALESCE(SUM(CASE WHEN l.{$idColumn} IS NOT NULL THEN l.credit ELSE 0 END),0) AS assigned_credit,
                    SUM(CASE WHEN l.{$idColumn} IS NULL THEN 1 ELSE 0 END) AS unassigned_count
             FROM journal_entry_lines l
             JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
               AND m.mapping_key = ? AND m.account_id = l.account_id
             WHERE l.organization_id = ? AND e.status = 'POSTED'"
        );
        $statement->execute([$mapping, $this->organizationId]);
        $row = $statement->fetch() ?: [];
        $result = [
            'total_debit' => (float) ($row['total_debit'] ?? 0),
            'total_credit' => (float) ($row['total_credit'] ?? 0),
            'assigned_debit' => (float) ($row['assigned_debit'] ?? 0),
            'assigned_credit' => (float) ($row['assigned_credit'] ?? 0),
            'unassigned_count' => (int) ($row['unassigned_count'] ?? 0),
            'is_balanced' => false,
        ];
        $result['is_balanced'] = $result['unassigned_count'] === 0
            && abs($result['total_debit'] - $result['assigned_debit']) < 0.005
            && abs($result['total_credit'] - $result['assigned_credit']) < 0.005;
        return $result;
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
                       COALESCE(o.outstanding,0) AS outstanding, COALESCE(o.open_count,0) AS open_count,
                       COALESCE(op.has_opening,0) AS has_period_opening
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
                    SELECT DISTINCT l.{$idColumn} AS party_id, 1 AS has_opening
                    FROM journal_entry_lines l JOIN journal_entries e
                      ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                    WHERE l.organization_id = ? AND l.{$idColumn} IS NOT NULL AND e.status = 'POSTED'
                      AND e.entry_type = 'OPENING' AND e.entry_date BETWEEN ? AND ?
                ) op ON op.party_id = p.id
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
            $this->organizationId, $from, $to,
            $expectedDirection, $this->organizationId, $type, $this->organizationId];
        if ($search !== '') {
            $like = '%' . $search . '%';
            array_push($params, $like, $like, $like, $like);
        }
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $rows = $statement->fetchAll();
        foreach ($rows as &$row) {
            if (!empty($row['has_period_opening'])) {
                $row['opening_balance'] = 0.0;
                $row['closing_balance'] = (float) $row['period_balance'];
            }
        }
        unset($row);
        return $rows;
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
        $mappingKey = $type === 'SUPPLIER' ? 'TRADE_PAYABLES' : 'TRADE_RECEIVABLES';
        $accountStatement = $this->db->prepare(
            'SELECT a.id, a.code, a.name, a.account_type
             FROM accounting_account_mappings m
             JOIN chart_of_accounts a ON a.id = m.account_id AND a.organization_id = m.organization_id
             WHERE m.organization_id = ? AND m.mapping_key = ? LIMIT 1'
        );
        $accountStatement->execute([$this->organizationId, $mappingKey]);
        $collectiveAccount = $accountStatement->fetch() ?: [
            'id' => null, 'code' => '',
            'name' => $type === 'SUPPLIER' ? 'Debiti verso fornitori' : 'Crediti verso clienti',
            'account_type' => $type === 'SUPPLIER' ? 'LIABILITY' : 'ASSET',
        ];
        $statement = $this->db->prepare(
            "SELECT e.id AS entry_id, e.entry_date, e.competence_date, e.protocol_number, e.source_protocol, e.entry_type,
                    e.document_number, COALESCE(d.document_date, e.competence_date) AS document_date,
                    e.description AS entry_description, l.description, l.debit, l.credit
             FROM journal_entry_lines l JOIN journal_entries e
               ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             LEFT JOIN documents d ON e.source_type = 'DOCUMENT' AND d.id = e.source_id
               AND d.organization_id = e.organization_id
             WHERE l.organization_id = ? AND l.{$idColumn} = ? AND e.status = 'POSTED' AND e.entry_date <= ?
             ORDER BY e.entry_date, e.id, l.line_number, l.id"
        );
        $statement->execute([$this->organizationId, $partyId, $to]);
        $movementRows = $statement->fetchAll();
        $hasPeriodOpening = count(array_filter($movementRows, static fn (array $row): bool =>
            (string) ($row['entry_type'] ?? '') === 'OPENING'
            && (string) $row['entry_date'] >= $from
            && (string) $row['entry_date'] <= $to
        )) > 0;
        $opening = $openingDebit = $openingCredit = $balance = $debit = $credit = 0;
        $rows = [];
        foreach ($movementRows as $row) {
            $movement = $sign * ((float) $row['debit'] - (float) $row['credit']);
            if ((string) $row['entry_date'] < $from) {
                if (!$hasPeriodOpening) {
                    $opening += $movement;
                    $openingDebit += (float) $row['debit'];
                    $openingCredit += (float) $row['credit'];
                }
                continue;
            }
            $balance += $movement;
            $debit += (float) $row['debit'];
            $credit += (float) $row['credit'];
            $row['running_balance'] = round($opening + $balance, 2);
            $row['accounting_running_balance'] = round(
                ($openingDebit + $debit) - ($openingCredit + $credit),
                2
            );
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
            'party' => $party, 'collective_account' => $collectiveAccount, 'rows' => $rows, 'open_items' => $openItems,
            'totals' => ['opening' => round($opening, 2), 'debit' => round($debit, 2), 'credit' => round($credit, 2),
                'closing' => round($opening + $balance, 2), 'outstanding' => round($outstanding, 2),
                'opening_debit' => round($openingDebit, 2), 'opening_credit' => round($openingCredit, 2),
                'progressive_debit' => round($openingDebit + $debit, 2),
                'progressive_credit' => round($openingCredit + $credit, 2),
                'accounting_opening' => round($openingDebit - $openingCredit, 2),
                'accounting_closing' => round(($openingDebit + $debit) - ($openingCredit + $credit), 2)],
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

    private function matchHistoricalLinesFromDatevRows(bool $repairAssigned = false): int
    {
        $updated = 0;
        foreach ([
            ['suppliers', 'supplier_id', 'TRADE_PAYABLES'],
            ['customers', 'customer_id', 'TRADE_RECEIVABLES'],
        ] as [$table, $idColumn, $mapping]) {
            $targetStatement = $this->db->prepare(
                "SELECT DISTINCT l.journal_entry_id
                 FROM journal_entry_lines l
                 JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
                   AND m.mapping_key = ? AND m.account_id = l.account_id
                 WHERE l.organization_id = ? " . ($repairAssigned ? '' : "AND l.{$idColumn} IS NULL ") . "AND e.status = 'POSTED'
                   AND e.entry_type IN ('DATEV_IMPORT','OPENING','CLOSING')"
            );
            $targetStatement->execute([$mapping, $this->organizationId]);
            $entryIds = array_values(array_unique(array_map('intval', $targetStatement->fetchAll(PDO::FETCH_COLUMN))));
            if ($entryIds === []) {
                continue;
            }
            $linesByEntry = [];
            foreach (array_chunk($entryIds, 500) as $chunk) {
                $placeholders = implode(',', array_fill(0, count($chunk), '?'));
                $lineStatement = $this->db->prepare(
                    "SELECT l.id AS line_id, l.journal_entry_id AS entry_id, l.line_number, a.code AS account_code,
                            CASE WHEN m.account_id IS NULL THEN 0 ELSE 1 END AS is_target, l.{$idColumn} AS party_id
                     FROM journal_entry_lines l
                     JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = l.organization_id
                     LEFT JOIN accounting_account_mappings m ON m.organization_id = l.organization_id
                       AND m.mapping_key = ? AND m.account_id = l.account_id
                     WHERE l.organization_id = ? AND l.journal_entry_id IN ({$placeholders})
                     ORDER BY l.journal_entry_id, l.line_number, l.id"
                );
                $lineStatement->execute(array_merge([$mapping, $this->organizationId], $chunk));
                foreach ($lineStatement->fetchAll() as $line) {
                    $linesByEntry[(int) $line['entry_id']][] = $line;
                }
            }
            $targetAccountCodes = [];
            foreach ($linesByEntry as $entryLines) {
                foreach ($entryLines as $entryLine) {
                    if ((int) $entryLine['is_target']) {
                        $targetAccountCodes[trim((string) $entryLine['account_code'])] = true;
                    }
                }
            }
            $sourceStatement = $this->db->prepare(
                "SELECT r.batch_id, r.imported_entity_id AS entry_id, r.normalized_data_json
                 FROM import_rows r
                 JOIN import_batches b ON b.id = r.batch_id AND b.organization_id = r.organization_id
                   AND b.import_type = 'journal_entries'
                 WHERE r.organization_id = ? AND r.status = 'IMPORTED'
                   AND r.imported_entity_id IS NOT NULL
                 ORDER BY r.imported_entity_id, r.source_file_id, r.source_row_number, r.id"
            );
            $sourceStatement->execute([$this->organizationId]);
            $sourcesByEntry = [];
            foreach ($sourceStatement->fetchAll() as $source) {
                $entryId = (int) $source['entry_id'];
                if (isset($linesByEntry[$entryId])) {
                    $sourcesByEntry[$entryId][] = $source;
                }
            }
            $sourcePartyNames = [];
            foreach ($linesByEntry as $entryId => $entryLines) {
                $entrySources = $sourcesByEntry[$entryId] ?? [];
                if (count($entryLines) !== count($entrySources)) {
                    continue;
                }
                foreach ($entryLines as $index => $entryLine) {
                    if (!(int) $entryLine['is_target'] || (!$repairAssigned && !empty($entryLine['party_id']))) {
                        continue;
                    }
                    try {
                        $sourceData = json_decode((string) $entrySources[$index]['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
                    } catch (\JsonException) {
                        continue;
                    }
                    $sourceAccount = trim((string) ($sourceData['account_code'] ?? $sourceData['codice_conto'] ?? $sourceData['conto'] ?? ''));
                    $sourceName = trim((string) ($sourceData['account_name'] ?? $sourceData['nome_conto'] ?? $sourceData['descrizione_conto'] ?? ''));
                    if ($sourceAccount !== '' && isset($targetAccountCodes[$sourceAccount]) && $sourceName !== '') {
                        $sourcePartyNames[$this->canonical($sourceName)] = [
                            'name' => $sourceName,
                            'batch_id' => (int) $entrySources[$index]['batch_id'],
                        ];
                    }
                }
            }
            $parties = $this->ensureDatevParties($table, $sourcePartyNames);
            $otherColumn = $idColumn === 'supplier_id' ? 'customer_id' : 'supplier_id';
            $save = $this->db->prepare(
                "UPDATE journal_entry_lines SET {$idColumn} = ?, {$otherColumn} = NULL
                 WHERE id = ? AND organization_id = ? " . ($repairAssigned ? '' : "AND {$idColumn} IS NULL")
            );
            $clearAmbiguous = $repairAssigned
                ? $this->db->prepare("UPDATE journal_entry_lines SET {$idColumn} = NULL, {$otherColumn} = NULL WHERE id = ? AND organization_id = ?")
                : null;
            foreach ($linesByEntry as $entryId => $lines) {
                $sources = $sourcesByEntry[$entryId] ?? [];
                // Una riga sorgente genera esattamente una riga contabile. Se
                // l'archivio e incompleto non tentiamo associazioni posizionali.
                if (count($lines) !== count($sources)) {
                    continue;
                }
                foreach ($lines as $index => $line) {
                    if (!(int) $line['is_target']) {
                        continue;
                    }
                    $source = $sources[$index];
                    try {
                        $row = json_decode((string) $source['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
                    } catch (\JsonException) {
                        continue;
                    }
                    $sourceAccount = trim((string) ($row['account_code'] ?? $row['codice_conto'] ?? $row['conto'] ?? ''));
                    if ($sourceAccount === '' || $sourceAccount !== trim((string) $line['account_code'])) {
                        continue;
                    }
                    $sourceName = trim((string) ($row['account_name'] ?? $row['nome_conto'] ?? $row['descrizione_conto'] ?? ''));
                    $ids = $this->partyCandidatesForSourceName($sourceName, $parties);
                    if (count($ids) === 1) {
                        $save->execute([$ids[0], $line['line_id'], $this->organizationId]);
                        $updated += $save->rowCount();
                    } elseif ($clearAmbiguous !== null) {
                        // Un'associazione precedente non dimostrabile non deve
                        // restare nascosta su un soggetto probabilmente errato.
                        $clearAmbiguous->execute([$line['line_id'], $this->organizationId]);
                    }
                }
            }
        }
        return $updated;
    }

    private function datevPartyRepairRequired(): bool
    {
        $statement = $this->db->prepare('SELECT datev_party_reconciliation_version FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        return (int) $statement->fetchColumn() < 1;
    }

    private function markDatevPartyRepairCompleted(): void
    {
        $this->db->prepare(
            'INSERT INTO accounting_settings (organization_id, datev_party_reconciliation_version)
             VALUES (?, 1) ON DUPLICATE KEY UPDATE datev_party_reconciliation_version = 1, updated_at = NOW()'
        )->execute([$this->organizationId]);
    }

    /** @param array<string,array{name:string,batch_id:int}> $sourcePartyNames */
    private function ensureDatevParties(string $table, array $sourcePartyNames): array
    {
        $parties = $this->partyMatchIndex($table);
        uasort($sourcePartyNames, static fn (array $left, array $right): int => mb_strlen($right['name']) <=> mb_strlen($left['name']));
        $prefix = $table === 'suppliers' ? 'FOR-DATEV-' : 'CLI-DATEV-';
        $insert = $this->db->prepare(
            "INSERT IGNORE INTO {$table}
             (organization_id, code, business_name, country_code, active, source_import_batch_id, created_at, updated_at)
             VALUES (?, ?, ?, 'IT', 1, ?, NOW(), NOW())"
        );
        foreach ($sourcePartyNames as $canonical => $source) {
            if (!$this->isUsableDatevPartyName($canonical)) {
                continue;
            }
            $candidates = $this->partyCandidatesForSourceName($source['name'], $parties);
            if ($candidates !== []) {
                continue;
            }
            $code = $prefix . strtoupper(substr(hash('sha256', $canonical), 0, 12));
            $name = mb_substr(trim($source['name']), 0, 190);
            $insert->execute([$this->organizationId, $code, $name, $source['batch_id'] ?: null]);
            if ($insert->rowCount() !== 1) {
                continue;
            }
            $party = [
                'id' => (int) $this->db->lastInsertId(),
                'business_name' => $name,
                'vat_number' => null,
                'tax_code' => null,
                'canonical_name' => $canonical,
                'vat_digits' => '',
                'tax_key' => '',
            ];
            $parties['names'][$canonical][] = $party['id'];
            $parties['parties'][] = $party;
        }
        return $parties;
    }

    /** @param array{names:array<string,array<int,int>>,parties:array<int,array<string,mixed>>} $parties @return array<int,int> */
    private function partyCandidatesForSourceName(string $sourceName, array $parties): array
    {
        $canonical = $this->canonical($sourceName);
        $exact = array_values(array_unique($parties['names'][$canonical] ?? []));
        if ($exact !== []) {
            return $exact;
        }
        if (strlen($canonical) < 8) {
            return [];
        }
        $matches = [];
        foreach ($parties['parties'] as $party) {
            $partyName = (string) ($party['canonical_name'] ?? '');
            if ($partyName !== '' && (str_starts_with($partyName, $canonical) || str_starts_with($canonical, $partyName))) {
                $matches[] = (int) $party['id'];
            }
        }
        return array_values(array_unique($matches));
    }

    private function isUsableDatevPartyName(string $canonical): bool
    {
        if (strlen($canonical) < 3) {
            return false;
        }
        foreach (['FORNITORI BENI E SERVIZI', 'DEBITI VERSO FORNITORI', 'CREDITI VERSO CLIENTI', 'CLIENTI ITALIA'] as $generic) {
            if (str_contains($canonical, $generic)) {
                return false;
            }
        }
        return true;
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
