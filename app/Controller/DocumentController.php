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
        $statement = $5Ó‹h‘éì¶»§q«^u÷&WÆ6R‚rârÂrrÂGb“²Gc×7G%÷&WÆ6R‚rÂrÂrârÂGb“·×&WGW&â†fÆöB’Gc·Ð¢&—fFRgVæ7F–öâ&VÆöæw2‡7G&–ærGF&ÆRÆ–çBF–BÆ–çBF÷&r“¦&ööÇ²G3ÒGF†—2ÓæF"Óç&W&R‚%4TÄT5Be$ôÒ²GF&ÆWÒt„U$R–CÓòäB÷&væ—¦F–öåö–CÓò"“²G2ÓæW†V7WFR…²F–BÂF÷&uÒ“·&WGW&â†&ööÂ’G2ÓæfWF6„6öÇVÖâ‚“·Ð§Ð