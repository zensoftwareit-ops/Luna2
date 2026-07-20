<?php

declare(strict_types=1);

namespace Luna\Service;

use PDO;
use Throwable;

final class OrganizationService
{
    public function __construct(private readonly PDO $db)
    {
    }

    public function create(array $data): int
    {
        $fields = [
            'business_name', 'vat_number', 'tax_code', 'fiscal_regime', 'sdi_code', 'pec',
            'email', 'phone', 'address', 'postal_code', 'city', 'province', 'country_code', 'iban',
        ];
        $values = [];
        foreach ($fields as $field) {
            $value = trim((string) ($data[$field] ?? ''));
            $values[$field] = $value === '' ? null : $value;
        }
        $values['fiscal_regime'] ??= 'RF01';
        $values['country_code'] = strtoupper((string) ($values['country_code'] ?? 'IT'));

        $this->db->beginTransaction();
        try {
            $columns = implode(', ', array_keys($values));
            $placeholders = implode(', ', array_fill(0, count($values), '?'));
            $statement = $this->db->prepare("INSERT INTO organizations ({$columns}) VALUES ({$placeholders})");
            $statement->execute(array_values($values));
            $organizationId = (int) $this->db->lastInsertId();
            $this->seedCore($organizationId);
            $this->db->commit();
            return $organizationId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function seedCore(int $organizationId): void
    {
        $accounts = [
            ['1000', 'Crediti verso clienti', 'ASSET', 'TRADE_RECEIVABLES'],
            ['1100', 'Banca c/c', 'ASSET', 'BANK'],
            ['1200', 'IVA a credito', 'ASSET', 'VAT_RECEIVABLE'],
            ['2000', 'Debiti verso fornitori', 'LIABILITY', 'TRADE_PAYABLES'],
            ['2100', 'IVA a debito', 'LIABILITY', 'VAT_PAYABLE'],
            ['3000', 'Patrimonio netto', 'EQUITY', 'EQUITY'],
            ['4000', 'Ricavi vendite e prestazioni', 'REVENUE', 'SALES_REVENUE'],
            ['5000', 'Costi per acquisti e servizi', 'EXPENSE', 'PURCHASE_COSTS'],
        ];
        $statement = $this->db->prepare(
            'INSERT INTO chart_of_accounts (organization_id, code, name, account_type, system_key, is_postable, active)
             VALUES (?, ?, ?, ?, ?, 1, 1)'
        );
        foreach ($accounts as $account) {
            $statement->execute(array_merge([$organizationId], $account));
        }

        $vatCodes = [
            ['22', 'IVA ordinaria 22%', 22, null, null],
            ['10', 'IVA ridotta 10%', 10, null, null],
            ['5', 'IVA ridotta 5%', 5, null, null],
            ['4', 'IVA ridotta 4%', 4, null, null],
            ['N1', 'Escluse ex art. 15', 0, 'N1', 'Art. 15 DPR 633/72'],
            ['N2.2', 'Non soggette - altri casi', 0, 'N2.2', null],
            ['N3.5', 'Non imponibili - dichiarazioni intento', 0, 'N3.5', null],
            ['N4', 'Operazioni esenti', 0, 'N4', 'Art. 10 DPR 633/72'],
            ['N6.7', 'Reverse charge - edilizia', 0, 'N6.7', null],
        ];
        $statement = $this->db->prepare(
            'INSERT INTO vat_codes (organization_id, code, description, rate, nature, legal_reference)
             VALUES (?, ?, ?, ?, ?, ?)'
        );
        foreach ($vatCodes as $vatCode) {
            $statement->execute(array_merge([$organizationId], $vatCode));
        }

        $this->db->prepare("INSERT INTO warehouses (organization_id, code, name, active) VALUES (?, 'MAIN', 'Magazzino principale', 1)")
            ->execute([$organizationId]);
        $stages = [['Nuovo', 10], ['Contattato', 20], ['Qualificato', 40], ['Proposta', 60], ['Negoziazione', 80], ['Vinto', 100]];
        $statement = $this->db->prepare(
            'INSERT INTO pipeline_stages (organization_id, name, position, probability, won) VALUES (?, ?, ?, ?, ?)'
        );
        foreach ($stages as $position => [$name, $probability]) {
            $statement->execute([$organizationId, $name, $position + 1, $probability, $name === 'Vinto' ? 1 : 0]);
        }
    }
}
