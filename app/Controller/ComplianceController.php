<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use DateTimeImmutable;
use Luna\Core\Auth;
use Luna\Service\AssetService;
use Luna\Service\ClosingService;
use Luna\Service\TabularExportService;
use Luna\Service\VatComplianceService;
use RuntimeException;

final class ComplianceController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function index(): never
    {
        $this->authorize();
        $organizationId = Auth::organizationId();
        $year = min(2200, max(2000, (int) ($_GET['year'] ?? date('Y'))));
        $setting = $this->query('SELECT fiscal_year_start_month FROM accounting_settings WHERE organization_id = ?', [$organizationId]);
        $startMonth = min(12, max(1, (int) ($setting[0]['fiscal_year_start_month'] ?? 1)));
        $periodStart = new DateTimeImmutable(sprintf('%04d-%02d-01', $year, $startMonth));
        $periodEnd = $periodStart->modify('+1 year -1 day');
        $adjustments = $this->query('SELECT * FROM vat_adjustments WHERE organization_id = ? AND fiscal_year = ? ORDER BY period_month, id', [$organizationId, $year]);
        $lipe = $this->query('SELECT * FROM lipe_communications WHERE organization_id = ? AND fiscal_year = ? ORDER BY quarter_number', [$organizationId, $year]);
        $annual = $this->query('SELECT * FROM vat_annual_summaries WHERE organization_id = ? AND fiscal_year = ?', [$organizationId, $year]);
        $closingRuns = $this->query('SELECT * FROM accounting_closing_runs WHERE organization_id = ? ORDER BY fiscal_year DESC LIMIT 10', [$organizationId]);
        $schedules = $this->query(
            'SELECT s.*, a.code AS source_code, b.code AS counterpart_code FROM accounting_adjustment_schedules s
             JOIN chart_of_accounts a ON a.id = s.source_account_id JOIN chart_of_accounts b ON b.id = s.counterpart_account_id
             WHERE s.organization_id = ? ORDER BY s.posting_date DESC LIMIT 300', [$organizationId]
        );
        $categories = $this->query('SELECT * FROM fixed_asset_categories WHERE organization_id = ? ORDER BY code', [$organizationId]);
        $registerYears = $this->query('SELECT y.*, a.asset_code, a.description, a.purchase_date, a.purchase_document_number,
                   s.business_name AS supplier_name FROM fixed_asset_register_years y
            JOIN fixed_assets a ON a.id = y.fixed_asset_id AND a.organization_id = y.organization_id
            LEFT JOIN suppliers s ON s.id = a.supplier_id AND s.organization_id = a.organization_id
            WHERE y.organization_id = ? AND y.fiscal_year = ? ORDER BY a.asset_code', [$organizationId, $year]);
        $assets = $this->query(
            'SELECT a.*, c.code AS category_code, s.business_name AS supplier_name FROM fixed_assets a LEFT JOIN fixed_asset_categories c ON c.id = a.category_id
             LEFT JOIN suppliers s ON s.id = a.supplier_id AND s.organization_id = a.organization_id
             WHERE a.organization_id = ? ORDER BY a.asset_code', [$organizationId]
        );
        $suppliers = $this->query('SELECT id, code, business_name, vat_number FROM suppliers WHERE organization_id = ? AND active = 1 ORDER BY business_name', [$organizationId]);
        $purchaseDocuments = $this->query("SELECT id, number, document_date, counterparty_name, counterparty_id, total FROM documents WHERE organization_id = ? AND document_type IN ('PURCHASE_INVOICE','CREDIT_NOTE') ORDER BY document_date DESC, id DESC LIMIT 1000", [$organizationId]);
        $depreciations = $this->query(
            'SELECT d.*, a.asset_code, a.description FROM depreciation_entries d JOIN fixed_assets a ON a.id = d.fixed_asset_id
             WHERE d.organization_id = ? AND d.fiscal_year = ? ORDER BY a.asset_code', [$organizationId, $year]
        );
        $accounts = $this->query(
            'SELECT id, code, name, account_type FROM chart_of_accounts WHERE organization_id = ? AND active = 1 AND is_postable = 1 ORDER BY code', [$organizationId]
        );
        $statements = $this->query(
            "SELECT a.id, a.code, a.name, a.account_type, COALESCE(a.statement_section, 'NON_CLASSIFICATO') AS statement_section,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit ELSE 0 END),0) AS debit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit ELSE 0 END),0) AS credit
             FROM chart_of_accounts a LEFT JOIN journal_entry_lines l ON l.account_id = a.id AND l.organization_id = a.organization_id
             LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
             WHERE a.organization_id = ? GROUP BY a.id, a.code, a.name, a.account_type, a.statement_section
             HAVING debit <> 0 OR credit <> 0 ORDER BY a.account_type, a.statement_section, a.code",
            [$periodStart->format('Y-m-d'), $periodEnd->format('Y-m-d'), $organizationId]
        );
        $statementTotals = [];
        foreach ($statements as &$row) {
            $row['display_balance'] = in_array($row['account_type'], ['LIABILITY','EQUITY','REVENUE'], true)
                ? (float) $row['credit'] - (float) $row['debit']
                : (float) $row['debit'] - (float) $row['credit'];
            $statementTotals[$row['statement_section']] = ($statementTotals[$row['statement_section']] ?? 0) + $row['display_balance'];
        }
        unset($row);
        $this->view->render('accounting/compliance', compact(
            'year', 'adjustments', 'lipe', 'annual', 'closingRuns', 'schedules', 'categories', 'assets',
            'depreciations', 'accounts', 'statements', 'statementTotals', 'periodStart', 'periodEnd', 'registerYears',
            'suppliers', 'purchaseDocuments'
        ) + ['title' => 'Adempimenti e chiusure']);
    }

    public function vatAdjustment(): never { $this->perform(fn (): int => $this->vat()->saveAdjustment($_POST), 'Rettifica IVA salvata.', 'CREATE', 'vat_adjustments'); }
    public function generateLipe(): never { $this->perform(fn (): int => $this->vat()->generateLipe((int) ($_POST['year'] ?? 0), (int) ($_POST['quarter'] ?? 0)), 'Prospetto LIPE di raccordo rigenerato.', 'GENERATE', 'lipe_communications'); }
    public function exportLipe(string $id, string $format): never
    {
        $this->authorize();
        if (!in_array(strtolower($format), ['pdf', 'xlsx', 'csv'], true)) {
            $this->redirect('/accounting/compliance', 'Formato di esportazione non supportato.', 'error');
        }
        $rows = $this->query(
            'SELECT * FROM lipe_communications WHERE id = ? AND organization_id = ?',
            [(int) $id, Auth::organizationId()]
        );
        $lipe = $rows[0] ?? null;
        if (!$lipe) {
            $this->redirect('/accounting/compliance', 'Prospetto LIPE non trovato.', 'error');
        }
        $payload = json_decode((string) $lipe['payload_json'], true);
        if (!is_array($payload)) {
            $this->redirect('/accounting/compliance?year=' . (int) $lipe['fiscal_year'], 'Il dettaglio del raccordo LIPE non è leggibile.', 'error');
        }
        $validation = json_decode((string) $lipe['validation_json'], true);
        $validation = is_array($validation) ? $validation : ['valid' => false, 'errors' => ['Validazione non leggibile.']];
        if (strtolower($format) === 'pdf') {
            $this->exporter()->streamLipePdf($lipe, $payload, $validation);
        }
        if (strtolower($format) === 'xlsx') {
            $this->exporter()->streamLipeXlsx($lipe, $payload, $validation);
        }

        $periodicity = strtoupper((string) ($payload['periodicity'] ?? ''));
        $baseExport = [
            'fiscal_year' => (int) $lipe['fiscal_year'],
            'quarter' => (int) $lipe['quarter_number'],
            'periodicity' => $periodicity === 'MONTHLY' ? 'Mensile' : ($periodicity === 'QUARTERLY' ? 'Trimestrale' : $periodicity),
            'schema_version' => (string) $lipe['schema_version'],
            'lipe_status' => match (strtoupper((string) $lipe['status'])) { 'DRAFT' => 'Bozza', 'REVIEWED' => 'Revisionato', 'LOCKED' => 'Bloccato', default => (string) $lipe['status'] },
            'validation' => !empty($validation['valid']) ? 'OK' : 'CON ANOMALIE',
            'warning' => 'Prospetto di raccordo interno non trasmissibile',
        ];
        $exportRows = [];
        foreach ((array) ($payload['months'] ?? []) as $month) {
            $monthNumber = (int) ($month['month'] ?? 0);
            $period = $monthNumber >= 1 && $monthNumber <= 12
                ? str_pad((string) $monthNumber, 2, '0', STR_PAD_LEFT) . '/' . (int) $lipe['fiscal_year']
                : (string) $monthNumber;
            $exportRows[] = $baseExport + [
                'section' => 'Riepilogo mensile',
                'period' => $period,
                'active_operations' => $month['active_operations'] ?? 0,
                'passive_operations' => $month['passive_operations'] ?? 0,
                'cash_recognized_taxable' => $month['cash_recognized_taxable'] ?? 0,
                'vat_debit' => $month['vat_due'] ?? 0,
                'vat_credit' => $month['vat_credit'] ?? 0,
                'previous_credit' => null,
                'interest' => null,
                'balance' => round((float) ($month['vat_due'] ?? 0) - (float) ($month['vat_credit'] ?? 0), 2),
                'status' => '',
                'due_date' => null,
                'payment_date' => null,
            ];
        }
        foreach ((array) ($payload['settlements'] ?? []) as $settlement) {
            $periodType = strtoupper((string) ($settlement['period_type'] ?? ''));
            $periodNumber = (int) ($settlement['period_number'] ?? 0);
            $period = $periodType === 'QUARTERLY'
                ? 'T' . $periodNumber . '/' . (int) $lipe['fiscal_year']
                : str_pad((string) $periodNumber, 2, '0', STR_PAD_LEFT) . '/' . (int) $lipe['fiscal_year'];
            $status = strtoupper((string) ($settlement['status'] ?? ''));
            $creditMode = strtoupper((string) ($settlement['previous_credit_mode'] ?? ''));
            $exportRows[] = $baseExport + [
                'section' => $periodType === 'QUARTERLY' ? 'Liquidazione trimestrale' : 'Liquidazione mensile',
                'period' => $period,
                'active_operations' => null,
                'passive_operations' => null,
                'cash_recognized_taxable' => null,
                'vat_debit' => $settlement['vat_debit'] ?? 0,
                'vat_credit' => $settlement['vat_credit'] ?? 0,
                'previous_credit' => $settlement['previous_credit'] ?? 0,
                'previous_credit_mode' => $creditMode === 'AUTO' ? 'Automatico' : ($creditMode === 'MANUAL' ? 'Manuale' : $creditMode),
                'interest' => $settlement['interest_amount'] ?? 0,
                'balance' => $settlement['balance'] ?? 0,
                'status' => match ($status) { 'DRAFT' => 'Bozza', 'CALCULATED' => 'Calcolata', 'SUBMITTED' => 'Presentata', 'PAID' => 'Pagata', 'OVERDUE' => 'Scaduta', default => $status },
                'due_date' => $settlement['payment_due_date'] ?? null,
                'payment_date' => $settlement['payment_date'] ?? null,
                'payment_reference' => (string) ($settlement['payment_reference'] ?? ''),
                'notes' => (string) ($settlement['notes'] ?? ''),
                'calculated_at' => $settlement['calculated_at'] ?? null,
                'reconciliation_mode' => (string) ($settlement['source_reconciliation_mode'] ?? ''),
                'excluded_document_rows' => $settlement['excluded_document_rows'] ?? 0,
                'excluded_document_vat_debit' => $settlement['excluded_document_vat_debit'] ?? 0,
                'excluded_document_vat_credit' => $settlement['excluded_document_vat_credit'] ?? 0,
                'excluded_document_purchase_vat' => $settlement['excluded_document_purchase_vat'] ?? 0,
            ];
        }

        $this->exporter()->stream(
            $format,
            'Raccordo LIPE T' . (int) $lipe['quarter_number'] . ' - ' . (int) $lipe['fiscal_year'],
            [
                ['key' => 'fiscal_year', 'label' => 'Esercizio', 'type' => 'number'],
                ['key' => 'quarter', 'label' => 'Trimestre', 'type' => 'number'],
                ['key' => 'periodicity', 'label' => 'Periodicità'],
                ['key' => 'schema_version', 'label' => 'Schema'],
                ['key' => 'lipe_status', 'label' => 'Stato raccordo'],
                ['key' => 'validation', 'label' => 'Validazione'],
                ['key' => 'warning', 'label' => 'Avvertenza'],
                ['key' => 'section', 'label' => 'Sezione'],
                ['key' => 'period', 'label' => 'Periodo'],
                ['key' => 'active_operations', 'label' => 'Operazioni attive', 'type' => 'money'],
                ['key' => 'passive_operations', 'label' => 'Operazioni passive', 'type' => 'money'],
                ['key' => 'cash_recognized_taxable', 'label' => 'Imponibile per cassa', 'type' => 'money'],
                ['key' => 'vat_debit', 'label' => 'IVA a debito', 'type' => 'money'],
                ['key' => 'vat_credit', 'label' => 'IVA a credito', 'type' => 'money'],
                ['key' => 'previous_credit', 'label' => 'Credito precedente', 'type' => 'money'],
                ['key' => 'previous_credit_mode', 'label' => 'Origine credito'],
                ['key' => 'interest', 'label' => 'Interessi', 'type' => 'money'],
                ['key' => 'balance', 'label' => 'Saldo', 'type' => 'money'],
                ['key' => 'status', 'label' => 'Stato'],
                ['key' => 'due_date', 'label' => 'Scadenza', 'type' => 'date'],
                ['key' => 'payment_date', 'label' => 'Pagamento', 'type' => 'date'],
                ['key' => 'payment_reference', 'label' => 'Riferimento pagamento'],
                ['key' => 'notes', 'label' => 'Note'],
                ['key' => 'calculated_at', 'label' => 'Calcolata il', 'type' => 'datetime'],
                ['key' => 'reconciliation_mode', 'label' => 'Modalità riconciliazione'],
                ['key' => 'excluded_document_rows', 'label' => 'Righe escluse', 'type' => 'number'],
                ['key' => 'excluded_document_vat_debit', 'label' => 'IVA debito esclusa', 'type' => 'money'],
                ['key' => 'excluded_document_vat_credit', 'label' => 'IVA credito esclusa', 'type' => 'money'],
                ['key' => 'excluded_document_purchase_vat', 'label' => 'IVA acquisti esclusa', 'type' => 'money'],
            ],
            $exportRows,
            [
                'Schema' => (string) $lipe['schema_version'],
                'Periodicità' => (string) ($payload['periodicity'] ?? ''),
                'Stato raccordo' => (string) $lipe['status'],
                'Generato' => (string) ($payload['generated_at'] ?? ''),
                'Validazione' => !empty($validation['valid']) ? 'OK' : 'CON ANOMALIE',
                'Anomalie' => implode(' | ', (array) ($validation['errors'] ?? [])),
                'Avvertenza' => 'Prospetto di raccordo non trasmissibile',
            ],
            'Luna2',
            'raccordo-lipe-' . (int) $lipe['fiscal_year'] . '-t' . (int) $lipe['quarter_number'],
        );
    }
    public function generateAnnual(): never { $this->perform(fn (): int => $this->vat()->generateAnnual((int) ($_POST['year'] ?? 0)), 'Prospetto IVA annuale di raccordo rigenerato.', 'GENERATE', 'vat_annual_summaries'); }
    public function vatStatus(string $kind, string $id): never
    {
        $table = $kind === 'annual' ? 'vat_annual_summaries' : 'lipe_communications';
        $this->perform(function () use ($table, $id): int { $this->vat()->changeStatus($table, (int) $id, (string) ($_POST['status'] ?? '')); return (int) $id; }, 'Stato del prospetto aggiornato.', 'STATUS', $table);
    }
    public function prepareClosing(): never { $this->perform(fn (): int => $this->closing()->prepare((int) ($_POST['year'] ?? 0)), 'Controlli di chiusura completati.', 'PREPARE', 'accounting_closing_runs'); }
    public function postClosing(string $id): never { $this->perform(function () use ($id): int { $this->closing()->post((int) $id); return (int) $id; }, 'Chiusura e apertura contabilizzate.', 'POST', 'accounting_closing_runs'); }
    public function adjustment(): never { $this->perform(fn (): int => $this->closing()->saveAdjustment($_POST, isset($_POST['post_now'])), 'Assestamento salvato.', 'CREATE', 'accounting_adjustment_schedules'); }
    public function postAdjustment(string $id): never { $this->perform(fn (): int => $this->closing()->postAdjustment((int) $id), 'Assestamento contabilizzato.', 'POST', 'accounting_adjustment_schedules'); }
    public function assetCategory(): never { $this->perform(fn (): int => $this->assets()->saveCategory($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null), 'Categoria cespite salvata.', 'SAVE', 'fixed_asset_categories'); }
    public function asset(): never { $this->perform(fn (): int => $this->assets()->saveAsset($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null), 'Cespite salvato.', 'SAVE', 'fixed_assets'); }
    public function calculateDepreciation(string $id): never { $this->perform(fn (): int => $this->assets()->calculate((int) $id, (int) ($_POST['year'] ?? 0)), 'Quota di ammortamento calcolata.', 'CALCULATE', 'depreciation_entries'); }
    public function postDepreciation(string $id): never { $this->perform(fn (): int => $this->assets()->postDepreciation((int) $id), 'Ammortamento contabilizzato.', 'POST', 'depreciation_entries'); }

    public function assetRegisterYear(): never
    {
        $this->perform(fn (): int => $this->assets()->saveRegisterYear($_POST), 'Scheda annuale cespite salvata.', 'REGISTER_YEAR', 'fixed_assets');
    }

    private function perform(callable $operation, string $success, string $auditAction, string $entity): never
    {
        $this->authorize();
        try {
            $id = (int) $operation();
            $this->audit($auditAction, $entity, $id);
            $year = (int) ($_POST['year'] ?? date('Y'));
            $this->redirect('/accounting/compliance?year=' . $year, $success);
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/compliance', $exception->getMessage(), 'error');
        }
    }

    private function authorize(): void { $this->requireFeature('accounting'); $this->requireRoles(self::ROLES); }
    private function vat(): VatComplianceService { return new VatComplianceService($this->db, Auth::organizationId(), Auth::id()); }
    private function closing(): ClosingService { return new ClosingService($this->db, Auth::organizationId(), Auth::id()); }
    private function assets(): AssetService { return new AssetService($this->db, Auth::organizationId(), Auth::id()); }
    private function exporter(): TabularExportService
    {
        $company = $this->db->prepare('SELECT business_name, vat_number, tax_code, address, postal_code, city, province FROM organizations WHERE id = ?');
        $company->execute([Auth::organizationId()]);
        return new TabularExportService($company->fetch() ?: []);
    }
    private function query(string $sql, array $params): array { $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetchAll(); }
}
