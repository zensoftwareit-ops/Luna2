<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class InventoryService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function postMovement(array $data): int
    {
        return $this->transaction(fn (): int => $this->applyMovement($data));
    }

    public function createTransfer(int $sourceWarehouseId, int $destinationWarehouseId, array $lines, ?string $date = null, ?string $notes = null): int
    {
        if ($sourceWarehouseId === $destinationWarehouseId) {
            throw new InvalidArgumentException('I magazzini di origine e destinazione devono essere diversi.');
        }
        return $this->transaction(function () use ($sourceWarehouseId, $destinationWarehouseId, $lines, $date, $notes): int {
            $this->warehouse($sourceWarehouseId);
            $this->warehouse($destinationWarehouseId);
            $number = $this->nextOperationalNumber('TRF', $date ?? date('Y-m-d'));
            $this->db->prepare("INSERT INTO inventory_transfers (organization_id, transfer_number, transfer_date, source_warehouse_id, destination_warehouse_id, status, notes, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $number, $date ?? date('Y-m-d'), $sourceWarehouseId, $destinationWarehouseId, $notes, $this->userId, $this->userId]);
            $transferId = (int) $this->db->lastInsertId();
            $insert = $this->db->prepare('INSERT INTO inventory_transfer_lines (organization_id, transfer_id, product_id, requested_quantity, shipped_quantity, received_quantity, unit_cost) VALUES (?, ?, ?, ?, 0, 0, ?)');
            foreach ($lines as $line) {
                $productId = (int) ($line['product_id'] ?? 0);
                $quantity = round((float) ($line['quantity'] ?? 0), 4);
                if ($productId <= 0 || $quantity <= 0) {
                    continue;
                }
                $balance = $this->balance($sourceWarehouseId, $productId, true);
                if ((float) $balance['quantity'] - (float) $balance['reserved_quantity'] + 0.00005 < $quantity) {
                    throw new InvalidArgumentException('Giacenza disponibile insufficiente per il prodotto ' . $productId . '.');
                }
                $insert->execute([$this->organizationId, $transferId, $productId, $quantity, $balance['average_cost']]);
            }
            $count = $this->one('SELECT COUNT(*) count FROM inventory_transfer_lines WHERE transfer_id = ?', [$transferId]);
            if ((int) $count['count'] === 0) {
                throw new InvalidArgumentException('Inserisci almeno una riga valida nel trasferimento.');
            }
            return $transferId;
        });
    }

    public function confirmTransfer(int $transferId): void
    {
        $this->transaction(function () use ($transferId): void {
            $transfer = $this->transfer($transferId, true);
            if ($transfer['status'] !== 'DRAFT') {
                throw new InvalidArgumentException('Puoi confermare solo un trasferimento in bozza.');
            }
            $lines = $this->all('SELECT * FROM inventory_transfer_lines WHERE transfer_id = ? AND organization_id = ? FOR UPDATE', [$transferId, $this->organizationId]);
            foreach ($lines as $line) {
                $this->applyMovement([
                    'movement_uuid' => $this->uuidFor('transfer-out', $transferId, (int) $line['id']),
                    'movement_date' => $transfer['transfer_date'], 'product_id' => $line['product_id'], 'warehouse_id' => $transfer['source_warehouse_id'],
                    'movement_type' => 'TRANSFER_OUT', 'quantity' => $line['requested_quantity'], 'unit_cost' => $line['unit_cost'],
                    'reason' => 'Trasferimento ' . $transfer['transfer_number'], 'source_type' => 'INVENTORY_TRANSFER', 'source_id' => $transferId,
                ]);
            }
            $this->db->prepare("UPDATE inventory_transfer_lines SET shipped_quantity = requested_quantity WHERE transfer_id = ?")->execute([$transferId]);
            $this->db->prepare("UPDATE inventory_transfers SET status = 'IN_TRANSIT', confirmed_at = NOW(), updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([$this->userId, $transferId, $this->organizationId]);
        });
    }

    public function receiveTransfer(int $transferId, array $receivedQuantities = []): void
    {
        $this->transaction(function () use ($transferId, $receivedQuantities): void {
            $transfer = $this->transfer($transferId, true);
            if (!in_array($transfer['status'], ['CONFIRMED', 'IN_TRANSIT'], true)) {
                throw new InvalidArgumentException('Il trasferimento non è in transito.');
            }
            $lines = $this->all('SELECT * FROM inventory_transfer_lines WHERE transfer_id = ? AND organization_id = ? FOR UPDATE', [$transferId, $this->organizationId]);
            foreach ($lines as $line) {
                $quantity = isset($receivedQuantities[(string) $line['id']]) ? round((float) $receivedQuantities[(string) $line['id']], 4) : (float) $line['shipped_quantity'];
                if ($quantity < 0 || $quantity > (float) $line['shipped_quantity'] + 0.00005) {
                    throw new InvalidArgumentException('Quantità ricevuta non valida.');
                }
                if ($quantity > 0) {
                    $this->applyMovement([
                        'movement_uuid' => $this->uuidFor('transfer-in', $transferId, (int) $line['id']),
                        'movement_date' => date('Y-m-d'), 'product_id' => $line['product_id'], 'warehouse_id' => $transfer['destination_warehouse_id'],
                        'movement_type' => 'TRANSFER_IN', 'quantity' => $quantity, 'unit_cost' => $line['unit_cost'],
                        'reason' => 'Ricezione ' . $transfer['transfer_number'], 'source_type' => 'INVENTORY_TRANSFER', 'source_id' => $transferId,
                    ]);
                }
                $this->db->prepare('UPDATE inventory_transfer_lines SET received_quantity = ? WHERE id = ?')->execute([$quantity, $line['id']]);
            }
            $this->db->prepare("UPDATE inventory_transfers SET status = 'RECEIVED', received_at = NOW(), updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([$this->userId, $transferId, $this->organizationId]);
        });
    }

    public function cancelTransfer(int $transferId): void
    {
        $this->transaction(function () use ($transferId): void {
            $transfer=$this->transfer($transferId,true);
            if($transfer['status']!=='DRAFT'){throw new InvalidArgumentException('Puoi annullare solo un trasferimento ancora in bozza.');}
            $this->db->prepare("UPDATE inventory_transfers SET status='CANCELLED',updated_by=?,updated_at=NOW() WHERE id=? AND organization_id=?")->execute([$this->userId,$transferId,$this->organizationId]);
        });
    }

    public function createPickList(int $documentId, int $warehouseId, ?int $assignedTo = null): int
    {
        return $this->transaction(function () use ($documentId, $warehouseId, $assignedTo): int {
            $this->warehouse($warehouseId);
            $document = $this->one('SELECT * FROM documents WHERE id = ? AND organization_id = ? FOR UPDATE', [$documentId, $this->organizationId]);
            if (!$document || !in_array($document['document_type'], ['SALES_ORDER', 'DDT'], true)) {
                throw new InvalidArgumentException('Il picking può essere creato solo per ordini cliente o DDT.');
            }
            $existing = $this->one('SELECT id,status,warehouse_id FROM inventory_pick_lists WHERE document_id = ? AND organization_id = ? FOR UPDATE', [$documentId, $this->organizationId]);
            if ($existing) {
                if($existing['status']==='CANCELLED'){$items=$this->all('SELECT * FROM inventory_pick_items WHERE pick_list_id=? AND organization_id=? FOR UPDATE',[$existing['id'],$this->organizationId]);foreach($items as $item){$balance=$this->balance($warehouseId,(int)$item['product_id'],true);if((float)$balance['quantity']-(float)$balance['reserved_quantity']+0.00005<(float)$item['required_quantity']){throw new InvalidArgumentException('Giacenza non più sufficiente per riaprire il picking.');}$this->db->prepare('UPDATE inventory_balances SET reserved_quantity=reserved_quantity+? WHERE id=?')->execute([$item['required_quantity'],$balance['id']]);$this->db->prepare('UPDATE document_lines SET reserved_quantity=reserved_quantity+?,warehouse_id=?,updated_at=NOW() WHERE id=?')->execute([$item['required_quantity'],$warehouseId,$item['document_line_id']]);}$this->db->prepare("UPDATE inventory_pick_items SET picked_quantity=0,status='OPEN' WHERE pick_list_id=?")->execute([$existing['id']]);$this->db->prepare("UPDATE inventory_pick_lists SET status='OPEN',warehouse_id=?,assigned_to=?,started_at=NULL,completed_at=NULL WHERE id=?")->execute([$warehouseId,$assignedTo,$existing['id']]);}
                return (int) $existing['id'];
            }
            $number = $this->nextOperationalNumber('PICK', date('Y-m-d'));
            $this->db->prepare("INSERT INTO inventory_pick_lists (organization_id, pick_number, warehouse_id, document_id, status, assigned_to, created_by, created_at) VALUES (?, ?, ?, ?, 'OPEN', ?, ?, NOW())")
                ->execute([$this->organizationId, $number, $warehouseId, $documentId, $assignedTo, $this->userId]);
            $pickId = (int) $this->db->lastInsertId();
            $lines = $this->all('SELECT dl.*, p.ean, p.sku FROM document_lines dl INNER JOIN products p ON p.id = dl.product_id AND p.track_inventory = 1 WHERE dl.document_id = ? AND dl.organization_id = ? FOR UPDATE', [$documentId, $this->organizationId]);
            $insert = $this->db->prepare("INSERT INTO inventory_pick_items (organization_id, pick_list_id, document_line_id, product_id, barcode, required_quantity, picked_quantity, status) VALUES (?, ?, ?, ?, ?, ?, 0, 'OPEN')");
            foreach ($lines as $line) {
                $required = round((float) $line['quantity'] - (float) $line['fulfilled_quantity'], 4);
                if ($required <= 0) {
                    continue;
                }
                $balance = $this->balance($warehouseId, (int) $line['product_id'], true);
                if ((float) $balance['quantity'] - (float) $balance['reserved_quantity'] + 0.00005 < $required) {
                    throw new InvalidArgumentException('Giacenza disponibile insufficiente per ' . $line['description'] . '.');
                }
                $this->db->prepare('UPDATE inventory_balances SET reserved_quantity = reserved_quantity + ? WHERE id = ?')->execute([$required, $balance['id']]);
                $this->db->prepare('UPDATE document_lines SET reserved_quantity = reserved_quantity + ?, warehouse_id = ?, updated_at = NOW() WHERE id = ?')->execute([$required, $warehouseId, $line['id']]);
                $insert->execute([$this->organizationId, $pickId, $line['id'], $line['product_id'], $line['ean'] ?: $line['sku'], $required]);
            }
            $count = $this->one('SELECT COUNT(*) count FROM inventory_pick_items WHERE pick_list_id = ?', [$pickId]);
            if ((int) $count['count'] === 0) {
                throw new InvalidArgumentException('Il documento non contiene prodotti gestiti a magazzino da prelevare.');
            }
            $this->db->prepare("UPDATE documents SET warehouse_id = ?, fulfillment_status = 'OPEN', updated_at = NOW() WHERE id = ?")->execute([$warehouseId, $documentId]);
            return $pickId;
        });
    }

    public function scanBarcode(int $pickId, string $barcode, float $quantity = 1): array
    {
        return $this->transaction(function () use ($pickId, $barcode, $quantity): array {
            $barcode = trim($barcode);
            $quantity = round($quantity, 4);
            $pick = $this->one('SELECT * FROM inventory_pick_lists WHERE id = ? AND organization_id = ? FOR UPDATE', [$pickId, $this->organizationId]);
            if (!$pick || !in_array($pick['status'], ['OPEN', 'PICKING'], true) || $quantity <= 0) {
                throw new InvalidArgumentException('Lista di picking o quantità non valida.');
            }
            $product = $this->one('SELECT id FROM products WHERE organization_id = ? AND active = 1 AND (ean = ? OR sku = ? OR code = ?) LIMIT 1', [$this->organizationId, $barcode, $barcode, $barcode]);
            $result = 'UNKNOWN';
            $message = 'Barcode non riconosciuto.';
            $item = false;
            if ($product) {
                $item = $this->one('SELECT * FROM inventory_pick_items WHERE pick_list_id = ? AND product_id = ? FOR UPDATE', [$pickId, $product['id']]);
                if (!$item) {
                    $result = 'EXCESS';
                    $message = 'Il prodotto non è previsto in questo picking.';
                } elseif ((float) $item['picked_quantity'] + $quantity > (float) $item['required_quantity'] + 0.00005) {
                    $result = 'EXCESS';
                    $message = 'Quantità superiore a quella richiesta.';
                } else {
                    $picked = round((float) $item['picked_quantity'] + $quantity, 4);
                    $status = $picked + 0.00005 >= (float) $item['required_quantity'] ? 'PICKED' : 'PARTIAL';
                    $this->db->prepare('UPDATE inventory_pick_items SET picked_quantity = ?, status = ? WHERE id = ?')->execute([$picked, $status, $item['id']]);
                    $this->db->prepare("UPDATE inventory_pick_lists SET status = 'PICKING', started_at = COALESCE(started_at, NOW()) WHERE id = ?")->execute([$pickId]);
                    $result = 'ACCEPTED';
                    $message = 'Scansione acquisita.';
                }
            }
            $this->db->prepare('INSERT INTO inventory_barcode_events (organization_id, pick_list_id, warehouse_id, product_id, barcode, quantity, result, message, scanned_by, scanned_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())')
                ->execute([$this->organizationId, $pickId, $pick['warehouse_id'], $product['id'] ?? null, $barcode, $quantity, $result, $message, $this->userId]);
            return ['result' => $result, 'message' => $message];
        });
    }

    public function completePick(int $pickId): void
    {
        $this->transaction(function () use ($pickId): void {
            $pick = $this->one('SELECT * FROM inventory_pick_lists WHERE id = ? AND organization_id = ? FOR UPDATE', [$pickId, $this->organizationId]);
            if (!$pick || !in_array($pick['status'], ['OPEN', 'PICKING'], true)) {
                throw new InvalidArgumentException('Lista di picking non completabile.');
            }
            $items = $this->all('SELECT pi.*, dl.description, d.number, d.document_type FROM inventory_pick_items pi INNER JOIN document_lines dl ON dl.id = pi.document_line_id INNER JOIN documents d ON d.id = dl.document_id WHERE pi.pick_list_id = ? AND pi.organization_id = ? FOR UPDATE', [$pickId, $this->organizationId]);
            foreach ($items as $item) {
                if ((float) $item['picked_quantity'] + 0.00005 < (float) $item['required_quantity']) {
                    throw new InvalidArgumentException('Completa il prelievo di ' . $item['description'] . '.');
                }
                $this->applyMovement([
                    'movement_uuid' => $this->uuidFor('pick', $pickId, (int) $item['id']), 'movement_date' => date('Y-m-d'),
                    'product_id' => $item['product_id'], 'warehouse_id' => $pick['warehouse_id'], 'movement_type' => 'OUT',
                    'quantity' => $item['picked_quantity'], 'reason' => 'Picking ' . $pick['pick_number'],
                    'document_type' => $item['document_type'], 'document_number' => $item['number'], 'source_type' => 'PICK_LIST', 'source_id' => $pickId,
                ], true);
                $this->db->prepare('UPDATE document_lines SET reserved_quantity = GREATEST(0, reserved_quantity - ?), fulfilled_quantity = fulfilled_quantity + ?, updated_at = NOW() WHERE id = ?')
                    ->execute([$item['required_quantity'], $item['picked_quantity'], $item['document_line_id']]);
            }
            $this->db->prepare("UPDATE inventory_pick_lists SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?")->execute([$pickId]);
            $this->db->prepare("UPDATE documents SET fulfillment_status = 'FULFILLED', updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([$pick['document_id'], $this->organizationId]);
        });
    }

    public function cancelPick(int $pickId): void
    {
        $this->transaction(function () use ($pickId): void {
            $pick=$this->one('SELECT * FROM inventory_pick_lists WHERE id=? AND organization_id=? FOR UPDATE',[$pickId,$this->organizationId]);if(!$pick||!in_array($pick['status'],['OPEN','PICKING'],true)){throw new InvalidArgumentException('Lista di picking non annullabile.');}
            $items=$this->all('SELECT * FROM inventory_pick_items WHERE pick_list_id=? AND organization_id=? FOR UPDATE',[$pickId,$this->organizationId]);
            foreach($items as $item){$balance=$this->balance((int)$pick['warehouse_id'],(int)$item['product_id'],true);$release=(float)$item['required_quantity'];$this->db->prepare('UPDATE inventory_balances SET reserved_quantity=GREATEST(0,reserved_quantity-?) WHERE id=?')->execute([$release,$balance['id']]);$this->db->prepare('UPDATE document_lines SET reserved_quantity=GREATEST(0,reserved_quantity-?),updated_at=NOW() WHERE id=?')->execute([$release,$item['document_line_id']]);}
            $this->db->prepare("UPDATE inventory_pick_lists SET status='CANCELLED' WHERE id=?")->execute([$pickId]);
        });
    }

    private function applyMovement(array $data, bool $consumeReservation = false): int
    {
        $uuid = (string) ($data['movement_uuid'] ?? $this->uuid());
        $existing = $this->one('SELECT id FROM inventory_movements WHERE organization_id = ? AND movement_uuid = ?', [$this->organizationId, $uuid]);
        if ($existing) {
            return (int) $existing['id'];
        }
        $productId = (int) ($data['product_id'] ?? 0);
        $warehouseId = (int) ($data['warehouse_id'] ?? 0);
        $type = strtoupper((string) ($data['movement_type'] ?? ''));
        $quantity = round(abs((float) ($data['quantity'] ?? 0)), 4);
        if (!in_array($type, ['IN', 'OUT', 'TRANSFER_IN', 'TRANSFER_OUT', 'ADJUSTMENT'], true) || $quantity <= 0) {
            throw new InvalidArgumentException('Movimento di magazzino non valido.');
        }
        $product = $this->one('SELECT id, code, track_inventory FROM products WHERE id = ? AND organization_id = ? AND active = 1', [$productId, $this->organizationId]);
        $warehouse = $this->warehouse($warehouseId);
        if (!$product || !(bool) $product['track_inventory']) {
            throw new InvalidArgumentException('Prodotto non valido o non gestito a magazzino.');
        }
        $balance = $this->balance($warehouseId, $productId, true);
        $direction = in_array($type, ['IN', 'TRANSFER_IN'], true) ? 1 : -1;
        if ($type === 'ADJUSTMENT') {
            $direction = ((float) ($data['signed_quantity'] ?? $data['quantity'])) >= 0 ? 1 : -1;
        }
        $newQuantity = round((float) $balance['quantity'] + ($direction * $quantity), 4);
        $availableAfter = $newQuantity - (float) $balance['reserved_quantity'] + ($consumeReservation ? $quantity : 0);
        if ($newQuantity < -0.00005 || $availableAfter < -0.00005) {
            throw new RuntimeException('Giacenza insufficiente: il movimento porterebbe il saldo sotto zero.');
        }
        $unitCost = isset($data['unit_cost']) ? round((float) $data['unit_cost'], 4) : (float) $balance['average_cost'];
        $averageCost = (float) $balance['average_cost'];
        if ($direction > 0 && $newQuantity > 0) {
            $averageCost = round((((float) $balance['quantity'] * $averageCost) + ($quantity * $unitCost)) / $newQuantity, 4);
        }
        $reserved = $consumeReservation ? max(0, (float) $balance['reserved_quantity'] - $quantity) : (float) $balance['reserved_quantity'];
        $this->db->prepare('UPDATE inventory_balances SET quantity = ?, reserved_quantity = ?, average_cost = ?, updated_at = NOW() WHERE id = ?')
            ->execute([$newQuantity, $reserved, $averageCost, $balance['id']]);
        $this->db->prepare('INSERT INTO inventory_movements (movement_uuid, organization_id, movement_date, product_id, product_code, warehouse_id, warehouse_code, movement_type, quantity, unit_cost, reason, document_type, document_number, source_type, source_id, reversed_movement_id, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())')
            ->execute([$uuid, $this->organizationId, $data['movement_date'] ?? date('Y-m-d'), $productId, $product['code'], $warehouseId, $warehouse['code'], $type, $quantity, $unitCost, (string) ($data['reason'] ?? 'Movimento manuale'), $data['document_type'] ?? null, $data['document_number'] ?? null, $data['source_type'] ?? null, $data['source_id'] ?? null, $data['reversed_movement_id'] ?? null, $this->userId, $this->userId]);
        return (int) $this->db->lastInsertId();
    }

    private function balance(int $warehouseId, int $productId, bool $lock): array
    {
        $suffix = $lock ? ' FOR UPDATE' : '';
        $balance = $this->one('SELECT * FROM inventory_balances WHERE organization_id = ? AND warehouse_id = ? AND product_id = ?' . $suffix, [$this->organizationId, $warehouseId, $productId]);
        if (!$balance) {
            $this->db->prepare('INSERT INTO inventory_balances (organization_id, warehouse_id, product_id, quantity, reserved_quantity, minimum_stock, average_cost, updated_at) VALUES (?, ?, ?, 0, 0, 0, 0, NOW())')
                ->execute([$this->organizationId, $warehouseId, $productId]);
            $balance = $this->one('SELECT * FROM inventory_balances WHERE organization_id = ? AND warehouse_id = ? AND product_id = ?' . $suffix, [$this->organizationId, $warehouseId, $productId]);
        }
        return $balance;
    }

    private function warehouse(int $id): array
    {
        $warehouse = $this->one('SELECT id, code FROM warehouses WHERE id = ? AND organization_id = ? AND active = 1', [$id, $this->organizationId]);
        if (!$warehouse) {
            throw new InvalidArgumentException('Magazzino non valido.');
        }
        return $warehouse;
    }

    private function transfer(int $id, bool $lock): array
    {
        $transfer = $this->one('SELECT * FROM inventory_transfers WHERE id = ? AND organization_id = ?' . ($lock ? ' FOR UPDATE' : ''), [$id, $this->organizationId]);
        if (!$transfer) {
            throw new InvalidArgumentException('Trasferimento non trovato.');
        }
        return $transfer;
    }

    private function nextOperationalNumber(string $prefix, string $date): string
    {
        $year = substr($date, 0, 4);
        $key = 'OPS-' . $prefix . '-' . $year;
        $sequence = $this->one('SELECT id, next_value, padding FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE', [$this->organizationId, $key]);
        if (!$sequence) {
            $this->db->prepare('INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at) VALUES (?, ?, ?, 2, 5, NOW(), NOW())')->execute([$this->organizationId, $key, $prefix . '-' . $year . '-']);
            return $prefix . '-' . $year . '-00001';
        }
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + 1 WHERE id = ?')->execute([$sequence['id']]);
        return $prefix . '-' . $year . '-' . str_pad((string) $sequence['next_value'], (int) $sequence['padding'], '0', STR_PAD_LEFT);
    }

    private function uuidFor(string $scope, int $parent, int $line): string
    {
        $hex = hash('sha256', implode('|', [$this->organizationId, $scope, $parent, $line]));
        return substr($hex, 0, 8) . '-' . substr($hex, 8, 4) . '-4' . substr($hex, 13, 3) . '-a' . substr($hex, 17, 3) . '-' . substr($hex, 20, 12);
    }

    private function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($bytes), 4));
    }

    private function one(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetch();
    }

    private function all(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetchAll();
    }

    private function transaction(callable $callback): mixed
    {
        $owns = !$this->db->inTransaction();
        if ($owns) {
            $this->db->beginTransaction();
        }
        try {
            $result = $callback();
            if ($owns) {
                $this->db->commit();
            }
            return $result;
        } catch (Throwable $exception) {
            if ($owns && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }
}
