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
                "SELECT id, document_type, number, document_date, counterparty_name, vat_collectability
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
            $registerId = $this->defaultRegisterId($register);
            $sign = $document['document_type'] === 'CREDIT_NOTE' ? -1 : 1;
            $statement = $this->db->prepare(
                "SELECT COALESCE(NULLIF(vat_code, ''), NULLIF(vat_nature, ''), CONCAT(REPLACE(FORMAT(vat_rate, 2), '.00', ''), '%')) AS vat_code,
                        MAX(vat_nature) AS vat_nature, MAX(vat_rate) AS vat_rate,
                        SUM(taxable_amount) AS taxable_amount, SUM(vat_amount) AS vat_amount
                 FROM document_lines WHERE document_id = ? AND organization_id = ?
                 GROUP BY COALESCE(NULLIF(vat_code, ''), NULLIF(vat_nature, ''), CONCAT(REPLACE(FORMAT(vat_rate, 2), '.00', ''), '%'))"
            );
            $statement->execute([$documentId, $this->organizationId]);
            $groups = $statement->fetchAll();

            $cashEvents = $this->db->prepare(
                'SELECT COUNT(*) FROM vat_cash_events e JOIN vat_movements m ON m.id = e.vat_movement_id
                 WHERE m.organization_id = ? AND m.document_id = ?'
            );
            $cashEvents->execute([$this->organizationId, $documentId]);
            if ((int) $cashEvents->fetchColumn() > 0) {
                throw new RuntimeException('Il documento ha già eventi IVA per cassa: stornare prima i pagamenti collegati.');
            }
            $this->db->prepare('DELETE FROM vat_movements WHERE organization_id = ? AND document_id = ?')
                ->execute([$this->organizationId, $documentId]);
            $insert = $this->db->prepare(
                'INSERT INTO vat_movements
                 (organization_id, document_id, source_type, register_type, vat_register_id, operation_type, collectability,
                  movement_date, tax_point_date, protocol_number, counterparty_name, description, vat_code,
                  taxable_amount, vat_amount, vat_due_amount, deductible_vat, deductibility_percent, pro_rata_amount,
                  period_year, period_month, created_by, created_at, updated_at)
                 VALUES (?, ?, \'DOCUMENT\', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $proRata = $this->proRataPercent();
            foreach ($groups as $group) {
                $vat = round((float) $group['vat_amount'] * $sign, 2);
                $operation = $this->operationType((string) ($group['vat_nature'] ?? ''), (string) ($document['vat_collectability'] ?? ''));
                $collectability = $this->collectability((string) ($document['vat_collectability'] ?? ''), $operation);
                if ($this->cashVatEnabled() && $collectability === 'IMMEDIATE') {
                    $collectability = 'CASH';
                    $operation = 'CASH';
                }
                $deductible = $register === 'PURCHASES' ? round($vat * $proRata / 100, 2) : 0.0;
                $proRataAmount = $register === 'PURCHASES' ? round($vat - $deductible, 2) : 0.0;
                $vatDue = match (true) {
                    $collectability === 'SPLIT' || in_array($collectability, ['CASH', 'DEFERRED'], true) => 0.0,
                    $register === 'PURCHASES' && $operation !== 'REVERSE_CHARGE' => 0.0,
                    default => $vat,
                };
                $insert->execute([
                    $this->organizationId, $documentId, $register, $registerId, $operation, $collectability,
                    $document['document_date'], in_array($collectability, ['IMMEDIATE', 'SPLIT'], true) ? $document['document_date'] : null,
                    $document['number'], $document['counterparty_name'], $this->documentDescription((string) $document['document_type']),
                    (string) ($group['vat_code'] ?: 'N/D'), round((float) $group['taxable_amount'] * $sign, 2), $vat,
                    $vatDue, $deductible, $proRata, $proRataAmount,
                    (int) $date->format('Y'), (int) $date->format('n'), $this->userId,
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
        $operation = strtoupper((string) ($data['operation_type'] ?? 'DOMESTIC'));
        $collectability = strtoupper((string) ($data['collectability'] ?? 'IMMEDIATE'));
        $date = $this->isoDate($data['movement_date'] ?? date('Y-m-d'));
        $taxable = $this->decimal($data['taxable_amount'] ?? 0);
        $vat = $this->decimal($data['vat_amount'] ?? 0);
        $deductibleInput = trim((string) ($data['deductible_vat'] ?? ''));
        $deductibility = min(100, max(0, $this->decimal($data['deductibility_percent'] ?? $this->proRataPercent())));
        $deductible = $register === 'PURCHASES'
            ? ($deductibleInput === '' ? round($vat * $deductibility / 100, 2) : $this->decimal($deductibleInput))
            : 0.0;
        $operations = ['DOMESTIC','REVERSE_CHARGE','SPLIT_PAYMENT','CASH','INTRA_EU','EXTRA_EU','MARGIN','EXEMPT','NON_TAXABLE','ADJUSTMENT'];
        if (!in_array($register, self::REGISTERS, true) || !in_array($operation, $operations, true)
            || !in_array($collectability, ['IMMEDIATE','DEFERRED','SPLIT','CASH'], true)
            || trim((string) ($data['vat_code'] ?? '')) === '') {
            throw new InvalidArgumentException('Registro e codice IVA sono obbligatori.');
        }
        $registerId = !empty($data['vat_register_id']) ? (int) $data['vat_register_id'] : $this->defaultRegisterId($register);
        $vatDue = $this->decimal($data['vat_due_amount'] ?? match (true) {
            $collectability === 'SPLIT' || in_array($collectability, ['CASH', 'DEFERRED'], true) => 0,
            $register === 'PURCHASES' && $operation !== 'REVERSE_CHARGE' => 0,
            default => $vat,
        });
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
                 (organization_id, document_id, source_type, register_type, vat_register_id, operation_type, collectability,
                  movement_date, tax_point_date, protocol_number, counterparty_name, description, vat_code,
                  taxable_amount, vat_amount, vat_due_amount, deductible_vat, deductibility_percent, pro_rata_amount,
                  period_year, period_month, created_by, created_at, updated_at)
                 VALUES (?, NULL, \'MANUAL\', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $statement->execute([
                $this->organizationId, $register, $registerId, $operation, $collectability, $date->format('Y-m-d'),
                in_array($collectability, ['IMMEDIATE', 'SPLIT'], true) ? $date->format('Y-m-d') : null,
                $this->nullValue($data['protocol_number'] ?? null),
                $this->nullValue($data['counterparty_name'] ?? null), $this->nullValue($data['description'] ?? null),
                trim((string) $data['vat_code']), round($taxable, 2), round($vat, 2), round($vatDue, 2), round($deductible, 2),
                $deductibility, round($register === 'PURCHASES' ? $vat - $deductible : 0, 2),
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

        $settings = $this->db->prepare('SELECT vat_periodicity FROM accounting_settings WHERE organization_id = ?');
        $settings->execute([$this->organizationId]);
        $configuredType = (string) ($settings->fetchColumn() ?: 'MONTHLY');
        if ($configuredType !== $periodType) {
            throw new InvalidArgumentException('La liquidazione non rispetta la periodicitÃ  IVA configurata.');
        }

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
                COALESCE(SUM(vat_debit), 0) AS vat_debit,
                COALESCE(SUM(vat_credit), 0) AS vat_credit
             FROM (
                SELECT vat_due_amount AS vat_debit,
                       CASE WHEN register_type = 'PURCHASES' AND collectability NOT IN ('CASH','DEFERRED') THEN deductible_vat ELSE 0 END AS vat_credit
                FROM vat_movements
                WHERE organization_id = ? AND period_year = ? AND period_month BETWEEN ? AND ? AND lipe_excluded = 0
                UNION ALL
                SELECT e.recognized_vat_due AS vat_debit, e.recognized_vat_credit AS vat_credit
                FROM vat_cash_events e
                WHERE e.organization_id = ? AND YEAR(e.recognition_date) = ? AND MONTH(e.recognition_date) BETWEEN ? AND ?
                UNION ALL
                SELECT vat_debit_delta AS vat_debit, vat_credit_delta AS vat_credit
                FROM vat_adjustments
                WHERE organization_id = ? AND fiscal_year = ? AND period_month BETWEEN ? AND ?
                  AND status IN ('APPROVED','POSTED')
             ) totals"
        );
        $summary->execute([
            $this->organizationId, $year, $monthFrom, $monthTo,
            $this->organizationId, $year, $monthFrom, $monthTo,
            $this->organizationId, $year, $monthFrom, $monthTo,
        ]);
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
                "SELECT register_type, vat_code, SUM(taxable_amount) AS taxable_amount,
                        SUM(vat_amount) AS vat_amount, SUM(deductible_vat) AS deductible_vat
                 FROM (
                    SELECT register_type, COALESCE(vat_code, 'N/D') AS vat_code, taxable_amount,
                           vat_due_amount AS vat_amount,
                           CASE WHEN register_type = 'PURCHASES' AND collectability NOT IN ('CASH','DEFERRED') THEN deductible_vat ELSE 0 END AS deductible_vat
                    FROM vat_movements
                    WHERE organization_id = ? AND period_year = ? AND period_month BETWEEN ? AND ? AND lipe_excluded = 0
                    UNION ALL
                    SELECT m.register_type, COALESCE(m.vat_code, 'N/D'), e.recognized_taxable,
                           e.recognized_vat_due, e.recognized_vat_credit
                    FROM vat_cash_events e JOIN vat_movements m ON m.id = e.vat_movement_id
                    WHERE e.organization_id = ? AND YEAR(e.recognition_date) = ? AND MONTH(e.recognition_date) BETWEEN ? AND ?
                 ) detail_rows
                 GROUP BY register_type, vat_code"
            );
            $details->execute([
                $this->organizationId, $year, $monthFrom, $monthTo,
                $this->organizationId, $year, $monthFrom, $monthTo,
            ]);
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

    public function invalidateForDate(string $date): void
    {
        $value = $this->isoDate($date);
        $this->assertPeriodOpen((int) $value->format('Y'), (int) $value->format('n'));
        $this->invalidateCalculatedPeriods((int) $value->format('Y'), (int) $value->format('n'));
    }

    private function assertPeriodOpen(int $year, int $month): void
    {
        $date = sprintf('%04d-%02d-01', $year, $month);
        $lock = $this->db->prepare(
            "SELECT COUNT(*) FROM accounting_period_locks
             WHERE organization_id = ? AND scope IN ('VAT','ACCOUNTING') AND unlocked_at IS NULL
               AND ? BETWEEN starts_on AND ends_on"
        );
        $lock->execute([$this->organizationId, $date]);
        if ((int) $lock->fetchColumn() > 0) {
            throw new RuntimeException('Il periodo IVA Ã¨ bloccato da una chiusura contabile o fiscale.');
        }
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

    private function defaultRegisterId(string $type): ?int
    {
        $statement = $this->db->prepare(
            'SELECT id FROM vat_registers WHERE organization_id = ? AND register_type = ? AND active = 1
             ORDER BY is_default DESC, id LIMIT 1'
        );
        $statement->execute([$this->organizationId, $type]);
        return ($id = $statement->fetchColumn()) ? (int) $id : null;
    }

    private function proRataPercent(): float
    {
        $statement = $this->db->prepare('SELECT pro_rata_percent FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        $value = $statement->fetchColumn();
        return $value === false ? 100.0 : min(100, max(0, (float) $value));
    }

    private function cashVatEnabled(): bool
    {
        $statement = $this->db->prepare('SELECT cash_vat_enabled FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        return (bool) $statement->fetchColumn();
    }

    private function operationType(string $nature, string $collectability): string
    {
        $nature = strtoupper($nature);
        return match (true) {
            str_starts_with($nature, 'N6') => 'REVERSE_CHARGE',
            strtoupper($collectability) === 'S' => 'SPLIT_PAYMENT',
            $nature === 'N4' => 'EXEMPT',
            str_starts_with($nature, 'N2') || str_starts_with($nature, 'N3') => 'NON_TAXABLE',
            default => 'DOMESTIC',
        };
    }

    private function collectability(string $value, string $operation): string
    {
        return match (strtoupper($value)) {
            'S' => 'SPLIT',
            'D' => 'DEFERRED',
            default => $operation === 'CASH' ? 'CASH' : 'IMMEDIATE',
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
