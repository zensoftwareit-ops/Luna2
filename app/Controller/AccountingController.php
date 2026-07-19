<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\AccountingService;

final class AccountingController extends BaseController
{
    public function journal(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $statement = $this->db->prepare(
            'SELECT id, protocol_number, entry_date, entry_type, description, document_number, counterparty, total_debit, total_credit, status
             FROM journal_entries WHERE organization_id = ? AND entry_date BETWEEN ? AND ? ORDER BY entry_date DESC, protocol_number DESC LIMIT 1000'
        );
        $statement->execute([Auth::organizationId(), $from, $to]);
        $entries = $statement->fetchAll();
        $this->view->render('accounting/journal', compact('entries', 'from', 'to') + ['title' => 'Libro giornale']);
    }

    public function create(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $statement = $this->db->prepare('SELECT id, code, name, account_type FROM chart_of_accounts WHERE organization_id = ? AND active = 1 AND is_postable = 1 ORDER BY code');
        $statement->execute([Auth::organizationId()]);
        $accounts = $statement->fetchAll();
        $this->view->render('accounting/form', compact('accounts') + ['title' => 'Nuova registrazione']);
    }

    public function post(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $header = [
            'entry_date' => $_POST['entry_date'] ?? date('Y-m-d'),
            'competence_date' => $_POST['competence_date'] ?? null,
            'entry_type' => $_POST['entry_type'] ?? 'MANUAL',
            'description' => $_POST['description'] ?? '',
            'document_number' => $_POST['document_number'] ?? null,
            'source_type' => 'MANUAL', 'source_id' => null,
            'counterparty' => $_POST['counterparty'] ?? null,
            'notes' => $_POST['notes'] ?? null,
        ];
        $service = new AccountingService($this->db, Auth::organizationId(), Auth::id());
        $id = $service->postManual($header, $_POST['lines'] ?? []);
        $this->audit('POST', 'journal_entries', $id, $header);
        $this->redirect('/accounting/journal', 'Registrazione contabilizzata e quadrata.');
    }

    public function trialBalance(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $statement = $this->db->prepare(
            "SELECT a.id, a.code, a.name, a.account_type,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit ELSE 0 END), 0) AS debit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit ELSE 0 END), 0) AS credit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit - l.credit ELSE 0 END), 0) AS balance
             FROM chart_of_accounts a
             LEFT JOIN journal_entry_lines l ON l.account_id = a.id
             LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
             WHERE a.organization_id = ?
             GROUP BY a.id, a.code, a.name, a.account_type
             HAVING debit <> 0 OR credit <> 0
             ORDER BY a.code"
        );
        $statement->execute([$from, $to, Auth::organizationId()]);
        $accounts = $statement->fetchAll();
        $totals = ['debit' => array_sum(array_column($accounts, 'debit')), 'credit' => array_sum(array_column($accounts, 'credit'))];
        $this->view->render('accounting/trial-balance', compact('accounts', 'totals', 'from', 'to') + ['title' => 'Bilancio di verifica']);
    }

    public function ledger(string $id): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $statement = $this->db->prepare('SELECT id, code, name FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $account = $statement->fetch();
        if (!$account) {
            $this->redirect('/accounting/trial-balance', 'Conto non trovato.', 'error');
        }
        $statement = $this->db->prepare(
            "SELECT e.entry_date, e.protocol_number, e.description AS entry_description, e.document_number,
                    l.description, l.debit, l.credit,
                    SUM(l.debit - l.credit) OVER (ORDER BY e.entry_date, e.id, l.line_number) AS running_balance
             FROM journal_entry_lines l JOIN journal_entries e ON e.id = l.journal_entry_id
             WHERE l.organization_id = ? AND l.account_id = ? AND e.status = 'POSTED'
             ORDER BY e.entry_date, e.id, l.line_number"
        );
        $statement->execute([Auth::organizationId(), (int) $id]);
        $lines = $statement->fetchAll();
        $this->view->render('accounting/ledger', compact('account', 'lines') + ['title' => 'Mastrino ' . $account['code']]);
    }

    private function guard(): void
    {
        $this->requireFeature('accounting');
    }
}
