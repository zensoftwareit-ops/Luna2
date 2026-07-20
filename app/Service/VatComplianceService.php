<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use RuntimeException;

final class VatComplianceService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function saveAdjustment(array $data): int
    {
        $year = (int) ($data['fiscal_year'] ?? date('Y'));
        $month = !empty($data['period_month']) ? (int) $data['period_month'] : null;
        $type = strtoupper((string) ($data['adjustment_type'] ?? 'OTHER'));
        $description = trim((string) ($data['description'] ?? ''));
        $debit = round($this->decimal($data['vat_debit_delta'] ?? 0), 2);
        $credit = round($this->decimal($data['vat_credit_delta'] ?? 0), 2);
        $types = ['PRO_RATA','PRIOR_CREDIT','ROUNDING','ANNUAL_CORRECTION','REFUND','COMPENSATION','OTHER'];
        if ($year < 2000 || $year > 2200 || ($month !== null && ($month < 1 || $month > 12))
            || !in_array($type, $types, true) || $description === '' || ($debit == 0.0 && $credit == 0.0)) {
            throw new InvalidArgumentException('Rettifica IVA non valida.');
        }
        $status = isset($data['approved']) ? 'APPROVED' : 'DRAFT';
        $statement = $this->db->prepare(
            'INSERT INTO vat_adjustments
             (organization_id, fiscal_year, period_month, adjustment_type, description, vat_debit_delta,
              vat_credit_delta, status, created_by, approved_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute([
            $this->organizationId, $year, $month, $type, $description, $debit, $credit, $status,
            $this->userId, $status === 'APPROVED' ? $this->userId : null,
        ]);
        if ($month !== null && $status === 'APPROVED') {
            (new VatService($this->db, $this->organizationId, $this->userId))->invalidateForDate(sprintf('%04d-%02d-01', $year, $month));
        }
        return (int) $this->db->lastInsertId();
    }

    public function generateLipe(int $year, int $quarter): int
    {
        if ($year < 2000 || $year > 2200 || $quarter < 1 || $quarter > 4) {
            throw new InvalidArgumentException('Periodo LIPE non valido.');
        }
        $existing = $this->db->prepare(
            'SELECT id, status FROM lipe_communications WHERE organization_id = ? AND fiscal_year = ? AND quarter_number = ?'
        );
        $existing->execute([$this->organizationId, $year, $quarter]);
        $current = $existing->fetch();
        if ($current && in_array($current['status'], ['LOCKED', 'EXPORTED'], true)) {
            throw new RuntimeException('Il prospetto LIPE è bloccato: riaprilo prima di rigenerarlo.');
        }
        $monthFrom = (($quarter - 1) * 3) + 1;
        $monthTo = $quarter * 3;
        $settings = $this->settings();
        $months = [];
        for ($month = $monthFrom; $month <= $monthTo; $month++) {
            $months[$month] = $this->monthSummary($year, $month);
        }
        $settlements = $this->db->prepare(
            "SELECT id, period_type, period_number, vat_debit, vat_credit, previous_credit, interest_amount,
                    balance, status, payment_due_date, payment_date
             FROM vat_settlements
             WHERE organization_id = ? AND period_year = ? AND
               ((period_type = 'MONTHLY' AND period_number BETWEEN ? AND ?)
                OR (period_type = 'QUARTERLY' AND period_number = ?))
             ORDER BY period_type, period_number"
        );
        $settlements->execute([$this->organizationId, $year, $monthFrom, $monthTo, $quarter]);
        $settlementRows = $settlements->fetchAll();
        $errors = [];
        $expected = $settings['vat_periodicity'] === 'MONTHLY' ? 3 : 1;
        $matching = array_values(array_filter($settlementRows, static fn (array $row): bool =>
            $row['period_type'] === ($expected === 3 ? 'MONTHLY' : 'QUARTERLY')));
        if (count($matching) !== $expected) {
            $errors[] = 'Mancano una o più liquidazioni coerenti con la periodicità configurata.';
        }
        foreach ($matching as $row) {
            if (!in_array($row['status'], ['CALCULATED', 'SUBMITTED', 'PAID'], true)) {
                $errors[] = 'La liquidazione del periodo ' . $row['period_number'] . ' non è calcolata.';
            }
        }
        $payload = [
            'artifact' => 'LIPE_RECONCILIATION_DRAFT',
            'transmittable' => false,
            'schema_version' => $settings['lipe_schema_version'],
            'fiscal_year' => $year,
            'quarter' => $quarter,
            'periodicity' => $settings['vat_periodicity'],
            'months' => array_values($months),
            'settlements' => $settlementRows,
            'generated_at' => date(DATE_ATOM),
            'warning' => 'Prospetto di raccordo da validare con il software di controllo dell’Agenzia delle Entrate prima di ogni invio.',
        ];
        $validation = ['valid' => $errors === [], 'errors' => $errors, 'checked_at' => date(DATE_ATOM)];
        $statement = $this->db->prepare(
            "INSERT INTO lipe_communications
             (organization_id, fiscal_year, quarter_number, schema_version, payload_json, validation_json,
              status, created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE schema_version = VALUES(schema_version), payload_json = VALUES(payload_json),
              validation_json = VALUES(validation_json), status = 'DRAFT', export_checksum = NULL,
              updated_by = VALUES(updated_by), updated_at = NOW()"
        );
        $statement->execute([
            $this->organizationId, $year, $quarter, $settings['lipe_schema_version'],
            $this->json($payload), $this->json($validation), $this->userId, $this->userId,
        ]);
        return $current ? (int) $current['id'] : (int) $this->db->lastInsertId();
    }

    public function generateAnnual(int $year): int
    {
        if ($year < 2000 || $year > 2200) {
            throw new InvalidArgumentException('Anno IVA non valido.');
        }
        $existing = $this->db->prepare('SELECT id, status FROM vat_annual_summaries WHERE organization_id = ? AND fiscal_year = ?');
        $existing->execute([$this->organizationId, $year]);
        $current = $existing->fetch();
        if ($current && in_array($current['status'], ['LOCKED', 'EXPORTED'], true)) {
            throw new RuntimeException('Il prospetto annuale è bloccato.');
        }
        $settings = $this->settings();
        $movement = $this->db->prepare(
            "SELECT
                COALESCE(SUM(CASE WHEN register_type IN ('SALES','CORRISPETTIVI') THEN taxable_amount ELSE 0 END), 0) AS sales_taxable,
                COALESCE(SUM(CASE WHEN register_type = 'PURCHASES' THEN taxable_amount ELSE 0 END), 0) AS purchases_taxable,
                COALESCE(SUM(vat_due_amount), 0) AS vat_debit,
                COALESCE(SUM(CASE WHEN register_type = 'PURCHASES' AND collectability NOT IN ('CASH','DEFERRED') THEN deductible_vat ELSE 0 END), 0) AS vat_credit
             FROM vat_movements WHERE organization_id = ? AND period_year = ? AND lipe_excluded = 0"
        );
        $movement->execute([$this->organizationId, $year]);
        $totals = $movement->fetch();
        $cash = $this->db->prepare(
            'SELECT COALESCE(SUM(recognized_vat_due),0) AS debit, COALESCE(SUM(recognized_vat_credit),0) AS credit
             FROM vat_cash_events WHERE organization_id = ? AND YEAR(recognition_date) = ?'
        );
        $cash->execute([$this->organizationId, $year]);
        $cashTotals = $cash->fetch();
        $adjustments = $this->db->prepare(
            "SELECT COALESCE(SUM(vat_debit_delta),0) AS debit, COALESCE(SUM(vat_credit_delta),0) AS credit
             FROM vat_adjustments WHERE organization_id = ? AND fiscal_year = ? AND status IN ('APPROVED','POSTED')"
        );
        $adjustments->execute([$this->organizationId, $year]);
        $adjustmentTotals = $adjustments->fetch();
        $settlements = $this->db->prepare(
            "SELECT * FROM vat_settlements WHERE organization_id = ? AND period_year = ? AND period_type = ? ORDER BY period_number"
        );
        $settlements->execute([$this->organizationId, $year, $settings['vat_periodicity']]);
        $settlementRows = $settlements->fetchAll();
        $vatDebit = round((float) $totals['vat_debit'] + (float) $cashTotals['debit'], 2);
        $vatCredit = round((float) $totals['vat_credit'] + (float) $cashTotals['credit'], 2);
        $adjustDebit = round((float) $adjustmentTotals['debit'], 2);
        $adjustCredit = round((float) $adjustmentTotals['credit'], 2);
        $payments = round(array_sum(array_map(static fn (array $row): float =>
            $row['status'] === 'PAID' ? max(0, (float) $row['balance']) : 0.0, $settlementRows)), 2);
        $priorCredit = $settlementRows ? (float) ($settlementRows[0]['previous_credit'] ?? 0) : 0.0;
        $final = round($vatDebit + $adjustDebit - $vatCredit - $adjustCredit - $priorCredit - $payments, 2);
        $errors = [];
        if ($settlementRows === []) {
            $errors[] = 'Nessuna liquidazione IVA disponibile per l’esercizio.';
        }
        if (array_filter($settlementRows, static fn (array $row): bool => $row['status'] === 'DRAFT')) {
            $errors[] = 'Sono presenti liquidazioni IVA da ricalcolare.';
        }
        $detail = [
            'artifact' => 'VAT_ANNUAL_RECONCILIATION_DRAFT', 'transmittable' => false,
            'schema_version' => $settings['vat_return_schema_version'], 'settlements' => $settlementRows,
            'cash_vat' => $cashTotals, 'generated_at' => date(DATE_ATOM),
            'warning' => 'Prospetto di raccordo, non file telematico IVA.',
        ];
        $validation = ['valid' => $errors === [], 'errors' => $errors, 'checked_at' => date(DATE_ATOM)];
        $statement = $this->db->prepare(
            "INSERT INTO vat_annual_summaries
             (organization_id, fiscal_year, schema_version, sales_taxable, purchases_taxable, vat_debit, vat_credit,
              adjustments_debit, adjustments_credit, prior_credit, payments_amount, final_balance, detail_json,
              validation_json, status, created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE schema_version = VALUES(schema_version), sales_taxable = VALUES(sales_taxable),
              purchases_taxable = VALUES(purchases_taxable), vat_debit = VALUES(vat_debit), vat_credit = VALUES(vat_credit),
              adjustments_debit = VALUES(adjustments_debit), adjustments_credit = VALUES(adjustments_credit),
              prior_credit = VALUES(prior_credit), payments_amount = VALUES(payments_amount), final_balance = VALUES(final_balance),
              detail_json = VALUES(detail_json), validation_json = VALUES(validation_json), status = 'DRAFT',
              export_checksum = NULL, updated_by = VALUES(updated_by), updated_at = NOW()"
        );
        $statement->execute([
            $this->organizationId, $year, $settings['vat_return_schema_version'], $totals['sales_taxable'],
            $totals['purchases_taxable'], $vatDebit, $vatCredit, $adjustDebit, $adjustCredit, $priorCredit,
            $payments, $final, $this->json($detail), $this->json($validation), $this->userId, $this->userId,
        ]);
        return $current ? (int) $current['id'] : (int) $this->db->lastInsertId();
    }

    public function changeStatus(string $table, int $id, string $status): void
    {
        $allowedTables = ['lipe_communications', 'vat_annual_summaries'];
        $status = strtoupper($status);
        if (!in_array($table, $allowedTables, true) || !in_array($status, ['DRAFT','REVIEWED','LOCKED'], true)) {
            throw new InvalidArgumentException('Stato del prospetto non valido.');
        }
        if (in_array($status, ['REVIEWED','LOCKED'], true)) {
            $check = $this->db->prepare("SELECT validation_json FROM `{$table}` WHERE id = ? AND organization_id = ?");
            $check->execute([$id, $this->organizationId]);
            $validation = json_decode((string) $check->fetchColumn(), true);
            if (!is_array($validation) || empty($validation['valid'])) {
                throw new RuntimeException('Il prospetto contiene anomalie e non può essere revisionato o bloccato.');
            }
        }
        $reviewed = in_array($status, ['REVIEWED','LOCKED'], true);
        $statement = $this->db->prepare(
            "UPDATE `{$table}` SET status = ?, reviewed_by = IF(? = 1, ?, reviewed_by),
             reviewed_at = IF(? = 1, NOW(), reviewed_at), updated_by = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ?"
        );
        $statement->execute([$status, $reviewed ? 1 : 0, $this->userId, $reviewed ? 1 : 0, $this->userId, $id, $this->organizationId]);
        if ($statement->rowCount() === 0) {
            throw new InvalidArgumentException('Prospetto non trovato.');
        }
    }

    private function monthSummary(int $year, int $month): array
    {
        $statement = $this->db->prepare(
            "SELECT
                COALESCE(SUM(CASE WHEN register_type IN ('SALES','CORRISPETTIVI') THEN taxable_amount ELSE 0 END),0) AS active_operations,
                COALESCE(SUM(CASE WHEN register_type = 'PURCHASES' THEN taxable_amount ELSE 0 END),0) AS passive_operations,
                COALESCE(SUM(vat_due_amount),0) AS vat_due,
                COALESCE(SUM(CASE WHEN register_type = 'PURCHASES' AND collectability NOT IN ('CASH','DEFERRED') THEN deductible_vat ELSE 0 END),0) AS vat_credit
             FROM vat_movements WHERE organization_id = ? AND period_year = ? AND period_month = ? AND lipe_excluded = 0"
        );
        $statement->execute([$this->organizationId, $year, $month]);
        $row = $statement->fetch();
        $cash = $this->db->prepare(
            'SELECT COALESCE(SUM(recognized_taxable),0) AS taxable, COALESCE(SUM(recognized_vat_due),0) AS vat_due,
                    COALESCE(SUM(recognized_vat_credit),0) AS vat_credit
             FROM vat_cash_events WHERE organization_id = ? AND YEAR(recognition_date) = ? AND MONTH(recognition_date) = ?'
        );
        $cash->execute([$this->organizationId, $year, $month]);
        $cashRow = $cash->fetch();
        return [
            'month' => $month,
            'active_operations' => round((float) $row['active_operations'], 2),
            'passive_operations' => round((float) $row['passive_operations'], 2),
            'cash_recognized_taxable' => round((float) $cashRow['taxable'], 2),
            'vat_due' => round((float) $row['vat_due'] + (float) $cashRow['vat_due'], 2),
            'vat_credit' => round((float) $row['vat_credit'] + (float) $cashRow['vat_credit'], 2),
        ];
    }

    private function settings(): array
    {
        $statement = $this->db->prepare('SELECT * FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        return $statement->fetch() ?: [
            'vat_periodicity' => 'MONTHLY', 'lipe_schema_version' => '2026-draft',
            'vat_return_schema_version' => 'IVA26-draft',
        ];
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

    private function json(array $value): string
    {
        return json_encode($value, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }
}
