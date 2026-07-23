<?php

declare(strict_types=1);

namespace Luna\Service;

use PDO;

final class DocumentNumberService
{
    private const PREFIXES = [
        'QUOTE' => 'PREV', 'SALES_ORDER' => 'ORD', 'DDT' => 'DDT',
        'PROFORMA' => 'PF', 'SALES_INVOICE' => 'FT', 'CREDIT_NOTE' => 'NC',
        'PURCHASE_ORDER' => 'ORDF', 'PURCHASE_INVOICE' => 'FP',
    ];

    public function __construct(private readonly PDO $db, private readonly int $organizationId)
    {
    }

    public function next(string $documentType, string $date): string
    {
        $year = (int) substr($date, 0, 4);
        $key = $documentType . '-' . $year;
        $statement = $this->db->prepare('SELECT id, prefix, next_value, padding FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE');
        $statement->execute([$this->organizationId, $key]);
        $sequence = $statement->fetch();
        if (!$sequence) {
            $prefix = (self::PREFIXES[$documentType] ?? $documentType) . '-' . $year . '-';
            $this->db->prepare('INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at) VALUES (?, ?, ?, 2, 4, NOW(), NOW())')
                ->execute([$this->organizationId, $key, $prefix]);
            return $prefix . '0001';
        }
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + 1, updated_at = NOW() WHERE id = ?')->execute([$sequence['id']]);
        return (string) $sequence['prefix'] . str_pad((string) $sequence['next_value'], (int) $sequence['padding'], '0', STR_PAD_LEFT);
    }
}
