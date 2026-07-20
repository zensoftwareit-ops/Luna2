<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class ReceivablesService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function syncDocuments(): int
    {
        $statement = $this->db->prepare(
            "SELECT id, document_type, number, document_date, due_date, counterparty_type, counterparty_id,
                    counterparty_name, total, balance_due, currency, status
             FROM documents
             WHERE organization_id = ? AND document_type IN ('SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE')
               AND status NOT IN ('DRAFT','CANCELLED') ORDER BY document_date, id"
        );
        $statement->execute([$this->organizationId]);
        $created = 0;
        foreach ($statement->fetchAll() as $document) {
            $created += $this->syncDocument($document);
        }
        return $created;
    }

    public function recordPayment(array $data): int
    {
        $openItemId = (int) ($data['open_item_id'] ?? 0);
        $date = $this->date((string) ($data['payment_date'] ?? ''));
        $amount = round($this->decimal($data['amount'] ?? 0), 2);
        if ($openItemId <= 0 || $amount <= 0) {
            throw new InvalidArgumentException('Partita e importo sono obbligatori.');
        }
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT * FROM accounting_open_items
                 WHERE id = ? AND organization_id = ? AND status IN ('OPEN','PARTIAL','OVERDUE','DISPUTED') FOR UPDATE"
            );
            $statement->execute([$openItemId, $this->organizationId]);
            $item = $statement->fetch();
            if (!$item) {
                throw new InvalidArgumentException('Partita non trovata o già chiusa.');
            }
            $outstanding = round((float) $item['original_amount'] - (float) $item['settled_amount'], 2);
            if ($amount > $outstanding + .005) {
                throw new InvalidArgumentException(sprintf('L’importo supera il residuo di %.2f euro.', $outstanding));
            }
            $paymentType = $item['direction'] === 'RECEIVABLE' ? 'RECEIPT' : 'PAYMENT';
            $bankAccountId = !empty($data['bank_account_id']) ? (int) $data['bank_account_id'] : null;
            $cashAccountId = $this->cashOrBankAccount($bankAccountId);
            $causeId = $this->causeId($paymentType === 'RECEIPT' ? 'RECEIPT' : 'PAYMENT');

            $insert = $this->db->prepare(
                "INSERT INTO payments
                 (organization_id, document_id, payment_schedule_id, payment_type, customer_id, supplier_id,
                  bank_account_id, cause_id, payment_date, amount, currency, bank_amount, method, status, reference_number,
                  bank_name, description, reconciled, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'POSTED', ?, ?, ?, 0, ?, ?, NOW(), NOW())"
            );
            $insert->execute([
                $this->organizationId, $item['document_id'], $item['payment_schedule_id'], $paymentType,
                $item['party_type'] === 'CUSTOMER' ? $item['party_id'] : null,
                $item['party_type'] === 'SUPPLIER' ? $item['party_id'] : null,
                $bankAccountId, $causeId, $date, $amount, $item['currency'], $amount,
                trim((string) ($data['method'] ?? 'BANK_TRANSFER')) ?: 'BANK_TRANSFER',
                $this->nullable($data['reference_number'] ?? null), $this->nullable($data['bank_name'] ?? null),
                $this->nullable($data['description'] ?? null), $this->userId, $this->userId,
            ]);
            $paymentId = (int) $this->db->lastInsertId();

            $this->db->prepare(
                'INSERT INTO payment_allocations (organization_id, payment_id, open_item_id, amount, created_by, created_at)
                 VALUES (?, ?, ?, ?, ?, NOW())'
            )->execute([$this->organizationId, $paymentId, $openItemId, $amount, $this->userId]);

            $newSettled = round((float) $item['settled_amount'] + $amount, 2);
            $newStatus = $newSettled >= (float) $item['original_amount'] - .005 ? 'SETTLED' : 'PARTIAL';
            $this->db->prepare(
                'UPDATE accounting_open_items SET settled_amount = ?, status = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([$newSettled, $newStatus, $this->userId, $openItemId, $this->organizationId]);

            $partyAccountId = (int) $item['account_id'];
            $lines = $paymentType === 'RECEIPT'
                ? [
                    ['account_id' => $cashAccountId, 'debit' => $amount, 'credit' => 0, 'description' => 'Incasso ' . $item['party_name']],
                    ['account_id' => $partyAccountId, 'debit' => 0, 'credit' => $amount, 'description' => 'Chiusura credito ' . ($item['reference'] ?: '')],
                ]
                : [
                    ['account_id' => $partyAccountId, 'debit' => $amount, 'credit' => 0, 'description' => 'Chiusura debito ' . ($item['reference'] ?: '')],
                    ['account_id' => $cashAccountId, 'debit' => 0, 'credit' => $amount, 'description' => 'Pagamento ' . $item['party_name']],
                ];
            $lines = array_merge($lines, $this->cashVatLines($item, $amount));
            $entryId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
                'entry_date' => $date, 'competence_date' => $date, 'entry_type' => $paymentType,
                'cause_id' => $causeId, 'description' => ($paymentType === 'RECEIPT' ? 'Incasso ' : 'Pagamento ') . $item['party_name'],
                'document_number' => $item['reference'], 'counterparty' => $item['party_name'],
            ], $lines, 'PAYMENT', $paymentId);
            $this->db->prepare('UPDATE payments SET journal_entry_id = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$entryId, $paymentId, $this->organizationId]);

            $this->updateScheduleAndDocument($item, $amount, $date);
            if ($this->recognizeCashVat($item, $paymentId, $amount, $date) > 0) {
                (new VatService($this->db, $this->organizationId, $this->userId))->invalidateForDate($date);
            }
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $paymentId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function reversePayment(int $paymentId, string $reversalDate): int
    {
        $date = $this->date($reversalDate);
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT p.*, a.open_item_id, a.amount AS allocated_amount
                 FROM payments p JOIN payment_allocations a ON a.payment_id = p.id
                 WHERE p.id = ? AND p.organization_id = ? AND p.status = 'POSTED' FOR UPDATE"
            );
            $statement->execute([$paymentId, $this->organizationId]);
            $payment = $statement->fetch();
            if (!$payment) {
                throw new InvalidArgumentException('Pagamento non trovato o già stornato.');
            }
            $links = $this->db->prepare('SELECT COUNT(*) FROM reconciliation_links WHERE organization_id = ? AND payment_id = ?');
            $links->execute([$this->organizationId, $paymentId]);
            if ((int) $links->fetchColumn() > 0) {
                throw new RuntimeException('Il pagamento è riconciliato: annulla prima la riconciliazione bancaria.');
            }
            $item = $this->db->prepare('SELECT * FROM accounting_open_items WHERE id = ? AND organization_id = ? FOR UPDATE');
            $item->execute([$payment['open_item_id'], $this->organizationId]);
            $openItem = $item->fetch();
            if (!$openItem) {
                throw new RuntimeException('Partita collegata non trovata.');
            }
            $amount = (float) $payment['allocated_amount'];
            $cashAccountId = $this->cashOrBankAccount($payment['bank_account_id'] ? (int) $payment['bank_account_id'] : null);
            $partyAccountId = (int) $openItem['account_id'];
            $lines = $payment['payment_type'] === 'RECEIPT'
                ? [
                    ['account_id' => $partyAccountId, 'debit' => $amount, 'credit' => 0, 'description' => 'Storno incasso'],
                    ['account_id' => $cashAccountId, 'debit' => 0, 'credit' => $amount, 'description' => 'Storno incasso'],
                ]
                : [
                    ['account_id' => $cashAccountId, 'debit' => $amount, 'credit' => 0, 'description' => 'Storno pagamento'],
                    ['account_id' => $partyAccountId, 'debit' => 0, 'credit' => $amount, 'description' => 'Storno pagamento'],
                ];
            $entryId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
                'entry_date' => $date, 'competence_date' => $date, 'entry_type' => 'PAYMENT_REVERSAL',
                'description' => 'Storno pagamento #' . $paymentId, 'counterparty' => $openItem['party_name'],
            ], $lines, 'PAYMENT_REVERSAL', $paymentId);
            $settled = max(0, round((float) $openItem['settled_amount'] - $amount, 2));
            $status = $settled <= .005 ? ((string) $openItem['due_date'] < date('Y-m-d') ? 'OVERDUE' : 'OPEN') : 'PARTIAL';
            $this->db->prepare('UPDATE accounting_open_items SET settled_amount = ?, status = ?, updated_by = ?, updated_at = NOW() WHERE id = ?')
                ->execute([$settled, $status, $this->userId, $openItem['id']]);
            $this->db->prepare("UPDATE payments SET status = 'REVERSED', updated_by = ?, updated_at = NOW() WHERE id = ?")
                ->execute([$this->userId, $paymentId]);
            $cashDates = $this->db->prepare('SELECT DISTINCT recognition_date FROM vat_cash_events WHERE organization_id = ? AND payment_id = ?');
            $cashDates->execute([$this->organizationId, $paymentId]);
            foreach ($cashDates->fetchAll(PDO::FETCH_COLUMN) as $cashDate) {
                (new VatService($this->db, $this->organizationId, $this->userId))->invalidateForDate((string) $cashDate);
            }
            $this->db->prepare('DELETE FROM vat_cash_events WHERE organization_id = ? AND payment_id = ?')
                ->execute([$this->organizationId, $paymentId]);
            if ($payment['payment_schedule_id']) {
                $this->db->prepare(
                    "UPDATE payment_schedules SET paid_amount = GREATEST(0, paid_amount - ?),
                     status = CASE WHEN paid_amount - ? <= 0 THEN IF(due_date < CURRENT_DATE(), 'OVERDUE', 'OPEN') ELSE 'PARTIAL' END,
                     updated_at = NOW() WHERE id = ? AND organization_id = ?"
                )->execute([$amount, $amount, $payment['payment_schedule_id'], $this->organizationId]);
            }
            $this->refreshDocument((int) ($payment['document_id'] ?? 0));
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

    private function syncDocument(array $document): int
    {
        $statement = $this->db->prepare('SELECT * FROM payment_schedules WHERE organization_id = ? AND document_id = ? ORDER BY installment_number');
        $statement->execute([$this->organizationId, $document['id']]);
        $schedules = $statement->fetchAll();
        if ($schedules === []) {
            $amount = abs((float) ($document['total'] ?? 0));
            if ($amount <= .005) {
                return 0;
            }
            $dueDate = $document['due_date'] ?: $document['document_date'];
            $this->db->prepare(
                'INSERT INTO payment_schedules
                 (organization_id, document_id, installment_number, due_date, amount, paid_amount, status, created_at, updated_at)
                 VALUES (?, ?, 1, ?, ?, 0, ?, NOW(), NOW())'
            )->execute([$this->organizationId, $document['id'], $dueDate, $amount, $dueDate < date('Y-m-d') ? 'OVERDUE' : 'OPEN']);
            $statement->execute([$this->organizationId, $document['id']]);
            $schedules = $statement->fetchAll();
        }
        $direction = $document['document_type'] === 'PURCHASE_INVOICE' ? 'PAYABLE' : 'RECEIVABLE';
        if ($document['document_type'] === 'CREDIT_NOTE') {
            $direction = 'PAYABLE';
        }
        $partyType = $document['counterparty_type'] === 'SUPPLIER' ? 'SUPPLIER' : 'CUSTOMER';
        $accountId = $this->mapping($direction === 'RECEIVABLE' ? 'TRADE_RECEIVABLES' : 'TRADE_PAYABLES');
        $insert = $this->db->prepare(
            'INSERT INTO accounting_open_items
             (organization_id, direction, party_type, party_id, party_name, document_id, payment_schedule_id,
              account_id, reference, issue_date, due_date, original_amount, settled_amount, currency, status,
              created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $created = 0;
        foreach ($schedules as $schedule) {
            $exists = $this->db->prepare('SELECT id FROM accounting_open_items WHERE organization_id = ? AND payment_schedule_id = ?');
            $exists->execute([$this->organizationId, $schedule['id']]);
            if ($exists->fetchColumn()) {
                continue;
            }
            $settled = min((float) $schedule['amount'], (float) $schedule['paid_amount']);
            $status = $settled >= (float) $schedule['amount'] - .005 ? 'SETTLED'
                : ($settled > .005 ? 'PARTIAL' : ((string) $schedule['due_date'] < date('Y-m-d') ? 'OVERDUE' : 'OPEN'));
            $insert->execute([
                $this->organizationId, $direction, $partyType, $document['counterparty_id'], $document['counterparty_name'],
                $document['id'], $schedule['id'], $accountId, $document['number'], $document['document_date'],
                $schedule['due_date'], abs((float) $schedule['amount']), $settled, $document['currency'], $status,
                $this->userId, $this->userId,
            ]);
            $created++;
        }
        return $created;
    }

    private function updateScheduleAndDocument(array $item, float $amount, string $date): void
    {
        if ($item['payment_schedule_id']) {
            $this->db->prepare(
                "UPDATE payment_schedules SET paid_amount = LEAST(amount, paid_amount + ?),
                 status = CASE WHEN paid_amount + ? >= amount - 0.005 THEN 'PAID' ELSE 'PARTIAL' END,
                 updated_at = NOW() WHERE id = ? AND organization_id = ?"
            )->execute([$amount, $amount, $item['payment_schedule_id'], $this->organizationId]);
        }
        $this->refreshDocument((int) ($item['document_id'] ?? 0));
    }

    private function refreshDocument(int $documentId): void
    {
        if ($documentId <= 0) {
            return;
        }
        $statement = $this->db->prepare(
            'SELECT COALESCE(SUM(original_amount - settled_amount), 0) FROM accounting_open_items
             WHERE organization_id = ? AND document_id = ? AND status <> \'CANCELLED\''
        );
        $statement->execute([$this->organizationId, $documentId]);
        $balance = max(0, round((float) $statement->fetchColumn(), 2));
        $this->db->prepare(
            "UPDATE documents SET balance_due = ?, status = CASE
                WHEN ? <= 0.005 THEN 'PAID'
                WHEN ? < ABS(total) THEN 'PARTIALLY_PAID'
                WHEN due_date < CURRENT_DATE() THEN 'OVERDUE'
                ELSE status END, updated_at = NOW()
             WHERE id = ? AND organization_id = ?"
        )->execute([$balance, $balance, $balance, $documentId, $this->organizationId]);
    }

    private function recognizeCashVat(array $item, int $paymentId, float $amount, string $date): int
    {
        if (!$item['document_id']) {
            return 0;
        }
        $statement = $this->db->prepare(
            "SELECT * FROM vat_movements
             WHERE organization_id = ? AND document_id = ? AND collectability IN ('CASH','DEFERRED')"
        );
        $statement->execute([$this->organizationId, $item['document_id']]);
        $ratio = min(1, $amount / $this->documentTotal((int) $item['document_id']));
        $insert = $this->db->prepare(
            'INSERT INTO vat_cash_events
             (organization_id, vat_movement_id, payment_id, recognition_date, recognized_taxable,
              recognized_vat_due, recognized_vat_credit, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW())'
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
            "SELECT register_type, SUM(vat_amount) AS vat_amount, SUM(deductible_vat) AS deductible_vat
             FROM vat_movements WHERE organization_id = ? AND document_id = ? AND collectability IN ('CASH','DEFERRED')
             GROUP BY register_type"
        );
        $statement->execute([$this->organizationId, $item['document_id']]);
        $ratio = min(1, $amount / $this->documentTotal((int) $item['document_id']));
        $lines = [];
        foreach ($statement->fetchAll() as $row) {
            $recognized = round((float) ($row['register_type'] === 'PURCHASES' ? $row['deductible_vat'] : $row['vat_amount']) * $ratio, 2);
            if (abs($recognized) <= .005) {
                continue;
            }
            if ($row['register_type'] === 'PURCHASES') {
                $lines[] = ['account_id' => $this->mapping('VAT_RECEIVABLE'), 'debit' => $recognized, 'credit' => 0, 'description' => 'Esigibilità IVA acquisti'];
                $lines[] = ['account_id' => $this->mapping('VAT_CLEARING'), 'debit' => 0, 'credit' => $recognized, 'description' => 'Esigibilità IVA acquisti'];
            } else {
                $lines[] = ['account_id' => $this->mapping('VAT_CLEARING'), 'debit' => $recognized, 'credit' => 0, 'description' => 'Esigibilità IVA vendite'];
                $lines[] = ['account_id' => $this->mapping('VAT_PAYABLE'), 'debit' => 0, 'credit' => $recognized, 'description' => 'Esigibilità IVA vendite'];
            }
        }
        return $lines;
    }

    private function documentTotal(int $documentId): float
    {
        $statement = $this->db->prepare('SELECT ABS(total) FROM documents WHERE id = ? AND organization_id = ?');
        $statement->execute([$documentId, $this->organizationId]);
        return max(.01, (float) $statement->fetchColumn());
    }

    private function mapping(string $key): int
    {
        $statement = $this->db->prepare(
            'SELECT a.id FROM accounting_account_mappings m JOIN chart_of_accounts a ON a.id = m.account_id
             WHERE m.organization_id = ? AND m.mapping_key = ? AND a.active = 1 AND a.is_postable = 1'
        );
        $statement->execute([$this->organizationId, $key]);
        $id = (int) $statement->fetchColumn();
        return $id > 0 ? $id : throw new RuntimeException('Manca il collegamento contabile “' . $key . '”.');
    }

    private function cashOrBankAccount(?int $bankAccountId): int
    {
        if ($bankAccountId !== null) {
            $statement = $this->db->prepare(
                'SELECT chart_account_id FROM bank_accounts WHERE id = ? AND organization_id = ? AND active = 1'
            );
            $statement->execute([$bankAccountId, $this->organizationId]);
            if ($id = $statement->fetchColumn()) {
                return (int) $id;
            }
        }
        return $this->mapping($bankAccountId === null ? 'CASH' : 'BANK');
    }

    private function causeId(string $category): ?int
    {
        $statement = $this->db->prepare(
            'SELECT id FROM accounting_causes WHERE organization_id = ? AND category = ? AND active = 1 ORDER BY automatic DESC, id LIMIT 1'
        );
        $statement->execute([$this->organizationId, $category]);
        return ($id = $statement->fetchColumn()) ? (int) $id : null;
    }

    private function nullable(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
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
}
