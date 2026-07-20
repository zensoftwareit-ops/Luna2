<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class WithholdingService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function record(array $data): int
    {
        $openItemId = (int) ($data['open_item_id'] ?? 0);
        $date = $this->date((string) ($data['record_date'] ?? ''));
        $dueDate = !empty($data['due_date']) ? $this->date((string) $data['due_date']) : null;
        $type = strtoupper((string) ($data['withholding_type'] ?? 'IRPEF'));
        $gross = round($this->decimal($data['gross_amount'] ?? 0), 2);
        $taxablePercent = $this->percent($data['taxable_percent'] ?? 100);
        $rate = $this->percent($data['rate_percent'] ?? 20);
        $social = round(max(0, $this->decimal($data['social_security_amount'] ?? 0)), 2);
        if ($openItemId <= 0 || !in_array($type, ['IRPEF','INPS','ENASARCO','OTHER'], true) || $gross <= 0 || $rate <= 0) {
            throw new InvalidArgumentException('Dati della ritenuta non validi.');
        }
        $withholding = round($gross * $taxablePercent / 100 * $rate / 100, 2);
        $net = round($gross - $withholding - $social, 2);
        if ($net <= 0) {
            throw new InvalidArgumentException('Il netto professionista deve essere positivo.');
        }
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT * FROM accounting_open_items
                 WHERE id = ? AND organization_id = ? AND direction = 'PAYABLE'
                   AND status IN ('OPEN','PARTIAL','OVERDUE','DISPUTED') FOR UPDATE"
            );
            $statement->execute([$openItemId, $this->organizationId]);
            $item = $statement->fetch();
            if (!$item) {
                throw new InvalidArgumentException('Partita fornitore non disponibile.');
            }
            $outstanding = round((float) $item['original_amount'] - (float) $item['settled_amount'], 2);
            if ($gross > $outstanding + .005) {
                throw new InvalidArgumentException('Il lordo supera il residuo della partita.');
            }
            $bankAccountId = !empty($data['bank_account_id']) ? (int) $data['bank_account_id'] : null;
            $bankAccount = $this->cashOrBankAccount($bankAccountId);
            $causeId = $this->causeId();
            $insertPayment = $this->db->prepare(
                "INSERT INTO payments
                 (organization_id, document_id, payment_schedule_id, payment_type, supplier_id, bank_account_id,
                  cause_id, payment_date, amount, currency, bank_amount, method, status, reference_number,
                  description, reconciled, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, 'PAYMENT', ?, ?, ?, ?, ?, ?, ?, ?, 'POSTED', ?, ?, 0, ?, ?, NOW(), NOW())"
            );
            $insertPayment->execute([
                $this->organizationId, $item['document_id'], $item['payment_schedule_id'], $item['party_id'],
                $bankAccountId, $causeId, $date, $gross, $item['currency'], $net,
                trim((string) ($data['method'] ?? 'BANK_TRANSFER')) ?: 'BANK_TRANSFER',
                $this->nullable($data['reference_number'] ?? null), 'Pagamento professionista ' . $item['party_name'],
                $this->userId, $this->userId,
            ]);
            $paymentId = (int) $this->db->lastInsertId();
            $this->db->prepare(
                'INSERT INTO payment_allocations (organization_id, payment_id, open_item_id, amount, created_by, created_at)
                 VALUES (?, ?, ?, ?, ?, NOW())'
            )->execute([$this->organizationId, $paymentId, $openItemId, $gross, $this->userId]);

            $lines = [
                ['account_id' => $item['account_id'], 'debit' => $gross, 'credit' => 0, 'description' => 'Chiusura debito professionista'],
                ['account_id' => $bankAccount, 'debit' => 0, 'credit' => $net, 'description' => 'Netto pagato'],
                ['account_id' => $this->mapping('WITHHOLDING_PAYABLE'), 'debit' => 0, 'credit' => $withholding, 'description' => 'Ritenuta ' . $type],
            ];
            if ($social > .005) {
                $lines[] = ['account_id' => $this->mapping('SOCIAL_SECURITY_PAYABLE'), 'debit' => 0, 'credit' => $social, 'description' => 'Contributo previdenziale trattenuto'];
            }
            $lines = array_merge($lines, $this->cashVatLines($item, $gross));
            $journalId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
                'entry_date' => $date, 'competence_date' => $date, 'entry_type' => 'WITHHOLDING', 'cause_id' => $causeId,
                'description' => 'Pagamento professionista ' . $item['party_name'], 'document_number' => $item['reference'],
                'counterparty' => $item['party_name'],
            ], $lines, 'WITHHOLDING_PAYMENT', $paymentId);
            $this->db->prepare('UPDATE payments SET journal_entry_id = ? WHERE id = ?')->execute([$journalId, $paymentId]);

            $settled = round((float) $item['settled_amount'] + $gross, 2);
            $status = $settled >= (float) $item['original_amount'] - .005 ? 'SETTLED' : 'PARTIAL';
            $this->db->prepare(
                'UPDATE accounting_open_items SET settled_amount = ?, status = ?, updated_by = ?, updated_at = NOW() WHERE id = ?'
            )->execute([$settled, $status, $this->userId, $openItemId]);
            if ($item['payment_schedule_id']) {
                $this->db->prepare(
                    "UPDATE payment_schedules SET paid_amount = LEAST(amount, paid_amount + ?),
                     status = CASE WHEN paid_amount + ? >= amount - 0.005 THEN 'PAID' ELSE 'PARTIAL' END, updated_at = NOW()
                     WHERE id = ? AND organization_id = ?"
                )->execute([$gross, $gross, $item['payment_schedule_id'], $this->organizationId]);
            }
            $this->refreshDocument((int) ($item['document_id'] ?? 0));
            if ($this->recognizeCashVat($item, $paymentId, $gross, $date) > 0) {
                (new VatService($this->db, $this->organizationId, $this->userId))->invalidateForDate($date);
            }
            $record = $this->db->prepare(
                "INSERT INTO withholding_records
                 (organization_id, supplier_id, document_id, payment_id, record_date, due_date, withholding_type,
                  gross_amount, taxable_percent, rate_percent, withholding_amount, social_security_amount, net_amount,
                  status, journal_entry_id, notes, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'WITHHELD', ?, ?, ?, ?, NOW(), NOW())"
            );
            $record->execute([
                $this->organizationId, $item['party_id'], $item['document_id'], $paymentId, $date, $dueDate, $type,
                $gross, $taxablePercent, $rate, $withholding, $social, $net, $journalId,
                $this->nullable($data['notes'] ?? null), $this->userId, $this->userId,
            ]);
            $recordId = (int) $this->db->lastInsertId();
            if ($dueDate) {
                $this->db->prepare(
                    "INSERT INTO tax_deadlines
                     (organization_id, due_date, deadline_type, description, reference_period, amount, status,
                      notes, created_by, updated_by, created_at, updated_at)
                     VALUES (?, ?, 'WITHHOLDING', ?, ?, ?, 'OPEN', ?, ?, ?, NOW(), NOW())"
                )->execute([
                    $this->organizationId, $dueDate, 'Versamento ritenuta ' . $item['party_name'], substr($date, 0, 7),
                    $withholding + $social, 'Ritenuta #' . $recordId, $this->userId, $this->userId,
                ]);
            }
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $recordId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function markPaid(int $recordId, string $date, ?int $bankAccountId): int
    {
        $date = $this->date($date);
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT * FROM withholding_records WHERE id = ? AND organization_id = ? AND status = 'WITHHELD' FOR UPDATE"
            );
            $statement->execute([$recordId, $this->organizationId]);
            $record = $statement->fetch();
            if (!$record) {
                throw new InvalidArgumentException('Ritenuta non trovata o già versata.');
            }
            $bank = $this->cashOrBankAccount($bankAccountId);
            $lines = [
                ['account_id' => $this->mapping('WITHHOLDING_PAYABLE'), 'debit' => $record['withholding_amount'], 'credit' => 0, 'description' => 'Versamento ritenuta'],
            ];
            if ((float) $record['social_security_amount'] > .005) {
                $lines[] = ['account_id' => $this->mapping('SOCIAL_SECURITY_PAYABLE'), 'debit' => $record['social_security_amount'], 'credit' => 0, 'description' => 'Versamento contributo'];
            }
            $total = round((float) $record['withholding_amount'] + (float) $record['social_security_amount'], 2);
            $lines[] = ['account_id' => $bank, 'debit' => 0, 'credit' => $total, 'description' => 'Versamento ritenute'];
            $journalId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
                'entry_date' => $date, 'competence_date' => $date, 'entry_type' => 'WITHHOLDING_PAYMENT',
                'description' => 'Versamento ritenuta #' . $recordId,
            ], $lines, 'WITHHOLDING_SETTLEMENT', $recordId);
            $this->db->prepare(
                "UPDATE withholding_records SET status = 'PAID', paid_at = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?"
            )->execute([$date, $this->userId, $recordId, $this->organizationId]);
            $this->db->prepare(
                "UPDATE tax_deadlines SET status = 'COMPLETED', completed_at = NOW(), updated_by = ?, updated_at = NOW()
                 WHERE organization_id = ? AND deadline_type = 'WITHHOLDING' AND notes = ? AND status <> 'COMPLETED'"
            )->execute([$this->userId, $this->organizationId, 'Ritenuta #' . $recordId]);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $journalId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function refreshDocument(int $documentId): void
    {
        if ($documentId <= 0) {
            return;
        }
        $statement = $this->db->prepare(
            "SELECT COALESCE(SUM(original_amount - settled_amount),0) FROM accounting_open_items
             WHERE organization_id = ? AND document_id = ? AND status <> 'CANCELLED'"
        );
        $statement->execute([$this->organizationId, $documentId]);
        $balance = max(0, round((float) $statement->fetchColumn(), 2));
        $this->db->prepare(
            "UPDATE documents SET balance_due = ?, status = CASE
                WHEN ? <= 0.005 THEN 'PAID' WHEN ? < ABS(total) THEN 'PARTIALLY_PAID'
                WHEN due_date < CURRENT_DATE() THEN 'OVERDUE' ELSE status END, updated_at = NOW()
             WHERE id = ? AND organization_id = ?"
        )->execute([$balance, $balance, $balance, $documentId, $this->organizationId]);
    }

    private function recognizeCashVat(array $item, int $paymentId, float $amount, string $date): int
    {
        if (empty($item['document_id'])) {
            return 0;
        }
        $statement = $this->db->prepare(
            "SELECT * FROM vat_movements WHERE organization_id = ? AND document_id = ? AND collectability IN ('CASH','DEFERRED')"
        );
        $statement->execute([$this->organizationId, $item['document_id']]);
        $ratio = min(1, $amount / $this->documentTotal((int) $item['document_id']));
        $insert = $this->db->prepare(
            'INSERT INTO vat_cash_events
             (organization_id, vat_movement_id, payment_id, recognition_date, recognized_taxable,
              recognized_vat_due, recognized_vat_credit, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())'
        );
        $count = 0;
        foreach ($statement->fetchAll() as $movement) {
            $insert->execute([
                $this->organizationId, $movement['id'], $paymentId, $date,
                round((float) $movement['taxable_amount'] * $ratio, 2),
                $movement['register_type'] === 'PURCHASES' ? 0 : round((float) $movement['vat_amount'] * $ratio, 2),
                $movement['register_type'] === 'PURCHASES' ? round((float) $movement['deductible_vat'] * $ratio, 2) : 0,
            ]);
            $count++;
        }
        return $count;
    }

    private function cashVatLines(array $item, float $amount): array
    {
        if (empty($item['document_id'])) {
            return [];
        }
        $statement = $this->db->prepare(
            "SELECT SUM(deductible_vat) FROM vat_movements
             WHERE organization_id = ? AND document_id = ? AND register_type = 'PURCHASES' AND collectability IN ('CASH','DEFERRED')"
        );
        $statement->execute([$this->organizationId, $item['document_id']]);
        $recognized = round((float) $statement->fetchColumn() * min(1, $amount / $this->documentTotal((int) $item['document_id'])), 2);
        return abs($recognized) <= .005 ? [] : [
            ['account_id' => $this->mapping('VAT_RECEIVABLE'), 'debit' => $recognized, 'credit' => 0, 'description' => 'Esigibilità IVA acquisti'],
            ['account_id' => $this->mapping('VAT_CLEARING'), 'debit' => 0, 'credit' => $recognized, 'description' => 'Esigibilità IVA acquisti'],
        ];
    }

    private function documentTotal(int $documentId): float
    {
        $statement = $this->db->prepare('SELECT ABS(total) FROM documents WHERE id = ? AND organization_id = ?');
        $statement->execute([$documentId, $this->organizationId]);
        return max(.01, (float) $statement->fetchColumn());
    }

    private function mapping(string $key): int
    {
        $statement = $this->db->prepare('SELECT account_id FROM accounting_account_mappings WHERE organization_id = ? AND mapping_key = ?');
        $statement->execute([$this->organizationId, $key]);
        $id = (int) $statement->fetchColumn();
        return $id > 0 ? $id : throw new RuntimeException('Manca il conto automatico “' . $key . '”.');
    }

    private function cashOrBankAccount(?int $bankAccountId): int
    {
        if ($bankAccountId) {
            $statement = $this->db->prepare('SELECT chart_account_id FROM bank_accounts WHERE id = ? AND organization_id = ? AND active = 1');
            $statement->execute([$bankAccountId, $this->organizationId]);
            if ($id = $statement->fetchColumn()) {
                return (int) $id;
            }
        }
        return $this->mapping($bankAccountId ? 'BANK' : 'CASH');
    }

    private function causeId(): ?int
    {
        $statement = $this->db->prepare(
            "SELECT id FROM accounting_causes WHERE organization_id = ? AND category = 'WITHHOLDING' AND active = 1 LIMIT 1"
        );
        $statement->execute([$this->organizationId]);
        return ($id = $statement->fetchColumn()) ? (int) $id : null;
    }

    private function percent(mixed $value): float
    {
        return min(100, max(0, $this->decimal($value)));
    }

    private function decimal(mixed $value): float
    {
        $value = trim((string) $value);
        if (str_contains($value, ',')) {
            $value = str_replace('.', '', $value);
            $value = str_replace(',', '.', $value);
        }
        return (float) $value;
    }

    private function date(string $value): string
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
        $errors = DateTimeImmutable::getLastErrors();
        if ($date === false || ($errors !== false && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))) {
            throw new InvalidArgumentException('Data non valida.');
        }
        return $date->format('Y-m-d');
    }

    private function nullable(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
    }
}
