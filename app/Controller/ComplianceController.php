<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use DateTimeImmutable;
use Luna\Core\Auth;
use Luna\Service\AssetService;
use Luna\Service\ClosingService;
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
        $assets = $this->query(
            'SELECT a.*, c.code AS category_code FROM fixed_assets a LEFT JOIN fixed_asset_categories c ON c.id = a.category_id
             WHERE a.organization_id = ? ORDER BY a.asset_code', [$organizationId]
        );
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
            'depreciations', 'accounts', 'statements', 'statementTotals', 'periodStart', 'periodEnd'
        ) + ['title' => 'Adempimenti e chiusure']);
    }

    public function vatAdjustment(): never { $this->perform(fn (): int => $this->vat()->saveAdjustment($_POST), 'Rettifica IVA salvata.', 'CREATE', 'vat_adjustments'); }
    public function generateLipe(): never { $this->perform(fn (): int => $this->vat()->generateLipe((int) ($_POST['year'] ?? 0), (int) ($_POST['quarter'] ?? 0)), 'Prospetto LIPE di raccordo rigenerato.', 'GENERATE', 'lipe_communications'); }
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
    private function query(string $sql, array $params): array { $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetchAll(); }
}
