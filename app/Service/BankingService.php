<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class BankingService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function saveAccount(array $data, ?int $accountId = null): int
    {
        $name = trim((string) ($data['name'] ?? ''));
        $iban = strtoupper(str_replace(' ', '', (string) ($data['iban'] ?? '')));
        $chartAccountId = !empty($data['chart_account_id']) ? (int) $data['chart_account_id'] : null;
        if ($name === '' || !preg_match('/^[A-Z]{2}[A-Z0-9]{13,32}$/', $iban)) {
            throw new InvalidArgumentException('Nome o IBAN del conto bancario non validi.');
        }
        if ($chartAccountId !== null) {
            $statement = $this->db->prepare(
                "SELECT COUNT(*) FROM chart_of_accounts WHERE id = ? AND organization_id = ? AND active = 1 AND is_postable = 1"
            );
            $statement->execute([$chartAccountId, $this->organizationId]);
            if (!(int) $statement->fetchColumn()) {
                throw new InvalidArgumentException('Conto contabile bancario non valido.');
            }
        }
        $values = [
            $name, $iban, $this->nullable($data['bic'] ?? null),
            strtoupper(trim((string) ($data['currency'] ?? 'EUR'))) ?: 'EUR', $chartAccountId,
            isset($data['active']) ? 1 : 0,
        ];
        if ($accountId !== null) {
            $statement = $this->db->prepare(
                'UPDATE bank_accounts SET name = ?, iban = ?, bic = ?, currency = ?, chart_account_id = ?, active = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$accountId, $this->organizationId]));
            return $accountId;
        }
        $statement = $this->db->prepare(
            'INSERT INTO bank_accounts (organization_id, name, iban, bic, currency, chart_account_id, active, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge([$this->organizationId], $values));
        return (int) $this->db->lastInsertId();
    }

    public function saveTransaction(array $data): int
    {
        $bankAccountId = (int) ($data['bank_account_id'] ?? 0);
        $bookingDate = $this->date((string) ($data['booking_date'] ?? ''));
        $amount = round($this->decimal($data['amount'] ?? 0), 2);
        $description = trim((string) ($data['description'] ?? ''));
        if ($bankAccountId <= 0 || abs($amount) <= .005 || $description === '') {
            throw new InvalidArgumentException('Movimento bancario non valido.');
        }
        $account = $this->db->prepare('SELECT id, currency FROM bank_accounts WHERE id = ? AND organization_id = ? AND active = 1');
        $account->execute([$bankAccountId, $this->organizationId]);
        $bank = $account->fetch();
        if (!$bank) {
            throw new InvalidArgumentException('Conto bancario non trovato.');
        }
        $externalId = $this->nullable($data['external_id'] ?? null);
        $hash = hash('sha256', implode('|', [
            $bankAccountId, $bookingDate, number_format($amount, 2, '.', ''),
            trim((string) ($data['reference'] ?? '')), $description,
        ]));
        $statement = $this->db->prepare(
            "INSERT INTO bank_transactions
             (organization_id, bank_account_id, booking_date, value_date, amount, currency, description,
              counterparty, reference, external_id, import_hash, reconciliation_status, reconciled_amount, notes,
              bank_statement_import_id, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UNMATCHED', 0, ?, ?, NOW())"
        );
        try {
            $statement->execute([
                $this->organizationId, $bankAccountId, $bookingDate,
                !empty($data['value_date']) ? $this->date((string) $data['value_date']) : null,
                $amount, $bank['currency'], $description, $this->nullable($data['counterparty'] ?? null),
                $this->nullable($data['reference'] ?? null), $externalId, $hash, $this->nullable($data['notes'] ?? null),
                !empty($data['bank_statement_import_id']) ? (int) $data['bank_statement_import_id'] : null,
            ]);
        } catch (Throwable $exception) {
            if (str_contains(strtolower($exception->getMessage()), 'duplicate')) {
                throw new InvalidArgumentException('Il movimento bancario risulta già importato.');
            }
            throw $exception;
        }
        return (int) $this->db->lastInsertId();
    }

    public function reconcile(int $transactionId, int $paymentId, float $amount): void
    {
        $amount = round(abs($amount), 2);
        if ($amount <= .005) {
            throw new InvalidArgumentException('Importo di riconciliazione non valido.');
        }
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                'SELECT * FROM bank_transactions WHERE id = ? AND organization_id = ? FOR UPDATE'
            );
            $statement->execute([$transactionId, $this->organizationId]);
            $transaction = $statement->fetch();
            $paymentStatement = $this->db->prepare(
                "SELECT * FROM payments WHERE id = ? AND organization_id = ? AND status = 'POSTED' FOR UPDATE"
            );
            $paymentStatement->execute([$paymentId, $this->organizationId]);
            $payment = $paymentStatement->fetch();
            if (!$transaction || !$payment) {
                throw new InvalidArgumentException('Movimento o pagamento non trovato.');
            }
            if (!empty($payment['bank_account_id']) && (int) $payment['bank_account_id'] !== (int) $transaction['bank_account_id']) {
                throw new InvalidArgumentException('Il movimento e il pagamento appartengono a conti bancari diversi.');
            }
            $transactionResidual = abs((float) $transaction['amount']) - (float) $transaction['reconciled_amount'];
            $paymentAmount = abs((float) ($payment['bank_amount'] ?: $payment['amount']));
            $already = $this->db->prepare('SELECT COALESCE(SUM(matched_amount),0) FROM reconciliation_links WHERE organization_id = ? AND payment_id = ?');
            $already->execute([$this->organizationId, $paymentId]);
            $paymentResidual = $paymentAmount - (float) $already->fetchColumn();
            if ($amount > $transactionResidual + .005 || $amount > $paymentResidual + .005) {
                throw new InvalidArgumentException('L’importo supera il residuo del movimento o del pagamento.');
            }
            if (($payment['payment_type'] === 'RECEIPT' && (float) $transaction['amount'] < 0)
                || ($payment['payment_type'] === 'PAYMENT' && (float) $transaction['amount'] > 0)) {
                throw new InvalidArgumentException('Il segno del movimento non è coerente con incasso/pagamento.');
            }
            $this->db->prepare(
                'INSERT INTO reconciliation_links
                 (organization_id, bank_transaction_id, payment_id, journal_entry_id, matched_amount, matched_by, matched_at)
                 VALUES (?, ?, ?, ?, ?, ?, NOW())'
            )->execute([
                $this->organizationId, $transactionId, $paymentId, $payment['journal_entry_id'], $amount, $this->userId,
            ]);
            $newMatched = round((float) $transaction['reconciled_amount'] + $amount, 2);
            $status = $newMatched >= abs((float) $transaction['amount']) - .005 ? 'MATCHED' : 'PARTIAL';
            $this->db->prepare(
                'UPDATE bank_transactions SET reconciled_amount = ?, reconciliation_status = ?, journal_entry_id = COALESCE(journal_entry_id, ?)
                 WHERE id = ? AND organization_id = ?'
            )->execute([$newMatched, $status, $payment['journal_entry_id'], $transactionId, $this->organizationId]);
            if ($amount >= $paymentResidual - .005) {
                $this->db->prepare('UPDATE payments SET reconciled = 1, reconciled_at = NOW(), updated_by = ?, updated_at = NOW() WHERE id = ?')
                    ->execute([$this->userId, $paymentId]);
            }
            if ($ownsTransaction) {
                $this->db->commit();
            }
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function unreconcile(int $linkId): void
    {
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                'SELECT * FROM reconciliation_links WHERE id = ? AND organization_id = ? FOR UPDATE'
            );
            $statement->execute([$linkId, $this->organizationId]);
            $link = $statement->fetch();
            if (!$link) {
                throw new InvalidArgumentException('Riconciliazione non trovata.');
            }
            $this->db->prepare('DELETE FROM reconciliation_links WHERE id = ? AND organization_id = ?')
                ->execute([$linkId, $this->organizationId]);
            $sum = $this->db->prepare('SELECT COALESCE(SUM(matched_amount),0) FROM reconciliation_links WHERE organization_id = ? AND bank_transaction_id = ?');
            $sum->execute([$this->organizationId, $link['bank_transaction_id']]);
            $matched = round((float) $sum->fetchColumn(), 2);
            $transaction = $this->db->prepare('SELECT amount FROM bank_transactions WHERE id = ? AND organization_id = ?');
            $transaction->execute([$link['bank_transaction_id'], $this->organizationId]);
            $total = abs((float) $transaction->fetchColumn());
            $status = $matched <= .005 ? 'UNMATCHED' : ($matched >= $total - .005 ? 'MATCHED' : 'PARTIAL');
            $this->db->prepare('UPDATE bank_transactions SET reconciled_amount = ?, reconciliation_status = ? WHERE id = ? AND organization_id = ?')
                ->execute([$matched, $status, $link['bank_transaction_id'], $this->organizationId]);
            $paymentMatched = $this->db->prepare('SELECT COALESCE(SUM(matched_amount),0) FROM reconciliation_links WHERE organization_id = ? AND payment_id = ?');
            $paymentMatched->execute([$this->organizationId, $link['payment_id']]);
            $matchedPayment = round((float) $paymentMatched->fetchColumn(), 2);
            $payment = $this->db->prepare('SELECT ABS(COALESCE(bank_amount, amount)) FROM payments WHERE id = ? AND organization_id = ?');
            $payment->execute([$link['payment_id'], $this->organizationId]);
            $paymentTotal = (float) $payment->fetchColumn();
            $isReconciled = $matchedPayment >= $paymentTotal - .005;
            $this->db->prepare('UPDATE payments SET reconciled = ?, reconciled_at = IF(? = 1, reconciled_at, NULL), updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                ->execute([$isReconciled ? 1 : 0, $isReconciled ? 1 : 0, $this->userId, $link['payment_id'], $this->organizationId]);
            if ($ownsTransaction) {
                $this->db->commit();
            }
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
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

    private function decimal(mixed $value): float
    {
        $value = trim((string) $value);
        if (str_contains($value, ',')) {
            $value = str_replace('.', '', $value);
            $value = str_replace(',', '.', $value);
        }
        return (float) $value;
    }

    private function nullable(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
    }
}
