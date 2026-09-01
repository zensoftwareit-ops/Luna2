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
        $values = $this->normalize($data);

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

    public function update(int $organizationId, array $data): void
    {
        $values = $this->normalize($data);
        if (($values['business_name'] ?? null) === null) {
            throw new \InvalidArgumentException('La ragione sociale è obbligatoria.');
        }
        $assignments = implode(', ', array_map(static fn (string $field): string => "{$field} = ?", array_keys($values)));
        $statement = $this->db->prepare("UPDATE organizations SET {$assignments}, updated_at = NOW() WHERE id = ? AND active = 1");
        $statement->execute([...array_values($values), $organizationId]);
        if ($statement->rowCount() === 0) {
            $check = $this->db->prepare('SELECT 1 FROM organizations WHERE id = ? AND active = 1');
            $check->execute([$organizationId]);
            if (!$check->fetchColumn()) {
                throw new \InvalidArgumentException('Azienda non disponibile.');
            }
        }
    }

    private function normalize(array $data): array
    {
        $fields = [
            'business_name', 'vat_number', 'tax_code', 'fiscal_regime', 'sdi_code', 'pec',
            'email', 'phone', 'address', 'postal_code', 'city', 'province', 'country_code', 'iban',
            'withholding_type', 'withholding_rate', 'withholding_taxable_percent', 'withholding_cause',
        ];
        $values = [];
        foreach ($fields as $field) {
            $value = trim((string) ($data[$field] ?? ''));
            $values[$field] = $value === '' ? null : $value;
        }
        $values['fiscal_regime'] ??= 'RF01';
        $values['country_code'] = strtoupper((string) ($values['country_code'] ?? 'IT'));
        $values['vat_number'] = PartyAutomationService::normalizeVat($values['vat_number'] ?? null, $values['country_code']);
        $values['withholding_enabled'] = !empty($data['withholding_enabled']) ? 1 : 0;
        $values['withholding_type'] ??= 'RT01';
        $values['withholding_rate'] = (float) ($values['withholding_rate'] ?? 20);
        $values['withholding_taxable_percent'] = (float) ($values['withholding_taxable_percent'] ?? 100);
        return $values;
    }

    private function seedCore(int $organizationId): void
    {
        $accounts = [
            ['1000', 'Crediti verso clienti', 'ASSET', 'DEBIT', 'TRADE_RECEIVABLES', 'SP.ATTIVO.CREDITI'],
            ['1100', 'Banca c/c', 'ASSET', 'DEBIT', 'BANK', 'SP.ATTIVO.LIQUIDITA'],
            ['1110', 'Cassa contanti', 'ASSET', 'DEBIT', 'CASH', 'SP.ATTIVO.LIQUIDITA'],
            ['1200', 'IVA a credito', 'ASSET', 'DEBIT', 'VAT_RECEIVABLE', 'SP.ATTIVO.CREDITI_TRIBUTARI'],
            ['1300', 'Risconti attivi', 'ASSET', 'DEBIT', 'PREPAID_EXPENSES', 'SP.ATTIVO.RATEI_RISCONTI'],
            ['1310', 'Ratei attivi', 'ASSET', 'DEBIT', 'ACCRUED_INCOME', 'SP.ATTIVO.RATEI_RISCONTI'],
            ['1500', 'Fondo ammortamento', 'ASSET', 'CREDIT', 'ACCUMULATED_DEPRECIATION', 'SP.ATTIVO.IMMOBILIZZAZIONI'],
            ['2000', 'Debiti verso fornitori', 'LIABILITY', 'CREDIT', 'TRADE_PAYABLES', 'SP.PASSIVO.DEBITI'],
            ['2100', 'IVA a debito', 'LIABILITY', 'CREDIT', 'VAT_PAYABLE', 'SP.PASSIVO.DEBITI_TRIBUTARI'],
            ['2200', 'Erario c/IVA', 'LIABILITY', 'CREDIT', 'VAT_CLEARING', 'SP.PASSIVO.DEBITI_TRIBUTARI'],
            ['2300', 'Erario c/ritenute', 'LIABILITY', 'CREDIT', 'WITHHOLDING_PAYABLE', 'SP.PASSIVO.DEBITI_TRIBUTARI'],
            ['2310', 'Enti previdenziali', 'LIABILITY', 'CREDIT', 'SOCIAL_SECURITY_PAYABLE', 'SP.PASSIVO.DEBITI'],
            ['2400', 'Ratei passivi', 'LIABILITY', 'CREDIT', 'ACCRUED_EXPENSES', 'SP.PASSIVO.RATEI_RISCONTI'],
            ['2410', 'Risconti passivi', 'LIABILITY', 'CREDIT', 'DEFERRED_INCOME', 'SP.PASSIVO.RATEI_RISCONTI'],
            ['3000', 'Patrimonio netto', 'EQUITY', 'CREDIT', 'EQUITY', 'SP.PASSIVO.PATRIMONIO_NETTO'],
            ['3100', 'Utili e perdite portati a nuovo', 'EQUITY', 'CREDIT', 'RETAINED_EARNINGS', 'SP.PASSIVO.PATRIMONIO_NETTO'],
            ['3900', 'Risultato d’esercizio', 'EQUITY', 'CREDIT', 'PROFIT_LOSS', 'SP.PASSIVO.PATRIMONIO_NETTO'],
            ['3990', 'Bilancio di apertura', 'EQUITY', 'CREDIT', 'OPENING_BALANCE', 'SP.PASSIVO.PATRIMONIO_NETTO'],
            ['4000', 'Ricavi vendite e prestazioni', 'REVENUE', 'CREDIT', 'SALES_REVENUE', 'CE.A.RICAVI'],
            ['4010', 'Altri ricavi e proventi', 'REVENUE', 'CREDIT', 'OTHER_REVENUE', 'CE.A.ALTRI_RICAVI'],
            ['5000', 'Costi per acquisti e servizi', 'EXPENSE', 'DEBIT', 'PURCHASE_COSTS', 'CE.B.COSTI'],
            ['5010', 'Abbuoni e differenze passive', 'EXPENSE', 'DEBIT', 'PAYMENT_DIFFERENCES', 'CE.B.COSTI'],
            ['5100', 'Ammortamenti', 'EXPENSE', 'DEBIT', 'DEPRECIATION_EXPENSE', 'CE.B.AMMORTAMENTI'],
        ];
        $statement = $this->db->prepare(
            'INSERT INTO chart_of_accounts
             (organization_id, code, name, account_type, normal_balance, system_key, statement_section, is_postable, active, locked)
             VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, 1)'
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

        $this->db->prepare('INSERT INTO accounting_settings (organization_id) VALUES (?)')->execute([$organizationId]);
        $register = $this->db->prepare(
            'INSERT INTO vat_registers (organization_id, code, name, register_type, is_default) VALUES (?, ?, ?, ?, 1)'
        );
        foreach ([['V1', 'Vendite', 'SALES'], ['A1', 'Acquisti', 'PURCHASES'], ['C1', 'Corrispettivi', 'CORRISPETTIVI']] as $row) {
            $register->execute(array_merge([$organizationId], $row));
        }
        $cause = $this->db->prepare(
            'INSERT INTO accounting_causes
             (organization_id, code, name, category, default_description, creates_open_item, automatic)
             VALUES (?, ?, ?, ?, ?, ?, ?)'
        );
        foreach ([
            ['GEN', 'Registrazione generica', 'GENERAL', 'Registrazione contabile', 0, 0],
            ['FAV', 'Fattura di vendita', 'SALES', 'Fattura di vendita', 1, 1],
            ['FAC', 'Fattura di acquisto', 'PURCHASE', 'Fattura di acquisto', 1, 1],
            ['INC', 'Incasso cliente', 'RECEIPT', 'Incasso cliente', 0, 1],
            ['PAG', 'Pagamento fornitore', 'PAYMENT', 'Pagamento fornitore', 0, 1],
        ] as $row) {
            $cause->execute(array_merge([$organizationId], $row));
        }
        $this->db->prepare(
            'INSERT INTO accounting_account_mappings (organization_id, mapping_key, account_id, description)
             SELECT organization_id, system_key, id, name FROM chart_of_accounts
             WHERE organization_id = ? AND system_key IS NOT NULL'
        )->execute([$organizationId]);

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
