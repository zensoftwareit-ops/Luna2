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

    public function postAutomated(array $header, array $lines, string $sourceType, int $sourceId): int
    {
        $entryDate = trim((string) ($header['entry_date'] ?? ''));
        $competenceDate = trim((string) ($header['competence_date'] ?? $entryDate));
        $description = trim((string) ($header['description'] ?? ''));
        if (!$this->isIsoDate($entryDate) || !$this->isIsoDate($competenceDate) || $description === '') {
            throw new InvalidArgumentException('Dati della registrazione automatica non validi.');
        }
        if (!preg_match('/^[A-Z0-9_:-]{2,50}$/', $sourceType) || $sourceId <= 0) {
            throw new InvalidArgumentException('Origine della registrazione automatica non valida.');
        }
        $normalized = $this->normalizeLines($lines, 2);
        $this->assertAccountsBelongToOrganization($normalized);
        $this->assertAnalyticParties($normalized);
        $this->assertHeaderReferences($header);
        $this->assertBalanced($normalized);
        $this->assertAccountingPeriodOpen($entryDate);

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $existing = $this->db->prepare(
                'SELECT id FROM journal_entries WHERE organization_id = ? AND source_type = ? AND source_id = ? FOR UPDATE'
            );
            $existing->execute([$this->organizationId, $sourceType, $sourceId]);
            if ($entryId = $existing->fetchColumn()) {
                if ($ownsTransaction) {
                    $this->db->commit();
                }
                return (int) $entryId;
            }

            $protocol = $this->nextProtocol($entryDate);
            $totalDebit = array_sum(array_column($normalized, 'debit'));
            $totalCredit = array_sum(array_column($normalized, 'credit'));
            $statement = $this->db->prepare(
                "INSERT INTO journal_entries
                 (organization_id, protocol_number, entry_date, competence_date, entry_type, cause_id, vat_register_id,
                  status, description, document_number, source_protocol, source_type, source_id, counterparty,
                  total_debit, total_credit, notes, created_by, updated_by, posted_at, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, 'POSTED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())"
            );
            $statement->execute([
                $this->organizationId, $protocol, $entryDate, $competenceDate,
                $header['entry_type'] ?? 'AUTOMATIC', !empty($header['cause_id']) ? (int) $header['cause_id'] : null,
                !empty($header['vat_register_id']) ? (int) $header['vat_register_id'] : null, $description,
                ($header['document_number'] ?? null) ?: null, ($header['source_protocol'] ?? null) ?: null,
                $sourceType, $sourceId, ($header['counterparty'] ?? null) ?: null, $totalDebit, $totalCredit,
                ($header['notes'] ?? null) ?: null, $this->userId, $this->userId,
            ]);
            $entryId = (int) $this->db->lastInsertId();
            $this->insertLines($entryId, $normalized);
            $this->invalidateJournalPrints(null, $entryDate);
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
        $this->assertAccountingPeriodOpen($entryDate);
        $normalized = $this->normalizeLines($lines, $post ? 2 : 1);
        $this->assertAccountsBelongToOrganization($normalized);
        $this->assertAnalyticParties($normalized);
        $this->assertHeaderReferences($header);
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
            $previousEntryDate = null;
            if ($entryId !== null) {
                $lock = $this->db->prepare(
                    "SELECT * FROM journal_entries
                     WHERE id = ? AND organization_id = ? AND status IN ('DRAFT','POSTED')
                       AND source_type IN ('MANUAL','DOCUMENT') AND is_finalized = 0 FOR UPDATE"
                );
                $lock->execute([$entryId, $this->organizationId]);
                $current = $lock->fetch();
                if (!$current) {
                    throw new InvalidArgumentException('La scrittura è definitiva, stornata o non può essere modificata.');
                }
                $previousEntryDate = (string) $current['entry_date'];
                $this->assertAccountingPeriodOpen((string) $current['entry_date']);
                if ((string) $current['source_type'] === 'DOCUMENT' && !empty($current['source_id'])) {
                    $documentDateStatement = $this->db->prepare('SELECT document_date FROM documents WHERE id = ? AND organization_id = ?');
                    $documentDateStatement->execute([(int) $current['source_id'], $this->organizationId]);
                    $documentDate = (string) $documentDateStatement->fetchColumn();
                    if ($documentDate === '' || $entryDate < $documentDate) {
                        throw new InvalidArgumentException('La data di registrazione non può precedere la data del documento.');
                    }
                }
                if ($current['status'] === 'POSTED') {
                    $reason = trim((string) ($header['change_reason'] ?? ''));
                    if ($reason === '') $reason = 'Modifica della scrittura contabile provvisoria';
                    $this->saveRevision($current, $reason);
                    $status = 'POSTED';
                }
                $protocol = substr((string) $current['entry_date'], 0, 4) === substr($entryDate, 0, 4)
                    ? (string) $current['protocol_number'] : $this->nextProtocol($entryDate);
                $statement = $this->db->prepare(
                    "UPDATE journal_entries SET protocol_number = ?, entry_date = ?, competence_date = ?, entry_type = ?, cause_id = ?, vat_register_id = ?, status = ?,
                     description = ?, document_number = ?, source_protocol = ?, counterparty = ?, total_debit = ?, total_credit = ?,
                     notes = ?, updated_by = ?, revision_number = revision_number + 1,
                     posted_at = IF(? = 'POSTED', COALESCE(posted_at, NOW()), NULL), updated_at = NOW()
                     WHERE id = ? AND organization_id = ?"
                );
                $statement->execute([
                    $protocol, $entryDate, $competenceDate, $header['entry_type'] ?? 'MANUAL',
                    !empty($header['cause_id']) ? (int) $header['cause_id'] : null,
                    !empty($header['vat_register_id']) ? (int) $header['vat_register_id'] : null, $status,
                    $description,
                    ($header['document_number'] ?? null) ?: null, ($header['source_protocol'] ?? null) ?: null,
                    ($header['counterparty'] ?? null) ?: null,
                    $totalDebit, $totalCredit, ($header['notes'] ?? null) ?: null, $this->userId, $status,
                    $entryId, $this->organizationId,
                ]);
                $this->db->prepare('DELETE FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?')
                    ->execute([$entryId, $this->organizationId]);
                if ((string) $current['source_type'] === 'DOCUMENT' && !empty($current['source_id']) && $entryDate !== $previousEntryDate) {
                    $this->db->prepare('UPDATE documents SET registration_date = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                        ->execute([$entryDate, (int) $current['source_id'], $this->organizationId]);
                }
            } else {
                $protocol = $this->nextProtocol($entryDate);
                $statement = $this->db->prepare(
                    "INSERT INTO journal_entries
                     (organization_id, protocol_number, entry_date, competence_date, entry_type, cause_id, vat_register_id,
                      status, description, document_number, source_protocol, source_type, source_id, counterparty, total_debit, total_credit, notes,
                      created_by, updated_by, posted_at, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'MANUAL', NULL, ?, ?, ?, ?, ?, ?, IF(? = 'POSTED', NOW(), NULL), NOW(), NOW())"
                );
                $statement->execute([
                    $this->organizationId, $protocol, $entryDate, $competenceDate, $header['entry_type'] ?? 'MANUAL',
                    !empty($header['cause_id']) ? (int) $header['cause_id'] : null,
                    !empty($header['vat_register_id']) ? (int) $header['vat_register_id'] : null,
                    $status, $description, ($header['document_number'] ?? null) ?: null,
                    ($header['source_protocol'] ?? null) ?: null, ($header['counterparty'] ?? null) ?: null,
                    $totalDebit, $totalCredit, ($header['notes'] ?? null) ?: null, $this->userId, $this->userId, $status,
                ]);
                $entryId = (int) $this->db->lastInsertId();
            }
            $this->insertLines($entryId, $normalized);
            if (isset($current) && (string) $current['source_type'] === 'DOCUMENT' && !empty($current['source_id']) && $entryDate !== $previousEntryDate) {
                (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument((int) $current['source_id']);
            }
            if ($status === 'POSTED') {
                $this->invalidateJournalPrints($previousEntryDate, $entryDate);
            }
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
        $header = $this->db->prepare(
            "SELECT entry_date FROM journal_entries
             WHERE id = ? AND organization_id = ? AND status = 'DRAFT' AND source_type = 'MANUAL'"
        );
        $header->execute([$entryId, $this->organizationId]);
        $entryDate = $header->fetchColumn();
        if (!$entryDate) {
            throw new InvalidArgumentException('Bozza non trovata.');
        }
        $this->assertAccountingPeriodOpen((string) $entryDate);
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
        $this->invalidateJournalPrints(null, (string) $entryDate);
    }

    /**
     * Sostituisce l'intera partita doppia di una scrittura ancora provvisoria.
     * Gli identificativi delle righe esistenti servono esclusivamente a conservare
     * gli eventuali collegamenti analitici (cliente, fornitore e centro di costo).
     */
    public function replaceProvisionalLines(int $entryId, array $lines, string $reason = ''): array
    {
        $normalized = $this->normalizeLines($lines, 2);
        $this->assertAccountsBelongToOrganization($normalized);
        $this->assertAnalyticParties($normalized);
        $this->assertBalanced($normalized);

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $entryStatement = $this->db->prepare(
                "SELECT * FROM journal_entries
                 WHERE id = ? AND organization_id = ? AND status IN ('DRAFT','POSTED')
                   AND is_finalized = 0 FOR UPDATE"
            );
            $entryStatement->execute([$entryId, $this->organizationId]);
            $entry = $entryStatement->fetch();
            if (!$entry) {
                throw new InvalidArgumentException('La scrittura è definitiva, stornata o non modificabile.');
            }
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);

            $existingStatement = $this->db->prepare(
                'SELECT id, cost_center_id, customer_id, supplier_id
                 FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ? FOR UPDATE'
            );
            $existingStatement->execute([$entryId, $this->organizationId]);
            $existing = [];
            foreach ($existingStatement->fetchAll() as $existingLine) {
                $existing[(int) $existingLine['id']] = $existingLine;
            }

            $seen = [];
            foreach ($normalized as &$line) {
                $lineId = (int) ($line['line_id'] ?? 0);
                if ($lineId <= 0) continue;
                if (!isset($existing[$lineId]) || isset($seen[$lineId])) {
                    throw new InvalidArgumentException('Una delle righe indicate non appartiene alla registrazione o è duplicata.');
                }
                $seen[$lineId] = true;
                $line['cost_center_id'] = $existing[$lineId]['cost_center_id'] ?: null;
                if (empty($line['customer_id']) && empty($line['supplier_id'])) {
                    $line['customer_id'] = $existing[$lineId]['customer_id'] ?: null;
                    $line['supplier_id'] = $existing[$lineId]['supplier_id'] ?: null;
                }
            }
            unset($line);

            $revisionReason = trim($reason);
            if ($revisionReason === '') $revisionReason = 'Modifica completa delle righe contabili provvisorie';
            $this->saveRevision($entry, mb_substr($revisionReason, 0, 500));

            $this->db->prepare('DELETE FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?')
                ->execute([$entryId, $this->organizationId]);
            $this->insertLines($entryId, $normalized);

            $totalDebit = round(array_sum(array_column($normalized, 'debit')), 2);
            $totalCredit = round(array_sum(array_column($normalized, 'credit')), 2);
            $this->db->prepare(
                'UPDATE journal_entries SET total_debit = ?, total_credit = ?, revision_number = revision_number + 1,
                        updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([$totalDebit, $totalCredit, $this->userId, $entryId, $this->organizationId]);
            $this->invalidateJournalPrints(null, (string) $entry['entry_date']);

            if ($ownsTransaction) $this->db->commit();
            return [
                'entry_id' => $entryId,
                'rows_before' => count($existing),
                'rows_after' => count($normalized),
                'total_debit' => $totalDebit,
                'total_credit' => $totalCredit,
            ];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
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

    /** @return array{old_account_id:int,new_account_id:int,old_account_code:string,new_account_code:string} */
    public function changeLineAccount(int $entryId, int $lineId, int $newAccountId, string $reason, string $description = ''): array
    {
        $reason = trim($reason);
        if ($entryId <= 0 || $lineId <= 0 || $newAccountId <= 0) {
            throw new InvalidArgumentException('Registrazione, riga o nuovo conto non validi.');
        }
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $entryStatement = $this->db->prepare(
                "SELECT * FROM journal_entries
                 WHERE id = ? AND organization_id = ? AND status IN ('DRAFT','POSTED') AND is_finalized = 0
                 FOR UPDATE"
            );
            $entryStatement->execute([$entryId, $this->organizationId]);
            $entry = $entryStatement->fetch();
            if (!$entry) {
                throw new InvalidArgumentException('La scrittura è definitiva, stornata o non modificabile.');
            }
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);

            $lineStatement = $this->db->prepare(
                'SELECT l.id, l.account_id, l.debit, l.credit, l.description, l.customer_id, l.supplier_id,
                        a.code, a.name, a.account_type
                 FROM journal_entry_lines l
                 JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = l.organization_id
                 WHERE l.id = ? AND l.journal_entry_id = ? AND l.organization_id = ? FOR UPDATE'
            );
            $lineStatement->execute([$lineId, $entryId, $this->organizationId]);
            $line = $lineStatement->fetch();
            if (!$line) {
                throw new InvalidArgumentException('Riga contabile non trovata.');
            }

            $accountStatement = $this->db->prepare(
                'SELECT id, code, name, account_type FROM chart_of_accounts
                 WHERE id = ? AND organization_id = ? AND active = 1 AND is_postable = 1'
            );
            $accountStatement->execute([$newAccountId, $this->organizationId]);
            $newAccount = $accountStatement->fetch();
            if (!$newAccount) {
                throw new InvalidArgumentException('Il nuovo conto non è attivo o movimentabile.');
            }
            $description = trim($description);
            if ((int) $line['account_id'] === $newAccountId && ($description === '' || $description === trim((string) $line['description']))) {
                throw new InvalidArgumentException('Conto e descrizione sono già quelli indicati.');
            }
            if (!in_array((string) $newAccount['account_type'], $this->compatibleAccountTypes($entry, $line), true)) {
                throw new InvalidArgumentException('Il nuovo conto non è compatibile con questa riga contabile.');
            }

            $revisionReason = sprintf('Cambio conto %s → %s', (string) $line['code'], (string) $newAccount['code']);
            if ($reason !== '') {
                $revisionReason .= '. ' . mb_substr($reason, 0, 350);
            }
            $this->saveRevision($entry, $revisionReason);
            $this->db->prepare(
                'UPDATE journal_entry_lines SET account_id = ?, description = ? WHERE id = ? AND journal_entry_id = ? AND organization_id = ?'
            )->execute([$newAccountId, $description !== '' ? mb_substr($description, 0, 500) : $line['description'], $lineId, $entryId, $this->organizationId]);
            $this->db->prepare(
                'UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([$this->userId, $entryId, $this->organizationId]);
            $this->invalidateJournalPrints(null, (string) $entry['entry_date']);

            if ($ownsTransaction) {
                $this->db->commit();
            }
            return [
                'old_account_id' => (int) $line['account_id'],
                'new_account_id' => $newAccountId,
                'old_account_code' => (string) $line['code'],
                'new_account_code' => (string) $newAccount['code'],
            ];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    /** @param array<int,array{account_id:mixed,amount:mixed,description?:mixed}> $allocations */
    public function splitLine(int $entryId, int $lineId, array $allocations, string $reason): array
    {
        if ($entryId <= 0 || $lineId <= 0 || count($allocations) < 2 || count($allocations) > 20) {
            throw new InvalidArgumentException('Inserisci almeno due ripartizioni valide.');
        }
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $entryStatement = $this->db->prepare("SELECT * FROM journal_entries WHERE id = ? AND organization_id = ? AND status IN ('DRAFT','POSTED') AND is_finalized = 0 FOR UPDATE");
            $entryStatement->execute([$entryId, $this->organizationId]);
            $entry = $entryStatement->fetch();
            if (!$entry) throw new InvalidArgumentException('La scrittura è definitiva, stornata o non modificabile.');
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);

            $lineStatement = $this->db->prepare(
                'SELECT l.*, a.code, a.name, a.account_type FROM journal_entry_lines l
                 JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = l.organization_id
                 WHERE l.id = ? AND l.journal_entry_id = ? AND l.organization_id = ? FOR UPDATE'
            );
            $lineStatement->execute([$lineId, $entryId, $this->organizationId]);
            $line = $lineStatement->fetch();
            if (!$line) throw new InvalidArgumentException('Riga contabile non trovata.');
            $side = (float) $line['debit'] > 0 ? 'debit' : 'credit';
            $originalAmount = round((float) $line[$side], 2);
            $allowedTypes = $this->compatibleAccountTypes($entry, $line);
            $normalized = [];
            $total = 0.0;
            foreach ($allocations as $allocation) {
                $accountId = (int) ($allocation['account_id'] ?? 0);
                $amount = round($this->decimal($allocation['amount'] ?? 0), 2);
                if ($accountId <= 0 || $amount <= 0) throw new InvalidArgumentException('Conto e importo sono obbligatori in ogni ripartizione.');
                $account = $this->db->prepare('SELECT id, code, name, account_type FROM chart_of_accounts WHERE id = ? AND organization_id = ? AND active = 1 AND is_postable = 1');
                $account->execute([$accountId, $this->organizationId]);
                $account = $account->fetch();
                if (!$account || !in_array((string) $account['account_type'], $allowedTypes, true)) {
                    throw new InvalidArgumentException('Uno dei conti scelti non è compatibile con la riga.');
                }
                $total += $amount;
                $normalized[] = ['account' => $account, 'amount' => $amount, 'description' => trim((string) ($allocation['description'] ?? ''))];
            }
            if (abs(round($total, 2) - $originalAmount) > 0.005) {
                throw new InvalidArgumentException(sprintf('La ripartizione deve totalizzare %s.', number_format($originalAmount, 2, ',', '.')));
            }

            $revisionReason = 'Ripartizione riga ' . (int) $line['line_number'] . ' su ' . count($normalized) . ' conti';
            if (trim($reason) !== '') $revisionReason .= '. ' . mb_substr(trim($reason), 0, 350);
            $this->saveRevision($entry, $revisionReason);
            $first = array_shift($normalized);
            $this->db->prepare("UPDATE journal_entry_lines SET account_id = ?, {$side} = ?, description = ? WHERE id = ? AND journal_entry_id = ? AND organization_id = ?")
                ->execute([(int) $first['account']['id'], $first['amount'], $first['description'] ?: $line['description'], $lineId, $entryId, $this->organizationId]);
            $max = $this->db->prepare('SELECT COALESCE(MAX(line_number),0) FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?');
            $max->execute([$entryId, $this->organizationId]);
            $lineNumber = (int) $max->fetchColumn();
            $insert = $this->db->prepare(
                'INSERT INTO journal_entry_lines (organization_id,journal_entry_id,line_number,account_id,debit,credit,description,cost_center_id,customer_id,supplier_id,created_at)
                 VALUES (?,?,?,?,?,?,?,?,?,?,NOW())'
            );
            foreach ($normalized as $allocation) {
                $lineNumber++;
                $insert->execute([$this->organizationId, $entryId, $lineNumber, (int) $allocation['account']['id'],
                    $side === 'debit' ? $allocation['amount'] : 0, $side === 'credit' ? $allocation['amount'] : 0,
                    $allocation['description'] ?: $line['description'], $line['cost_center_id'], $line['customer_id'], $line['supplier_id']]);
            }
            $this->db->prepare('UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$this->userId, $entryId, $this->organizationId]);
            $this->invalidateJournalPrints(null, (string) $entry['entry_date']);
            if ($ownsTransaction) $this->db->commit();
            return ['line_id' => $lineId, 'allocations' => 1 + count($normalized), 'amount' => $originalAmount];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
    }

    public function changeDocumentVatDeductibility(int $entryId, string $vatCode, float $vatRate, string $vatNature, float $percent, bool $saveRevision = true): array
    {
        $percent = round($percent, 2);
        if ($percent < 0 || $percent > 100) throw new InvalidArgumentException('La percentuale detraibile deve essere compresa tra 0 e 100.');
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $statement = $this->db->prepare("SELECT e.*, d.document_type, d.fatturapa_type, d.vat_total, d.vat_collectability FROM journal_entries e JOIN documents d ON d.id = e.source_id AND d.organization_id = e.organization_id WHERE e.id = ? AND e.organization_id = ? AND e.source_type = 'DOCUMENT' AND e.status IN ('DRAFT','POSTED') AND e.is_finalized = 0 FOR UPDATE");
            $statement->execute([$entryId, $this->organizationId]);
            $entry = $statement->fetch();
            if (!$entry || (string) $entry['document_type'] !== 'PURCHASE_INVOICE') throw new InvalidArgumentException('La detraibilità può essere modificata solo su una fattura di acquisto non definitiva.');
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);
            if ($saveRevision) {
                $this->saveRevision($entry, sprintf('IVA %s al %.2f%% detraibile', $vatCode ?: number_format($vatRate, 2), $percent));
            }
            $matching = $this->db->prepare('SELECT COUNT(*) FROM document_lines WHERE organization_id = ? AND document_id = ? AND COALESCE(vat_code,\'\') = ? AND vat_rate = ? AND COALESCE(vat_nature,\'\') = ?');
            $matching->execute([$this->organizationId, (int) $entry['source_id'], $vatCode, $vatRate, $vatNature]);
            if ((int) $matching->fetchColumn() === 0) throw new InvalidArgumentException('Gruppo IVA del documento non trovato.');
            $update = $this->db->prepare('UPDATE document_lines SET deductible_percent = ?, updated_at = NOW() WHERE organization_id = ? AND document_id = ? AND COALESCE(vat_code,\'\') = ? AND vat_rate = ? AND COALESCE(vat_nature,\'\') = ?');
            $update->execute([$percent, $this->organizationId, (int) $entry['source_id'], $vatCode, $vatRate, $vatNature]);
            (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument((int) $entry['source_id']);

            $deductible = $this->db->prepare('SELECT COALESCE(SUM(ABS(deductible_vat)),0) FROM vat_movements WHERE organization_id = ? AND document_id = ?');
            $deductible->execute([$this->organizationId, (int) $entry['source_id']]);
            $deductibleAmount = round((float) $deductible->fetchColumn(), 2);
            $grossVat = round(abs((float) $entry['vat_total']), 2);
            $nonDeductible = round(max(0, $grossVat - $deductibleAmount), 2);
            $vatAccountKey = $this->isDeferredVat($entry) ? 'VAT_CLEARING' : 'VAT_RECEIVABLE';
            $accounts = $this->systemAccounts([$vatAccountKey, 'PURCHASE_COSTS']);
            $isCreditNote = in_array(strtoupper((string) ($entry['fatturapa_type'] ?? '')), ['TD04','TD08'], true);
            $side = $isCreditNote ? 'credit' : 'debit';
            $otherSide = $side === 'debit' ? 'credit' : 'debit';
            $this->db->prepare(
                "DELETE FROM journal_entry_lines WHERE organization_id = ? AND journal_entry_id = ?
                   AND (LOWER(description) LIKE 'iva a credito%' OR LOWER(description) LIKE 'storno iva a credito%'
                        OR LOWER(description) LIKE 'iva differita%' OR LOWER(description) LIKE 'storno iva differita%')"
            )->execute([$this->organizationId, $entryId]);
            $vatBreakdown = $this->db->prepare(
                "SELECT COALESCE(vat_code,'') AS vat_code, COALESCE(vat_nature,'') AS vat_nature, vat_rate,
                        COALESCE(SUM(ABS(deductible_vat)),0) AS deductible_vat
                 FROM vat_movements WHERE organization_id = ? AND document_id = ?
                 GROUP BY COALESCE(vat_code,''), COALESCE(vat_nature,''), vat_rate
                 HAVING COALESCE(SUM(ABS(deductible_vat)),0) > 0.005 ORDER BY vat_rate DESC, vat_code"
            );
            $vatBreakdown->execute([$this->organizationId, (int) $entry['source_id']]);
            $max = $this->db->prepare('SELECT COALESCE(MAX(line_number),0) FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?');
            $max->execute([$entryId, $this->organizationId]);
            $lineNumber = (int) $max->fetchColumn();
            $insertVatLine = $this->db->prepare("INSERT INTO journal_entry_lines (organization_id,journal_entry_id,line_number,account_id,debit,credit,description,created_at) VALUES (?,?,?,?,?,?,?,NOW())");
            foreach ($vatBreakdown->fetchAll() as $vatBreakdownRow) {
                $amount = round((float) $vatBreakdownRow['deductible_vat'], 2);
                $article = (string) ($vatBreakdownRow['vat_code'] ?: ($vatBreakdownRow['vat_nature'] ?: number_format((float) $vatBreakdownRow['vat_rate'], 2, ',', '.') . '%'));
                $descriptionVat = ($isCreditNote ? 'Storno ' : '') . ($vatAccountKey === 'VAT_CLEARING' ? 'IVA differita' : 'IVA a credito') . ' · ' . $article;
                $insertVatLine->execute([$this->organizationId, $entryId, ++$lineNumber, $accounts[$vatAccountKey],
                    $side === 'debit' ? $amount : 0, $side === 'credit' ? $amount : 0, $descriptionVat]);
            }
            $description = 'IVA indetraibile documento ' . $entry['document_number'];
            $costLine = $this->db->prepare('SELECT id FROM journal_entry_lines WHERE organization_id = ? AND journal_entry_id = ? AND description = ? LIMIT 1');
            $costLine->execute([$this->organizationId, $entryId, $description]);
            $costLineId = (int) $costLine->fetchColumn();
            if ($nonDeductible > 0) {
                if ($costLineId) {
                    $this->db->prepare("UPDATE journal_entry_lines SET {$side} = ?, {$otherSide} = 0 WHERE id = ? AND organization_id = ?")->execute([$nonDeductible, $costLineId, $this->organizationId]);
                } else {
                    $max = $this->db->prepare('SELECT COALESCE(MAX(line_number),0)+1 FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?');
                    $max->execute([$entryId, $this->organizationId]);
                    $this->db->prepare("INSERT INTO journal_entry_lines (organization_id,journal_entry_id,line_number,account_id,debit,credit,description,created_at) VALUES (?,?,?,?,?,?,?,NOW())")
                        ->execute([$this->organizationId, $entryId, (int) $max->fetchColumn(), $accounts['PURCHASE_COSTS'], $side === 'debit' ? $nonDeductible : 0, $side === 'credit' ? $nonDeductible : 0, $description]);
                }
            } elseif ($costLineId) {
                $this->db->prepare('DELETE FROM journal_entry_lines WHERE id = ? AND organization_id = ?')->execute([$costLineId, $this->organizationId]);
            }
            $this->db->prepare('UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')->execute([$this->userId, $entryId, $this->organizationId]);
            $this->invalidateJournalPrints(null, (string) $entry['entry_date']);
            if ($ownsTransaction) $this->db->commit();
            return ['deductibility_percent' => $percent, 'deductible_vat' => $deductibleAmount, 'non_deductible_vat' => $nonDeductible];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
    }

    public function replaceDocumentVatSummary(int $entryId, array $rows, string $reason = ''): array
    {
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $statement = $this->db->prepare(
                "SELECT e.*, d.id AS document_id, d.document_type, d.counterparty_type, d.fatturapa_type,
                        d.vat_collectability, d.taxable_total AS document_taxable_total,
                        d.vat_total AS document_vat_total, d.number AS source_document_number
                 FROM journal_entries e
                 JOIN documents d ON d.id = e.source_id AND d.organization_id = e.organization_id
                 WHERE e.id = ? AND e.organization_id = ? AND e.source_type = 'DOCUMENT'
                   AND e.status IN ('DRAFT','POSTED') AND e.is_finalized = 0 FOR UPDATE"
            );
            $statement->execute([$entryId, $this->organizationId]);
            $entry = $statement->fetch();
            $isPurchase = $entry && ((string) $entry['document_type'] === 'PURCHASE_INVOICE'
                || (string) $entry['counterparty_type'] === 'SUPPLIER');
            if (!$entry || !$isPurchase) {
                throw new InvalidArgumentException('Il riepilogo IVA è modificabile soltanto sulle fatture e note di credito di acquisto non definitive.');
            }
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);
            if ($rows === []) throw new InvalidArgumentException('Inserisci almeno una riga nel riepilogo IVA.');

            $articleStatement = $this->db->prepare(
                "SELECT code, description, rate, nature, legal_reference, deductible_percent,
                        vat_regime, reverse_charge, split_payment, fatturapa_type
                 FROM vat_codes WHERE organization_id = ? AND UPPER(code) = ? AND active = 1
                   AND operation_scope IN ('BOTH','PURCHASES') LIMIT 1"
            );
            $classifications = ['ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE'];
            $normalized = [];
            $taxableTotal = 0.0;
            $vatTotal = 0.0;
            foreach ($rows as $index => $row) {
                if (!is_array($row)) throw new InvalidArgumentException('Riga IVA non valida.');
                $code = mb_strtoupper(trim((string) ($row['vat_code'] ?? '')));
                $taxable = round($this->decimal($row['taxable_amount'] ?? 0), 2);
                $vat = round($this->decimal($row['vat_amount'] ?? 0), 2);
                $deductible = round($this->decimal($row['deductible_percent'] ?? 100), 2);
                $classification = strtoupper(trim((string) ($row['purchase_classification'] ?? 'ORDINARY')));
                if ($code === '' || $taxable < 0 || $vat < 0 || ($taxable <= .005 && $vat <= .005)) {
                    throw new InvalidArgumentException('Completa articolo, imponibile e IVA della riga ' . ($index + 1) . '.');
                }
                if ($deductible < 0 || $deductible > 100 || !in_array($classification, $classifications, true)) {
                    throw new InvalidArgumentException('Detraibilità o classificazione non valida alla riga ' . ($index + 1) . '.');
                }
                $articleStatement->execute([$this->organizationId, $code]);
                $article = $articleStatement->fetch();
                if (!$article) throw new InvalidArgumentException('Articolo IVA ' . $code . ' inesistente, disattivato o non utilizzabile negli acquisti.');
                $normalized[] = [
                    'vat_code' => (string) $article['code'],
                    'vat_rate' => (float) $article['rate'],
                    'vat_nature' => trim((string) ($article['nature'] ?? '')) ?: null,
                    'vat_description' => (string) $article['description'],
                    'vat_legal_reference' => trim((string) ($article['legal_reference'] ?? '')) ?: null,
                    'vat_reverse_charge' => (int) $article['reverse_charge'],
                    'vat_split_payment' => (int) $article['split_payment'],
                    'vat_fatturapa_type' => trim((string) ($article['fatturapa_type'] ?? '')) ?: null,
                    'deductible_percent' => $deductible,
                    'purchase_classification' => $classification,
                    'taxable_amount' => $taxable,
                    'vat_amount' => $vat,
                ];
                $taxableTotal += $taxable;
                $vatTotal += $vat;
            }
            $expectedTaxable = round(abs((float) $entry['document_taxable_total']), 2);
            $expectedVat = round(abs((float) $entry['document_vat_total']), 2);
            $taxableTotal = round($taxableTotal, 2);
            $vatTotal = round($vatTotal, 2);
            if (abs($taxableTotal - $expectedTaxable) > .02 || abs($vatTotal - $expectedVat) > .02) {
                throw new InvalidArgumentException(sprintf(
                    'Riepilogo non quadrato: imponibile %.2f su %.2f, IVA %.2f su %.2f.',
                    $taxableTotal, $expectedTaxable, $vatTotal, $expectedVat
                ));
            }

            $this->saveRevision($entry, trim($reason) ?: 'Aggiornamento riepilogo IVA documento');
            $this->db->prepare('DELETE FROM document_vat_summaries WHERE organization_id = ? AND document_id = ?')
                ->execute([$this->organizationId, (int) $entry['document_id']]);
            $insert = $this->db->prepare(
                'INSERT INTO document_vat_summaries
                 (organization_id, document_id, line_number, vat_code, vat_rate, vat_nature, vat_description,
                  vat_legal_reference, vat_reverse_charge, vat_split_payment, vat_fatturapa_type,
                  deductible_percent, purchase_classification, taxable_amount, vat_amount, source_type,
                  created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, \'MANUAL\', ?, ?, NOW(), NOW())'
            );
            foreach ($normalized as $index => $row) {
                $insert->execute([
                    $this->organizationId, (int) $entry['document_id'], $index + 1,
                    $row['vat_code'], $row['vat_rate'], $row['vat_nature'], $row['vat_description'],
                    $row['vat_legal_reference'], $row['vat_reverse_charge'], $row['vat_split_payment'],
                    $row['vat_fatturapa_type'], $row['deductible_percent'], $row['purchase_classification'],
                    $row['taxable_amount'], $row['vat_amount'], $this->userId, $this->userId,
                ]);
            }
            (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument((int) $entry['document_id']);
            $this->rebuildDocumentVatJournalLines($entry);
            $this->db->prepare('UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$this->userId, $entryId, $this->organizationId]);
            $this->invalidateJournalPrints(null, (string) $entry['entry_date']);
            if ($ownsTransaction) $this->db->commit();
            return ['rows' => count($normalized), 'taxable_total' => $taxableTotal, 'vat_total' => $vatTotal];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
    }

    private function rebuildDocumentVatJournalLines(array $entry): void
    {
        $entryId = (int) $entry['id'];
        $isCreditNote = in_array(strtoupper((string) ($entry['fatturapa_type'] ?? '')), ['TD04','TD08'], true)
            || (string) ($entry['document_type'] ?? '') === 'CREDIT_NOTE';
        $vatAccountKey = $this->isDeferredVat($entry) ? 'VAT_CLEARING' : 'VAT_RECEIVABLE';
        $accounts = $this->systemAccounts([$vatAccountKey, 'PURCHASE_COSTS']);
        $this->db->prepare(
            "DELETE FROM journal_entry_lines WHERE organization_id = ? AND journal_entry_id = ?
               AND (LOWER(description) LIKE 'iva a credito%' OR LOWER(description) LIKE 'storno iva a credito%'
                    OR LOWER(description) LIKE 'iva differita%' OR LOWER(description) LIKE 'storno iva differita%'
                    OR LOWER(description) LIKE 'rettifica iva indetraibile da riepilogo%')"
        )->execute([$this->organizationId, $entryId]);

        $breakdown = $this->db->prepare(
            "SELECT vat_code, vat_nature, vat_rate, COALESCE(SUM(ABS(deductible_vat)),0) AS deductible_vat
             FROM vat_movements WHERE organization_id = ? AND document_id = ?
             GROUP BY vat_code, vat_nature, vat_rate
             HAVING COALESCE(SUM(ABS(deductible_vat)),0) > 0.005 ORDER BY vat_rate DESC, vat_code"
        );
        $breakdown->execute([$this->organizationId, (int) $entry['document_id']]);
        $max = $this->db->prepare('SELECT COALESCE(MAX(line_number),0) FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ?');
        $max->execute([$entryId, $this->organizationId]);
        $lineNumber = (int) $max->fetchColumn();
        $insert = $this->db->prepare('INSERT INTO journal_entry_lines (organization_id,journal_entry_id,line_number,account_id,debit,credit,description,created_at) VALUES (?,?,?,?,?,?,?,NOW())');
        foreach ($breakdown->fetchAll() as $row) {
            $amount = round((float) $row['deductible_vat'], 2);
            $article = (string) ($row['vat_code'] ?: ($row['vat_nature'] ?: number_format((float) $row['vat_rate'], 2, ',', '.') . '%'));
            $description = ($isCreditNote ? 'Storno ' : '') . ($vatAccountKey === 'VAT_CLEARING' ? 'IVA differita' : 'IVA a credito') . ' · ' . $article;
            $insert->execute([$this->organizationId, $entryId, ++$lineNumber, $accounts[$vatAccountKey],
                $isCreditNote ? 0 : $amount, $isCreditNote ? $amount : 0, $description]);
        }

        $totals = $this->db->prepare('SELECT COALESCE(SUM(debit),0) debit, COALESCE(SUM(credit),0) credit FROM journal_entry_lines WHERE organization_id = ? AND journal_entry_id = ?');
        $totals->execute([$this->organizationId, $entryId]);
        $current = $totals->fetch();
        $difference = round((float) $current['debit'] - (float) $current['credit'], 2);
        if (abs($difference) > .005) {
            $insert->execute([
                $this->organizationId, $entryId, ++$lineNumber, $accounts['PURCHASE_COSTS'],
                $difference < 0 ? abs($difference) : 0, $difference > 0 ? $difference : 0,
                'Rettifica IVA indetraibile da riepilogo documento ' . ($entry['source_document_number'] ?? $entry['document_number']),
            ]);
        }
        $totals->execute([$this->organizationId, $entryId]);
        $updated = $totals->fetch();
        $this->db->prepare('UPDATE journal_entries SET total_debit = ?, total_credit = ? WHERE id = ? AND organization_id = ?')
            ->execute([round((float) $updated['debit'], 2), round((float) $updated['credit'], 2), $entryId, $this->organizationId]);
    }

    public function changeDocumentLinePurchaseClassification(int $entryId, int $lineId, string $classification): array
    {
        $classification = strtoupper(trim($classification));
        $allowed = ['ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE'];
        if (!in_array($classification, $allowed, true)) {
            throw new InvalidArgumentException('Classificazione fiscale acquisto non valida.');
        }
        $statement = $this->db->prepare(
            "SELECT e.*, d.id AS document_id, d.document_type FROM journal_entries e
             JOIN documents d ON d.id = e.source_id AND d.organization_id = e.organization_id
             WHERE e.id = ? AND e.organization_id = ? AND e.source_type = 'DOCUMENT'
               AND e.status IN ('DRAFT','POSTED') AND e.is_finalized = 0"
        );
        $statement->execute([$entryId, $this->organizationId]);
        $entry = $statement->fetch();
        if (!$entry || (string) $entry['document_type'] !== 'PURCHASE_INVOICE') {
            throw new InvalidArgumentException('La classificazione è disponibile solo per fatture di acquisto non definitive.');
        }
        $this->assertAccountingPeriodOpen((string) $entry['entry_date']);
        $line = $this->db->prepare('SELECT id, description, purchase_classification FROM document_lines WHERE id = ? AND document_id = ? AND organization_id = ?');
        $line->execute([$lineId, (int) $entry['document_id'], $this->organizationId]);
        $documentLine = $line->fetch();
        if (!$documentLine) throw new InvalidArgumentException('Riga documento non trovata.');
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $this->saveRevision($entry, 'Classificazione fiscale riga: ' . $classification);
            $this->db->prepare('UPDATE document_lines SET purchase_classification = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$classification, $lineId, $this->organizationId]);
            (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument((int) $entry['document_id']);
            $this->db->prepare('UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$this->userId, $entryId, $this->organizationId]);
            if ($ownsTransaction) $this->db->commit();
            return ['line_id' => $lineId, 'previous' => $documentLine['purchase_classification'], 'classification' => $classification];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
    }

    public function changeDocumentLineVatArticle(int $entryId, int $lineId, string $vatCode): array
    {
        $vatCode = mb_strtoupper(trim($vatCode));
        if ($vatCode === '') throw new InvalidArgumentException('Seleziona un articolo IVA.');
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) $this->db->beginTransaction();
        try {
            $statement = $this->db->prepare(
                "SELECT e.*, d.id AS document_id, d.document_type FROM journal_entries e
                 JOIN documents d ON d.id = e.source_id AND d.organization_id = e.organization_id
                 WHERE e.id = ? AND e.organization_id = ? AND e.source_type = 'DOCUMENT'
                   AND e.status IN ('DRAFT','POSTED') AND e.is_finalized = 0 FOR UPDATE"
            );
            $statement->execute([$entryId, $this->organizationId]);
            $entry = $statement->fetch();
            if (!$entry) throw new InvalidArgumentException('Registrazione documentale non modificabile.');
            $this->assertAccountingPeriodOpen((string) $entry['entry_date']);
            $scope = (string) $entry['document_type'] === 'PURCHASE_INVOICE' ? 'PURCHASES' : 'SALES';
            $articleStatement = $this->db->prepare(
                "SELECT * FROM vat_codes WHERE organization_id = ? AND code = ? AND active = 1
                   AND operation_scope IN ('BOTH', ?) LIMIT 1"
            );
            $articleStatement->execute([$this->organizationId, $vatCode, $scope]);
            $article = $articleStatement->fetch();
            if (!$article) throw new InvalidArgumentException('Articolo IVA inesistente, obsoleto o non utilizzabile per questo tipo di documento.');
            $lineStatement = $this->db->prepare('SELECT * FROM document_lines WHERE id = ? AND document_id = ? AND organization_id = ? FOR UPDATE');
            $lineStatement->execute([$lineId, (int) $entry['document_id'], $this->organizationId]);
            $line = $lineStatement->fetch();
            if (!$line) throw new InvalidArgumentException('Riga documento non trovata.');
            $this->saveRevision($entry, 'Articolo IVA riga ' . (int) $line['line_number'] . ': ' . $vatCode);
            $classification = (string) ($line['purchase_classification'] ?? 'ORDINARY');
            $regime = mb_strtolower((string) ($article['vat_regime'] ?? ''));
            if (str_contains($regime, 'ammortizz')) $classification = 'DEPRECIABLE_ASSET';
            elseif (str_contains($regime, 'rivend')) $classification = 'RESALE';
            elseif (str_contains($regime, 'leasing')) $classification = 'LEASING';
            $nature = mb_strtoupper(trim((string) ($article['nature'] ?? '')));
            $this->db->prepare(
                'UPDATE document_lines SET vat_code = ?, vat_nature = ?, vat_description = ?, vat_legal_reference = ?,
                 vat_reverse_charge = ?, vat_split_payment = ?, vat_fatturapa_type = ?, purchase_classification = ?,
                 vat_article_review_required = 0, updated_at = NOW() WHERE id = ? AND organization_id = ?'
            )->execute([
                $vatCode, $nature ?: null, $article['description'], $article['legal_reference'],
                (int) $article['reverse_charge'], (int) $article['split_payment'], $article['fatturapa_type'] ?: null,
                $classification, $lineId, $this->organizationId,
            ]);
            if ((string) $entry['document_type'] === 'PURCHASE_INVOICE') {
                $result = $this->changeDocumentVatDeductibility(
                    $entryId, $vatCode, (float) $line['vat_rate'], $nature, (float) $article['deductible_percent'], false
                );
            } else {
                (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument((int) $entry['document_id']);
                $this->db->prepare('UPDATE journal_entries SET revision_number = revision_number + 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                    ->execute([$this->userId, $entryId, $this->organizationId]);
                $this->invalidateJournalPrints(null, (string) $entry['entry_date']);
                $result = ['deductibility_percent' => 0.0, 'deductible_vat' => 0.0, 'non_deductible_vat' => 0.0];
            }
            if ($ownsTransaction) $this->db->commit();
            return $result + ['line_id' => $lineId, 'previous' => $line['vat_code'], 'vat_code' => $vatCode];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
    }

    /** @return list<string> */
    private function compatibleAccountTypes(array $entry, array $line): array
    {
        $description = mb_strtolower(trim((string) ($line['description'] ?? '')));
        $isPurchaseAllocation = (string) ($entry['source_type'] ?? '') === 'DOCUMENT'
            && (string) ($entry['entry_type'] ?? '') === 'PURCHASE_INVOICE'
            && empty($line['customer_id']) && empty($line['supplier_id'])
            && !str_contains($description, 'iva a credito') && !str_contains($description, 'iva differita');
        return $isPurchaseAllocation ? ['EXPENSE', 'ASSET', 'LIABILITY'] : [(string) $line['account_type']];
    }

    private function saveRevision(array $entry, string $reason): void
    {
        $lines = $this->db->prepare(
            'SELECT line_number, account_id, debit, credit, description, cost_center_id, customer_id, supplier_id
             FROM journal_entry_lines WHERE journal_entry_id = ? AND organization_id = ? ORDER BY line_number, id'
        );
        $lines->execute([(int) $entry['id'], $this->organizationId]);
        $insert = $this->db->prepare(
            'INSERT INTO journal_entry_revisions
             (organization_id, journal_entry_id, revision_number, change_reason, header_json, lines_json, changed_by, changed_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW())'
        );
        $insert->execute([
            $this->organizationId, (int) $entry['id'], (int) $entry['revision_number'], mb_substr($reason, 0, 500),
            json_encode($entry, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            json_encode($lines->fetchAll(), JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            $this->userId,
        ]);
    }

    private function invalidateJournalPrints(?string $previousDate, string $newDate): void
    {
        $dates = array_values(array_unique(array_filter([$previousDate, $newDate])));
        foreach ($dates as $date) {
            $statement = $this->db->prepare(
                "UPDATE official_print_runs
                 SET status = 'CANCELLED', notes = CONCAT(COALESCE(notes, ''), ?), updated_at = NOW()
                 WHERE organization_id = ? AND print_type = 'JOURNAL' AND status IN ('GENERATED','VALIDATED')
                   AND ? BETWEEN period_start AND period_end"
            );
            $statement->execute(["\nAnnullata automaticamente: prima nota modificata dopo la generazione.", $this->organizationId, $date]);
        }
    }

    public function changeDocumentRegistrationDate(int $documentId, string $registrationDate): array
    {
        $registrationDate = trim($registrationDate);
        if (!$this->isIsoDate($registrationDate)) {
            throw new InvalidArgumentException('La data di registrazione non è valida.');
        }

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare('SELECT * FROM documents WHERE id = ? AND organization_id = ? FOR UPDATE');
            $statement->execute([$documentId, $this->organizationId]);
            $document = $statement->fetch();
            if (!$document || !in_array($document['document_type'], ['SALES_INVOICE', 'PURCHASE_INVOICE', 'CREDIT_NOTE'], true)) {
                throw new InvalidArgumentException('Fattura o nota di credito non trovata.');
            }
            if (($document['status'] ?? '') === 'HISTORICAL') {
                throw new InvalidArgumentException('Il documento è un originale storico e non genera prima nota o IVA operativa.');
            }
            if ($registrationDate < (string) $document['document_date']) {
                throw new InvalidArgumentException('La data di registrazione non può precedere la data documento.');
            }

            $previousDate = (string) ($document['registration_date'] ?: $document['document_date']);
            if ($previousDate === $registrationDate) {
                if ($ownsTransaction) {
                    $this->db->commit();
                }
                return ['previous_date' => $previousDate, 'registration_date' => $registrationDate, 'journal_entry_id' => null];
            }

            $this->assertAccountingPeriodOpen($previousDate);
            $this->assertAccountingPeriodOpen($registrationDate);
            $vatService = new VatService($this->db, $this->organizationId, $this->userId);
            $vatService->invalidateForDate($previousDate);

            $entryStatement = $this->db->prepare(
                "SELECT * FROM journal_entries
                 WHERE organization_id = ? AND source_type = 'DOCUMENT' AND source_id = ? FOR UPDATE"
            );
            $entryStatement->execute([$this->organizationId, $documentId]);
            $entry = $entryStatement->fetch();
            if ($entry && ((int) ($entry['is_finalized'] ?? 0) === 1 || !in_array($entry['status'], ['DRAFT', 'POSTED'], true))) {
                throw new InvalidArgumentException('La scrittura collegata è definitiva, stornata o non modificabile.');
            }

            $this->db->prepare(
                'UPDATE documents SET registration_date = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([$registrationDate, $this->userId, $documentId, $this->organizationId]);

            $entryId = null;
            if ($entry) {
                $entryId = (int) $entry['id'];
                $this->saveRevision($entry, 'Correzione data di registrazione del documento ' . $document['number']);
                $protocol = (string) $entry['protocol_number'];
                if (substr($previousDate, 0, 4) !== substr($registrationDate, 0, 4)) {
                    $protocol = $this->nextProtocol($registrationDate);
                }
                $this->db->prepare(
                    'UPDATE journal_entries
                     SET protocol_number = ?, entry_date = ?, competence_date = ?, updated_by = ?,
                         revision_number = revision_number + 1, updated_at = NOW()
                     WHERE id = ? AND organization_id = ?'
                )->execute([$protocol, $registrationDate, $registrationDate, $this->userId, $entryId, $this->organizationId]);
            }

            $vatService->syncDocument($documentId);
            $this->invalidateJournalPrints($previousDate, $registrationDate);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return ['previous_date' => $previousDate, 'registration_date' => $registrationDate, 'journal_entry_id' => $entryId];
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

            $existing = $this->db->prepare("SELECT id, is_finalized FROM journal_entries WHERE organization_id = ? AND source_type = 'DOCUMENT' AND source_id = ? LIMIT 1");
            $existing->execute([$this->organizationId, $documentId]);
            $existingEntry = $existing->fetch() ?: null;

            $date = (string) ($document['registration_date'] ?: $document['document_date']);
            $lines = [];
            $total = (float) $document['total'];
            $net = (float) $document['taxable_total'];
            $vat = (float) $document['vat_total'];
            $withholding = max(0, round(abs((float) ($document['withholding_total'] ?? 0)), 2));
            $partyAmount = $withholding > .005
                ? round(max(0, abs((float) ($document['balance_due'] ?? 0))) ?: max(0, abs($total) - $withholding), 2)
                : abs($total);
            $vatAccount = $this->isDeferredVat($document) ? 'VAT_CLEARING' : null;
            $isCreditNote = $document['document_type'] === 'CREDIT_NOTE'
                || in_array(strtoupper((string) ($document['fatturapa_type'] ?? '')), ['TD04', 'TD08'], true);
            $isPurchase = $document['document_type'] === 'PURCHASE_INVOICE'
                || strtoupper((string) ($document['counterparty_type'] ?? '')) === 'SUPPLIER';
            $partyId = (int) ($document['counterparty_id'] ?? 0);
            $partyTag = $partyId > 0
                ? ($isPurchase ? ['supplier_id' => $partyId] : ['customer_id' => $partyId])
                : [];
            (new VatService($this->db, $this->organizationId, $this->userId))->syncDocument($documentId);
            $vatGroupsStatement = $this->db->prepare(
                "SELECT COALESCE(vat_code, '') AS vat_code, COALESCE(vat_nature, '') AS vat_nature,
                        vat_rate, COALESCE(vat_description, '') AS vat_description,
                        deductible_percent, SUM(vat_amount) AS vat_amount
                 FROM document_vat_summaries WHERE document_id = ? AND organization_id = ?
                 GROUP BY COALESCE(vat_code, ''), COALESCE(vat_nature, ''), vat_rate,
                          COALESCE(vat_description, ''), deductible_percent
                 HAVING ABS(SUM(vat_amount)) > 0.005
                 ORDER BY vat_rate DESC, vat_code"
            );
            $vatGroupsStatement->execute([$documentId, $this->organizationId]);
            $documentVatGroups = $vatGroupsStatement->fetchAll();
            if ($documentVatGroups === [] && abs($vat) > .005) {
                $documentVatGroups = [[
                    'vat_code' => '', 'vat_nature' => '', 'vat_rate' => 0,
                    'vat_description' => '', 'deductible_percent' => 100, 'vat_amount' => $vat,
                ]];
            }
            $deductibleVatTotal = 0.0;
            $nonDeductibleVatTotal = 0.0;
            foreach ($documentVatGroups as &$vatGroup) {
                $grossGroupVat = round(abs((float) $vatGroup['vat_amount']), 2);
                $deductiblePercent = $isPurchase ? max(0, min(100, (float) ($vatGroup['deductible_percent'] ?? 100))) : 100;
                $vatGroup['deductible_amount'] = round($grossGroupVat * $deductiblePercent / 100, 2);
                $vatGroup['non_deductible_amount'] = round($grossGroupVat - $vatGroup['deductible_amount'], 2);
                $deductibleVatTotal += $vatGroup['deductible_amount'];
                $nonDeductibleVatTotal += $vatGroup['non_deductible_amount'];
            }
            unset($vatGroup);
            $requiredAccounts = $isPurchase
                ? ['TRADE_PAYABLES', 'PURCHASE_COSTS']
                : ['TRADE_RECEIVABLES', 'SALES_REVENUE'];
            if ($isPurchase && $withholding > .005) $requiredAccounts[] = 'WITHHOLDING_PAYABLE';
            if ($vat > 0) {
                $requiredAccounts[] = $vatAccount ?: ($isPurchase ? 'VAT_RECEIVABLE' : 'VAT_PAYABLE');
            }
            $accounts = $this->systemAccounts($requiredAccounts);

            if (!$isCreditNote && !$isPurchase) {
                $lines = [
                    ['account_id' => $accounts['TRADE_RECEIVABLES'], 'debit' => $total, 'credit' => 0.0, 'description' => 'Credito verso ' . $document['counterparty_name']] + $partyTag,
                    ['account_id' => $accounts['SALES_REVENUE'], 'debit' => 0.0, 'credit' => $net, 'description' => 'Ricavi documento ' . $document['number']],
                ];
                foreach ($documentVatGroups as $vatGroup) {
                    $amount = (float) $vatGroup['deductible_amount'];
                    if ($amount <= .005) continue;
                    $article = (string) ($vatGroup['vat_code'] ?: ($vatGroup['vat_nature'] ?: number_format((float) $vatGroup['vat_rate'], 2, ',', '.') . '%'));
                    $lines[] = ['account_id' => $accounts[$vatAccount ?: 'VAT_PAYABLE'], 'debit' => 0.0, 'credit' => $amount, 'description' => ($vatAccount ? 'IVA differita' : 'IVA a debito') . ' · ' . $article];
                }
            } elseif ($isCreditNote && !$isPurchase) {
                $lines = [
                    ['account_id' => $accounts['SALES_REVENUE'], 'debit' => $net, 'credit' => 0.0, 'description' => 'Storno ricavi ' . $document['number']],
                    ['account_id' => $accounts['TRADE_RECEIVABLES'], 'debit' => 0.0, 'credit' => $total, 'description' => 'Storno credito verso ' . $document['counterparty_name']] + $partyTag,
                ];
                foreach ($documentVatGroups as $vatGroup) {
                    $amount = (float) $vatGroup['deductible_amount'];
                    if ($amount <= .005) continue;
                    $article = (string) ($vatGroup['vat_code'] ?: ($vatGroup['vat_nature'] ?: number_format((float) $vatGroup['vat_rate'], 2, ',', '.') . '%'));
                    $lines[] = ['account_id' => $accounts[$vatAccount ?: 'VAT_PAYABLE'], 'debit' => $amount, 'credit' => 0.0, 'description' => ($vatAccount ? 'Storno IVA differita' : 'Storno IVA a debito') . ' · ' . $article];
                }
            } elseif (!$isCreditNote) {
                $lines = [
                    ['account_id' => $accounts['PURCHASE_COSTS'], 'debit' => round($net + $nonDeductibleVatTotal, 2), 'credit' => 0.0, 'description' => 'Costo documento ' . $document['number']],
                    ['account_id' => $accounts['TRADE_PAYABLES'], 'debit' => 0.0, 'credit' => $partyAmount, 'description' => 'Netto dovuto a ' . $document['counterparty_name']] + $partyTag,
                ];
                if ($withholding > .005) {
                    $lines[] = ['account_id' => $accounts['WITHHOLDING_PAYABLE'], 'debit' => 0.0, 'credit' => $withholding, 'description' => 'Ritenuta da versare all’erario'];
                }
                foreach ($documentVatGroups as $vatGroup) {
                    $amount = (float) $vatGroup['deductible_amount'];
                    if ($amount <= .005) continue;
                    $article = (string) ($vatGroup['vat_code'] ?: ($vatGroup['vat_nature'] ?: number_format((float) $vatGroup['vat_rate'], 2, ',', '.') . '%'));
                    $lines[] = ['account_id' => $accounts[$vatAccount ?: 'VAT_RECEIVABLE'], 'debit' => $amount, 'credit' => 0.0, 'description' => ($vatAccount ? 'IVA differita' : 'IVA a credito') . ' · ' . $article];
                }
            } else {
                $lines = [
                    ['account_id' => $accounts['TRADE_PAYABLES'], 'debit' => $partyAmount, 'credit' => 0.0, 'description' => 'Storno netto dovuto a ' . $document['counterparty_name']] + $partyTag,
                    ['account_id' => $accounts['PURCHASE_COSTS'], 'debit' => 0.0, 'credit' => round($net + $nonDeductibleVatTotal, 2), 'description' => 'Storno costi ' . $document['number']],
                ];
                if ($withholding > .005) {
                    $lines[] = ['account_id' => $accounts['WITHHOLDING_PAYABLE'], 'debit' => $withholding, 'credit' => 0.0, 'description' => 'Storno ritenuta erario'];
                }
                foreach ($documentVatGroups as $vatGroup) {
                    $amount = (float) $vatGroup['deductible_amount'];
                    if ($amount <= .005) continue;
                    $article = (string) ($vatGroup['vat_code'] ?: ($vatGroup['vat_nature'] ?: number_format((float) $vatGroup['vat_rate'], 2, ',', '.') . '%'));
                    $lines[] = ['account_id' => $accounts[$vatAccount ?: 'VAT_RECEIVABLE'], 'debit' => 0.0, 'credit' => $amount, 'description' => ($vatAccount ? 'Storno IVA differita' : 'Storno IVA a credito') . ' · ' . $article];
                }
            }
            if ($existingEntry) {
                if ($withholding > .005 && empty($existingEntry['is_finalized'])) {
                    $withholdingLine = $this->db->prepare(
                        "SELECT COALESCE(SUM(l.credit-l.debit),0) FROM journal_entry_lines l
                         JOIN accounting_account_mappings m ON m.organization_id=l.organization_id AND m.account_id=l.account_id
                         WHERE l.organization_id=? AND l.journal_entry_id=? AND m.mapping_key='WITHHOLDING_PAYABLE'"
                    );
                    $withholdingLine->execute([$this->organizationId, (int) $existingEntry['id']]);
                    if (abs((float) $withholdingLine->fetchColumn() - $withholding) > .005) {
                        $this->replaceProvisionalLines((int) $existingEntry['id'], $lines, 'Riallineamento automatico fattura con ritenuta');
                    }
                }
                if ($ownsTransaction) $this->db->commit();
                return (int) $existingEntry['id'];
            }
            $entryId = $this->postAutomated([
                'entry_date' => $date,
                'competence_date' => $date,
                'entry_type' => $document['document_type'],
                'cause_id' => $withholding > .005
                    ? $this->causeForWithholdingDocument((string) $document['document_type'])
                    : $this->causeForDocument((string) $document['document_type']),
                'description' => $document['counterparty_name'] . ' - ' . $document['number'],
                'document_number' => $document['number'],
                'counterparty' => $document['counterparty_name'],
            ], $lines, 'DOCUMENT', $documentId);
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
                'line_id' => !empty($line['line_id']) ? (int) $line['line_id'] : null,
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

    private function assertAnalyticParties(array $lines): void
    {
        foreach ([['customer_id', 'customers'], ['supplier_id', 'suppliers']] as [$field, $table]) {
            $ids = array_values(array_unique(array_filter(array_map(static fn (array $line): int => (int) ($line[$field] ?? 0), $lines))));
            if ($ids === []) continue;
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $statement = $this->db->prepare("SELECT COUNT(*) FROM {$table} WHERE organization_id = ? AND active = 1 AND id IN ({$placeholders})");
            $statement->execute(array_merge([$this->organizationId], $ids));
            if ((int) $statement->fetchColumn() !== count($ids)) {
                throw new InvalidArgumentException('Uno dei sottoconti cliente/fornitore selezionati non è valido per questa azienda.');
            }
        }
        foreach ($lines as $line) {
            if (!empty($line['customer_id']) && !empty($line['supplier_id'])) {
                throw new InvalidArgumentException('Una riga non può appartenere contemporaneamente a un cliente e a un fornitore.');
            }
        }
    }

    private function assertHeaderReferences(array $header): void
    {
        foreach ([
            'cause_id' => 'accounting_causes',
            'vat_register_id' => 'vat_registers',
        ] as $field => $table) {
            if (empty($header[$field])) {
                continue;
            }
            $statement = $this->db->prepare("SELECT COUNT(*) FROM {$table} WHERE id = ? AND organization_id = ? AND active = 1");
            $statement->execute([(int) $header[$field], $this->organizationId]);
            if ((int) $statement->fetchColumn() !== 1) {
                throw new InvalidArgumentException('Causale o sezionale IVA non valido per questa azienda.');
            }
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

    private function systemAccounts(array $keys): array
    {
        $keys = array_values(array_unique($keys));
        $placeholders = implode(',', array_fill(0, count($keys), '?'));
        // Imported plans of accounts (for example DATEV) deliberately keep their
        // original codes and have no system_key. The user-facing mappings are the
        // authoritative link between those accounts and Luna2 automations.
        $statement = $this->db->prepare(
            "SELECT a.id, m.mapping_key AS system_key
             FROM accounting_account_mappings m
             JOIN chart_of_accounts a ON a.id = m.account_id AND a.organization_id = m.organization_id
             WHERE m.organization_id = ? AND m.mapping_key IN ({$placeholders})
               AND a.active = 1 AND a.is_postable = 1"
        );
        $statement->execute(array_merge([$this->organizationId], $keys));
        $accounts = [];
        foreach ($statement->fetchAll() as $row) {
            $accounts[$row['system_key']] = (int) $row['id'];
        }
        $missing = array_diff($keys, array_keys($accounts));
        if ($missing !== []) {
            $fallbackPlaceholders = implode(',', array_fill(0, count($missing), '?'));
            $fallback = $this->db->prepare(
                "SELECT id, system_key FROM chart_of_accounts
                 WHERE organization_id = ? AND system_key IN ({$fallbackPlaceholders})
                   AND active = 1 AND is_postable = 1"
            );
            $fallback->execute(array_merge([$this->organizationId], array_values($missing)));
            foreach ($fallback->fetchAll() as $row) {
                $accounts[$row['system_key']] = (int) $row['id'];
            }
        }
        $missing = array_diff($keys, array_keys($accounts));
        if ($missing !== []) {
            throw new RuntimeException(
                'Automatismi contabili incompleti: collega i conti per ' . implode(', ', $missing)
                . ' in Contabilità → Piano dei conti e causali → Automatismi.'
            );
        }
        return $accounts;
    }

    private function causeForDocument(string $documentType): ?int
    {
        $category = $documentType === 'PURCHASE_INVOICE' ? 'PURCHASE' : 'SALES';
        $statement = $this->db->prepare(
            'SELECT id FROM accounting_causes WHERE organization_id = ? AND category = ? AND active = 1
             ORDER BY automatic DESC, id LIMIT 1'
        );
        $statement->execute([$this->organizationId, $category]);
        return ($id = $statement->fetchColumn()) ? (int) $id : null;
    }

    private function causeForWithholdingDocument(string $documentType): ?int
    {
        $statement = $this->db->prepare(
            "SELECT id FROM accounting_causes WHERE organization_id = ? AND active = 1
             AND UPPER(code) IN ('A19','B19') ORDER BY FIELD(UPPER(code),'A19','B19'), id LIMIT 1"
        );
        $statement->execute([$this->organizationId]);
        return ($id = $statement->fetchColumn()) ? (int) $id : $this->causeForDocument($documentType);
    }

    private function isDeferredVat(array $document): bool
    {
        if (strtoupper((string) ($document['vat_collectability'] ?? '')) === 'D') {
            return true;
        }
        $statement = $this->db->prepare('SELECT cash_vat_enabled FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        return (bool) $statement->fetchColumn();
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

    private function assertAccountingPeriodOpen(string $date): void
    {
        $statement = $this->db->prepare(
            "SELECT COUNT(*) FROM accounting_period_locks
             WHERE organization_id = ? AND scope IN ('ACCOUNTING','SALES','PURCHASES')
               AND unlocked_at IS NULL AND ? BETWEEN starts_on AND ends_on"
        );
        $statement->execute([$this->organizationId, $date]);
        if ((int) $statement->fetchColumn() > 0) {
            throw new RuntimeException('Il periodo contabile è bloccato. Riaprilo prima di registrare movimenti.');
        }
        $year = (int) substr($date, 0, 4);
        $statement = $this->db->prepare(
            "SELECT status FROM fiscal_years WHERE organization_id = ? AND year = ? LIMIT 1"
        );
        $statement->execute([$this->organizationId, $year]);
        if ($statement->fetchColumn() === 'CLOSED') {
            throw new RuntimeException('L’esercizio contabile è chiuso.');
        }
    }
}
