<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\PartyLedgerService;
use Luna\Service\TabularExportService;

final class PartyLedgerController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function index(): never
    {
        $this->authorize();
        $type = strtoupper((string) ($_GET['party_type'] ?? 'SUPPLIER'));
        if (!in_array($type, ['CUSTOMER', 'SUPPLIER'], true)) {
            $type = 'SUPPLIER';
        }
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $search = trim((string) ($_GET['q'] ?? ''));
        $partyId = max(0, (int) ($_GET['party_id'] ?? 0));
        try {
            $service = $this->service();
            $synchronized = $service->synchronize();
            $parties = $service->overview($type, $from, $to, $search);
            $detail = $partyId > 0 ? $service->detail($type, $partyId, $from, $to) : [];
            $unassigned = $service->unassigned($type);
            $unassignedCount = $service->unassignedCount($type);
            $reconcileLineId = max(0, (int) ($_GET['reconcile_line'] ?? 0));
            $reconcileLine = $reconcileLineId > 0
                ? (array_values(array_filter($unassigned, static fn (array $line): bool => (int) $line['line_id'] === $reconcileLineId))[0] ?? [])
                : [];
            $partyChoices = $reconcileLine !== [] ? $service->partyChoices($type) : [];
        } catch (InvalidArgumentException $exception) {
            $this->redirect('/accounting/subledgers', $exception->getMessage(), 'error');
        }
        $totals = [
            'debit' => array_sum(array_column($parties, 'period_debit')),
            'credit' => array_sum(array_column($parties, 'period_credit')),
            'balance' => array_sum(array_column($parties, 'closing_balance')),
            'outstanding' => array_sum(array_column($parties, 'outstanding')),
        ];
        $this->view->render('accounting/subledgers', compact(
            'type', 'from', 'to', 'search', 'parties', 'partyId', 'detail', 'totals', 'synchronized',
            'unassigned', 'unassignedCount', 'reconcileLineId', 'reconcileLine', 'partyChoices'
        ) + ['title' => 'Partitario clienti e fornitori']);
    }

    public function reconcile(): never
    {
        $this->authorize();
        $type = strtoupper((string) ($_POST['party_type'] ?? ''));
        try {
            $result = $this->service()->assignLine(
                $type,
                (int) ($_POST['line_id'] ?? 0),
                (int) ($_POST['party_id'] ?? 0),
            );
            $this->audit('RECONCILE_PARTY_SUBLEDGER', 'journal_entry_lines', $result['line_id'], $result);
            $this->redirect('/accounting/subledgers?party_type=' . rawurlencode($type), 'Movimento associato al sottoconto “' . $result['party_name'] . '”.');
        } catch (InvalidArgumentException $exception) {
            $this->redirect('/accounting/subledgers?party_type=' . rawurlencode($type ?: 'SUPPLIER'), $exception->getMessage(), 'error');
        }
    }

    public function export(string $format): never
    {
        $this->authorize();
        $type = strtoupper((string) ($_GET['party_type'] ?? 'SUPPLIER'));
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $search = trim((string) ($_GET['q'] ?? ''));
        $partyId = max(0, (int) ($_GET['party_id'] ?? 0));
        $service = $this->service();
        if ($partyId > 0) {
            $detail = $service->detail($type, $partyId, $from, $to);
            $rows = $detail['rows'];
            array_unshift($rows, ['entry_description' => 'SALDO INIZIALE', 'running_balance' => $detail['totals']['opening']]);
            $rows[] = ['entry_description' => 'TOTALI / SALDO FINALE', 'debit' => $detail['totals']['debit'],
                'credit' => $detail['totals']['credit'], 'running_balance' => $detail['totals']['closing']];
            $this->exporter()->stream($format, 'Partitario · ' . $detail['party']['business_name'], [
                ['key' => 'entry_date', 'label' => 'Data', 'type' => 'date'],
                ['key' => 'protocol_number', 'label' => 'Protocollo'],
                ['key' => 'document_number', 'label' => 'Documento'],
                ['key' => 'entry_description', 'label' => 'Registrazione'],
                ['key' => 'description', 'label' => 'Descrizione'],
                ['key' => 'debit', 'label' => 'Dare', 'type' => 'money'],
                ['key' => 'credit', 'label' => 'Avere', 'type' => 'money'],
                ['key' => 'running_balance', 'label' => 'Saldo progressivo', 'type' => 'money'],
            ], ['Dal' => $from, 'Al' => $to, 'Sottoconto' => $detail['party']['code']], Auth::organizationName(), 'partitario-' . $detail['party']['code']);
        }
        $rows = $service->overview($type, $from, $to, $search);
        $this->exporter()->stream($format, $type === 'SUPPLIER' ? 'Partitario fornitori' : 'Partitario clienti', [
            ['key' => 'code', 'label' => 'Sottoconto'],
            ['key' => 'business_name', 'label' => 'Ragione sociale'],
            ['key' => 'vat_number', 'label' => 'Partita IVA'],
            ['key' => 'opening_balance', 'label' => 'Saldo iniziale', 'type' => 'money'],
            ['key' => 'period_debit', 'label' => 'Dare', 'type' => 'money'],
            ['key' => 'period_credit', 'label' => 'Avere', 'type' => 'money'],
            ['key' => 'closing_balance', 'label' => 'Saldo finale', 'type' => 'money'],
            ['key' => 'outstanding', 'label' => $type === 'SUPPLIER' ? 'Da pagare' : 'Da incassare', 'type' => 'money'],
        ], array_filter(['Dal' => $from, 'Al' => $to, 'Ricerca' => $search]), Auth::organizationName(), $type === 'SUPPLIER' ? 'partitario-fornitori' : 'partitario-clienti');
    }

    private function authorize(): void
    {
        $this->requireFeature('accounting');
        $this->requireRoles(self::ROLES);
    }

    private function service(): PartyLedgerService
    {
        return new PartyLedgerService($this->db, Auth::organizationId());
    }

    private function exporter(): TabularExportService
    {
        $statement = $this->db->prepare('SELECT business_name, vat_number, tax_code, address, postal_code, city, province FROM organizations WHERE id = ?');
        $statement->execute([Auth::organizationId()]);
        return new TabularExportService($statement->fetch() ?: []);
    }
}
