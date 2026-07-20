<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class VatService
{
    private const REGISTERS = ['SALES', 'PURCHASES', 'CORRISPETTIVI'];

    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId)
    {
    }

    public function syncDocuments(): int
    {
        $statement = $this->db->prepare(
            "SELECT id FROM documents
             WHERE organization_id = ?
               AND document_type IN ('SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE')
               AND status IN ('ISSUED','RECEIVED','PARTIALLY_PAID','PAID','OVERDUE')
             ORDER BY document_date, id"
        );
        $statement->execute([$this->organizationId]);
        $count = 0;
        foreach ($statement->fetchAll(PDO::FETCH_COLUMN) as $documentId) {
            $count += $this->syncDocument((int) $documentId);
        }
        return $count;
    }

    public function syncDocument(int $documentId): int
    {
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT id, document_type, number, document_date, counterparty_name
                 FROM documents WHERE id = ? AND organization_id = ? FOR UPDATE"
            );
            $statement->execute([$documentId, $this->organizationId]);
            $document = $statement->fetch();
            if (!$document || !in_array($document['document_type'], ['SALES_INVOICE', 'PURCHASE_INVOICE', 'CREDIT_NOTE'], true)) {
                if ($ownsTransaction) {
                    $this->db->commit();
                }
                return 0;
            }

            $date = new DateTimeImmutable((string) $document['document_date']);
            $this->assertPeriodOpen((int) $date->format('Y'), (int) $date->format('n'));
            $register = $document['document_type'] === 'PURCHASE_INVOICE' ? 'PURCHASES' : 'SALES';
            $sign = $document['document_type'] === 'CREDIT_NOTE' ? -1 : 1;
            $statement = $this->db->prepare(
                "SELECT COALESCE(NULLIF(vat_code, ''), NULLIF(vat_nature, ''), CONCAT(REPLACE(FORMAT(vat_rate, 2), '.00', ''), '%')) AS vat_code,
                        SUM(taxable_amount) AS taxable_amount, SUM(vat_amount) AS vat_amount
                 FROM document_lines WHERE document_id = ? AND organization_id = ?
                 GROUP BY COALESCE(NULLIF(vat_code, ''), NULLIF(vat_nature, ''), CONCAT(REPLACE(FORMAT(vat_rate, 2), '.00', ''), '%'))"
            );
            $statement->execute([$documentId, $this->organizationId]);
            $groups = $statement->fetchAll();

            $this->db->prepare('DELETE FROM vat_movements WHERE organization_id = ? AND document_id = ?')
                ->execute([$this->organizationId, $documentId]);
            $insert = $this->db->prepare(
                'INSERT INTO vat_movements
                 (organization_id, document_id, source_type, register_type, movement_date, protocol_number,
                  counterparty_name, description, vat_code, taxable_amount, vat_amount, deductible_vat,
                  period_year, period_month, created_by, created_at, updated_at)
                 VALUES (?, ?, \'DOCUMENT\', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            foreach ($groups as $group) {
                $vat = round((float) $group['vat_amount'] * $sign, 2);
                $insert->execute([
                    $this->organizationId, $documentId, $register, $document['document_date'], $document['number'],
                    $document['counterparty_name'], $this->documentDescription((string) $document['document_type']),
                    (string) ($group['vat_code'] ?: 'N/D'), round((float) $group['taxable_amount'] * $sign, 2), $vat,
                    $register === 'PURCHASES' ? $vat : 0, (int) $date->format('Y'), (int) $date->format('n'), $this->userId,
                ]);
            }
            $this->invalidateCalculatedPeriods((int) $date->format('Y'), (int) $date->format('n'));
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return count($groups);
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function saveManual(array $data): int
    {
        $register = strtoupper((string) ($data['register_type'] ?? ''));
        $date = $this->isoDate($data['movement_date'] ?? date('Y-m-d'));
        $taxable = $this->decimal($data['taxable_amount'] ?? 0);
        $vat = $this->decimal($data['vat_amount'] ?? 0);
        $deductibleInput = trim((string) ($data['deductible_vat'] ?? ''));
        $deductible = $register === 'PURCHASES'
            ? ($deductibleInput === '' ? $vat : $this->decimal($deductibleInput))
            : 0.0;
        if (!in_array($register, self::REGISTERS, true) || trim((string) ($data['vat_code'] ?? '')) === '') {
            throw new InvalidArgumentException('Registro e codice IVA sono obbligatori.');
        }
        if ($taxable == 0.0 && $vat == 0.0) {
            throw new InvalidArgumentException('Il movimento IVA non può avere imponibile e imposta entrambi a zero.');
        }
        if (abs($deductible) > abs($vat) + .005) {
            throw new InvalidArgumentException('L’IVA detraibile non può superare l’IVA del movimento.');
        }
        if ($deductible != 0.0 && $vat != 0.0 && ($deductible < 0) !== ($vat < 0)) {
            throw new InvalidArgumentException('IVA e IVA detraibile devono avere lo stesso segno.');
        }
        $this->assertPeriodOpen((int) $date->format('Y'), (int) $date->format('n'));
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                'INSERT INTO vat_movements
                 (organization_id, document_id, source_type, register_type, movement_date, protocol_number,
                  counterparty_name, description, vat_code, taxable_amount, vat_amount, deductible_vat,
                  period_year, period_month, created_by, created_at, updated_at)
                 VALUES (?, NULL, \'MANUAL\', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $statement->execute([
                $this->organizationId, $register, $date->format('Y-m-d'), $this->nullValue($data['protocol_number'] ?? null),
                $this->nullValue($data['counterparty_name'] ?? null), $this->nullValue($data['description'] ?? null),
                trim((string) $data['vat_code']), round($taxable, 2), round($vat, 2), round($deductible, 2),
                (int) $date->format('Y'), (int) $date->format('n'), $this->userId,
            ]);
            $movementId = (int) $this->db->lastInsertId();
            $this->invalidateCalculatedPeriods((int) $date->format('Y'), (int) $date->format('n'));
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $movementId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function deleteManual(int $movementId): bool
    {
        $statement = $this->db->prepare(
            "SELECT period_year, period_month FROM vat_movements
             WHERE id = ? AND organization_id = ? AND source_type = 'MANUAL' AND document_id IS NULL"
        );
        $statement->execute([$movementId, $this->organizationId]);
        $movement = $statement->fetch();
        if (!$movement) {
            return false;
        }
        $this->assertPeriodOpen((int) $movement['period_year'], (int) $movement['period_month']);
        $delete = $this->db->prepare('DELETE FROM vat_movements WHERE id = ? AND organization_id = ?');
        $delete->execute([$movementId, $this->organizationId]);
        $this->invalidateCalculatedPeriods((int) $movement['period_year'], (int) $movement['period_month']);
        return $delete->rowCount() > 0;
    }

    public function calculateSettlement(array $data): int
    {
        $periodType = strtoupper((string) ($data['period_type'] ?? 'MONTHLY'));
        $year = (int) ($data['period_year'] ?? date('Y'));
        $period = (int) ($data['period_number'] ?? date('n'));
        if (!in_array($periodType, ['MONTHLY', 'QUARTERLY'], true)
            || $year < 2000 || $year > 2200
            || ($periodType === 'MONTHLY' && ($period < 1 || $period > 12))
            || ($periodType === 'QUARTERLY' && ($period < 1 || $period > 4))) {
            throw new InvalidArgumentException('Periodo di liquidazione non valido.');
        }
        [$monthFrom, $monthTo] = $periodType === 'MONTHLY'
            ? [$period, $period]
            : [(($period - 1) * 3) + 1, $period * 3];

        $existing = $this->db->prepare(
            'SELECT id, status FROM vat_settlements
             WHERE organization_id = ? AND period_type = ? AND period_year = ? AND period_number = ?'
        );
        $existing->execute([$this->organizationId, $periodType, $year, $period]);
        $settlement = $existing->fetch();
        if ($settlement && in_array($settlement['status'], ['SUBMITTED', 'PAID'], true)) {
            throw new RuntimeException('La liquidazione è definitiva e non può essere ricalcolata.');
        }

        $summary = $this->db->prepare(
            "SELECT
                COALESCE(SUM(CASE WHEN register_type IN ('SALES','CORRISPETTIVI') THEN vat_amount ELSE 0 END), 0) AS vat_debit,
                COALESCE(SUM(CASE WHEN register_type = 'PURCHASES' THEN deductible_vat ELSE 0 END), 0) AS vat_credit
             FROM vat_movements
             WHERE organization_id = ? AND period_year = ? AND period_month BETWEEN ? AND ?"
        );
        $summary->execute([$this->organizationId, $year, $monthFrom, $monthTo]);
        $totals = $summary->fetch();
        $vatDebit = round((float) $totals['vat_debit'], 2);
        $vatCredit = round((float) $totals['vat_credit'], 2);
        $previousCredit = round(max(0, $this->decimal($data['previous_credit'] ?? 0)), 2);
        $interest = round(max(0, $this->decimal($data['interest_amount'] ?? 0)), 2);
        $balance = round($vatDebit - $vatCredit - $previousCredit + $interest, 2);

        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $upsert = $this->db->prepare(
                "INSERT INTO vat_settlements
                 (organization_id, period_type, period_year, period_number, vat_debit, vat_credit,
                  previous_credit, interest_amount, balance, status, payment_due_date, notes,
                  created_by, updated_by, calculated_at, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'CALCULATED', ?, ?, ?, ?, NOW(), NOW(), NOW())
                 ON DUPLICATE KEY UPDATE vat_debit = VALUES(vat_debit), vat_credit = VALUES(vat_credit),
                  previous_credit = VALUES(previous_credit), interest_amount = VALUES(interest_amount),
                  balance = VALUES(balance), status = 'CALCULATED', payment_due_date = VALUES(payment_due_date),
                  notes = VALUES(notes), updated_by = VALUES(updated_by), calculated_at = NOW(), updated_at = NOW()"
            );
            $upsert->execute([
                $this->organizationId, $periodType, $year, $period, $vatDebit, $vatCredit, $previousCredit,
                $interest, $balance, $this->nullValue($data['payment_due_date'] ?? null),
                $this->nullValue($data['notes'] ?? null), $this->userId, $this->userId,
            ]);
            if ($settlement) {
                $settlementId = (int) $settlement['id'];
            } else {
                $settlementId = (int) $this->db->lastInsertId();
            }
            $this->db->prepare('DELETE FROM vat_settlement_details WHERE settlement_id = ? AND organization_id = ?')
                ->execute([$settlementId, $this->organizationId]);
            $details = $this->db->prepare(
                'SELECT register_type, COALESCE(vat_code, \'N/D\') AS vat_code,
                        SUM(taxable_amount) AS taxable_amount, SUM(vat_amount) AS vat_amount,
                        SUM(deductible_vat) AS deductible_vat
                 FROM vat_movements
                 WHERE organization_id = ? AND period_year = ? AND period_month BETWEEN ? AND ?
                 GROUP BY register_type, COALESCE(vat_code, \'N/D\')'
            );
            $details->execute([$this->organizationId, $year, $monthFrom, $monthTo]);
            $insert = $this->db->prepare(
                'INSERT INTO vat_settlement_details
                 (organization_id, settlement_id, register_type, vat_code, taxable_amount, vat_amount, deductible_vat, created_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, NOW())'
            );
            foreach ($details->fetchAll() as $detail) {
                $insert->execute([
                    $this->organizationId, $settlementId, $detail['register_type'], $detail['vat_code'],
                    $detail['taxable_amount'], $detail['vat_amount'], $detail['deductible_vat'],
                ]);
            }
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $settlementId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function updateSettlementStatus(int $settlementId, string $status, ?string $paymentDate = null): void
    {
        $status = strtoupper($status);
        if (!in_array($status, ['CALCULATED', 'SUBMITTED', 'PAID'], true)) {
            throw new InvalidArgumentException('Stato liquidazione non valido.');
        }
        $statement = $this->db->prepare(
            "UPDATE vat_settlements SET status = ?, payment_date = ?, updated_by = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ? AND status <> 'DRAFT'"
        );
        $statement->execute([$status, $status === 'PAID' ? ($paymentDate ?: date('Y-m-d')) : null, $this->userId, $settlementId, $this->organizationId]);
        if ($statement->rowCount() === 0) {
            throw new InvalidArgumentException('Liquidazione non trovata o non calcolata.');
        }
    }

    private function assertPeriodOpen(int $year, int $month): void
    {
        $quarter = (int) ceil($month / 3);
        $statement = $this->db->prepare(
            "SELECT COUNT(*) FROM vat_settlements
             WHERE organization_id = ? AND period_year = ? AND status IN ('SUBMITTED','PAID')
               AND ((period_type = 'MONTHLY' AND period_number = ?)
                 OR (period_type = 'QUARTERLY' AND period_number = ?))"
        );
        $statement->execute([$this->organizationId, $year, $month, $quarter]);
        if ((int) $statement->fetchColumn() > 0) {
            throw new RuntimeException('Il periodo IVA è definitivo: riapri la liquidazione prima di modificare i movimenti.');
        }
    }

    private function invalidateCalculatedPeriods(int $year, int $month): void
    {
        $quarter = (int) ceil($month / 3);
        $statement = $this->db->prepare(
            "SELECT id FROM vat_settlements
             WHERE organization_id = ? AND period_year = ? AND status = 'CALCULATED'
               AND ((period_type = 'MONTHLY' AND period_number = ?)
                 OR (period_type = 'QUARTERLY' AND period_number = ?))"
        );
        $statement->execute([$this->organizationId, $year, $month, $quarter]);
        $ids = array_map('intval', $statement->fetchAll(PDO::FETCH_COLUMN));
        if ($ids === []) {
            return;
        }
        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        $update = $this->db->prepare(
            "UPDATE vat_settlements SET status = 'DRAFT', vat_debit = 0, vat_credit = 0,
             previous_credit = 0, interest_amount = 0, balance = 0, calculated_at = NULL, updated_at = NOW()
             WHERE organization_id = ? AND id IN ({$placeholders})"
        );
        $update->execute(array_merge([$this->organizationId], $ids));
        $delete = $this->db->prepare(
            "DELETE FROM vat_settlement_details WHERE organization_id = ? AND settlement_id IN ({$placeholders})"
        );
        $delete->execute(array_merge([$this->organizationId], $ids));
    }

    private function documentDescription(string $type): string
    {
        return match ($type) {
            'SALES_INVOICE' => 'Fattura di vendita',
            'PURCHASE_INVOICE' => 'Fattura di acquisto',
            'CREDIT_NOTE' => 'Nota di credito',
            default => 'Documento',
        };
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

    private function nullValue(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
    }

    private function isoDate(mixed $value): DateTimeImmutable
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', trim((string) $value));
        $errors = DateTimeImmutable::getLastErrors();
        if ($date === false || ($errors !== false && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))) {
            throw new InvalidArgumentException('Data movimento IVA non valida.');
        }
        return $date;
    }
}
