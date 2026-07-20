<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\VatService;
use RuntimeException;

final class AccountingController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function journal(): never
    {
        $this->authorize();
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $status = strtoupper((string) ($_GET['status'] ?? ''));
        $type = strtoupper((string) ($_GET['type'] ?? ''));
        $search = trim((string) ($_GET['q'] ?? ''));
        $sql = 'SELECT id, protocol_number, entry_date, competence_date, entry_type, description, document_number,
                       counterparty, total_debit, total_credit, status, source_type
                FROM journal_entries WHERE organization_id = :organization_id AND entry_date BETWEEN :date_from AND :date_to';
        $params = ['organization_id' => Auth::organizationId(), 'date_from' => $from, 'date_to' => $to];
        if (in_array($status, ['DRAFT', 'POSTED', 'REVERSED'], true)) {
            $sql .= ' AND status = :status';
            $params['status'] = $status;
        }
        if ($type !== '') {
            $sql .= ' AND entry_type = :entry_type';
            $params['entry_type'] = $type;
        }
        if ($search !== '') {
            $sql .= ' AND (protocol_number LIKE :search OR description LIKE :search OR document_number LIKE :search OR counterparty LIKE :search)';
            $params['search'] = '%' . $search . '%';
        }
        $sql .= ' ORDER BY entry_date DESC, id DESC LIMIT 1500';
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $entries = $statement->fetchAll();
        $totals = [
            'debit' => array_sum(array_column($entries, 'total_debit')),
            'credit' => array_sum(array_column($entries, 'total_credit')),
            'drafts' => count(array_filter($entries, static fn (array $entry): bool => $entry['status'] === 'DRAFT')),
        ];
        $this->view->render('accounting/journal', compact('entries', 'totals', 'from', 'to', 'status', 'type', 'search') + ['title' => 'Prima nota']);
    }

    public function create(): never
    {
        $this->authorize();
        $accounts = $this->accounts();
        $entry = ['entry_date' => date('Y-m-d'), 'competence_date' => date('Y-m-d'), 'entry_type' => 'MANUAL'];
        $lines = [];
        $this->view->render('accounting/form', compact('accounts', 'entry', 'lines') + ['title' => 'Nuova registrazione']);
    }

    public function edit(string $id): never
    {
        $this->authorize();
        [$entry, $lines] = $this->entry((int) $id);
        if ($entry['status'] !== 'DRAFT' || $entry['source_type'] !== 'MANUAL') {
            $this->redirect('/accounting/journal/' . (int) $id, 'Solo le bozze manuali possono essere modificate.', 'error');
        }
        $accounts = $this->accounts();
        $this->view->render('accounting/form', compact('accounts', 'entry', 'lines') + ['title' => 'Modifica prima nota']);
    }

    public function show(string $id): never
    {
        $this->authorize();
        [$entry, $lines] = $this->entry((int) $id);
        $this->view->render('accounting/entry', compact('entry', 'lines') + ['title' => 'Registrazione ' . $entry['protocol_number']]);
    }

    public function save(): never
    {
        $this->authorize();
        $intent = (string) ($_POST['intent'] ?? 'draft');
        $post = $intent === 'post';
        $header = $this->headerFromRequest();
        $service = new AccountingService($this->db, Auth::organizationId(), Auth::id());
        try {
            $id = $service->saveManual($header, (array) ($_POST['lines'] ?? []), !empty($_POST['id']) ? (int) $_POST['id'] : null, $post);
        } catch (InvalidArgumentException $exception) {
            $path = !empty($_POST['id']) ? '/accounting/journal/' . (int) $_POST['id'] . '/edit' : '/accounting/journal/create';
            $this->redirect($path, $exception->getMessage(), 'error');
        }
        $this->audit($post ? 'POST' : 'SAVE_DRAFT', 'journal_entries', $id, $header);
        $this->redirect('/accounting/journal/' . $id, $post ? 'Registrazione contabilizzata e quadrata.' : 'Bozza di prima nota salvata.');
    }

    public function post(): never
    {
        $_POST['intent'] = 'post';
        $this->save();
    }

    public function postDraft(string $id): never
    {
        $this->authorize();
        try {
            (new AccountingService($this->db, Auth::organizationId(), Auth::id()))->postDraft((int) $id);
        } catch (InvalidArgumentException $exception) {
            $this->redirect('/accounting/journal/' . (int) $id . '/edit', $exception->getMessage(), 'error');
        }
        $this->audit('POST', 'journal_entries', (int) $id);
        $this->redirect('/accounting/journal/' . (int) $id, 'Bozza contabilizzata definitivamente.');
    }

    public function deleteDraft(string $id): never
    {
        $this->authorize();
        $deleted = (new AccountingService($this->db, Auth::organizationId(), Auth::id()))->deleteDraft((int) $id);
        if ($deleted) {
            $this->audit('DELETE_DRAFT', 'journal_entries', (int) $id);
        }
        $this->redirect('/accounting/journal', $deleted ? 'Bozza eliminata.' : 'La registrazione non è una bozza eliminabile.', $deleted ? 'success' : 'error');
    }

    public function trialBalance(): never
    {
        $this->authorize();
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
        $this->authorize();
        $statement = $this->db->prepare('SELECT id, code, name FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $account = $statement->fetch();
        if (!$account) {
            $this->redirect('/accounting/trial-balance', 'Conto non trovato.', 'error');
        }
        $statement = $this->db->prepare(
            "SELECT e.id AS entry_id, e.entry_date, e.protocol_number, e.description AS entry_description, e.document_number,
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

    public function vatRegisters(): never
    {
        $this->authorize();
        $register = strtoupper((string) ($_GET['register'] ?? 'SALES'));
        $year = (int) ($_GET['year'] ?? date('Y'));
        $month = (int) ($_GET['month'] ?? date('n'));
        if (!in_array($register, ['SALES', 'PURCHASES', 'CORRISPETTIVI'], true)) {
            $register = 'SALES';
        }
        $statement = $this->db->prepare(
            'SELECT id, document_id, source_type, movement_date, protocol_number, counterparty_name, description,
                    vat_code, taxable_amount, vat_amount, deductible_vat
             FROM vat_movements
             WHERE organization_id = ? AND register_type = ? AND period_year = ? AND period_month = ?
             ORDER BY movement_date, id'
        );
        $statement->execute([Auth::organizationId(), $register, $year, $month]);
        $movements = $statement->fetchAll();
        $statement = $this->db->prepare(
            'SELECT COALESCE(vat_code, \'N/D\') AS vat_code, SUM(taxable_amount) AS taxable_amount,
                    SUM(vat_amount) AS vat_amount, SUM(deductible_vat) AS deductible_vat
             FROM vat_movements
             WHERE organization_id = ? AND register_type = ? AND period_year = ? AND period_month = ?
             GROUP BY COALESCE(vat_code, \'N/D\') ORDER BY vat_code'
        );
        $statement->execute([Auth::organizationId(), $register, $year, $month]);
        $summary = $statement->fetchAll();
        $totals = [
            'taxable' => array_sum(array_column($movements, 'taxable_amount')),
            'vat' => array_sum(array_column($movements, 'vat_amount')),
            'deductible' => array_sum(array_column($movements, 'deductible_vat')),
        ];
        $vatCodes = $this->db->prepare('SELECT code, description, rate, nature FROM vat_codes WHERE organization_id = ? AND active = 1 ORDER BY rate DESC, code');
        $vatCodes->execute([Auth::organizationId()]);
        $vatCodes = $vatCodes->fetchAll();
        $this->view->render('accounting/vat-registers', compact('register', 'year', 'month', 'movements', 'summary', 'totals', 'vatCodes') + ['title' => 'Registri IVA']);
    }

    public function saveVatMovement(): never
    {
        $this->authorize();
        try {
            $id = (new VatService($this->db, Auth::organizationId(), Auth::id()))->saveManual($_POST);
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/vat-registers', $exception->getMessage(), 'error');
        }
        $this->audit('CREATE', 'vat_movements', $id, ['register_type' => $_POST['register_type'] ?? null]);
        $date = strtotime((string) ($_POST['movement_date'] ?? 'now')) ?: time();
        $this->redirect('/accounting/vat-registers?register=' . urlencode((string) ($_POST['register_type'] ?? 'SALES')) . '&year=' . date('Y', $date) . '&month=' . date('n', $date), 'Movimento IVA registrato.');
    }

    public function deleteVatMovement(string $id): never
    {
        $this->authorize();
        try {
            $deleted = (new VatService($this->db, Auth::organizationId(), Auth::id()))->deleteManual((int) $id);
        } catch (RuntimeException $exception) {
            $this->redirect('/accounting/vat-registers', $exception->getMessage(), 'error');
        }
        if ($deleted) {
            $this->audit('DELETE', 'vat_movements', (int) $id);
        }
        $this->redirect('/accounting/vat-registers', $deleted ? 'Movimento IVA eliminato.' : 'Il movimento deriva da un documento o non è eliminabile.', $deleted ? 'success' : 'error');
    }

    public function syncVatDocuments(): never
    {
        $this->authorize();
        try {
            $count = (new VatService($this->db, Auth::organizationId(), Auth::id()))->syncDocuments();
        } catch (RuntimeException $exception) {
            $this->redirect('/accounting/vat-registers', $exception->getMessage(), 'error');
        }
        $this->audit('SYNC', 'vat_movements', null, ['rows' => $count]);
        $this->redirect('/accounting/vat-registers', "Registri aggiornati: {$count} righe IVA sincronizzate.");
    }

    public function vatSettlements(): never
    {
        $this->authorize();
        $year = (int) ($_GET['year'] ?? date('Y'));
        $statement = $this->db->prepare(
            'SELECT * FROM vat_settlements WHERE organization_id = ? AND period_year = ?
             ORDER BY period_type, period_number'
        );
        $statement->execute([Auth::organizationId(), $year]);
        $settlements = $statement->fetchAll();
        $this->view->render('accounting/vat-settlements', compact('settlements', 'year') + ['title' => 'Liquidazioni IVA']);
    }

    public function calculateVatSettlement(): never
    {
        $this->authorize();
        try {
            $id = (new VatService($this->db, Auth::organizationId(), Auth::id()))->calculateSettlement($_POST);
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/vat-settlements', $exception->getMessage(), 'error');
        }
        $this->audit('CALCULATE', 'vat_settlements', $id, ['period_type' => $_POST['period_type'] ?? null, 'period_number' => $_POST['period_number'] ?? null]);
        $this->redirect('/accounting/vat-settlements/' . $id, 'Liquidazione IVA calcolata sui movimenti del periodo.');
    }

    public function showVatSettlement(string $id): never
    {
        $this->authorize();
        $statement = $this->db->prepare('SELECT * FROM vat_settlements WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $settlement = $statement->fetch();
        if (!$settlement) {
            $this->redirect('/accounting/vat-settlements', 'Liquidazione non trovata.', 'error');
        }
        $statement = $this->db->prepare(
            'SELECT * FROM vat_settlement_details WHERE settlement_id = ? AND organization_id = ? ORDER BY register_type, vat_code'
        );
        $statement->execute([(int) $id, Auth::organizationId()]);
        $details = $statement->fetchAll();
        $this->view->render('accounting/vat-settlement', compact('settlement', 'details') + ['title' => 'Liquidazione IVA']);
    }

    public function updateVatSettlementStatus(string $id): never
    {
        $this->authorize();
        $status = strtoupper((string) ($_POST['status'] ?? ''));
        try {
            (new VatService($this->db, Auth::organizationId(), Auth::id()))->updateSettlementStatus((int) $id, $status, $_POST['payment_date'] ?? null);
        } catch (InvalidArgumentException $exception) {
            $this->redirect('/accounting/vat-settlements/' . (int) $id, $exception->getMessage(), 'error');
        }
        $this->audit('STATUS_CHANGE', 'vat_settlements', (int) $id, ['status' => $status]);
        $this->redirect('/accounting/vat-settlements/' . (int) $id, 'Stato della liquidazione aggiornato.');
    }

    private function authorize(): void
    {
        $this->requireFeature('accounting');
        $this->requireRoles(self::ROLES);
    }

    private function accounts(): array
    {
        $statement = $this->db->prepare(
            'SELECT id, code, name, account_type FROM chart_of_accounts
             WHERE organization_id = ? AND active = 1 AND is_postable = 1 ORDER BY code'
        );
        $statement->execute([Auth::organizationId()]);
        return $statement->fetchAll();
    }

    private function entry(int $id): array
    {
        $statement = $this->db->prepare('SELECT * FROM journal_entries WHERE id = ? AND organization_id = ?');
        $statement->execute([$id, Auth::organizationId()]);
        $entry = $statement->fetch();
        if (!$entry) {
            $this->redirect('/accounting/journal', 'Registrazione non trovata.', 'error');
        }
        $statement = $this->db->prepare(
            'SELECT l.*, a.code AS account_code, a.name AS account_name
             FROM journal_entry_lines l JOIN chart_of_accounts a ON a.id = l.account_id
             WHERE l.journal_entry_id = ? AND l.organization_id = ? ORDER BY l.line_number'
        );
        $statement->execute([$id, Auth::organizationId()]);
        return [$entry, $statement->fetchAll()];
    }

    private function headerFromRequest(): array
    {
        return [
            'entry_date' => $_POST['entry_date'] ?? date('Y-m-d'),
            'competence_date' => $_POST['competence_date'] ?? null,
            'entry_type' => $_POST['entry_type'] ?? 'MANUAL',
            'description' => $_POST['description'] ?? '',
            'document_number' => $_POST['document_number'] ?? null,
            'counterparty' => $_POST['counterparty'] ?? null,
            'notes' => $_POST['notes'] ?? null,
        ];
    }
}
