<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use Throwable;

final class DocumentWorkflowService
{
    private const CONVERSIONS = [
        'QUOTE' => ['SALES_ORDER'],
        'SALES_ORDER' => ['DDT', 'SALES_INVOICE'],
        'DDT' => ['SALES_INVOICE'],
        'PROFORMA' => ['SALES_INVOICE'],
        'PURCHASE_ORDER' => ['PURCHASE_INVOICE'],
    ];

    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function targets(string $sourceType): array
    {
        return self::CONVERSIONS[$sourceType] ?? [];
    }

    public function convert(int $sourceId, string $targetType, array $requestedQuantities = [], ?string $date = null, ?int $warehouseId = null): int
    {
        $date ??= date('Y-m-d');
        $this->db->beginTransaction();
        try {
            $document = $this->one('SELECT * FROM documents WHERE id = ? AND organization_id = ? FOR UPDATE', [$sourceId, $this->organizationId]);
            if (!$document) {
                throw new InvalidArgumentException('Documento sorgente non trovato.');
            }
            if (!in_array($targetType, $this->targets((string) $document['document_type']), true)) {
                throw new InvalidArgumentException('Conversione documentale non consentita.');
            }
            if (in_array((string) $document['status'], ['CANCELLED', 'REJECTED'], true)) {
                throw new InvalidArgumentException('Il documento annullato o rifiutato non può essere convertito.');
            }

            $statement = $this->db->prepare('SELECT * FROM document_lines WHERE document_id = ? AND organization_id = ? ORDER BY line_number FOR UPDATE');
            $statement->execute([$sourceId, $this->organizationId]);
            $sourceLines = $statement->fetchAll();
            $lines = [];
            foreach ($sourceLines as $line) {
                $remaining = round((float) $line['quantity'] - (float) $line['converted_quantity'], 4);
                $quantity = array_key_exists((string) $line['id'], $requestedQuantities)
                    ? round((float) $requestedQuantities[(string) $line['id']], 4)
                    : $remaining;
                if ($quantity <= 0) {
                    continue;
                }
                if ($quantity > $remaining + 0.00005) {
                    throw new InvalidArgumentException('La quantità convertita supera il residuo della riga ' . $line['line_number'] . '.');
                }
                $ratio = $quantity / (float) $line['quantity'];
                $line['target_quantity'] = $quantity;
                $line['target_taxable'] = round((float) $line['taxable_amount'] * $ratio, 2);
                $line['target_vat'] = round((float) $line['vat_amount'] * $ratio, 2);
                $line['target_total'] = round((float) $line['total_amount'] * $ratio, 2);
                $lines[] = $line;
            }
            if ($lines === []) {
                throw new InvalidArgumentException('Non ci sono quantità residue da convertire.');
            }

            if ($warehouseId !== null) {
                $warehouse = $this->one('SELECT id FROM warehouses WHERE id = ? AND organization_id = ? AND active = 1', [$warehouseId, $this->organizationId]);
                if (!$warehouse) {
                    throw new InvalidArgumentException('Magazzino non valido.');
                }
            }
            $taxable = round(array_sum(array_column($lines, 'target_taxable')), 2);
            $vat = round(array_sum(array_column($lines, 'target_vat')), 2);
            $total = round(array_sum(array_column($lines, 'target_total')), 2);
            $number = (new DocumentNumberService($this->db, $this->organizationId))->next($targetType, $date);
            $dueDate = in_array($targetType, ['SALES_INVOICE', 'PURCHASE_INVOICE'], true) ? date('Y-m-d', strtotime($date . ' +30 days')) : $document['due_date'];
            $fulfillment = in_array($targetType, ['SALES_ORDER', 'DDT'], true) ? 'OPEN' : 'NOT_REQUIRED';
            $status = in_array($targetType, ['SALES_INVOICE', 'PURCHASE_INVOICE'], true) ? 'DRAFT' : 'DRAFT';

            $insert = $this->db->prepare(
                'INSERT INTO documents (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type, counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, withholding_total, stamp_duty_total, total, balance_due, converted_total, status, fulfillment_status, fatturapa_type, vat_collectability, payment_method_code, transport_reason, packages_count, carrier, destination_address, notes, source_document_id, warehouse_id, project_id, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $insert->execute([
                $this->organizationId, $targetType, $number, (int) substr($date, 0, 4), $date, $dueDate,
                $document['counterparty_type'], $document['counterparty_id'], $document['counterparty_name'], $document['subject'], $document['currency'],
                $taxable, $vat, 0, 0, $total, $total, $status, $fulfillment,
                $targetType === 'CREDIT_NOTE' ? 'TD04' : (in_array($targetType, ['SALES_INVOICE', 'PURCHASE_INVOICE'], true) ? 'TD01' : null),
                $document['vat_collectability'], $document['payment_method_code'], $document['transport_reason'], $document['packages_count'],
                $document['carrier'], $document['destination_address'], trim('Generato da ' . $document['number'] . "\n" . (string) $document['notes']),
                $sourceId, $warehouseId ?? $document['warehouse_id'], $document['project_id'], $this->userId, $this->userId,
            ]);
            $targetId = (int) $this->db->lastInsertId();
            $insertLine = $this->db->prepare(
                'INSERT INTO document_lines (organization_id, document_id, source_line_id, line_number, product_id, warehouse_id, project_id, product_code, description, quantity, converted_quantity, reserved_quantity, fulfilled_quantity, unit, unit_price, discount_percent, taxable_amount, vat_code, vat_rate, vat_nature, vat_amount, total_amount, cost_center_id, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $updateLine = $this->db->prepare('UPDATE document_lines SET converted_quantity = converted_quantity + ?, updated_at = NOW() WHERE id = ?');
            foreach ($lines as $index => $line) {
                $insertLine->execute([
                    $this->organizationId, $targetId, $line['id'], $index + 1, $line['product_id'], $warehouseId ?? $line['warehouse_id'], $line['project_id'],
                    $line['product_code'], $line['description'], $line['target_quantity'], $line['unit'], $line['unit_price'], $line['discount_percent'],
                    $line['target_taxable'], $line['vat_code'], $line['vat_rate'], $line['vat_nature'], $line['target_vat'], $line['target_total'], $line['cost_center_id'],
                ]);
                $updateLine->execute([$line['target_quantity'], $line['id']]);
            }
            $this->db->prepare("INSERT INTO document_links (organization_id, source_document_id, target_document_id, link_type, created_by, created_at) VALUES (?, ?, ?, 'CONVERSION', ?, NOW())")
                ->execute([$this->organizationId, $sourceId, $targetId, $this->userId]);
            $this->refreshSourceStatus($sourceId);
            $this->db->commit();
            return $targetId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function refreshSourceStatus(int $documentId): void
    {
        $statement = $this->db->prepare('SELECT COALESCE(SUM(quantity),0) quantity, COALESCE(SUM(converted_quantity),0) converted FROM document_lines WHERE document_id = ? AND organization_id = ?');
        $statement->execute([$documentId, $this->organizationId]);
        $totals = $statement->fetch();
        $quantity = (float) $totals['quantity'];
        $converted = (float) $totals['converted'];
        $fulfillment = $converted <= 0 ? 'OPEN' : ($converted + 0.00005 >= $quantity ? 'FULFILLED' : 'PARTIAL');
        $this->db->prepare('UPDATE documents SET fulfillment_status = ?, converted_total = total * ?, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
            ->execute([$fulfillment, $quantity > 0 ? min(1, $converted / $quantity) : 0, $this->userId, $documentId, $this->organizationId]);
    }

    private function one(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetch();
    }
}
