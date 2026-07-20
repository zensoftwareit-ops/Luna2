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
        return $this->saveManual($header, $lines, null, true);
    }

    public function saveManual(array $header, array $lines, ?int $entryId = null, bool $post = false): int
    {
        $entryDate = trim((string) ($header['entry_date'] ?? ''));
        $competenceDate = trim((string) ($header['competence_date'] ?? $entryDate));
        $description = trim((string) ($header['description'] ?? ''));
        if (!$this->isIsoDate($entryDate) || !$this->isIsoDate($competenceDate)) {
            throw new InvalidArgumentException('Data registrazione o competenza non valida.');
        }
        if ($description === '') {
            throw new InvalidArgumentException('La descrizione della registrazione è obbligatoria.');
        }
        $normalized = $this->normalizeLines($lines, $post ? 2 : 1);
        $this->assertAccountsBelongToOrganization($normalized);
        if ($post) {
            $this->assertBalanced($normalized);
        }

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $totalDebit = array_sum(array_column($normalized, 'debit'));
            $totalCredit = array_sum(array_column($normalized, 'credit'));
            $status = $post ? 'POSTED' : 'DRAFT';
            if ($entryId !== null) {
                $lock = $this->db->prepare(
                    "SELECT id FROM journal_entries
                     WHERE id = ? AND organization_id = ? AND status = 'DRAFT' AND source_type = 'MANUAL' FOR UPDATE"
                );
                $lock->execute([$entryId, $this->organizationId]);
                if (!$lock->fetchColumn()) {
                    throw new InvalidArgumentException('È possibile modificare soltanto una bozza manuale.');
                }
                $statement = $this->db->prepare(
                    "UPDATE journal_entries SET entry_date = ?, competence_date = ?, entry_type = ?, status = ?,
                     description = ?, document_number = ?, counterparty = ?, total_debit = ?, total_credit = ?,
                     notes = ?, updated_by = ?, posted_at = IF(? = 'POSTED', NOW(), NULL), updated_at = NOW()
                     WHERE id = ? AND organization_id = ?"
                );
                $statement->execute([
                    $entryDate, $competenceDate,
                    $header['entry_type'] ?? 'MANUAL', $status,
                    $description,
                    ($header['document_number'] ?? null) ?: null, ($header['counterparty'] ?? null) ?: null,
                    $totalDebit, $totalCredit, ($header['notes'] ?? null) ?: null, $this->userId, $status,
                    $entryId, $this->organizationId,
                ]);
                $this->db->prepare('DELETE FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?')
                    ->execute([$entryId, $this->organizationId]);
            } else {
                $protocol = $this->nextProtocol($entryDate);
                $statement = $this->db->prepare(
                    "INSERT INTO journal_entries
                     (organization_id, protocol_number, entry_date, competence_date, entry_type, status, description,
                      document_number, source_type, source_id, counterparty, total_debit, total_credit, notes,
                      created_by, updated_by, posted_at, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'MANUAL', NULL, ?, ?, ?, ?, ?, ?, IF(? = 'POSTED', NOW(), NULL), NOW(), NOW())"
                );
                $statement->execute([
                    $this->organizationId, $protocol, $entryDate, $competenceDate,
                    $header['entry_type'] ?? 'MANUAL', $status,
                    $description,
                    ($header['document_number'] ?? null) ?: null, ($header['counterparty'] ?? null) ?: null,
                    $totalDebit, $totalCredit, ($header['notes'] ?? null) ?: null, $this->userId, $this->userId, $status,
                ]);
                $entryId = (int) $this->db->lastInsertId();
            }
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

    public function postDraft(int $entryId): void
    {
        $statement = $this->db->prepare(
            "SELECT l.account_id, l.debit, l.credit, l.description, l.cost_center_id, l.customer_id, l.supplier_id
             FROM journal_entries e JOIN journal_entry_lines l ON l.journal_entry_id = e.id
             WHERE e.id = ? AND e.organization_id = ? AND e.status = 'DRAFT' AND e.source_type = 'MANUAL'
             ORDER BY l.line_number"
        );
        $statement->execute([$entryId, $this->organizationId]);
        $lines = $statement->fetchAll();
        $this->assertBalanced($lines);
        $update = $this->db->prepare(
            "UPDATE journal_entries SET status = 'POSTED', posted_at = NOW(), updated_by = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ? AND status = 'DRAFT' AND source_type = 'MANUAL'"
        );
        $update->execute([$this->userId, $entryId, $this->organizationId]);
        if ($update->rowCount() === 0) {
            throw new InvalidArgumentException('Bozza non trovata.');
        }
    }

    public function deleteDraft(int $entryId): bool
    {
        $statement = $this->db->prepare(
            "DELETE FROM journal_entries
             WHERE id = ? AND organization_id = ? AND status = 'DRAFT' AND source_type = 'MANUAL'"
        );
        $statement->execute([$entryId, $this->organizationId]);
        return $statement->rowCount() > 0;
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
                (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument($documentId);
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
            (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument($documentId);
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

    private function normalizeLines(array $lines, int $minimumLines = 2): array
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
        if (count($normalized) < $minimumLines) {
            throw new InvalidArgumentException($minimumLines > 1
                ? 'La registrazione richiede almeno due righe valide.'
                : 'La bozza richiede almeno una riga valida.');
        }
        return $normalized;
    }

    private function assertAccountsBelongToOrganization(array $lines): void
    {
        $ids = array_values(array_unique(array_map(static fn (array $line): int => (int) $line['account_id'], $lines)));
        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        $statement = $this->db->prepare(
            "SELECT COUNT(*) FROM chart_of_accounts
             WHERE organization_id = ? AND active = 1 AND is_postable = 1 AND id IN ({$placeholders})"
        );
        $statement->execute(array_merge([$this->organizationId], $ids));
        if ((int) $statement->fetchColumn() !== count($ids)) {
            throw new InvalidArgumentException('Uno o più conti non appartengono all’azienda o non sono movimentabili.');
        }
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

    private function isIsoDate(string $value): bool
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
        $errors = DateTimeImmutable::getLastErrors();
        return $date !== false && ($errors === false || ($errors['warning_count'] === 0 && $errors['error_count'] === 0));
    }
}
