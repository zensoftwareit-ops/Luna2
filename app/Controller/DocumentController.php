<?php

declare(strict_types=1);

namespace Luna\Controller;

use Dompdf\Dompdf;
use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\FatturaPaService;
use Luna\Service\PartyAutomationService;
use Luna\Service\ReceivablesService;
use Luna\Service\DocumentNumberService;
use Luna\Service\DocumentWorkflowService;
use Luna\Service\TabularExportService;
use Throwable;

final class DocumentController extends BaseController
{
    private const TYPES = [
        'quotes' => ['feature' => 'sales', 'code' => 'QUOTE', 'title' => 'Preventivi', 'singular' => 'Preventivo', 'prefix' => 'PREV', 'counterparty' => 'customer'],
        'orders' => ['feature' => 'sales', 'code' => 'SALES_ORDER', 'title' => 'Ordini clienti', 'singular' => 'Ordine', 'prefix' => 'ORD', 'counterparty' => 'customer'],
        'ddt' => ['feature' => 'sales', 'code' => 'DDT', 'title' => 'Documenti di trasporto', 'singular' => 'DDT', 'prefix' => 'DDT', 'counterparty' => 'customer'],
        'invoices' => ['feature' => 'sales', 'code' => 'SALES_INVOICE', 'title' => 'Fatture attive', 'singular' => 'Fattura', 'prefix' => 'FT', 'counterparty' => 'customer'],
        'credit-notes' => ['feature' => 'sales', 'code' => 'CREDIT_NOTE', 'title' => 'Note di credito', 'singular' => 'Nota di credito', 'prefix' => 'NC', 'counterparty' => 'customer'],
        'proformas' => ['feature' => 'sales', 'code' => 'PROFORMA', 'title' => 'Proforma', 'singular' => 'Proforma', 'prefix' => 'PF', 'counterparty' => 'customer'],
        'purchase-orders' => ['feature' => 'purchases', 'code' => 'PURCHASE_ORDER', 'title' => 'Ordini fornitori', 'singular' => 'Ordine fornitore', 'prefix' => 'ORDF', 'counterparty' => 'supplier'],
        'purchase-invoices' => ['feature' => 'purchases', 'code' => 'PURCHASE_INVOICE', 'title' => 'Fatture passive', 'singular' => 'Fattura passiva', 'prefix' => 'FP', 'counterparty' => 'supplier'],
    ];

    public function index(string $type): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'VIEWER']);
        $definition = $this->type($type);
        [
            $documents, $search, $from, $to, $counterpartyId, $status,
            $sort, $direction, $perPage, $page, $pages, $total,
        ] = $this->documentList($definition, true);
        $table = $definition['counterparty'] === 'customer' ? 'customers' : 'suppliers';
        $statement = $this->db->prepare("SELECT id, code, business_name FROM {$table} WHERE organization_id = ? ORDER BY business_name");
        $statement->execute([Auth::organizationId()]);
        $counterparties = $statement->fetchAll();
        $this->view->render('documents/index', compact(
            'type', 'definition', 'documents', 'search', 'from', 'to', 'counterpartyId', 'status', 'counterparties',
            'sort', 'direction', 'perPage', 'page', 'pages', 'total'
        ) + ['title' => $definition['title']]);
    }

    public function export(string $type, string $format): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'VIEWER']);
        $definition = $this->type($type);
        [$documents, $search, $from, $to, $counterpartyId, $status] = $this->documentList($definition, false);
        (new TabularExportService())->stream(
            $format,
            $definition['title'],
            [
                ['key' => 'number', 'label' => 'Documento'],
                ['key' => 'document_date', 'label' => 'Data', 'type' => 'date'],
                ['key' => 'due_date', 'label' => 'Scadenza', 'type' => 'date'],
                ['key' => 'counterparty_name', 'label' => $definition['counterparty'] === 'customer' ? 'Cliente' : 'Fornitore'],
                ['key' => 'subject', 'label' => 'Oggetto'],
                ['key' => 'taxable_total', 'label' => 'Imponibile', 'type' => 'money'],
                ['key' => 'vat_total', 'label' => 'IVA', 'type' => 'money'],
                ['key' => 'total', 'label' => 'Totale', 'type' => 'money'],
                ['key' => 'balance_due', 'label' => 'Residuo', 'type' => 'money'],
                ['key' => 'status', 'label' => 'Stato'],
            ],
            $documents,
            array_filter([
                'Ricerca' => $search,
                'Dal' => $from,
                'Al' => $to,
                $definition['counterparty'] === 'customer' ? 'Cliente ID' : 'Fornitore ID' => $counterpartyId ? (string) $counterpartyId : '',
                'Stato' => $status,
            ]),
            Auth::organizationName(),
            $type,
        );
    }

    public function create(string $type): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        $table = $definition['counterparty'] === 'customer' ? 'customers' : 'suppliers';
        $statement = $this->db->prepare("SELECT id, code, business_name, vat_number, payment_terms, payment_days, payment_month_end,
            payment_method_code, iban, bank_name, bank_abi, bank_cab" . ($table === 'suppliers' ? ", withholding_enabled, withholding_type, withholding_rate, withholding_taxable_percent, withholding_cause" : "") . "
            FROM {$table} WHERE organization_id = ? AND active = 1 ORDER BY business_name");
        $statement->execute([Auth::organizationId()]);
        $counterparties = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT id, code, name, unit, sale_price, purchase_cost, vat_rate FROM products WHERE organization_id = ? AND active = 1 ORDER BY name');
        $statement->execute([Auth::organizationId()]);
        $products = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT code, description, rate, nature FROM vat_codes WHERE organization_id = ? AND active = 1 ORDER BY rate DESC, code');
        $statement->execute([Auth::organizationId()]);
        $vatCodes = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT withholding_enabled, withholding_type, withholding_rate, withholding_taxable_percent, withholding_cause FROM organizations WHERE id = ?');
        $statement->execute([Auth::organizationId()]);
        $organizationFiscal = $statement->fetch() ?: [];

        $document = [
            'document_date' => date('Y-m-d'), 'due_date' => date('Y-m-d', strtotime('+30 days')),
            'currency' => 'EUR', 'fatturapa_type' => $definition['code'] === 'CREDIT_NOTE' ? 'TD04' : 'TD01',
            'vat_collectability' => 'I', 'payment_method_code' => 'MP05',
        ];
        $this->view->render('documents/form', compact('type', 'definition', 'document', 'counterparties', 'products', 'vatCodes', 'organizationFiscal') + ['title' => 'Nuovo ' . $definition['singular']]);
    }

    public function save(string $type): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        $counterpartyId = (int) ($_POST['counterparty_id'] ?? 0);
        $counterparty = $this->counterparty($definition['counterparty'], $counterpartyId);
        if (!$counterparty) {
            $this->redirect('/documents/' . $type . '/create', 'Seleziona una controparte valida.', 'error');
        }

        $lines = $this->normalizeLines($_POST['lines'] ?? []);
        if ($lines === []) {
            $this->redirect('/documents/' . $type . '/create', 'Inserisci almeno una riga valida.', 'error');
        }
        $taxable = round(array_sum(array_column($lines, 'taxable_amount')), 2);
        $vat = round(array_sum(array_column($lines, 'vat_amount')), 2);
        $total = round($taxable + $vat, 2);
        $withholdingEnabled = !empty($_POST['withholding_enabled']) && in_array($definition['code'], ['SALES_INVOICE', 'PURCHASE_INVOICE'], true);
        $withholdingRate = $withholdingEnabled ? max(0, min(100, $this->decimal($_POST['withholding_rate'] ?? 0))) : 0;
        $withholdingTaxable = $withholdingEnabled ? max(0, min(100, $this->decimal($_POST['withholding_taxable_percent'] ?? 100))) : 100;
        $withholding = $withholdingEnabled ? round($taxable * $withholdingTaxable / 100 * $withholdingRate / 100, 2) : 0;

        $this->db->beginTransaction();
        try {
            $date = (string) ($_POST['document_date'] ?? date('Y-m-d'));
            $dueDate = PartyAutomationService::dueDate($date, $counterparty);
            $paymentMethod = trim((string) ($_POST['payment_method_code'] ?? '')) ?: ($counterparty['payment_method_code'] ?: 'MP05');
            $paymentLabel = PartyAutomationService::paymentLabel($counterparty);
            $number = $this->nextNumber($definition, $date);
            $statement = $this->db->prepare(
                "INSERT INTO documents
                 (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type,
                  counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, total, balance_due,
                  withholding_total, withholding_type, withholding_rate, withholding_taxable_percent, withholding_cause,
                  status, fatturapa_type, vat_collectability, payment_method_code, payment_terms_label, bank_name, bank_abi,
                  bank_cab, bank_iban, notes, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())"
            );
            $statement->execute([
                Auth::organizationId(), $definition['code'], $number, (int) substr($date, 0, 4), $date,
                $dueDate, strtoupper($definition['counterparty']), $counterpartyId,
                $counterparty['business_name'], trim((string) ($_POST['subject'] ?? '')), ($_POST['currency'] ?? 'EUR') ?: 'EUR',
                $taxable, $vat, $total, round($total - $withholding, 2), $withholding,
                $withholdingEnabled ? (($_POST['withholding_type'] ?? '') ?: 'RT01') : null, $withholdingRate, $withholdingTaxable,
                $withholdingEnabled ? (($_POST['withholding_cause'] ?? '') ?: null) : null,
                ($_POST['fatturapa_type'] ?? '') ?: null,
                ($_POST['vat_collectability'] ?? '') ?: null, $paymentMethod, $paymentLabel,
                ($counterparty['bank_name'] ?? '') ?: null, ($counterparty['bank_abi'] ?? '') ?: null,
                ($counterparty['bank_cab'] ?? '') ?: null, ($counterparty['iban'] ?? '') ?: null,
                trim((string) ($_POST['notes'] ?? '')) ?: null, Auth::id(), Auth::id(),
            ]);
            $documentId = (int) $this->db->lastInsertId();
            $insertLine = $this->db->prepare(
                'INSERT INTO document_lines
                 (organization_id, document_id, line_number, product_id, product_code, description, quantity, unit,
                  unit_price, discount_percent, taxable_amount, vat_code, vat_rate, vat_nature, vat_amount, total_amount, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            foreach ($lines as $index => $line) {
                $insertLine->execute([
                    Auth::organizationId(), $documentId, $index + 1, $line['product_id'], $line['product_code'],
                    $line['description'], $line['quantity'], $line['unit'], $line['unit_price'], $line['discount_percent'],
                    $line['taxable_amount'], $line['vat_code'], $line['vat_rate'], $line['vat_nature'], $line['vat_amount'], $line['total_amount'],
                ]);
            }
            $this->db->commit();
            $this->audit('CREATE', 'documents', $documentId, ['type' => $definition['code'], 'number' => $number, 'total' => $total]);
            $this->redirect("/documents/{$type}/{$documentId}", $definition['singular'] . ' creato correttamente.');
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function view(string $type, string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'VIEWER']);
        $definition = $this->type($type);
        [$document, $lines] = $this->loadDocument((int) $id, $definition['code']);
        $workflow = new DocumentWorkflowService($this->db, Auth::organizationId(), Auth::id());
        $conversionTargets = $workflow->targets($definition['code']);
        $targetSlugs = [];
        foreach (self::TYPES as $slug => $config) {
            $targetSlugs[$config['code']] = ['slug' => $slug, 'label' => $config['singular']];
        }
        $statement = $this->db->prepare('SELECT id, code, name FROM warehouses WHERE organization_id = ? AND active = 1 ORDER BY code');
        $statement->execute([Auth::organizationId()]);
        $warehouses = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT dl.*, d.number, d.document_type, d.document_date FROM document_links dl INNER JOIN documents d ON d.id = dl.target_document_id WHERE dl.organization_id = ? AND dl.source_document_id = ? ORDER BY dl.id DESC');
        $statement->execute([Auth::organizationId(), (int) $id]);
        $outgoingLinks = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT dl.*, d.number, d.document_type, d.document_date FROM document_links dl INNER JOIN documents d ON d.id = dl.source_document_id WHERE dl.organization_id = ? AND dl.target_document_id = ? ORDER BY dl.id DESC');
        $statement->execute([Auth::organizationId(), (int) $id]);
        $incomingLinks = $statement->fetchAll();
        $this->view->render('documents/view', compact('type', 'definition', 'document', 'lines', 'conversionTargets', 'targetSlugs', 'warehouses', 'outgoingLinks', 'incomingLinks') + ['title' => $definition['singular'] . ' ' . $document['number']]);
    }

    public function convert(string $type, string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        $targetType = strtoupper((string) ($_POST['target_type'] ?? ''));
        $quantities = [];
        foreach ((array) ($_POST['quantities'] ?? []) as $lineId => $quantity) {
            $quantities[(string) (int) $lineId] = $this->decimal($quantity);
        }
        $targetId = (new DocumentWorkflowService($this->db, Auth::organizationId(), Auth::id()))->convert(
            (int) $id, $targetType, $quantities, ($_POST['document_date'] ?? '') ?: null,
            (int) ($_POST['warehouse_id'] ?? 0) ?: null,
        );
        $targetSlug = null;
        foreach (self::TYPES as $slug => $config) {
            if ($config['code'] === $targetType) { $targetSlug = $slug; break; }
        }
        if (!$targetSlug) { throw new InvalidArgumentException('Tipo documento di destinazione non valido.'); }
        $this->audit('CONVERT', 'documents', (int) $id, ['source_type' => $definition['code'], 'target_type' => $targetType, 'target_id' => $targetId]);
        $this->redirect('/documents/' . $targetSlug . '/' . $targetId, 'Documento convertito correttamente.');
    }

    public function pdf(string $type, string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'VIEWER']);
        $definition = $this->type($type);
        [$document, $lines] = $this->loadDocument((int) $id, $definition['code']);
        $statement = $this->db->prepare('SELECT * FROM organizations WHERE id = ?');
        $statement->execute([Auth::organizationId()]);
        $organization = $statement->fetch();
        $partyTable = $document['counterparty_type'] === 'SUPPLIER' ? 'suppliers' : 'customers';
        $statement = $this->db->prepare("SELECT * FROM {$partyTable} WHERE id = ? AND organization_id = ?");
        $statement->execute([$document['counterparty_id'], Auth::organizationId()]);
        $counterparty = $statement->fetch() ?: [];
        $statement = $this->db->prepare('SELECT * FROM payment_schedules WHERE document_id = ? AND organization_id = ? ORDER BY installment_number');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $paymentSchedules = $statement->fetchAll();

        ob_start();
        require dirname(__DIR__, 2) . '/views/documents/pdf.php';
        $html = (string) ob_get_clean();
        $pdf = new Dompdf(['isRemoteEnabled' => false]);
        $pdf->loadHtml($html, 'UTF-8');
        $pdf->setPaper('A4');
        $pdf->render();
        $pdf->stream(preg_replace('/[^A-Za-z0-9._-]/', '-', $document['number']) . '.pdf', ['Attachment' => true]);
        exit;
    }

    public function xml(string $type, string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        if (!in_array($definition['code'], ['SALES_INVOICE', 'CREDIT_NOTE'], true)) {
            throw new InvalidArgumentException('Questo documento non prevede XML FatturaPA.');
        }
        $service = new FatturaPaService($this->db, Auth::organizationId());
        $xml = $service->generate((int) $id);
        header('Content-Type: application/xml; charset=UTF-8');
        header('Content-Disposition: attachment; filename="IT' . Auth::organizationId() . '_' . str_pad($id, 5, '0', STR_PAD_LEFT) . '.xml"');
        echo $xml;
        exit;
    }

    public function status(string $type, string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        $status = strtoupper((string) ($_POST['status'] ?? ''));
        $allowed = ['DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'CONFIRMED', 'IN_PROGRESS', 'FULFILLED', 'ISSUED', 'RECEIVED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'];
        if (!in_array($status, $allowed, true)) {
            throw new InvalidArgumentException('Stato non valido.');
        }
        $this->db->beginTransaction();
        try {
            $statement = $this->db->prepare('UPDATE documents SET status = ?, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ? AND document_type = ?');
            $statement->execute([$status, Auth::id(), (int) $id, Auth::organizationId(), $definition['code']]);
            if (in_array($status, ['ISSUED', 'RECEIVED'], true)) {
                (new AccountingService($this->db, Auth::organizationId(), Auth::id()))->postDocument((int) $id);
                (new ReceivablesService($this->db, Auth::organizationId(), Auth::id()))->syncDocumentById((int) $id);
            }
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
        $this->audit('STATUS_CHANGE', 'documents', (int) $id, ['status' => $status]);
        $this->redirect("/documents/{$type}/{$id}", 'Stato aggiornato.');
    }

    private function type(string $type): array
    {
        $definition = self::TYPES[$type] ?? throw new InvalidArgumentException('Tipo documento non valido.');
        $this->requireFeature($definition['feature']);
        return $definition;
    }

    private function documentList(array $definition, bool $paginate): array
    {
        $search = trim((string) ($_GET['q'] ?? ''));
        $from = (string) ($_GET['from'] ?? date('Y-01-01'));
        $to = (string) ($_GET['to'] ?? date('Y-12-31'));
        $counterpartyId = max(0, (int) ($_GET['counterparty_id'] ?? 0));
        $status = strtoupper(trim((string) ($_GET['status'] ?? '')));
        $allowedSort = ['number', 'counterparty_name', 'subject', 'document_date', 'due_date', 'taxable_total', 'vat_total', 'total', 'balance_due', 'status'];
        $sort = in_array((string) ($_GET['sort'] ?? ''), $allowedSort, true) ? (string) $_GET['sort'] : 'document_date';
        $direction = strtoupper((string) ($_GET['direction'] ?? 'DESC')) === 'ASC' ? 'ASC' : 'DESC';
        $perPage = in_array((int) ($_GET['per_page'] ?? 50), [25, 50, 100, 250], true) ? (int) $_GET['per_page'] : 50;
        $page = max(1, (int) ($_GET['page'] ?? 1));
        $where = ' FROM documents WHERE organization_id = :organization AND document_type = :type
                   AND document_date BETWEEN :date_from AND :date_to';
        $params = [
            'organization' => Auth::organizationId(), 'type' => $definition['code'],
            'date_from' => $from, 'date_to' => $to,
        ];
        if ($search !== '') {
            // Con PDO MySQL in modalita native ogni occorrenza deve avere un
            // segnaposto distinto. Riutilizzare :search produce HY093 e rende
            // indisponibili tutti gli elenchi documentali quando si ricerca.
            $where .= ' AND (number LIKE :search_number OR counterparty_name LIKE :search_counterparty OR subject LIKE :search_subject)';
            $searchValue = '%' . $search . '%';
            $params['search_number'] = $searchValue;
            $params['search_counterparty'] = $searchValue;
            $params['search_subject'] = $searchValue;
        }
        if ($counterpartyId > 0) {
            $where .= ' AND counterparty_id = :counterparty_id';
            $params['counterparty_id'] = $counterpartyId;
        }
        if ($status !== '') {
            $where .= ' AND status = :status';
            $params['status'] = $status;
        }
        $total = 0;
        $pages = 1;
        $offset = 0;
        if ($paginate) {
            $count = $this->db->prepare('SELECT COUNT(*)' . $where);
            $count->execute($params);
            $total = (int) $count->fetchColumn();
            $pages = max(1, (int) ceil($total / $perPage));
            $page = min($page, $pages);
            $offset = ($page - 1) * $perPage;
        }
        $sql = 'SELECT id, number, document_date, due_date, counterparty_id, counterparty_name, subject,
                       taxable_total, vat_total, total, balance_due, status' . $where
            . ' ORDER BY `' . $sort . '` ' . $direction . ', id ' . $direction;
        if ($paginate) {
            $sql .= ' LIMIT ' . $perPage . ' OFFSET ' . $offset;
        }
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return [
            $statement->fetchAll(), $search, $from, $to, $counterpartyId, $status,
            $sort, $direction, $perPage, $page, $pages, $total,
        ];
    }

    private function counterparty(string $type, int $id): array|false
    {
        $table = $type === 'customer' ? 'customers' : 'suppliers';
        $statement = $this->db->prepare("SELECT * FROM {$table} WHERE id = ? AND organization_id = ? AND active = 1");
        $statement->execute([$id, Auth::organizationId()]);
        return $statement->fetch();
    }

    private function normalizeLines(array $input): array
    {
        $lines = [];
        foreach ($input as $row) {
            $description = trim((string) ($row['description'] ?? ''));
            $quantity = $this->decimal($row['quantity'] ?? 0);
            $unitPrice = $this->decimal($row['unit_price'] ?? 0);
            $discount = max(0.0, min(100.0, $this->decimal($row['discount_percent'] ?? 0)));
            $vatRate = max(0.0, $this->decimal($row['vat_rate'] ?? 0));
            if ($description === '' || $quantity <= 0) {
                continue;
            }
            $taxable = round($quantity * $unitPrice * (1 - $discount / 100), 2);
            $vat = round($taxable * $vatRate / 100, 2);
            $lines[] = [
                'product_id' => !empty($row['product_id']) ? (int) $row['product_id'] : null,
                'product_code' => trim((string) ($row['product_code'] ?? '')) ?: null,
                'description' => $description,
                'quantity' => round($quantity, 4),
                'unit' => trim((string) ($row['unit'] ?? 'NR')) ?: 'NR',
                'unit_price' => round($unitPrice, 4),
                'discount_percent' => round($discount, 2),
                'taxable_amount' => $taxable,
                'vat_code' => trim((string) ($row['vat_code'] ?? '')) ?: null,
                'vat_rate' => round($vatRate, 2),
                'vat_nature' => trim((string) ($row['vat_nature'] ?? '')) ?: null,
                'vat_amount' => $vat,
                'total_amount' => round($taxable + $vat, 2),
            ];
        }
        return $lines;
    }

    private function nextNumber(array $definition, string $date): string
    {
        return (new DocumentNumberService($this->db, Auth::organizationId()))->next($definition['code'], $date);
    }

    private function loadDocument(int $id, string $code): array
    {
        $statement = $this->db->prepare('SELECT * FROM documents WHERE id = ? AND organization_id = ? AND document_type = ?');
        $statement->execute([$id, Auth::organizationId(), $code]);
        $document = $statement->fetch();
        if (!$document) {
            throw new InvalidArgumentException('Documento non trovato.');
        }
        $statement = $this->db->prepare('SELECT * FROM document_lines WHERE document_id = ? AND organization_id = ? ORDER BY line_number');
        $statement->execute([$id, Auth::organizationId()]);
        return [$document, $statement->fetchAll()];
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
}
