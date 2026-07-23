<?php

declare(strict_types=1);

namespace Luna\Controller;

use Dompdf\Dompdf;
use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\FatturaPaService;
use Luna\Service\DocumentNumberService;
use Luna\Service\DocumentWorkflowService;
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
        $search = trim((string) ($_GET['q'] ?? ''));
        $sql = 'SELECT id, number, document_date, due_date, counterparty_name, subject, taxable_total, vat_total, total, balance_due, status
                FROM documents WHERE organization_id = :organization AND document_type = :type';
        $params = ['organization' => Auth::organizationId(), 'type' => $definition['code']];
        if ($search !== '') {
            $sql .= ' AND (number LIKE :search OR counterparty_name LIKE :search OR subject LIKE :search)';
            $params['search'] = '%' . $search . '%';
        }
        $sql .= ' ORDER BY document_date DESC, id DESC LIMIT 500';
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $documents = $statement->fetchAll();

        $this->view->render('documents/index', compact('type', 'definition', 'documents', 'search') + ['title' => $definition['title']]);
    }

    public function create(string $type): never
    {
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES']);
        $definition = $this->type($type);
        $table = $definition['counterparty'] === 'customer' ? 'customers' : 'suppliers';
        $statement = $this->db->prepare("SELECT id, code, business_name, vat_number FROM {$table} WHERE organization_id = ? AND active = 1 ORDER BY business_name");
        $statement->execute([Auth::organizationId()]);
        $counterparties = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT id, code, name, unit, sale_price, purchase_cost, vat_rate FROM products WHERE organization_id = ? AND active = 1 ORDER BY name');
        $statement->execute([Auth::organizationId()]);
        $products = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT code, description, rate, nature FROM vat_codes WHERE organization_id = ? AND active = 1 ORDER BY rate DESC, code');
        $statement->execute([Auth::organizationId()]);
        $vatCodes = $statement->fetchAll();

        $document = [
            'document_date' => date('Y-m-d'), 'due_date' => date('Y-m-d', strtotime('+30 days')),
            'currency' => 'EUR', 'fatturapa_type' => $definition['code'] === 'CREDIT_NOTE' ? 'TD04' : 'TD01',
            'vat_collectability' => 'I', 'payment_method_code' => 'MP05',
        ];
        $this->view->render('documents/form', compact('type', 'definition', 'document', 'counterparties', 'products', 'vatCodes') + ['title' => 'Nuovo ' . $definition['singular']]);
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

        $this->db->beginTransaction();
        try {
            $date = (string) ($_POST['document_date'] ?? date('Y-m-d'));
            $number = $this->nextNumber($definition, $date);
            $statement = $this->db->prepare(
                "INSERT INTO documents
                 (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type,
                  counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, total, balance_due,
                  status, fatturapa_type, vat_collectability, payment_method_code, notes, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, NOW(), NOW())"
            );
            $statement->execute([
                Auth::organizationId(), $definition['code'], $number, (int) substr($date, 0, 4), $date,
                ($_POST['due_date'] ?? '') ?: null, strtoupper($definition['counterparty']), $counterpartyId,
                $counterparty['business_name'], trim((string) ($_POST['subject'] ?? '')), ($_POST['currency'] ?? 'EUR') ?: 'EUR',
                $taxable, $vat, $total, $total, ($_POST['fatturapa_type'] ?? '') ?: null,
                ($_POST['vat_collectability'] ?? '') ?: null, ($_POST['payment_method_code'] ?? '') ?: null,
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

    private function counterparty(string $type, int $id): array|false
    {
        $table = $type === 'customer' ? 'customers' : 'suppliers';ó®w¶‰žËkºwµçD6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂr÷"÷¶ÖöGVÆWÒrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷"÷¶ÖöGVÆWÒö7&VFRrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72Âv7&VFRuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷"÷¶ÖöGVÆWÒöW‡÷'BrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72ÂvW‡÷'BuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷"÷¶ÖöGVÆWÒ÷¶–GÒöVF—BrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72ÂvVF—BuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷"÷¶ÖöGVÆWÒ÷6fRrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72Âw6fRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷"÷¶ÖöGVÆWÒ÷¶–GÒöFVÆWFRrÂµ&W6÷W&6T6öçG&öÆÆW#£¦6Æ72ÂvFVÆWFRuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂröFö7VÖVçG2÷·G—WÒrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂröFö7VÖVçG2÷·G—WÒö7&VFRrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âv7&VFRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂröFö7VÖVçG2÷·G—WÒ÷6fRrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âw6fRuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂröFö7VÖVçG2÷·G—WÒ÷¶–GÒrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âwf–WruÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂröFö7VÖVçG2÷·G—WÒ÷¶–GÒ÷FbrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72ÂwFbuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂröFö7VÖVçG2÷·G—WÒ÷¶–GÒ÷†ÖÂrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âw†ÖÂuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂröFö7VÖVçG2÷·G—WÒ÷¶–GÒ÷7FGW2rÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âw7FGW2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂröFö7VÖVçG2÷·G—WÒ÷¶–GÒö6öçfW'BrÂ´Fö7VÖVçD6öçG&öÆÆW#£¦6Æ72Âv6öçfW'BuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2öÆöv—7F–72rÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72öÖ÷fVÖVçBrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72ÂvÖ÷fVÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷G&ç6fW'2rÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72ÂwG&ç6fW"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷G&ç6fW'2÷¶–GÒö6öæf—&ÒrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âv6öæf—&ÒuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷G&ç6fW'2÷¶–GÒ÷&V6V—fRrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âw&V6V—fRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷G&ç6fW'2÷¶–GÒö6æ6VÂrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âv6æ6VÅG&ç6fW"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷–6·2rÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âw–6²uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷–6·2÷¶–GÒ÷66ârÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âw66âuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷–6·2÷¶–GÒö6ö×ÆWFRrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âv6ö×ÆWFU–6²uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öÆöv—7F–72÷–6·2÷¶–GÒö6æ6VÂrÂ´Æöv—7F–746öçG&öÆÆW#£¦6Æ72Âv6æ6VÅ–6²uÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2÷&ö¦V7G2rÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2÷F–ÖRrÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72ÂwF–ÖRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2÷F–ÖR÷¶–GÒö&÷fRrÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72Âv&÷fUF–ÖRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2öW‡Vç6W2rÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72ÂvW‡Vç6RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2öÖ–ÆW7FöæW2rÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72ÂvÖ–ÆW7FöæRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2öÖ–ÆW7FöæW2÷¶–GÒ÷&VG’rÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72Âw&VG’uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&ö¦V7G2÷¶–GÒö–çfö–6RrÂµ&ö¦V7D÷46öçG&öÆÆW#£¦6Æ72Âv–çfö–6RuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2ö6öÖ×Væ–6F–öç2rÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö6öÖ×Væ–6F–öç2÷6WGF–æw2rÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72Âw6WGF–æw2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö6öÖ×Væ–6F–öç2÷VWVRrÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72ÂwVWVRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö6öÖ×Væ–6F–öç2÷&ö6W72rÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72Âw&ö6W72uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂröÖ–Âöò÷·Fö¶VçÒæv–brÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72Âv÷VâuÒÂfÇ6R“°¢G&÷WFW"ÓæFB‚ttUBrÂröÖ–Âö2÷·Fö¶VçÒrÂ´6öÖ×Væ–6F–öç46öçG&öÆÆW#£¦6Æ72Âv6Æ–6²uÒÂfÇ6R“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2ö‡"rÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"öÆVfRrÂ´‡$6öçG&öÆÆW#£¦6Æ72ÂvÆVfRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"öÆVfR÷¶–GÒöFV6–FRrÂ´‡$6öçG&öÆÆW#£¦6Æ72ÂvFV6–FRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"öÆVfR÷¶–GÒö6æ6VÂrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv6æ6VÄÆVfRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"ö&Ææ6W2rÂ´‡$6öçG&öÆÆW#£¦6Æ72Âw6fT&Ææ6RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂÖ6öæf–w2rÂ´‡$6öçG&öÆÆW#£¦6Æ72Âw6fU—&öÆÄ6öæf–ruÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv7&VFU'VâuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂ÷¶–GÒö6Æ7VÆFRrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv6Æ7VÆFRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂ÷¶–GÒö–×÷'BrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv–×÷'E—&öÆÂuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂ÷¶–GÒö6öæf—&ÒrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âv6öæf—&ÒuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö‡"÷—&öÆÂ÷¶–GÒ÷–BrÂ´‡$6öçG&öÆÆW#£¦6Æ72Âw–BuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2öV6öÖÖW&6RrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂvV6öÖÖW&6RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öV6öÖÖW&6Rö6†ææVÇ2÷¶–GÒöVçVWVRrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂvVçVWVRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öV6öÖÖW&6R÷&ö6W72rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âw&ö6W746öÖÖW&6RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2öV6öÖÖW&6Rö÷&FW'2÷¶–GÒö6öçfW'BrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âv6öçfW'D÷&FW"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷vV&†öö·2öV6öÖÖW&6R÷¶–GÒrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂwvV&†öö²uÒÂfÇ6RÂfÇ6R“°¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2÷&VçFÂrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âw&VçFÂuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&VçFÂö6öçG&7G2÷¶–GÒöÖWFW"rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂvÖWFW"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&VçFÂ÷F–6¶WG2÷¶–GÒ÷7FGW2rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂwF–6¶WBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2÷&VçFÂöWFöÖFRrÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂvWFöÖFU&VçFÂuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö÷W&F–öç2ö6ÆVæF"rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âv6ÆVæF"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö6ÆVæF"÷6WGF–æw2rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âv6ÆVæF%6WGF–æw2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö÷W&F–öç2ö6ÆVæF"÷¶–GÒ÷7–æ2rÂ´÷W&F–öç4–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âw7–æ46ÆVæF"uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷&W÷'G2öÖævVÖVçBrÂµ&W÷'G46öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷&W÷'G2öÖævVÖVçBövVæW&FRrÂµ&W÷'G46öçG&öÆÆW#£¦6Æ72ÂvvVæW&FRuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷&W÷'G2öÖævVÖVçB÷¶–GÒöF÷væÆöBrÂµ&W÷'G46öçG&öÆÆW#£¦6Æ72ÂvF÷væÆöBuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ærö¦÷W&æÂrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âv¦÷W&æÂuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ærö¦÷W&æÂö7&VFRrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âv7&VFRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö¦÷W&æÂ÷6fRrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw6fRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö¦÷W&æÂ÷÷7BrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw÷7BuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ærö¦÷W&æÂ÷¶–GÒöVF—BrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂvVF—BuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö¦÷W&æÂ÷¶–GÒ÷÷7BrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw÷7DG&gBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö¦÷W&æÂ÷¶–GÒöFVÆWFRrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂvFVÆWFTG&gBuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ærö¦÷W&æÂ÷¶–GÒrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw6†÷ruÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷G&–ÂÖ&Ææ6RrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂwG&–Ä&Ææ6RuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–æröÆVFvW"÷¶–GÒrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂvÆVFvW"uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷fB×&Vv—7FW'2rÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂwfE&Vv—7FW'2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷fB×&Vv—7FW'2öÖçVÂrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw6fUfDÖ÷fVÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷fB×&Vv—7FW'2÷7–æ2rÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw7–æ5fDFö7VÖVçG2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷fB×&Vv—7FW'2÷¶–GÒöFVÆWFRrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂvFVÆWFUfDÖ÷fVÖVçBuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷fB×6WGFÆVÖVçG2rÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂwfE6WGFÆVÖVçG2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷fB×6WGFÆVÖVçG2ö6Æ7VÆFRrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âv6Æ7VÆFUfE6WGFÆVÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷fB×6WGFÆVÖVçG2÷¶–GÒ÷7FGW2rÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72ÂwWFFUfE6WGFÆVÖVçE7FGW2uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷fB×6WGFÆVÖVçG2÷¶–GÒrÂ´66÷VçF–æt6öçG&öÆÆW#£¦6Æ72Âw6†÷ufE6WGFÆVÖVçBuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷6WGWrÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGWö66÷VçG2rÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âw6fT66÷VçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGWö66÷VçG2÷¶–GÒ÷FövvÆRrÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72ÂwFövvÆT66÷VçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGW÷6WGF–æw2rÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âw6fU6WGF–æw2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGW÷&Vv—7FW'2rÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âw6fU&Vv—7FW"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGWö6W6W2rÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âw6fT6W6RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷6WGWöÖ–æw2rÂ´66÷VçF–ætFÖ–ä6öçG&öÆÆW#£¦6Æ72Âw6fTÖ–æruÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ær÷G&V7W'’rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷7–æ2rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âw7–æ2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷–ÖVçG2rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âw–ÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷–ÖVçG2÷¶–GÒ÷&WfW'6RrÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âw&WfW'6U–ÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’ö&æ²Ö66÷VçG2rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âv&æ´66÷VçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’ö&æ²×G&ç67F–öç2rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âv&æµG&ç67F–öâuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷&V6öæ6–ÆRrÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âw&V6öæ6–ÆRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷&V6öæ6–Æ–F–öç2÷¶–GÒöFVÆWFRrÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72ÂwVç&V6öæ6–ÆRuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷v—F††öÆF–æw2rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âwv—F††öÆF–æruÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ær÷G&V7W'’÷v—F††öÆF–æw2÷¶–GÒ÷’rÂµG&V7W'”6öçG&öÆÆW#£¦6Æ72Âw•v—F††öÆF–æruÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö66÷VçF–ærö6ö×Æ–æ6RrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6R÷fBÖF§W7FÖVçG2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72ÂwfDF§W7FÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6RöÆ—RrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72ÂvvVæW&FTÆ—RuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6RöæçVÂ×fBrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72ÂvvVæW&FTæçVÂuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6R÷¶¶–æGÒ÷¶–GÒ÷7FGW2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72ÂwfE7FGW2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6Rö6Æ÷6–æw2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âw&W&T6Æ÷6–æruÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6Rö6Æ÷6–æw2÷¶–GÒ÷÷7BrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âw÷7D6Æ÷6–æruÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6RöF§W7FÖVçG2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72ÂvF§W7FÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6RöF§W7FÖVçG2÷¶–GÒ÷÷7BrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âw÷7DF§W7FÖVçBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6Rö76WBÖ6FVv÷&–W2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âv76WD6FVv÷'’uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6Rö76WG2rÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âv76WBuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6Rö76WG2÷¶–GÒö6Æ7VÆFRrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âv6Æ7VÆFTFW&V6–F–öâuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö66÷VçF–ærö6ö×Æ–æ6RöFW&V6–F–öç2÷¶–GÒ÷÷7BrÂ´6ö×Æ–æ6T6öçG&öÆÆW#£¦6Æ72Âw÷7DFW&V6–F–öâuÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂrö–×÷'G2rÂ´–×÷'D6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö–×÷'G2÷WÆöBrÂ´–×÷'D6öçG&öÆÆW#£¦6Æ72ÂwWÆöBuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂrö–×÷'G2÷¶–GÒrÂ´–×÷'D6öçG&öÆÆW#£¦6Æ72Âw&Wf–WruÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö–×÷'G2÷¶–GÒö6öÖÖ—BrÂ´–×÷'D6öçG&öÆÆW#£¦6Æ72Âv6öÖÖ—BuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂrö–×÷'G2÷¶–GÒ÷&öÆÆ&6²rÂ´–×÷'D6öçG&öÆÆW#£¦6Æ72Âw&öÆÆ&6²uÒ“° ¢G&÷WFW"ÓæFB‚ttUBrÂr÷6WGF–æw2öÖöGVÆW2rÂµ6WGF–æw46öçG&öÆÆW#£¦6Æ72ÂvÖöGVÆW2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2öÖöGVÆW2rÂµ6WGF–æw46öçG&öÆÆW#£¦6Æ72Âw6fTÖöGVÆW2uÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷6WGF–æw2÷7—7FVÒrÂµ6WGF–æw46öçG&öÆÆW#£¦6Æ72Âw7—7FVÒuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷6WGF–æw2öVæGö–çG2rÂ´–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72ÂvVæGö–çG2uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2öVæGö–çG2rÂ´–çFVw&F–öä6öçG&öÆÆW#£¦6Æ72Âw6fTVæGö–çBuÒ“°¢G&÷WFW"ÓæFB‚ttUBrÂr÷6WGF–æw2ö6ö×ç’rÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âv–æFW‚uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2ö6ö×ç’rÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âv7&VFT6ö×ç’uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2ö6ö×ç’÷¶–GÒ÷6VÆV7BrÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âw6VÆV7D6ö×ç’uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2÷W6W'2rÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âv7&VFUW6W"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2÷W6W'2÷¶–GÒ÷FövvÆRrÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72ÂwFövvÆUW6W"uÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2÷W6W'2÷¶–GÒ÷&W6WB×77v÷&BrÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âw&W6WEW6W%77v÷&BuÒ“°¢G&÷WFW"ÓæFB‚uõ5BrÂr÷6WGF–æw2÷6V7W&—G’÷77v÷&BrÂµÆFf÷&Ô6öçG&öÆÆW#£¦6Æ72Âv6†ævU77v÷&BuÒ“°¢Ð ¢V&Æ–2gVæ7F–öâ'Vâ‚“¢fö–@¢°¢G'’°¢GF‚Ò'6U÷W&Â‚Eõ4U%dU%²u$UTU5EõU$’uÒóòròrÂ…õU$ÅõD‚’ó¢ròs°¢GF†—2Óç&÷WFW"ÓæF—7F6‚‡7G'F÷WW"‚Eõ4U%dU%²u$UTU5EôÔUD„ôBuÒóòttUBr’Â'G&–Ò‚GF‚Âròr’ó¢ròr“°¢Ò6F6‚…F‡&÷v&ÆRFW†6WF–öâ’°¢G&VfW&Væ6RÒW'&÷%&W÷'FW#£§&W÷'B‚FW†6WF–öâÂGF†—2Óæ&6UF‚“°¢G66†VÖ—77VRÒW'&÷%&W÷'FW#£¦—566†VÖW'&÷"‚FW†6WF–öâ“°¢FÖW76vRÒG66†VÖ—77VP¢òt–ÂFF&6Ræöâ:‚6ö×ÆWFÖVçFRvv–÷&æFòâ6öçG&öÆÆÆRÖ–w&¦–öæ’FÆÆv–æ7FFòFVÂ6—7FVÖâp¢¢tæöâ:‚7FFò÷76–&–ÆR6ö×ÆWF&RÎ(	–÷W&¦–öæRâ–ÂFWGFvÆ–òFV6æ–6ò:‚7FFò&Vv—7G&Fòâs°¢–b„Vçc£¦&ööÂ‚tôDT%Trr’’°¢FÖW76vRãÒrrâFW†6WF–öâÓævWDÖW76vR‚“°¢Ð¢GF†—2Óçf–WrÓç&VæFW"‚vW'&÷"rÂ°¢wF—FÆRrÓâG66†VÖ—77VRòtFF&6RFvv–÷&æ&Rr¢tW'&÷&RÆ–6F—fòrÀ¢vÖW76vRrÓâFÖW76vRÀ¢w&VfW&Væ6RrÓâG&VfW&Væ6RÀ¢w66†VÖ—77VRrÓâG66†VÖ—77VRÀ¢v7F–öåW&ÂrÓâWFƒ£¦—57WW'W6W"‚’òr÷6WGF–æw2÷7—7FVÒr¢röF6†&ö&BrÀ¢v7F–öäÆ&VÂrÓâWFƒ£¦—57WW'W6W"‚’òt6öçG&öÆÆ–Â6—7FVÖr¢uF÷&æÆÆF6†&ö&BrÀ¢ÒÂG66†VÖ—77VRòS2¢S“°¢Ð¢Ð§Ð