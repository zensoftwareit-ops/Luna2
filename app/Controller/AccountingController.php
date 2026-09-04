<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\TabularExportService;
use Luna\Service\VatService;
use RuntimeException;

final class AccountingController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function journal(): never
    {
        $this->authorize();
        [$entries, $from, $to, $status, $type, $search, $accountId] = $this->journalDataset(1500);
        $totals = [
            'debit' => array_sum(array_column($entries, 'total_debit')),
            'credit' => array_sum(array_column($entries, 'total_credit')),
            'drafts' => count(array_filter($entries, static fn (array $entry): bool => $entry['status'] === 'DRAFT')),
        ];
        $accounts = $this->accounts();
        $this->view->render('accounting/journal', compact(
            'entries', 'totals', 'from', 'to', 'status', 'type', 'search', 'accountId', 'accounts'
        ) + ['title' => 'Prima nota']);
    }

    public function exportJournal(string $format): never
    {
        $this->authorize();
        [$rows, $from, $to, $status, $type, $search, $accountId] = $this->journalDataset(null);
        $this->exporter()->stream($format, 'Prima nota', [
            ['key' => 'entry_date', 'label' => 'Data', 'type' => 'date'],
            ['key' => 'protocol_number', 'label' => 'Protocollo'],
            ['key' => 'entry_type', 'label' => 'Tipo'],
            ['key' => 'description', 'label' => 'Descrizione'],
            ['key' => 'document_number', 'label' => 'Documento'],
            ['key' => 'counterparty', 'label' => 'Controparte'],
            ['key' => 'total_debit', 'label' => 'Dare', 'type' => 'money'],
            ['key' => 'total_credit', 'label' => 'Avere', 'type' => 'money'],
            ['key' => 'status', 'label' => 'Stato'],
        ], $rows, array_filter(['Dal' => $from, 'Al' => $to, 'Ricerca' => $search, 'Stato' => $status, 'Tipo' => $type, 'Conto ID' => $accountId ? (string) $accountId : '']), Auth::organizationName(), 'prima-nota');
    }

    public function create(): never
    {
        $this->authorize();
        $accounts = $this->accounts();
        [$causes, $vatRegisters] = $this->journalOptions();
        $entry = ['entry_date' => date('Y-m-d'), 'competence_date' => date('Y-m-d'), 'entry_type' => 'MANUAL'];
        $lines = [];
        $this->view->render('accounting/form', compact('accounts', 'causes', 'vatRegisters', 'entry', 'lines') + ['title' => 'Nuova registrazione']);
    }

    public function edit(string $id): never
    {
        $this->authorize();
        [$entry, $lines] = $this->entry((int) $id);
        if ($entry['status'] !== 'DRAFT' || $entry['source_type'] !== 'MANUAL') {
            $this->redirect('/accounting/journal/' . (int) $id, 'Solo le bozze manuali possono essere modificate.', 'error');
        }
        $accounts = $this->accounts();
        [$causes, $vatRegisters] = $this->journalOptions();
        $this->view->render('accounting/form', compact('accounts', 'causes', 'vatRegisters', 'entry', 'lines') + ['title' => 'Modifica prima nota']);
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
        [$accounts, $from, $to, $search, $accountType] = $this->trialBalanceDataset();
        $totals = ['debit' => array_sum(array_column($accounts, 'debit')), 'credit' => array_sum(array_column($accounts, 'credit'))];
        $this->view->render('accounting/trial-balance', compact('accounts', 'totals', 'from', 'to', 'search', 'accountType') + ['title' => 'Bilancio di verifica e mastrini']);
    }

    public function exportTrialBalance(string $format): never
    {
        $this->authorize();
        [$rows, $from, $to, $search, $accountType] = $this->trialBalanceDataset();
        $totals = [
            'debit' => round((float) array_sum(array_column($rows, 'debit')), 2),
            'credit' => round((float) array_sum(array_column($rows, 'credit')), 2),
        ];
        $difference = round($totals['debit'] - $totals['credit'], 2);
        $rows[] = ['code' => '', 'name' => 'TOTALI SALDI', 'account_type' => '', 'debit' => $totals['debit'], 'credit' => $totals['credit']];
        $rows[] = [
            'code' => '', 'name' => 'DIFFERENZA DARE / AVERE', 'account_type' => '',
            'debit' => $difference > 0 ? $difference : 0,
            'credit' => $difference < 0 ? abs($difference) : 0,
        ];
        $this->exporter()->stream($format, 'Bilancio di verifica', [
            ['key' => 'code', 'label' => 'Conto'],
            ['key' => 'name', 'label' => 'Descrizione'],
            ['key' => 'account_type', 'label' => 'Tipo'],
            ['key' => 'debit', 'label' => 'Saldo Dare', 'type' => 'money'],
            ['key' => 'credit', 'label' => 'Saldo Avere', 'type' => 'money'],
        ], $rows, array_filter(['Dal' => $from, 'Al' => $to, 'Ricerca conto' => $search, 'Tipo conto' => $accountType]), Auth::organizationName(), 'bilancio-verifica');
    }

    private function trialBalanceDataset(): array
    {
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $search = trim((string) ($_GET['q'] ?? ''));
        $accountType = strtoupper(trim((string) ($_GET['account_type'] ?? '')));
        $where = 'a.organization_id = :organization_id';
        $params = ['date_from' => $from, 'date_to' => $to, 'organization_id' => Auth::organizationId()];
        if ($search !== '') {
            $where .= ' AND (a.code LIKE :search_code OR a.name LIKE :search_name)';
            $params['search_code'] = '%' . $search . '%';
            $params['search_name'] = '%' . $search . '%';
        }
        if ($accountType !== '') {
            $where .= ' AND a.account_type = :account_type';
            $params['account_type'] = $accountType;
        }
        $statement = $this->db->prepare(
            "SELECT a.id, a.code, a.name, a.account_type,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit ELSE 0 END), 0) AS debit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit ELSE 0 END), 0) AS credit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit - l.credit ELSE 0 END), 0) AS balance
             FROM chart_of_accounts a
             LEFT JOIN journal_entry_lines l ON l.account_id = a.id
             LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.status = 'POSTED'
                  AND e.entry_date BETWEEN :date_from AND :date_to
             WHERE {$where}
             GROUP BY a.id, a.code, a.name, a.account_type
             HAVING ABS(balance) > 0.005
             ORDER BY a.code"
        );
        $statement->execute($params);
        $accounts = array_map(static function (array $account): array {
            $balance = round((float) $account['balance'], 2);
            $account['movement_debit'] = (float) $account['debit'];
            $account['movement_credit'] = (float) $account['credit'];
            $account['debit'] = $balance > 0 ? $balance : 0.0;
            $account['credit'] = $balance < 0 ? abs($balance) : 0.0;
            return $account;
        }, $statement->fetchAll());
        return [$accounts, $from, $to, $search, $accountType];
    }

    public function ledger(string $id): never
    {
        $this->authorize();
        [$account, $lines, $from, $to, $search] = $this->ledgerDataset((int) $id);
        $accounts = $this->accounts();
        $this->view->render('accounting/ledger', compact('account', 'lines', 'from', 'to', 'search', 'accounts') + ['title' => 'Mastrino ' . $account['code']]);
    }

    public function exportLedger(string $id, string $format): never
    {
        $this->authorize();
        [$account, $rows, $from, $to, $search] = $this->ledgerDataset((int) $id);
        $this->exporter()->stream($format, 'Mastrino ' . $account['code'] . ' · ' . $account['name'], [
            ['key' => 'entry_date', 'label' => 'Data', 'type' => 'date'],
            ['key' => 'protocol_number', 'label' => 'Protocollo'],
            ['key' => 'entry_description', 'label' => 'Registrazione'],
            ['key' => 'description', 'label' => 'Descrizione riga'],
            ['key' => 'document_number', 'label' => 'Documento'],
            ['key' => 'counterparty', 'label' => 'Controparte'],
            ['key' => 'debit', 'label' => 'Dare', 'type' => 'money'],
            ['key' => 'credit', 'label' => 'Avere', 'type' => 'money'],
            ['key' => 'running_balance', 'label' => 'Saldo progressivo', 'type' => 'money'],
        ], $rows, array_filter(['Dal' => $from, 'Al' => $to, 'Ricerca' => $search]), Auth::organizationName(), 'mastrino-' . $account['code']);
    }

    private function ledgerDataset(int $id): array
    {
        $statement = $this->db->prepare('SELECT id, code, name FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
        $statement->execute([$id, Auth::organizationId()]);
        $account = $statement->fetch();
        if (!$account) {
            $this->redirect('/accounting/trial-balance', 'Conto non trovato.', 'error');
        }
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $search = trim((string) ($_GET['q'] ?? ''));
        $filter = '';
        $openingStatement = $this->db->prepare(
            "SELECT COALESCE(SUM(l.debit - l.credit), 0)
             FROM journal_entry_lines l JOIN journal_entries e ON e.id = l.journal_entry_id
             WHERE l.organization_id = ? AND l.account_id = ? AND e.status = 'POSTED' AND e.entry_date < ?"
        );
        $openingStatement->execute([Auth::organizationId(), $id, $from]);
        $openingBalance = (float) $openingStatement->fetchColumn();
        $params = [
            'opening_balance' => $openingBalance, 'organization_id' => Auth::organizationId(),
            'account_id' => $id, 'date_from' => $from, 'date_to' => $to,
        ];
        if ($search !== '') {
            $filter = ' AND (e.protocol_number LIKE :search OR e.description LIKE :search OR e.document_number LIKE :search OR e.counterparty LIKE :search OR l.description LIKE :search)';
            $params['search'] = '%' . $search . '%';
        }
        $statement = $this->db->prepare(
            "SELECT e.id AS entry_id, e.entry_date, e.protocol_number, e.description AS entry_description, e.document_number,
                    e.counterparty, l.description, l.debit, l.credit,
                    :opening_balance + SUM(l.debit - l.credit) OVER (ORDER BY e.entry_date, e.id, l.line_number) AS running_balance
             FROM journal_entry_lines l JOIN journal_entries e ON e.id = l.journal_entry_id
             WHERE l.organization_id = :organization_id AND l.account_id = :account_id AND e.status = 'POSTED'
               AND e.entry_date BETWEEN :date_from AND :date_to {$filter}
             ORDER BY e.entry_date, e.id, l.line_number"
        );
        $statement->execute($params);
        $account['opening_balance'] = $openingBalance;
        return [$account, $statement->fetchAll(), $from, $to, $search];
    }

    public function vatRegisters(): never
    {
        $this->authorize();
        [$movements, $register, $year, $month, $search] = $this->vatMovementsDataset();
        $summary = $this->vatSummary($movements);
        $totals = [
            'taxable' => array_sum(array_column($movements, 'taxable_amount')),
            'vat' => array_sum(array_column($movements, 'vat_amount')),
            'deductible' => array_sum(array_column($movements, 'deductible_vat')),
        ];
        $vatCodes = $this->db->prepare('SELECT code, description, rate, nature FROM vat_codes WHERE organization_id = ? AND active = 1 ORDER BY rate DESC, code');
        $vatCodes->execute([Auth::organizationId()]);
        $vatCodes = $vatCodes->fetchAll();
        $registers = $this->db->prepare('SELECT id, code, name, register_type FROM vat_registers WHERE organization_id = ? AND active = 1 ORDER BY register_type, code');
        $registers->execute([Auth::organizationId()]);
        $registers = $registers->fetchAll();
        $this->view->render('accounting/vat-registers', compact('register', 'year', 'month', 'search', 'movements', 'summary', 'totals', 'vatCodes', 'registers') + ['title' => 'Registri IVA']);
    }

    public function exportVatRegisters(string $format): never
    {
        $this->authorize();
        [$rows, $register, $year, $month, $search] = $this->vatMovementsDataset();
        $summary = $this->vatSummary($rows);
        foreach ($summary as $item) {
            $rows[] = [
                'movement_date' => null,
                'protocol_number' => 'RIEPILOGO',
                'counterparty_name' => $item['vat_description'],
                'description' => $item['vat_nature'] ? 'Natura ' . $item['vat_nature'] : 'Totale articolo IVA',
                'vat_code' => $item['vat_code'],
                'vat_rate' => $item['vat_rate'],
                'vat_description' => $item['vat_description'],
                'vat_nature' => $item['vat_nature'],
                'taxable_amount' => $item['taxable_amount'],
                'vat_amount' => $item['vat_amount'],
                'vat_due_amount' => $item['vat_due_amount'],
                'deductible_vat' => $item['deductible_vat'],
            ];
        }
        $this->exporter()->stream($format, 'Registro IVA ' . $register, [
            ['key' => 'movement_date', 'label' => 'Data', 'type' => 'date'],
            ['key' => 'protocol_number', 'label' => 'Protocollo'],
            ['key' => 'counterparty_name', 'label' => 'Controparte'],
            ['key' => 'description', 'label' => 'Descrizione'],
            ['key' => 'vat_code', 'label' => 'Codice IVA'],
            ['key' => 'vat_rate', 'label' => 'Aliquota %', 'type' => 'decimal'],
            ['key' => 'vat_description', 'label' => 'Articolo IVA'],
            ['key' => 'vat_nature', 'label' => 'Natura'],
            ['key' => 'taxable_amount', 'label' => 'Imponibile', 'type' => 'money'],
            ['key' => 'vat_amount', 'label' => 'IVA', 'type' => 'money'],
            ['key' => 'vat_due_amount', 'label' => 'IVA dovuta', 'type' => 'money'],
            ['key' => 'deductible_vat', 'label' => 'IVA detraibile', 'type' => 'money'],
        ], $rows, array_filter(['Registro' => $register, 'Anno' => (string) $year, 'Mese' => (string) $month, 'Ricerca' => $search]), Auth::organizationName(), 'registro-iva-' . strtolower($register));
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

    public function exportVatSettlements(string $format): never
    {
        $this->authorize();
        $year = (int) ($_GET['year'] ?? date('Y'));
        $statement = $this->db->prepare(
            'SELECT period_type, period_number, vat_debit, vat_credit, previous_credit,
                    interest_amount, balance, payment_due_date, payment_date, status
             FROM vat_settlements WHERE organization_id = ? AND period_year = ? ORDER BY period_type, period_number'
        );
        $statement->execute([Auth::organizationId(), $year]);
        $this->exporter()->stream($format, 'Liquidazioni IVA ' . $year, [
            ['key' => 'period_type', 'label' => 'Periodicità'],
            ['key' => 'period_number', 'label' => 'Periodo', 'type' => 'number'],
            ['key' => 'vat_debit', 'label' => 'IVA a debito', 'type' => 'money'],
            ['key' => 'vat_credit', 'label' => 'IVA a credito', 'type' => 'money'],
            ['key' => 'previous_credit', 'label' => 'Credito precedente', 'type' => 'money'],
            ['key' => 'interest_amount', 'label' => 'Interessi', 'type' => 'money'],
            ['key' => 'balance', 'label' => 'Saldo', 'type' => 'money'],
            ['key' => 'payment_due_date', 'label' => 'Scadenza', 'type' => 'date'],
            ['key' => 'payment_date', 'label' => 'Pagamento', 'type' => 'date'],
            ['key' => 'status', 'label' => 'Stato'],
        ], $statement->fetchAll(), ['Anno' => (string) $year], Auth::organizationName(), 'liquidazioni-iva-' . $year);
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
            'SELECT d.*, COALESCE(c.description, d.vat_code) AS vat_description,
                    COALESCE(c.rate, 0) AS vat_rate, COALESCE(c.nature, \'\') AS vat_nature
             FROM vat_settlement_details d
             LEFT JOIN vat_codes c ON c.organization_id = d.organization_id AND c.code = d.vat_code
             WHERE d.settlement_id = ? AND d.organization_id = ? ORDER BY d.register_type, d.vat_code, c.rate'
        );
        $statement->execute([(int) $id, Auth::organizationId()]);
        $details = $statement->fetchAll();
        $this->view->render('accounting/vat-settlement', compact('settlement', 'details') + ['title' => 'Liquidazione IVA']);
    }

    public function exportVatSettlement(string $id, string $format): never
    {
        $this->authorize();
        $statement = $this->db->prepare('SELECT * FROM vat_settlements WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $settlement = $statement->fetch();
        if (!$settlement) {
            $this->redirect('/accounting/vat-settlements', 'Liquidazione non trovata.', 'error');
        }
        $statement = $this->db->prepare(
            'SELECT d.register_type, d.vat_code, COALESCE(c.description, d.vat_code) AS vat_description,
                    COALESCE(c.rate, 0) AS vat_rate, COALESCE(c.nature, \'\') AS vat_nature,
                    d.taxable_amount, d.vat_amount, d.deductible_vat
             FROM vat_settlement_details d
             LEFT JOIN vat_codes c ON c.organization_id = d.organization_id AND c.code = d.vat_code
             WHERE d.settlement_id = ? AND d.organization_id = ? ORDER BY d.register_type, d.vat_code, c.rate'
        );
        $statement->execute([(int) $id, Auth::organizationId()]);
        $rows = $statement->fetchAll();
        $rows[] = [
            'register_type' => 'TOTALE LIQUIDAZIONE', 'vat_code' => '', 'vat_description' => '', 'vat_rate' => null,
            'vat_nature' => '', 'taxable_amount' => array_sum(array_column($rows, 'taxable_amount')),
            'vat_amount' => $settlement['vat_debit'], 'deductible_vat' => $settlement['vat_credit'],
        ];
        $this->exporter()->stream($format, 'Liquidazione IVA', [
            ['key' => 'register_type', 'label' => 'Registro'],
            ['key' => 'vat_code', 'label' => 'Codice IVA'],
            ['key' => 'vat_description', 'label' => 'Articolo IVA'],
            ['key' => 'vat_rate', 'label' => 'Aliquota %', 'type' => 'decimal'],
            ['key' => 'vat_nature', 'label' => 'Natura'],
            ['key' => 'taxable_amount', 'label' => 'Imponibile', 'type' => 'money'],
            ['key' => 'vat_amount', 'label' => 'IVA a debito', 'type' => 'money'],
            ['key' => 'deductible_vat', 'label' => 'IVA detraibile', 'type' => 'money'],
        ], $rows, [
            'Periodicita' => (string) $settlement['period_type'],
            'Anno' => (string) $settlement['period_year'],
            'Periodo' => (string) $settlement['period_number'],
        ], Auth::organizationName(), 'liquidazione-iva-' . $settlement['period_year'] . '-' . $settlement['period_number']);
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

    private function journalDataset(?int $limit): array
    {
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $status = strtoupper((string) ($_GET['status'] ?? ''));
        $type = strtoupper((string) ($_GET['type'] ?? ''));
        $search = trim((string) ($_GET['q'] ?? ''));
        $accountId = max(0, (int) ($_GET['account_id'] ?? 0));
        $sql = 'SELECT id, protocol_number, entry_date, competence_date, entry_type, description, document_number,
                       counterparty, total_debit, total_credit, status, source_type
                FROM journal_entries e WHERE organization_id = :organization_id AND entry_date BETWEEN :date_from AND :date_to';
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
        if ($accountId > 0) {
            $sql .= ' AND EXISTS (SELECT 1 FROM journal_entry_lines fl WHERE fl.journal_entry_id = e.id AND fl.organization_id = e.organization_id AND fl.account_id = :account_id)';
            $params['account_id'] = $accountId;
        }
        $sql .= ' ORDER BY entry_date DESC, id DESC';
        if ($limit !== null) {
            $sql .= ' LIMIT ' . max(1, $limit);
        }
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return [$statement->fetchAll(), $from, $to, $status, $type, $search, $accountId];
    }

    private function vatMovementsDataset(): array
    {
        $register = strtoupper((string) ($_GET['register'] ?? 'SALES'));
        $year = (int) ($_GET['year'] ?? date('Y'));
        $month = (int) ($_GET['month'] ?? date('n'));
        $search = trim((string) ($_GET['q'] ?? ''));
        if (!in_array($register, ['SALES', 'PURCHASES', 'CORRISPETTIVI', 'REVERSE_CHARGE', 'SELF_INVOICES'], true)) {
            $register = 'SALES';
        }
        $sql = 'SELECT m.id, m.document_id, m.source_type, m.movement_date, m.protocol_number, m.counterparty_name, m.description,
                       COALESCE(m.vat_code, \'N/D\') AS vat_code, COALESCE(c.description, m.vat_code, \'N/D\') AS vat_description,
                       COALESCE(c.rate, 0) AS vat_rate, COALESCE(c.nature, \'\') AS vat_nature,
                       m.taxable_amount, m.vat_amount, m.vat_due_amount, m.deductible_vat, m.deductibility_percent,
                       m.operation_type, m.collectability, m.vat_register_id
                FROM vat_movements m
                LEFT JOIN vat_codes c ON c.organization_id = m.organization_id AND c.code = m.vat_code
                LEFT JOIN documents d ON d.id = m.document_id AND d.organization_id = m.organization_id
                WHERE m.organization_id = :organization_id
                  AND m.period_year = :period_year AND m.period_month = :period_month';
        $params = [
            'organization_id' => Auth::organizationId(), 'period_year' => $year, 'period_month' => $month,
        ];
        if ($register === 'REVERSE_CHARGE') {
            $sql .= " AND m.operation_type = 'REVERSE_CHARGE'";
        } elseif ($register === 'SELF_INVOICES') {
            $sql .= " AND d.fatturapa_type IN ('TD16','TD17','TD18','TD19','TD20','TD21','TD27','TD28')";
        } else {
            $sql .= ' AND m.register_type = :register_type';
            $params['register_type'] = $register;
        }
        if ($search !== '') {
            $sql .= ' AND (m.protocol_number LIKE :search_protocol OR m.counterparty_name LIKE :search_party
                       OR m.description LIKE :search_description OR m.vat_code LIKE :search_vat_code)';
            $searchValue = '%' . $search . '%';
            $params['search_protocol'] = $searchValue;
            $params['search_party'] = $searchValue;
            $params['search_description'] = $searchValue;
            $params['search_vat_code'] = $searchValue;
        }
        $sql .= ' ORDER BY m.movement_date, m.id';
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return [$statement->fetchAll(), $register, $year, $month, $search];
    }

    private function vatSummary(array $movements): array
    {
        $summary = [];
        foreach ($movements as $movement) {
            $code = (string) ($movement['vat_code'] ?: 'N/D');
            $rate = round((float) ($movement['vat_rate'] ?? 0), 2);
            $nature = (string) ($movement['vat_nature'] ?? '');
            $key = $code . '|' . number_format($rate, 2, '.', '') . '|' . $nature;
            if (!isset($summary[$key])) {
                $summary[$key] = [
                    'vat_code' => $code,
                    'vat_description' => (string) ($movement['vat_description'] ?? $code),
                    'vat_rate' => $rate,
                    'vat_nature' => $nature,
                    'taxable_amount' => 0.0,
                    'vat_amount' => 0.0,
                    'vat_due_amount' => 0.0,
                    'deductible_vat' => 0.0,
                ];
            }
            foreach (['taxable_amount', 'vat_amount', 'vat_due_amount', 'deductible_vat'] as $amount) {
                $summary[$key][$amount] += (float) ($movement[$amount] ?? 0);
            }
        }
        foreach ($summary as &$row) {
            foreach (['taxable_amount', 'vat_amount', 'vat_due_amount', 'deductible_vat'] as $amount) {
                $row[$amount] = round($row[$amount], 2);
            }
        }
        unset($row);
        uasort($summary, static fn (array $a, array $b): int => [$a['vat_code'], $a['vat_rate'], $a['vat_nature']] <=> [$b['vat_code'], $b['vat_rate'], $b['vat_nature']]);
        return array_values($summary);
    }

    private function exporter(): TabularExportService
    {
        return new TabularExportService();
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

    private function journalOptions(): array
    {
        $statement = $this->db->prepare('SELECT id, code, name, category FROM accounting_causes WHERE organization_id = ? AND active = 1 ORDER BY code');
        $statement->execute([Auth::organizationId()]);
        $causes = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT id, code, name, register_type FROM vat_registers WHERE organization_id = ? AND active = 1 ORDER BY register_type, code');
        $statement->execute([Auth::organizationId()]);
        return [$causes, $statement->fetchAll()];
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
            'cause_id' => $_POST['cause_id'] ?? null,
            'vat_register_id' => $_POST['vat_register_id'] ?? null,
            'description' => $_POST['description'] ?? '',
            'document_number' => $_POST['document_number'] ?? null,
            'source_protocol' => $_POST['source_protocol'] ?? null,
            'counterparty' => $_POST['counterparty'] ?? null,
            'notes' => $_POST['notes'] ?? null,
        ];
    }
}
