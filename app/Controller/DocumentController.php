<?php

declare(strict_types=1);

namespace Luna\Controller;

use Dompdf\Dompdf;
use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\AccountingService;
use Luna\Service\FatturaPaService;
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
        $this->view->render('documents/view', compact('type', 'definition', 'document', 'lines') + ['title' => $definition['singular'] . ' ' . $document['number']]);
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
        $year = (int) substr($date, 0, 4);
        $key = $definition['code'] . '-' . $year;
        $statement = $this->db->prepare('SELECT id, prefix, next_value, padding FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE');
        $statement->execute([Auth::organizationId(), $key]);
        $sequence = $statement->fetch();
        if (!$sequence) {
            $prefix = $definition['prefix'] . '-' . $year . '-';
            $this->db->prepare('INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at) VALUES (?, ?, ?, 2, 4, NOW(), NOW())')
                ->execute([Auth::organizationId(), $key, $prefix]);
            return $prefix . '0001';
        }
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + 1, updated_at = NOW() WHERE id = ?')->execute([$sequence['id']]);
        return $sequence['prefix'] . str_pad((string) $sequence['next_value'], (int) $sequence['padding'], '0', STR_PAD_LEFT);
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
