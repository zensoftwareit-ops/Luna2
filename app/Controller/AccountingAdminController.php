<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingSetupService;
use RuntimeException;

final class AccountingAdminController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function index(): never
    {
        $this->authorize();
        $organizationId = Auth::organizationId();
        $accounts = $this->query(
            'SELECT a.*, p.code AS parent_code, p.name AS parent_name,
                    (SELECT COUNT(*) FROM journal_entry_lines l WHERE l.organization_id = a.organization_id AND l.account_id = a.id) AS movements
             FROM chart_of_accounts a LEFT JOIN chart_of_accounts p ON p.id = a.parent_id
             WHERE a.organization_id = ? ORDER BY a.code', [$organizationId]
        );
        $settings = $this->one('SELECT * FROM accounting_settings WHERE organization_id = ?', [$organizationId]);
        $registers = $this->query('SELECT * FROM vat_registers WHERE organization_id = ? ORDER BY register_type, code', [$organizationId]);
        $causes = $this->query(
            'SELECT c.*, r.code AS register_code FROM accounting_causes c LEFT JOIN vat_registers r ON r.id = c.vat_register_id
             WHERE c.organization_id = ? ORDER BY c.code', [$organizationId]
        );
        $mappings = $this->query(
            'SELECT m.*, a.code AS account_code, a.name AS account_name FROM accounting_account_mappings m
             JOIN chart_of_accounts a ON a.id = m.account_id WHERE m.organization_id = ? ORDER BY m.mapping_key', [$organizationId]
        );
        $selectedAccount = $this->selected($accounts, (int) ($_GET['account_id'] ?? 0));
        $selectedRegister = $this->selected($registers, (int) ($_GET['register_id'] ?? 0));
        $selectedCause = $this->selected($causes, (int) ($_GET['cause_id'] ?? 0));
        $mappingLabels = AccountingSetupService::MAPPING_LABELS;
        $this->view->render('accounting/setup', compact('accounts', 'settings', 'registers', 'causes', 'mappings', 'mappingLabels', 'selectedAccount', 'selectedRegister', 'selectedCause') + [
            'title' => 'Configurazione contabile',
        ]);
    }

    public function saveAccount(): never
    {
        $this->authorize();
        try {
            $id = $this->service()->saveAccount($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null);
            $this->audit('SAVE', 'chart_of_accounts', $id);
            $this->redirect('/accounting/setup#chart', 'Conto salvato.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#chart', $exception->getMessage(), 'error');
        }
    }

    public function toggleAccount(string $id): never
    {
        $this->authorize();
        try {
            $this->service()->toggleAccount((int) $id);
            $this->audit('TOGGLE', 'chart_of_accounts', (int) $id);
            $this->redirect('/accounting/setup#chart', 'Stato del conto aggiornato.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#chart', $exception->getMessage(), 'error');
        }
    }

    public function saveSettings(): never
    {
        $this->authorize();
        try {
            $this->service()->saveSettings($_POST);
            $this->audit('SAVE', 'accounting_settings', Auth::organizationId());
            $this->redirect('/accounting/setup#settings', 'Impostazioni contabili aggiornate.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#settings', $exception->getMessage(), 'error');
        }
    }

    public function saveRegister(): never
    {
        $this->authorize();
        try {
            $id = $this->service()->saveRegister($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null);
            $this->audit('SAVE', 'vat_registers', $id);
            $this->redirect('/accounting/setup#registers', 'Registro IVA salvato.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#registers', $exception->getMessage(), 'error');
        }
    }

    public function saveCause(): never
    {
        $this->authorize();
        try {
            $id = $this->service()->saveCause($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null);
            $this->audit('SAVE', 'accounting_causes', $id);
            $this->redirect('/accounting/setup#causes', 'Causale contabile salvata.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#causes', $exception->getMessage(), 'error');
        }
    }

    public function saveMapping(): never
    {
        $this->authorize();
        try {
            $this->service()->saveMapping((string) ($_POST['mapping_key'] ?? ''), (int) ($_POST['account_id'] ?? 0));
            $this->audit('SAVE', 'accounting_account_mappings', (string) ($_POST['mapping_key'] ?? ''));
            $this->redirect('/accounting/setup#mappings', 'Collegamento automatico salvato.');
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/setup#mappings', $exception->getMessage(), 'error');
        }
    }

    private function authorize(): void
    {
        $this->requireFeature('accounting');
        $this->requireRoles(self::ROLES);
    }

    private function service(): AccountingSetupService
    {
        return new AccountingSetupService($this->db, Auth::organizationId(), Auth::id());
    }

    private function query(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetchAll();
    }

    private function one(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetch() ?: [];
    }

    private function selected(array $rows, int $id): array
    {
        foreach ($rows as $row) {
            if ((int) ($row['id'] ?? 0) === $id) {
                return $row;
            }
        }
        return [];
    }
}
