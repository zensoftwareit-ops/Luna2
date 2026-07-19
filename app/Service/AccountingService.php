<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class AccountingService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId)
    {
    }

    public function postManual(array $header, array $lines): int
    {
        $normalized = $this->normalizeLines($lines);
        $this->assertBalanced($normalized);

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $protocol = $this->nextProtocol((string) ($header['entry_date'] ?? date('Y-m-d')));
            $statement = $this->db->prepare(
                "INSERT INTO journal_entries
                 (organization_id, protocol_number, entry_date, competence_date, entry_type, status, description,
                  document_number, source_type, source_id, counterparty, total_debit, total_credit, notes,
                  created_by, updated_by, posted_at, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, 'POSTED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())"
            );
            $totalDebit = array_sum(array_column($normalized, 'debit'));
            $totalCredit = array_sum(array_column($normalized, 'credit'));
            $statement->execute([
                $this->organizationId,
                $protocol,
                $header['entry_date'] ?? date('Y-m-d'),
                $header['competence_date'] ?? ($header['entry_date'] ?? date('Y-m-d')),
                $header['entry_type'] ?? 'MANUAL',
                trim((string) ($header['description'] ?? 'Registrazione manuale')),
                $header['document_number'] ?: null,
                $header['source_type'] ?: 'MANUAL',
                $header['source_id'] ?: null,
                $header['counterparty'] ?: null,
                $totalDebit,
                $totalCredit,
                $header['notes'] ?: null,
                $this->userId,
                $this->userId,
            ]);
            $entryId = (int) $this->db->lastInsertId();
            $this->insertLines($entryId, $normalized);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $entryId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function postDocument(int $documentId): ?int
    {
        $statement = $this->db->prepare('SELECT * FROM documents WHERE id = ? AND organization_id = ? FOR UPDATE');
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement->execute([$documentId, $this->organizationId]);
            $document = $statement->fetch();
            if (!$document) {
                throw new InvalidArgumentException('Documento non trovato.');
            }
            if (!in_array($document['document_type'], ['SALES_INVOICE', 'CREDIT_NOTE', 'PURCHASE_INVOICE'], true)) {
                if ($ownsTransaction) {
                    $this->db->rollBack();
                }
                return null;
            }

            $existing = $this->db->prepare("SELECT id FROM journal_entries WHERE organization_id = ? AND source_type = 'DOCUMENT' AND source_id = ? LIMIT 1");
            $existing->execute([$this->organizationId, $documentId]);
            if ($id = $existing->fetchColumn()) {
                if ($ownsTransaction) {
                    $this->db->commit();
                }
                return (int) $id;
            }

            $accounts = $this->systemAccounts();
            $date = (string) $document['document_date'];
            $protocol = $this->nextProtocol($date);
            $lines = [];
            $total = (float) $document['total'];
            $net = (float) $document['taxable_total'];
            $vat = (float) $document['vat_total'];

            if ($document['document_type'] === 'SALES_INVOICE') {
                $lines = [
                    ['account_id' => $accounts['TRADE_RECEIVABLES'], 'debit' => $total, 'credit' => 0.0, 'description' => 'Credito verso ' . $document['counterparty_name']],
                    ['account_id' => $accounts['SALES_REVENUE'], 'debit' => 0.0, 'credit' => $net, 'description' => 'Ricavi documento ' . $document['number']],
                ];
                if ($vat > 0) {
                    $lines[] = ['account_id' => $accounts['VAT_PAYABLE'], 'debit' => 0.0, 'credit' => $vat, 'description' => 'IVA a debito'];
                }
            } elseif ($document['document_type'] === 'CREDIT_NOTE') {
                $lines = [
                    ['account_id' => $accounts['SALES_REVENUE'], 'debit' => $net, 'credit' => 0.0, 'description' => 'Storno ricavi ' . $document['number']],
                    ['account_id' => $accounts['TRADE_RECEIVABLES'], 'debit' => 0.0, 'credit' => $total, 'description' => 'Storno credito verso ' . $document['counterparty_name']],
                ];
                if ($vat > 0) {
                    $lines[] = ['account_id' => $accounts['VAT_PAYABLE'], 'debit' => $vat, 'credit' => 0.0, 'description' => 'Storno IVA a debito'];
                }
            } else {
                $lines = [
                    ['account_id' => $accounts['PURCHASE_COSTS'], 'debit' => $net, 'credit' => 0.0, 'description' => 'Costo documento ' . $document['number']],
                    ['account_id' => $accounts['TRADE_PAYABLES'], 'debit' => 0.0, 'credit' => $total, 'description' => 'Debito verso ' . $document['counterparty_name']],
                ];
                if ($vat > 0) {
                    $lines[] = ['account_id' => $accounts['VAT_RECEIVABLE'], 'debit' => $vat, 'credit' => 0.0, 'description' => 'IVA a credito'];
                }
            }
            $lines = $this->normalizeLines($lines);
            $this->assertBalanced($lines);

            $insert = $this->db->prepare(
                "INSERT INTO journal_entries
                 (organization_id, protocol_number, entry_date, competence_date, entry_type, status, description,
                  document_number, source_type, source_id, counterparty, total_debit, total_credit,
                  created_by, updated_by, posted_at, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, 'POSTED', ?, ?, 'DOCUMENT', ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())"
            );
            $totalDebit = array_sum(array_column($lines, 'debit'));
            $totalCredit = array_sum(array_column($lines, 'credit'));
            $insert->execute([
                $this->organizationId, $protocol, $date, $date, $document['document_type'],
                $document['counterparty_name'] . ' - ' . $document['number'], $document['number'], $documentId,
                $document['counterparty_name'], $totalDebit, $totalCredit, $this->userId, $this->userId,
            ]);
            $entryId = (int) $this->db->lastInsertId();
            $this->insertLines($entryId, $lines);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $entryId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function normalizeLines(array $lines): array
    {
        $normalized = [];
        foreach ($lines as $line) {
            $accountId = (int) ($line['account_id'] ?? 0);
            $debit = $this->decimal($line['debit'] ?? 0);
            $credit = $this->decimal($line['credit'] ?? 0);
            if ($accountId <= 0 || ($debit <= 0 && $credit <= 0)) {
                continue;
            }
            if ($debit > 0 && $credit > 0) {
                throw new InvalidArgumentException('Una riga contabile non può avere contemporaneamente Dare e Avere.');
            }
            $normalized[] = [
                'account_id' => $accountId,
                'debit' => round($debit, 2),
                'credit' => round($credit, 2),
                'description' => trim((string) ($line['description'] ?? '')),
                'cost_center_id' => !empty($line['cost_center_id']) ? (int) $line['cost_center_id'] : null,
                'customer_id' => !empty($line['customer_id']) ? (int) $line['customer_id'] : null,
                'supplier_id' => !empty($line['supplier_id']) ? (int) $line['supplier_id'] : null,
            ];
        }
        if (count($normalized) < 2) {
            throw new InvalidArgumentException('La registrazione richiede almeno due righe valide.');
        }
        return $normalized;
    }

    private function assertBalanced(array $lines): void
    {
        $debit = round(array_sum(array_column($lines, 'debit')), 2);
        $credit = round(array_sum(array_column($lines, 'credit')), 2);
        if (abs($debit - $credit) > 0.005 || $debit <= 0) {
            throw new InvalidArgumentException(sprintf('Registrazione non quadrata: Dare %.2f, Avere %.2f.', $debit, $credit));
        }
    }

    private function insertLines(int $entryId, array $lines): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO journal_entry_lines
             (organization_id, journal_entry_id, line_number, account_id, debit, credit, description, cost_center_id, customer_id, supplier_id, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())'
        );
        foreach ($lines as $index => $line) {
            $statement->execute([
                $this->organizationId, $entryId, $index + 1, $line['account_id'], $line['debit'], $line['credit'],
                $line['description'] ?: null, $line['cost_center_id'], $line['customer_id'], $line['supplier_id'],
            ]);
        }
    }

    private function systemAccounts(): array
    {
        $keys = ['TRADE_RECEIVABLES', 'TRADE_PAYABLES', 'SALES_REVENUE', 'PURCHASE_COSTS', 'VAT_PAYABLE', 'VAT_RECEIVABLE'];
        $placeholders = implode(',', array_fill(0, count($keys), '?'));
        $statement = $this->db->prepare("SELECT id, system_key FROM chart_of_accounts WHERE organization_id = ? AND system_key IN ({$placeholders})");
        $statement->execute(array_merge([$this->organizationId], $keys));
        $accounts = [];
        foreach ($statement->fetchAll() as $row) {
            $accounts[$row['system_key']] = (int) $row['id'];
        }
        $missing = array_diff($keys, array_keys($accounts));
        if ($missing !== []) {
            throw new RuntimeException('Piano dei conti incompleto: mancano ' . implode(', ', $missing) . '.');
        }
        return $accounts;
    }

    private function nextProtocol(string $date): string
    {
        $year = (new DateTimeImmutable($date))->format('Y');
        $key = 'JOURNAL-' . $year;
        $statement = $this->db->prepare('SELECT id, next_value FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE');
        $statement->execute([$this->organizationId, $key]);
        $sequence = $statement->fetch();
        if (!$sequence) {
            $this->db->prepare('INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at) VALUES (?, ?, ?, 2, 6, NOW(), NOW())')
                ->execute([$this->organizationId, $key, 'GEN-' . $year . '-']);
            return 'GEN-' . $year . '-000001';
        }
        $value = (int) $sequence['next_value'];
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + 1, updated_at = NOW() WHERE id = ?')->execute([$sequence['id']]);
        return 'GEN-' . $year . '-' . str_pad((string) $value, 6, '0', STR_PAD_LEFT);
    }

    private function decimal(mixed $value): float
    {
        $string = trim((string) $value);
        if (str_contains($string, ',')) {
            $string = str_replace('.', '', $string);
            $string = str_replace(',', '.', $string);
        }
        return (float) $string;
    }
}
