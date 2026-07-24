<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use PDO;
use RuntimeException;
use Throwable;

final class DemoDataSeeder
{
    private const DEMO_TAX_CODE = 'LUNA2-DEMO-DATA';

    /** @var array<string,array<string,true>> */
    private array $columns = [];
    /** @var array<string,int> */
    private array $counts = [];

    public function __construct(private readonly PDO $db, private readonly string $basePath)
    {
    }

    /** @return array{organization_id:int,email:string,password:string,months:int,counts:array<string,int>} */
    public function seed(int $months = 18, bool $reset = false): array
    {
        $months = max(3, min(36, $months));
        $existing = $this->findDemoOrganization();
        if ($existing > 0 && !$reset) {
            throw new RuntimeException(
                "L'azienda demo esiste già (ID {$existing}). Usa demo:seed --reset per rigenerarla integralmente."
            );
        }

        $this->requireTables([
            'organizations', 'users', 'customers', 'suppliers', 'products', 'documents',
            'journal_entries', 'journal_entry_lines', 'projects', 'time_records', 'payroll_runs',
            'ecommerce_channels', 'rental_contracts', 'calendar_events',
        ]);

        $password = 'Demo-' . strtoupper(bin2hex(random_bytes(4))) . '!';
        $hash = password_hash($password, PASSWORD_ARGON2ID);
        if ($hash === false) {
            throw new RuntimeException('Impossibile generare la password demo.');
        }

        $this->db->beginTransaction();
        try {
            if ($existing > 0) {
                $this->db->prepare('DELETE FROM organizations WHERE id = ?')->execute([$existing]);
            }

            $organizationId = $this->add('organizations', [
                'business_name' => 'Luna Demo S.r.l.',
                'vat_number' => '09999990969',
                'tax_code' => self::DEMO_TAX_CODE,
                'fiscal_regime' => 'RF01',
                'sdi_code' => 'DEMO000',
                'pec' => 'lunademo@pec.example',
                'email' => 'amministrazione@demo.luna.local',
                'phone' => '+39 02 5550 1000',
                'address' => 'Via dell’Innovazione 24',
                'postal_code' => '20100',
                'city' => 'Milano',
                'province' => 'MI',
                'country_code' => 'IT',
                'iban' => 'IT60X0542811101000000123456',
                'active' => 1,
            ]);

            $users = $this->seedUsers($organizationId, $hash);
            $this->seedModules($organizationId);
            [$customers, $suppliers] = $this->seedParties($organizationId, $users['owner']);
            [$products, $warehouses] = $this->seedCatalog($organizationId, $users['owner'], $suppliers);
            $accounting = $this->seedAccountingSetup($organizationId, $months, $users);
            $documents = $this->seedHistory(
                $organizationId,
                $months,
                $users,
                $customers,
                $suppliers,
                $products,
                $accounting
            );
            $this->seedInventory($organizationId, $users, $products, $warehouses, $documents);
            $this->seedCrmAndProjects($organizationId, $users, $customers, $documents, $products);
            $this->seedPeople($organizationId, $months, $users);
            $this->seedIntegrations($organizationId, $users, $customers, $products, $documents);
            $this->seedProfessionalWorkspace($organizationId, $users, $accounting);

            $this->add('audit_logs', [
                'organization_id' => $organizationId, 'user_id' => $users['owner'],
                'action' => 'DEMO_SEED', 'entity_type' => 'organization',
                'entity_id' => (string) $organizationId,
                'payload_json' => $this->json(['months' => $months, 'generated_at' => date(DATE_ATOM)]),
            ]);

            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw new RuntimeException('Generazione demo annullata: ' . $exception->getMessage(), 0, $exception);
        }

        ksort($this->counts);
        return [
            'organization_id' => $organizationId,
            'email' => 'owner.demo@luna.local',
            'password' => $password,
            'months' => $months,
            'counts' => $this->counts,
        ];
    }

    /** @return array<string,int> */
    private function seedUsers(int $organizationId, string $hash): array
    {
        $definitions = [
            'owner' => ['Federico Demo', 'owner.demo@luna.local', 'OWNER'],
            'accountant' => ['Anna Contabile', 'contabilita.demo@luna.local', 'ACCOUNTANT'],
            'sales' => ['Marco Commerciale', 'vendite.demo@luna.local', 'SALES'],
            'warehouse' => ['Luca Magazzino', 'magazzino.demo@luna.local', 'WAREHOUSE'],
            'hr' => ['Giulia Risorse Umane', 'hr.demo@luna.local', 'HR'],
            'viewer' => ['Paola Direzione', 'direzione.demo@luna.local', 'VIEWER'],
        ];
        $users = [];
        foreach ($definitions as $key => [$name, $email, $role]) {
            $users[$key] = $this->add('users', [
                'organization_id' => $organizationId, 'name' => $name, 'email' => $email,
                'password_hash' => $hash, 'role' => $role, 'active' => 1,
                'last_login_at' => date('Y-m-d H:i:s', strtotime('-' . (count($users) + 1) . ' days')),
            ]);
        }
        return $users;
    }

    private function seedModules(int $organizationId): void
    {
        $features = require $this->basePath . '/config/features.php';
        foreach (array_keys($features) as $key) {
            $this->add('module_settings', [
                'organization_id' => $organizationId, 'module_key' => (string) $key,
                'enabled' => 1, 'settings_json' => $this->json(['demo' => true]),
            ]);
        }
        foreach (['QUOTE','SALES_ORDER','PURCHASE_ORDER','DDT','PROFORMA','SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE'] as $type) {
            $this->add('document_sequences', [
                'organization_id' => $organizationId, 'sequence_key' => $type . ':' . date('Y'),
                'prefix' => substr($type, 0, 3) . '/', 'next_value' => 100, 'padding' => 4,
            ]);
        }
    }

    /** @return array{0:list<int>,1:list<int>} */
    private function seedParties(int $organizationId, int $userId): array
    {
        $customerNames = [
            'Alfa Industrie S.p.A.', 'Beta Retail S.r.l.', 'Gamma Servizi S.r.l.',
            'Delta Manufacturing S.p.A.', 'Epsilon Consulting S.r.l.', 'Zeta Food S.r.l.',
            'Eta Logistica S.p.A.', 'Theta Design Studio',
        ];
        $customers = [];
        foreach ($customerNames as $index => $name) {
            $n = $index + 1;
            $customers[] = $this->add('customers', [
                'organization_id' => $organizationId, 'code' => sprintf('CLI%03d', $n),
                'business_name' => $name, 'vat_number' => sprintf('08%09d', $n),
                'tax_code' => sprintf('DMO%08dXX', $n), 'sdi_code' => 'ABC' . sprintf('%04d', $n),
                'email' => 'amministrazione' . $n . '@cliente-demo.local', 'phone' => '+39 02 6000 ' . sprintf('%04d', $n),
                'address' => 'Via Cliente ' . $n, 'postal_code' => '20100', 'city' => $n % 2 ? 'Milano' : 'Monza',
                'province' => $n % 2 ? 'MI' : 'MB', 'country_code' => 'IT',
                'payment_terms' => $n % 3 === 0 ? 'Bonifico 60 gg' : 'Bonifico 30 gg',
                'credit_limit' => 15000 + $n * 5000, 'active' => 1, 'created_by' => $userId,
            ]);
        }
        $supplierNames = ['Omega Forniture S.r.l.', 'Sigma Technology S.p.A.', 'Carta & Imballi S.r.l.', 'Energia Demo S.p.A.', 'Studio Lavoro Associato'];
        $suppliers = [];
        foreach ($supplierNames as $index => $name) {
            $n = $index + 1;
            $suppliers[] = $this->add('suppliers', [
                'organization_id' => $organizationId, 'code' => sprintf('FOR%03d', $n),
                'business_name' => $name, 'vat_number' => sprintf('07%09d', $n),
                'email' => 'ufficio' . $n . '@fornitore-demo.local', 'phone' => '+39 02 7000 ' . sprintf('%04d', $n),
                'address' => 'Viale Fornitore ' . $n, 'postal_code' => '20090', 'city' => 'Milano',
                'province' => 'MI', 'country_code' => 'IT', 'payment_terms' => 'Bonifico 30 gg',
                'active' => 1, 'created_by' => $userId,
            ]);
        }
        foreach (array_slice($customers, 0, 4) as $index => $customerId) {
            $this->add('contacts', [
                'organization_id' => $organizationId, 'customer_id' => $customerId,
                'name' => ['Laura Bianchi','Andrea Rossi','Chiara Verdi','Davide Neri'][$index],
                'job_title' => $index % 2 ? 'Ufficio acquisti' : 'Amministrazione',
                'email' => 'referente' . ($index + 1) . '@cliente-demo.local', 'created_by' => $userId,
            ]);
        }
        return [$customers, $suppliers];
    }

    /** @return array{0:list<int>,1:list<int>} */
    private function seedCatalog(int $organizationId, int $userId, array $suppliers): array
    {
        $definitions = [
            ['HW-001','Notebook Business 15"', 'Hardware', 'PRODUCT', 1290, 820, 1],
            ['HW-002','Monitor professionale 27"', 'Hardware', 'PRODUCT', 390, 230, 1],
            ['HW-003','Docking station USB-C', 'Hardware', 'PRODUCT', 189, 108, 1],
            ['SW-001','Licenza gestionale annuale', 'Software', 'SERVICE', 960, 0, 0],
            ['SRV-001','Consulenza applicativa', 'Servizi', 'SERVICE', 95, 38, 0],
            ['SRV-002','Assistenza sistemistica', 'Servizi', 'SERVICE', 110, 45, 0],
            ['ACC-001','Tastiera professionale', 'Accessori', 'PRODUCT', 89, 44, 1],
            ['ACC-002','Mouse wireless', 'Accessori', 'PRODUCT', 59, 25, 1],
            ['NET-001','Firewall per PMI', 'Networking', 'PRODUCT', 780, 490, 1],
            ['NET-002','Access point Wi-Fi 6', 'Networking', 'PRODUCT', 210, 125, 1],
        ];
        $products = [];
        foreach ($definitions as $index => [$code, $name, $category, $type, $sale, $cost, $stock]) {
            $products[] = $this->add('products', [
                'organization_id' => $organizationId, 'code' => $code, 'sku' => 'SKU-' . $code,
                'ean' => $stock ? sprintf('805000000%03d', $index + 1) : null,
                'name' => $name, 'description' => 'Voce dimostrativa con storico commerciale e operativo.',
                'category' => $category, 'product_type' => $type, 'unit' => $type === 'SERVICE' ? 'ORA' : 'NR',
                'sale_price' => $sale, 'purchase_cost' => $cost, 'vat_rate' => 22,
                'track_inventory' => $stock, 'minimum_stock' => $stock ? 5 : null,
                'active' => 1, 'created_by' => $userId,
            ]);
        }
        $this->add('product_variants', [
            'organization_id' => $organizationId, 'product_id' => $products[0],
            'sku' => 'SKU-HW-001-RAM32', 'ean' => '805000000999', 'name' => 'RAM 32 GB',
            'attributes_json' => $this->json(['ram' => '32 GB', 'storage' => '1 TB']), 'sale_price' => 1490,
        ]);
        foreach (array_slice($products, 0, 6) as $index => $productId) {
            $this->add('supplier_price_lists', [
                'organization_id' => $organizationId, 'product_id' => $productId,
                'supplier_id' => $suppliers[$index % count($suppliers)], 'supplier_code' => 'SUP-' . ($index + 1),
                'purchase_price' => 40 + $index * 95, 'valid_from' => date('Y-01-01'), 'preferred' => 1,
            ]);
        }
        $warehouses = [
            $this->add('warehouses', ['organization_id' => $organizationId, 'code' => 'MAG-01', 'name' => 'Magazzino centrale', 'address' => 'Via Logistica 10, Milano', 'active' => 1, 'created_by' => $userId]),
            $this->add('warehouses', ['organization_id' => $organizationId, 'code' => 'MAG-02', 'name' => 'Deposito assistenza', 'address' => 'Via Service 4, Monza', 'active' => 1, 'created_by' => $userId]),
        ];
        foreach ($products as $index => $productId) {
            if (in_array($index, [3,4,5], true)) continue;
            foreach ($warehouses as $warehouseIndex => $warehouseId) {
                $this->add('inventory_balances', [
                    'organization_id' => $organizationId, 'warehouse_id' => $warehouseId, 'product_id' => $productId,
                    'quantity' => 12 + $index * 3 - $warehouseIndex * 4, 'reserved_quantity' => $index % 3,
                    'minimum_stock' => 5, 'average_cost' => 35 + $index * 70,
                ]);
            }
        }
        return [$products, $warehouses];
    }

    /** @return array<string,int> */
    private function seedAccountingSetup(int $organizationId, int $months, array $users): array
    {
        $year = (int) date('Y');
        $oldestYear = (int) (new DateTimeImmutable('first day of this month'))
            ->modify('-' . ($months - 1) . ' months')
            ->format('Y');
        foreach (range($oldestYear, $year) as $fiscalYear) {
            $this->add('fiscal_years', [
                'organization_id' => $organizationId, 'year' => $fiscalYear,
                'starts_on' => $fiscalYear . '-01-01', 'ends_on' => $fiscalYear . '-12-31',
                'status' => $fiscalYear === $year ? 'OPEN' : 'CLOSED',
            ]);
        }
        $accounts = [];
        foreach ([
            'cash' => ['1000.01','Cassa','ASSET','DEBIT'],
            'bank' => ['1000.02','Banca c/c','ASSET','DEBIT'],
            'customers' => ['1200.01','Crediti verso clienti','ASSET','DEBIT'],
            'suppliers' => ['2100.01','Debiti verso fornitori','LIABILITY','CREDIT'],
            'vat_credit' => ['1300.01','IVA a credito','ASSET','DEBIT'],
            'vat_debit' => ['2200.01','IVA a debito','LIABILITY','CREDIT'],
            'revenue' => ['4000.01','Ricavi vendite e servizi','REVENUE','CREDIT'],
            'costs' => ['5000.01','Acquisti merci e servizi','EXPENSE','DEBIT'],
            'payroll' => ['5100.01','Costo del personale','EXPENSE','DEBIT'],
            'assets' => ['1500.01','Immobilizzazioni materiali','ASSET','DEBIT'],
        ] as $key => [$code,$name,$type,$normal]) {
            $accounts[$key] = $this->add('chart_of_accounts', [
                'organization_id' => $organizationId, 'code' => $code, 'name' => $name,
                'account_type' => $type, 'normal_balance' => $normal, 'active' => 1,
            ]);
        }
        $costCenter = $this->add('cost_centers', [
            'organization_id' => $organizationId,
            'code' => "GEN",
            'name' => "Struttura generale",
            'active' => 1,
        ]);
        $vatCodeDefinitions = [
            ["22", "IVA ordinaria 22%", 22, null],
            ["10", "IVA ridotta 10%", 10, null],
            ["N1", "Escluso IVA", 0, "N1"],
            ["N3.2", "Non imponibile UE", 0, "N3.2"],
        ];
        foreach ($vatCodeDefinitions as [$code, $description, $rate, $nature]) {
            $this->add('vat_codes', [
                'organization_id' => $organizationId,
                'code' => $code,
                'description' => $description,
                'rate' => $rate,
                'nature' => $nature,
                'deductible_percent' => 100,
                'active' => 1,
            ]);
        }
        foreach ([['VEN', 'Registro vendite', 'SALES'], ['ACQ', 'Registro acquisti', 'PURCHASES'], ['COR', 'Registro corrispettivi', 'CORRISPETTIVI']] as [$code, $name, $type]) {
            $this->add('vat_registers', ['organization_id' => $organizationId, 'code' => $code, 'name' => $name, 'register_type' => $type, 'active' => 1]);
        }
        $this->add('accounting_settings', [
            'organization_id' => $organizationId, 'fiscal_year_start_month' => 1,
            'default_sales_vat_register_id' => null, 'default_purchase_vat_register_id' => null,
            'auto_post_documents' => 0, 'lock_posted_entries' => 1,
        ]);
        $cause = $this->add('accounting_causes', ['organization_id' => $organizationId, 'code' => 'FV', 'name' => 'Fattura vendita', 'category' => 'SALES', 'active' => 1]);
        $this->add('accounting_cause_lines', ['organization_id' => $organizationId, 'cause_id' => $cause, 'line_number' => 1, 'account_id' => $accounts['customers'], 'side' => 'DEBIT']);
        $this->add('accounting_cause_lines', ['organization_id' => $organizationId, 'cause_id' => $cause, 'line_number' => 2, 'account_id' => $accounts['revenue'], 'side' => 'CREDIT']);
        foreach (['SALES_REVENUE' => 'revenue','PURCHASE_COST' => 'costs','CUSTOMERS' => 'customers','SUPPLIERS' => 'suppliers','VAT_DEBIT' => 'vat_debit','VAT_CREDIT' => 'vat_credit'] as $mapping => $account) {
            $this->add('accounting_account_mappings', ['organization_id' => $organizationId, 'mapping_key' => $mapping, 'account_id' => $accounts[$account]]);
        }
        $bank = $this->add('bank_accounts', [
            'organization_id' => $organizationId, 'name' => 'Banca Demo - Conto operativo',
            'iban' => 'IT60X0542811101000000123456', 'bic' => 'DEMOITMM', 'currency' => 'EUR',
                'opening_balance' => 48500, 'active' => 1,
        ]);
        $accounts['bank_id'] = $bank;
        $accounts['cost_center'] = $costCenter;
        return $accounts;
    }

    /** @return array<string,mixed> */
    private function seedHistory(
        int $organizationId,
        int $months,
        array $users,
        array $customers,
        array $suppliers,
        array $products,
        array $accounts
    ): array {
        $documentIds = [];
        $lastSalesInvoice = 0;
        $lastPurchaseInvoice = 0;
        $today = new DateTimeImmutable('first day of this month');
        for ($offset = $months - 1; $offset >= 0; $offset--) {
            $month = $today->modify("-{$offset} months");
            $date = $month->modify('+' . (4 + ($offset % 10)) . ' days');
            $year = (int) $date->format('Y');
            $number = sprintf('%d/%04d', (int) $date->format('m'), $months - $offset);
            $customerId = $customers[$offset % count($customers)];
            $supplierId = $suppliers[$offset % count($suppliers)];
            $net = 1200 + (($offset * 317) % 4200);
            $vat = round($net * .22, 2);
            $total = $net + $vat;
            $paid = $offset > 1;

            $salesDocument = $this->add('documents', [
                'organization_id' => $organizationId, 'document_type' => 'SALES_INVOICE',
                'number' => 'FV-' . $number, 'fiscal_year' => $year, 'document_date' => $date->format('Y-m-d'),
                'due_date' => $date->modify('+30 days')->format('Y-m-d'), 'counterparty_type' => 'CUSTOMER',
                'counterparty_id' => $customerId, 'counterparty_name' => 'Cliente demo ' . (($offset % count($customers)) + 1),
                'subject' => 'Fornitura e servizi del periodo', 'taxable_total' => $net, 'vat_total' => $vat,
                'total' => $total, 'balance_due' => $paid ? 0 : $total,
                'status' => $paid ? 'PAID' : ($offset === 0 ? 'ISSUED' : 'OVERDUE'),
                'fatturapa_type' => 'TD01', 'vat_collectability' => 'I', 'payment_method_code' => 'MP05',
                'created_by' => $users['sales'],
            ]);
            $salesLine = $this->add('document_lines', [
                'organization_id' => $organizationId, 'document_id' => $salesDocument, 'line_number' => 1,
                'product_id' => $products[$offset % count($products)], 'product_code' => 'DEMO',
                'description' => 'Fornitura mensile e assistenza', 'quantity' => 1, 'unit' => 'NR',
                'unit_price' => $net, 'taxable_amount' => $net, 'vat_code' => '22',
                'vat_rate' => 22, 'vat_amount' => $vat, 'total_amount' => $total,
                'cost_center_id' => $accounts['cost_center'],
            ]);
            $schedule = $this->add('payment_schedules', [
                'organization_id' => $organizationId, 'document_id' => $salesDocument, 'installment_number' => 1,
                'due_date' => $date->modify('+30 days')->format('Y-m-d'), 'amount' => $total,
                'paid_amount' => $paid ? $total : 0, 'status' => $paid ? 'PAID' : ($offset === 0 ? 'OPEN' : 'OVERDUE'),
                'payment_method_code' => 'MP05',
            ]);
            $journal = $this->add('journal_entries', [
                'organization_id' => $organizationId, 'protocol_number' => 'PV-' . $date->format('Ym') . '-' . ($offset + 1),
                'entry_date' => $date->format('Y-m-d'), 'competence_date' => $date->format('Y-m-d'),
                'entry_type' => 'SALES_INVOICE', 'status' => 'POSTED', 'description' => 'Fattura vendita ' . $number,
                'document_number' => 'FV-' . $number, 'counterparty' => 'Cliente demo',
                'total_debit' => $total, 'total_credit' => $total, 'posted_at' => $date->format('Y-m-d H:i:s'),
                'created_by' => $users['accountant'],
            ]);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $journal, 'line_number' => 1, 'account_id' => $accounts['customers'], 'debit' => $total, 'credit' => 0, 'description' => 'Credito cliente', 'customer_id' => $customerId]);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $journal, 'line_number' => 2, 'account_id' => $accounts['revenue'], 'debit' => 0, 'credit' => $net, 'description' => 'Ricavo']);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $journal, 'line_number' => 3, 'account_id' => $accounts['vat_debit'], 'debit' => 0, 'credit' => $vat, 'description' => 'IVA a debito']);
            $vatMovement = $this->add('vat_movements', [
                'organization_id' => $organizationId, 'document_id' => $salesDocument, 'source_type' => 'DOCUMENT',
                'register_type' => 'SALES', 'movement_date' => $date->format('Y-m-d'), 'protocol_number' => 'VEN-' . $date->format('Ym'),
                'counterparty_name' => 'Cliente demo', 'description' => 'Fattura vendita',
                'vat_code' => '22', 'taxable_amount' => $net, 'vat_amount' => $vat,
                'vat_due_amount' => $vat, 'deductible_vat' => 0, 'period_year' => $year,
                'period_month' => (int) $date->format('m'), 'operation_type' => 'DOMESTIC',
            ]);
            $openItem = $this->add('accounting_open_items', [
                'organization_id' => $organizationId, 'direction' => 'RECEIVABLE', 'party_type' => 'CUSTOMER',
                'party_id' => $customerId, 'party_name' => 'Cliente demo', 'document_id' => $salesDocument,
                'payment_schedule_id' => $schedule, 'journal_entry_id' => $journal, 'account_id' => $accounts['customers'],
                'reference' => 'FV-' . $number, 'issue_date' => $date->format('Y-m-d'),
                'due_date' => $date->modify('+30 days')->format('Y-m-d'), 'original_amount' => $total,
                'settled_amount' => $paid ? $total : 0,
                'status' => $paid ? 'SETTLED' : ($offset === 0 ? 'OPEN' : 'OVERDUE'),
            ]);

            if ($paid) {
                $payment = $this->add('payments', [
                    'organization_id' => $organizationId, 'document_id' => $salesDocument,
                    'payment_schedule_id' => $schedule, 'payment_date' => $date->modify('+24 days')->format('Y-m-d'),
                    'amount' => $total, 'method' => 'BANK_TRANSFER', 'reference_number' => 'CRO-DEMO-' . $date->format('Ym'),
                    'bank_name' => 'Banca Demo', 'description' => 'Incasso fattura', 'reconciled' => 1,
                    'reconciled_at' => $date->modify('+25 days')->format('Y-m-d H:i:s'), 'created_by' => $users['accountant'],
                ]);
                $transaction = $this->add('bank_transactions', [
                    'organization_id' => $organizationId, 'bank_account_id' => $accounts['bank_id'],
                    'booking_date' => $date->modify('+25 days')->format('Y-m-d'), 'value_date' => $date->modify('+25 days')->format('Y-m-d'),
                    'amount' => $total, 'description' => 'BONIFICO CLIENTE DEMO', 'counterparty' => 'Cliente demo',
                    'reference' => 'FV-' . $number, 'external_id' => 'BNK-IN-' . $date->format('Ym'),
                    'reconciliation_status' => 'MATCHED',
                ]);
                $this->add('reconciliation_links', [
                    'organization_id' => $organizationId, 'bank_transaction_id' => $transaction,
                    'payment_id' => $payment, 'journal_entry_id' => $journal, 'matched_amount' => $total,
                    'matched_by' => $users['accountant'], 'matched_at' => $date->modify('+25 days')->format('Y-m-d H:i:s'),
                ]);
                $this->add('payment_allocations', [
                    'organization_id' => $organizationId, 'payment_id' => $payment,
                    'open_item_id' => $openItem, 'amount' => $total, 'created_by' => $users['accountant'],
                ]);
                $this->add('vat_cash_events', [
                    'organization_id' => $organizationId, 'vat_movement_id' => $vatMovement,
                    'payment_id' => $payment, 'recognition_date' => $date->modify('+24 days')->format('Y-m-d'),
                    'recognized_taxable' => $net, 'recognized_vat_due' => $vat, 'recognized_vat_credit' => 0,
                ]);
            } else {
                if ($offset === 0) {
                    $unmatched = $this->add('bank_transactions', [
                        'organization_id' => $organizationId, 'bank_account_id' => $accounts['bank_id'],
                        'booking_date' => date('Y-m-d'), 'amount' => $total,
                        'description' => 'Bonifico da riconciliare', 'counterparty' => 'Cliente demo',
                        'external_id' => 'BNK-PENDING-' . date('Ym'), 'reconciliation_status' => 'UNMATCHED',
                    ]);
                    $this->add('bank_reconciliation_suggestions', [
                        'organization_id' => $organizationId, 'bank_transaction_id' => $unmatched,
                        'open_item_id' => $openItem, 'confidence_score' => 92.5,
                        'reason_json' => $this->json(['amount' => 'exact', 'reference' => 'similar']), 'status' => 'PROPOSED',
                    ]);
                }
            }

            $purchaseNet = round($net * .46, 2);
            $purchaseVat = round($purchaseNet * .22, 2);
            $purchaseTotal = $purchaseNet + $purchaseVat;
            $purchaseDocument = $this->add('documents', [
                'organization_id' => $organizationId, 'document_type' => 'PURCHASE_INVOICE',
                'number' => 'FA-' . $number, 'fiscal_year' => $year, 'document_date' => $date->modify('+2 days')->format('Y-m-d'),
                'due_date' => $date->modify('+32 days')->format('Y-m-d'), 'counterparty_type' => 'SUPPLIER',
                'counterparty_id' => $supplierId, 'counterparty_name' => 'Fornitore demo',
                'subject' => 'Acquisti e servizi del periodo', 'taxable_total' => $purchaseNet, 'vat_total' => $purchaseVat,
                'total' => $purchaseTotal, 'balance_due' => $offset > 0 ? 0 : $purchaseTotal,
                'status' => $offset > 0 ? 'PAID' : 'RECEIVED', 'created_by' => $users['accountant'],
            ]);
            $this->add('document_lines', [
                'organization_id' => $organizationId, 'document_id' => $purchaseDocument, 'line_number' => 1,
                'product_id' => $products[$offset % count($products)], 'description' => 'Acquisti del periodo',
                'quantity' => 1, 'unit_price' => $purchaseNet, 'taxable_amount' => $purchaseNet,
                'vat_code' => '22', 'vat_rate' => 22, 'vat_amount' => $purchaseVat, 'total_amount' => $purchaseTotal,
            ]);
            $purchaseJournal = $this->add('journal_entries', [
                'organization_id' => $organizationId, 'protocol_number' => 'PA-' . $date->format('Ym') . '-' . ($offset + 1),
                'entry_date' => $date->modify('+2 days')->format('Y-m-d'), 'competence_date' => $date->format('Y-m-d'),
                'entry_type' => 'PURCHASE_INVOICE', 'status' => 'POSTED', 'description' => 'Fattura acquisto ' . $number,
                'document_number' => 'FA-' . $number, 'counterparty' => 'Fornitore demo',
                'total_debit' => $purchaseTotal, 'total_credit' => $purchaseTotal,
                'posted_at' => $date->modify('+2 days')->format('Y-m-d H:i:s'), 'created_by' => $users['accountant'],
            ]);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $purchaseJournal, 'line_number' => 1, 'account_id' => $accounts['costs'], 'debit' => $purchaseNet, 'credit' => 0, 'description' => 'Costo']);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $purchaseJournal, 'line_number' => 2, 'account_id' => $accounts['vat_credit'], 'debit' => $purchaseVat, 'credit' => 0, 'description' => 'IVA a credito']);
            $this->add('journal_entry_lines', ['organization_id' => $organizationId, 'journal_entry_id' => $purchaseJournal, 'line_number' => 3, 'account_id' => $accounts['suppliers'], 'debit' => 0, 'credit' => $purchaseTotal, 'description' => 'Debito fornitore', 'supplier_id' => $supplierId]);
            $this->add('vat_movements', [
                'organization_id' => $organizationId, 'document_id' => $purchaseDocument, 'source_type' => 'DOCUMENT',
                'register_type' => 'PURCHASES', 'movement_date' => $date->modify('+2 days')->format('Y-m-d'),
                'protocol_number' => 'ACQ-' . $date->format('Ym'), 'counterparty_name' => 'Fornitore demo',
                'description' => 'Fattura acquisto', 'vat_code' => '22', 'taxable_amount' => $purchaseNet,
                'vat_amount' => $purchaseVat, 'vat_due_amount' => 0, 'deductible_vat' => $purchaseVat,
                'period_year' => $year, 'period_month' => (int) $date->format('m'), 'operation_type' => 'DOMESTIC',
            ]);

            $settlement = $this->add('vat_settlements', [
                'organization_id' => $organizationId, 'period_type' => 'MONTHLY', 'period_year' => $year,
                'period_number' => (int) $date->format('m'), 'vat_debit' => $vat, 'vat_credit' => $purchaseVat,
                'balance' => $vat - $purchaseVat, 'status' => $offset > 0 ? 'CALCULATED' : 'DRAFT',
            ]);
            $this->add('vat_settlement_details', ['organization_id' => $organizationId, 'settlement_id' => $settlement, 'register_type' => 'SALES', 'vat_code' => '22', 'taxable_amount' => $net, 'vat_amount' => $vat, 'deductible_vat' => 0]);
            $this->add('vat_settlement_details', ['organization_id' => $organizationId, 'settlement_id' => $settlement, 'register_type' => 'PURCHASES', 'vat_code' => '22', 'taxable_amount' => $purchaseNet, 'vat_amount' => $purchaseVat, 'deductible_vat' => $purchaseVat]);

            $lastSalesInvoice = $salesDocument;
            $lastPurchaseInvoice = $purchaseDocument;
            $documentIds[] = $salesDocument;
            $documentIds[] = $purchaseDocument;
        }

        $quote = $this->simpleDocument($organizationId, 'QUOTE', 'PREV-DEMO-001', $customers[0], 'CUSTOMER', 6400, $users['sales'], $products[0]);
        $order = $this->simpleDocument($organizationId, 'SALES_ORDER', 'ORD-DEMO-001', $customers[0], 'CUSTOMER', 6400, $users['sales'], $products[0]);
        $ddt = $this->simpleDocument($organizationId, 'DDT', 'DDT-DEMO-001', $customers[0], 'CUSTOMER', 6400, $users['sales'], $products[0]);
        $this->add('document_links', ['organization_id' => $organizationId, 'source_document_id' => $quote, 'target_document_id' => $order, 'link_type' => 'CONVERSION']);
        $this->add('document_links', ['organization_id' => $organizationId, 'source_document_id' => $order, 'target_document_id' => $ddt, 'link_type' => 'CONVERSION']);
        $this->add('document_links', ['organization_id' => $organizationId, 'source_document_id' => $ddt, 'target_document_id' => $lastSalesInvoice, 'link_type' => 'CONVERSION']);

        return [
            'all' => $documentIds, 'sales_invoice' => $lastSalesInvoice,
            'purchase_invoice' => $lastPurchaseInvoice, 'quote' => $quote, 'order' => $order, 'ddt' => $ddt,
        ];
    }

    private function simpleDocument(int $org, string $type, string $number, int $partyId, string $partyType, float $net, int $user, int $product): int
    {
        $vat = round($net * .22, 2);
        $id = $this->add('documents', [
            'organization_id' => $org, 'document_type' => $type, 'number' => $number,
            'fiscal_year' => (int) date('Y'), 'document_date' => date('Y-m-d', strtotime('-10 days')),
            'due_date' => date('Y-m-d', strtotime('+20 days')), 'counterparty_type' => $partyType,
            'counterparty_id' => $partyId, 'counterparty_name' => 'Cliente demo',
            'subject' => 'Workflow commerciale completo', 'taxable_total' => $net, 'vat_total' => $vat,
            'total' => $net + $vat, 'balance_due' => $type === 'SALES_INVOICE' ? $net + $vat : 0,
            'status' => in_array($type, ['QUOTE','SALES_ORDER'], true) ? 'ACCEPTED' : 'ISSUED', 'created_by' => $user,
        ]);
        $this->add('document_lines', [
            'organization_id' => $org, 'document_id' => $id, 'line_number' => 1, 'product_id' => $product,
            'description' => 'Sistema completo e servizi di avviamento', 'quantity' => 5, 'unit' => 'NR',
            'unit_price' => $net / 5, 'taxable_amount' => $net, 'vat_code' => '22',
            'vat_rate' => 22, 'vat_amount' => $vat, 'total_amount' => $net + $vat,
        ]);
        return $id;
    }

    private function seedInventory(int $org, array $users, array $products, array $warehouses, array $documents): void
    {
        foreach (array_slice($products, 0, 7) as $index => $product) {
            $this->add('inventory_movements', [
                'organization_id' => $org, 'movement_date' => date('Y-m-d', strtotime('-' . (30 - $index) . ' days')),
                'product_id' => $product, 'product_code' => 'DEMO-' . ($index + 1),
                'warehouse_id' => $warehouses[0], 'warehouse_code' => 'MAG-01',
                'movement_type' => $index % 2 ? 'OUT' : 'IN', 'quantity' => $index % 2 ? 2 : 10,
                'unit_cost' => 50 + $index * 40, 'reason' => $index % 2 ? 'Vendita cliente' : 'Carico fornitore',
                'document_type' => 'DDT', 'document_number' => 'DDT-DEMO-001', 'created_by' => $users['warehouse'],
            ]);
        }
        $transfer = $this->add('inventory_transfers', [
            'organization_id' => $org, 'transfer_number' => 'TR-DEMO-001', 'transfer_date' => date('Y-m-d', strtotime('-5 days')),
            'source_warehouse_id' => $warehouses[0], 'destination_warehouse_id' => $warehouses[1],
            'status' => 'IN_TRANSIT', 'notes' => 'Trasferimento dimostrativo', 'created_by' => $users['warehouse'],
        ]);
        $this->add('inventory_transfer_lines', ['organization_id' => $org, 'transfer_id' => $transfer, 'product_id' => $products[0], 'requested_quantity' => 4, 'shipped_quantity' => 4, 'received_quantity' => 0]);
        $pick = $this->add('inventory_pick_lists', [
            'organization_id' => $org, 'pick_number' => 'PICK-DEMO-001', 'warehouse_id' => $warehouses[0],
            'document_id' => $documents['ddt'], 'status' => 'PICKING', 'assigned_to' => $users['warehouse'],
            'created_by' => $users['warehouse'],
        ]);
        $line = $this->value('SELECT id FROM document_lines WHERE document_id = ? ORDER BY id LIMIT 1', [$documents['ddt']]);
        $this->add('inventory_pick_items', ['organization_id' => $org, 'pick_list_id' => $pick, 'document_line_id' => $line, 'product_id' => $products[0], 'required_quantity' => 5, 'picked_quantity' => 3, 'status' => 'PARTIAL']);
        $this->add('inventory_barcode_events', ['organization_id' => $org, 'pick_list_id' => $pick, 'warehouse_id' => $warehouses[0], 'product_id' => $products[0], 'barcode' => '805000000001', 'quantity' => 1, 'result' => 'ACCEPTED', 'scanned_by' => $users['warehouse'], 'scanned_at' => date('Y-m-d H:i:s')]);
    }

    private function seedCrmAndProjects(int $org, array $users, array $customers, array $documents, array $products): void
    {
        $stages = [];
        foreach ([['Nuovo',10],['Qualificato',30],['Proposta',60],['Negoziazione',80],['Chiuso',100]] as [$name,$probability]) {
            $stages[] = $this->add('pipeline_stages', ['organization_id' => $org, 'name' => $name, 'position' => count($stages) + 1, 'probability' => $probability, 'active' => 1]);
        }
        $lead = $this->add('leads', [
            'organization_id' => $org, 'pipeline_stage_id' => $stages[2], 'company_name' => 'Nuova Opportunità Demo S.r.l.',
            'contact_name' => 'Elena Prospect', 'email' => 'elena@prospect-demo.local', 'phone' => '+39 02 9000 0001',
            'source' => 'Sito web', 'status' => 'PROPOSAL', 'estimated_value' => 18500, 'probability' => 65,
            'next_action_at' => date('Y-m-d H:i:s', strtotime('+3 days')), 'owner_name' => 'Marco Commerciale',
            'created_by' => $users['sales'],
        ]);
        $this->add('activities', ['organization_id' => $org, 'subject' => 'Demo prodotto al prospect', 'activity_type' => 'MEETING', 'status' => 'OPEN', 'starts_at' => date('Y-m-d H:i:s', strtotime('+3 days 10:00')), 'due_at' => date('Y-m-d H:i:s', strtotime('+3 days 11:30')), 'assigned_to' => 'Marco Commerciale', 'related_type' => 'LEAD', 'related_id' => $lead, 'created_by' => $users['sales']]);
        $this->add('tasks', ['organization_id' => $org, 'title' => 'Preparare offerta personalizzata', 'priority' => 'HIGH', 'status' => 'IN_PROGRESS', 'due_at' => date('Y-m-d H:i:s', strtotime('+2 days')), 'assigned_user_id' => $users['sales'], 'related_type' => 'LEAD', 'related_id' => $lead, 'created_by' => $users['sales']]);
        $tag = $this->add('tags', ['organization_id' => $org, 'name' => 'Priorità commerciale', 'color' => '#2563eb']);
        $this->add('taggables', ['organization_id' => $org, 'tag_id' => $tag, 'entity_type' => 'LEAD', 'entity_id' => $lead]);

        foreach ([['PRJ-001','Migrazione infrastruttura',0,68],['PRJ-002','Portale clienti',1,35],['PRJ-003','Consulenza continuativa',2,92]] as $index => [$code,$name,$customerIndex,$progress]) {
            $project = $this->add('projects', [
                'organization_id' => $org, 'code' => $code, 'name' => $name,
                'customer_id' => $customers[$customerIndex], 'customer_name' => 'Cliente demo ' . ($customerIndex + 1),
                'status' => $progress >= 90 ? 'COMPLETED' : 'ACTIVE',
                'start_date' => date('Y-m-d', strtotime('-' . (120 - $index * 20) . ' days')),
                'end_date' => date('Y-m-d', strtotime('+' . (60 + $index * 25) . ' days')),
                'budget' => 24000 + $index * 9000, 'progress_percent' => $progress,
                'description' => 'Commessa dimostrativa con tempi, costi, ricavi e milestone.',
                'source_document_id' => $documents['quote'], 'created_by' => $users['sales'],
            ]);
            $this->add('project_lines', ['organization_id' => $org, 'project_id' => $project, 'description' => 'Analisi e progettazione', 'quantity' => 40, 'unit_price' => 95, 'cost_amount' => 1800, 'revenue_amount' => 3800, 'status' => 'COMPLETED']);
            $this->add('project_lines', ['organization_id' => $org, 'project_id' => $project, 'description' => 'Implementazione', 'quantity' => 120, 'unit_price' => 105, 'cost_amount' => 5700, 'revenue_amount' => 12600, 'status' => 'IN_PROGRESS']);
            for ($week = 0; $week < 8; $week++) {
                $this->add('project_time_entries', ['organization_id' => $org, 'project_id' => $project, 'user_id' => $users[$week % 2 ? 'sales' : 'owner'], 'work_date' => date('Y-m-d', strtotime("-{$week} weeks")), 'description' => 'Attività di progetto', 'hours' => 5 + ($week % 4), 'hourly_cost' => 38, 'hourly_rate' => 95, 'billable' => 1, 'billing_status' => 'OPEN', 'approved_by' => $users['owner'], 'approved_at' => date('Y-m-d H:i:s', strtotime("-{$week} weeks +1 day"))]);
            }
            $this->add('project_expenses', ['organization_id' => $org, 'project_id' => $project, 'expense_date' => date('Y-m-d', strtotime('-20 days')), 'description' => 'Trasferta presso cliente', 'cost_amount' => 280, 'billable_amount' => 350, 'billable' => 1, 'billing_status' => 'OPEN', 'approved_by' => $users['owner']]);
            $this->add('project_milestones', ['organization_id' => $org, 'project_id' => $project, 'name' => 'Avvio operativo', 'due_date' => date('Y-m-d', strtotime('-60 days')), 'amount' => 6000, 'status' => 'INVOICED']);
            $this->add('project_milestones', ['organization_id' => $org, 'project_id' => $project, 'name' => 'Collaudo finale', 'due_date' => date('Y-m-d', strtotime('+30 days')), 'amount' => 9000, 'status' => 'OPEN']);
        }
    }

    private function seedPeople(int $org, int $months, array $users): void
    {
        $employees = array_intersect_key($users, array_flip(['owner','accountant','sales','warehouse','hr']));
        foreach ($employees as $index => $userId) {
            $this->add('payroll_employee_configs', [
                'organization_id' => $org, 'user_id' => $userId, 'employee_code' => 'DIP-' . sprintf('%03d', $index + 1),
                'standard_weekly_hours' => 40, 'hourly_rate' => 18 + $index * 2,
                'overtime_multiplier' => 1.3, 'inps_rate' => 9.19, 'inail_rate' => .8,
                'employer_contribution_rate' => 30, 'tax_rate' => 23, 'fixed_monthly_amount' => 2100 + $index * 180,
                'valid_from' => date('Y-01-01'), 'active' => 1, 'created_by' => $users['hr'],
            ]);
            foreach (['HOLIDAY' => [32, 144, 48], 'PERMIT' => [8, 48, 16], 'ROL' => [0, 32, 8]] as $type => [$opening,$accrued,$used]) {
                $this->add('leave_balances', ['organization_id' => $org, 'user_id' => $userId, 'balance_year' => (int) date('Y'), 'leave_type' => $type, 'opening_hours' => $opening, 'accrued_hours' => $accrued, 'used_hours' => $used, 'adjusted_hours' => 0, 'created_by' => $users['hr']]);
            }
        }
        for ($day = 1; $day <= 90; $day++) {
            $date = new DateTimeImmutable("-{$day} days");
            if ((int) $date->format('N') > 5) continue;
            foreach ($employees as $index => $userId) {
                $this->add('time_records', [
                    'organization_id' => $org, 'user_id' => $userId, 'work_date' => $date->format('Y-m-d'),
                    'employee_name' => ['Federico Demo','Anna Contabile','Marco Commerciale','Luca Magazzino','Giulia Risorse Umane'][$index],
                    'record_type' => 'WORK', 'check_in' => '09:00:00', 'check_out' => $index % 3 === 0 ? '18:30:00' : '18:00:00',
                    'hours' => 8, 'overtime_hours' => $index % 3 === 0 ? .5 : 0, 'approved' => 1,
                    'created_by' => $users['hr'],
                ]);
            }
        }
        $approval = $this->add('approval_requests', ['organization_id' => $org, 'request_type' => 'LEAVE', 'requester_id' => $users['sales'], 'approver_id' => $users['hr'], 'entity_type' => 'LEAVE_REQUEST', 'status' => 'SUBMITTED', 'requested_at' => date('Y-m-d H:i:s', strtotime('-2 days')), 'reason' => 'Ferie estive']);
        $leave = $this->add('leave_requests', ['organization_id' => $org, 'user_id' => $users['sales'], 'leave_type' => 'HOLIDAY', 'starts_at' => date('Y-m-d 09:00:00', strtotime('+20 days')), 'ends_at' => date('Y-m-d 18:00:00', strtotime('+24 days')), 'requested_hours' => 40, 'reason' => 'Ferie programmate', 'status' => 'SUBMITTED', 'approval_request_id' => $approval, 'submitted_at' => date('Y-m-d H:i:s', strtotime('-2 days'))]);
        $this->add('approval_history', ['organization_id' => $org, 'approval_request_id' => $approval, 'action' => 'SUBMIT', 'actor_id' => $users['sales'], 'notes' => 'Richiesta inviata']);

        $payrollMonths = min($months, 12);
        $first = new DateTimeImmutable('first day of this month');
        for ($offset = $payrollMonths - 1; $offset >= 0; $offset--) {
            $period = $first->modify("-{$offset} months");
            $grossTotal = count($employees) * 2450;
            $run = $this->add('payroll_runs', [
                'organization_id' => $org, 'period_label' => ucfirst($period->format('F Y')),
                'period_start' => $period->format('Y-m-01'), 'period_end' => $period->format('Y-m-t'),
                'calculation_mode' => 'IMPORTED_PAYSLIPS', 'employees_count' => count($employees),
                'gross_total' => $grossTotal, 'net_total' => round($grossTotal * .69, 2),
                'contributions_total' => round($grossTotal * .0919, 2), 'tax_total' => round($grossTotal * .218, 2),
                'status' => $offset === 0 ? 'CALCULATED' : 'PAID', 'created_by' => $users['hr'],
                'professional_validation_reference' => $offset === 0 ? null : 'CONSULENTE-' . $period->format('Ym'),
                'validated_at' => $offset === 0 ? null : $period->modify('+1 month +5 days')->format('Y-m-d H:i:s'),
            ]);
            foreach ($employees as $index => $userId) {
                $gross = 2200 + $index * 180;
                $contributions = round($gross * .0919, 2);
                $tax = round(($gross - $contributions) * .23, 2);
                $net = $gross - $contributions - $tax;
                $detail = $this->add('payroll_details', [
                    'organization_id' => $org, 'payroll_run_id' => $run, 'user_id' => $userId,
                    'employee_name' => ['Federico Demo','Anna Contabile','Marco Commerciale','Luca Magazzino','Giulia Risorse Umane'][$index],
                    'regular_hours' => 168, 'overtime_hours' => 4 + $index,
                    'gross_amount' => $gross, 'contributions_amount' => $contributions,
                    'employer_contributions_amount' => round($gross * .30, 2), 'tax_amount' => $tax,
                    'net_amount' => $net, 'employer_cost' => round($gross * 1.30, 2),
                    'source_reference' => 'CED-' . $period->format('Ym') . '-' . ($index + 1),
                    'calculation_json' => $this->json(['source' => 'demo-payslip']),
                ]);
                $this->add('payroll_components', ['organization_id' => $org, 'payroll_detail_id' => $detail, 'component_code' => 'BASE', 'description' => 'Retribuzione base', 'component_type' => 'EARNING', 'quantity' => 1, 'rate' => $gross, 'amount' => $gross, 'source' => 'IMPORTED']);
                $this->add('payroll_components', ['organization_id' => $org, 'payroll_detail_id' => $detail, 'component_code' => 'INPS', 'description' => 'Contributi dipendente', 'component_type' => 'EMPLOYEE_CONTRIBUTION', 'amount' => -$contributions, 'source' => 'IMPORTED']);
                $this->add('payroll_import_rows', ['organization_id' => $org, 'payroll_run_id' => $run, 'source_file' => 'cedolini-' . $period->format('Ym') . '.xlsx', 'source_row' => $index + 2, 'employee_code' => 'DIP-' . sprintf('%03d', $index + 1), 'normalized_json' => $this->json(['gross' => $gross, 'net' => $net]), 'status' => 'IMPORTED']);
            }
        }
    }

    private function seedIntegrations(int $org, array $users, array $customers, array $products, array $documents): void
    {
        $channel = $this->add('ecommerce_channels', ['organization_id' => $org, 'name' => 'Shop Demo WooCommerce', 'platform' => 'WOOCOMMERCE', 'base_url' => 'https://shop-demo.example', 'secret_reference' => 'ENV:DEMO_WOO_KEY', 'webhook_secret_reference' => 'ENV:DEMO_WOO_WEBHOOK', 'settings_json' => $this->json(['mode' => 'demo']), 'status' => 'CONNECTED', 'active' => 1, 'last_sync_at' => date('Y-m-d H:i:s', strtotime('-2 hours')), 'created_by' => $users['owner']]);
        for ($i = 1; $i <= 8; $i++) {
            $order = $this->add('ecommerce_orders', ['organization_id' => $org, 'ecommerce_channel_id' => $channel, 'order_date' => date('Y-m-d', strtotime("-{$i} days")), 'platform' => 'WOOCOMMERCE', 'external_order_id' => 'WEB-' . sprintf('%05d', 1000 + $i), 'customer_email' => 'webcliente' . $i . '@demo.local', 'total' => 120 + $i * 45, 'currency' => 'EUR', 'status' => $i < 6 ? 'COMPLETED' : 'PROCESSING', 'import_status' => $i < 7 ? 'IMPORTED' : 'PENDING', 'luna_document_id' => $i < 7 ? $documents['sales_invoice'] : null, 'raw_data_json' => $this->json(['demo' => true])]);
            $this->add('ecommerce_order_lines', ['organization_id' => $org, 'ecommerce_order_id' => $order, 'external_line_id' => 'ROW-' . $i, 'product_id' => $products[$i % count($products)], 'sku' => 'SKU-DEMO-' . $i, 'description' => 'Prodotto ordine web', 'quantity' => 1, 'unit_price' => 120 + $i * 45, 'tax_amount' => 22, 'total_amount' => 120 + $i * 45, 'raw_data_json' => $this->json(['demo' => true])]);
        }
        foreach (array_slice($products, 0, 5) as $i => $product) {
            $this->add('ecommerce_products', ['organization_id' => $org, 'ecommerce_channel_id' => $channel, 'platform' => 'WOOCOMMERCE', 'external_product_id' => 'EXT-P-' . ($i + 1), 'product_id' => $product, 'sku' => 'SKU-DEMO-' . ($i + 1), 'name' => 'Prodotto web demo ' . ($i + 1), 'price' => 99 + $i * 40, 'quantity' => 10 + $i, 'sync_status' => 'SYNCED', 'raw_data_json' => $this->json(['demo' => true])]);
        }
        $this->add('ecommerce_sync_logs', ['organization_id' => $org, 'ecommerce_channel_id' => $channel, 'sync_type' => 'PULL_ORDERS', 'status' => 'SUCCESS', 'started_at' => date('Y-m-d H:i:s', strtotime('-2 hours')), 'ended_at' => date('Y-m-d H:i:s', strtotime('-119 minutes')), 'processed_count' => 8, 'error_count' => 0, 'message' => 'Sincronizzazione dimostrativa completata']);
        $this->add('ecommerce_sync_queue', ['organization_id' => $org, 'ecommerce_channel_id' => $channel, 'operation' => 'PUSH_STOCK', 'entity_type' => 'PRODUCT', 'entity_id' => $products[0], 'payload_json' => $this->json(['quantity' => 12]), 'status' => 'QUEUED', 'available_at' => date('Y-m-d H:i:s')]);
        $this->add('webhook_events', ['organization_id' => $org, 'channel_id' => $channel, 'provider' => 'WOOCOMMERCE', 'event_type' => 'order.updated', 'external_id' => 'WH-DEMO-001', 'signature_valid' => 1, 'payload_json' => $this->json(['order' => 'WEB-01008']), 'status' => 'PROCESSED', 'received_at' => date('Y-m-d H:i:s', strtotime('-1 hour')), 'processed_at' => date('Y-m-d H:i:s', strtotime('-59 minutes'))]);

        $rental = $this->add('rental_contracts', ['organization_id' => $org, 'contract_number' => 'NOL-DEMO-001', 'customer_id' => $customers[0], 'customer_name' => 'Alfa Industrie S.p.A.', 'vehicle_plate' => 'DE123MO', 'start_date' => date('Y-m-d', strtotime('-14 months')), 'end_date' => date('Y-m-d', strtotime('+22 months')), 'monthly_fee' => 690, 'annual_km' => 30000, 'invoice_day' => 5, 'next_invoice_date' => date('Y-m-05', strtotime('+1 month')), 'included_km' => 90000, 'current_km' => 28450, 'renewal_notice_days' => 60, 'status' => 'ACTIVE', 'notes' => 'Contratto demo in corso', 'created_by' => $users['sales']]);
        $ticket = $this->add('rental_tickets', ['organization_id' => $org, 'ticket_number' => 'TCK-DEMO-001', 'rental_contract_id' => $rental, 'opened_at' => date('Y-m-d H:i:s', strtotime('-1 day')), 'customer_name' => 'Alfa Industrie S.p.A.', 'subject' => 'Richiesta cambio pneumatici', 'priority' => 'MEDIUM', 'status' => 'IN_PROGRESS', 'assigned_to' => $users['warehouse'], 'sla_due_at' => date('Y-m-d H:i:s', strtotime('+1 day')), 'response_due_at' => date('Y-m-d H:i:s', strtotime('+4 hours')), 'description' => 'Pianificare appuntamento con officina convenzionata.', 'created_by' => $users['sales']]);
        $this->add('rental_deadlines', ['organization_id' => $org, 'rental_contract_id' => $rental, 'deadline_type' => 'SERVICE', 'due_date' => date('Y-m-d', strtotime('+20 days')), 'description' => 'Tagliando programmato', 'status' => 'OPEN']);
        $this->add('rental_meter_readings', ['organization_id' => $org, 'rental_contract_id' => $rental, 'reading_date' => date('Y-m-d'), 'kilometers' => 28450, 'source' => 'CUSTOMER', 'notes' => 'Lettura portale cliente', 'created_by' => $users['sales']]);
        $this->add('rental_ticket_events', ['organization_id' => $org, 'rental_ticket_id' => $ticket, 'event_type' => 'ASSIGNED', 'old_value' => 'OPEN', 'new_value' => 'IN_PROGRESS', 'notes' => 'Assegnato al magazzino', 'actor_id' => $users['sales']]);
        $this->add('rental_invoice_links', ['organization_id' => $org, 'rental_contract_id' => $rental, 'document_id' => $documents['sales_invoice'], 'period_start' => date('Y-m-01', strtotime('-1 month')), 'period_end' => date('Y-m-t', strtotime('-1 month')), 'amount' => 690]);

        $calendar = $this->add('calendar_accounts', ['organization_id' => $org, 'user_id' => $users['sales'], 'provider' => 'CALDAV', 'account_email' => 'vendite.demo@luna.local', 'endpoint_url' => 'https://calendar-demo.example/caldav', 'auth_type' => 'APP_PASSWORD', 'secret_reference' => 'ENV:DEMO_CALENDAR_PASSWORD', 'external_calendar_id' => 'sales-demo', 'active' => 1, 'last_sync_at' => date('Y-m-d H:i:s', strtotime('-3 hours')), 'created_by' => $users['owner']]);
        for ($i = 0; $i < 6; $i++) {
            $this->add('calendar_events', ['organization_id' => $org, 'calendar_account_id' => $calendar, 'title' => ['Riunione cliente','Revisione commessa','Scadenza offerta','Demo commerciale','Inventario','Riunione mensile'][$i], 'starts_at' => date('Y-m-d H:i:s', strtotime('+' . ($i + 1) . ' days 10:00')), 'ends_at' => date('Y-m-d H:i:s', strtotime('+' . ($i + 1) . ' days 11:00')), 'all_day' => 0, 'location' => $i % 2 ? 'Microsoft Teams' : 'Sede cliente', 'provider' => 'LOCAL', 'provider_event_id' => 'EV-DEMO-' . $i, 'sync_status' => 'SYNCED', 'description' => 'Evento calendario dimostrativo', 'created_by' => $users['sales']]);
        }
        $this->add('calendar_sync_logs', ['organization_id' => $org, 'calendar_account_id' => $calendar, 'direction' => 'BIDIRECTIONAL', 'status' => 'SUCCESS', 'processed_count' => 6, 'error_count' => 0, 'started_at' => date('Y-m-d H:i:s', strtotime('-3 hours')), 'ended_at' => date('Y-m-d H:i:s', strtotime('-179 minutes')), 'message' => 'Calendario demo sincronizzato']);

        $this->add('integration_accounts', ['organization_id' => $org, 'integration_key' => 'DEMO_CONNECTOR', 'display_name' => 'Connettore dimostrativo', 'settings_json' => $this->json(['endpoint' => 'disabled']), 'status' => 'CONNECTED', 'last_check_at' => date('Y-m-d H:i:s')]);
        $endpoint = $this->add('api_endpoint_configs', ['organization_id' => $org, 'service_key' => 'EINVOICE_DEMO', 'display_name' => 'Fatturazione elettronica demo', 'base_url' => 'https://api-demo.example/einvoice', 'environment' => 'TEST', 'auth_type' => 'BEARER', 'secret_reference' => 'ENV:DEMO_EINVOICE_TOKEN', 'verify_tls' => 1, 'enabled' => 0]);
        $transmission = $this->add('sdi_transmissions', ['organization_id' => $org, 'document_id' => $documents['sales_invoice'], 'provider' => 'DEMO', 'filename' => 'IT09999990969_DEMO001.xml', 'checksum_sha256' => hash('sha256', 'demo-invoice'), 'provider_reference' => 'SDI-DEMO-001', 'status' => 'DELIVERED', 'sent_at' => date('Y-m-d H:i:s', strtotime('-10 days')), 'completed_at' => date('Y-m-d H:i:s', strtotime('-9 days'))]);
        $this->add('sdi_notifications', ['organization_id' => $org, 'sdi_transmission_id' => $transmission, 'notification_type' => 'RC', 'filename' => 'RC_DEMO001.xml', 'checksum_sha256' => hash('sha256', 'demo-receipt'), 'payload_xml' => '<RicevutaConsegna demo="true"/>', 'received_at' => date('Y-m-d H:i:s', strtotime('-9 days')), 'processed_at' => date('Y-m-d H:i:s', strtotime('-9 days'))]);

        $this->add('communication_settings', ['organization_id' => $org, 'transport' => 'DISABLED', 'from_email' => 'noreply@demo.luna.local', 'from_name' => 'Luna Demo', 'tracking_base_url' => 'https://demo.luna.local', 'tracking_enabled' => 1, 'max_attempts' => 5]);
        $message = $this->add('outbound_emails', ['organization_id' => $org, 'message_uuid' => $this->uuid(), 'document_id' => $documents['sales_invoice'], 'recipient' => 'amministrazione@cliente-demo.local', 'subject' => 'Fattura demo disponibile', 'html_body' => '<p>Gentile cliente, la fattura demo è disponibile.</p>', 'text_body' => 'La fattura demo è disponibile.', 'attachment_json' => $this->json(['document_id' => $documents['sales_invoice']]), 'tracking_token' => hash('sha256', 'demo-track-' . $org), 'status' => 'SENT', 'attempts' => 1, 'sent_at' => date('Y-m-d H:i:s', strtotime('-5 days')), 'message_id' => '<demo-' . $org . '@luna.local>', 'created_by' => $users['accountant']]);
        $this->add('email_events', ['organization_id' => $org, 'outbound_email_id' => $message, 'event_type' => 'DELIVERED', 'occurred_at' => date('Y-m-d H:i:s', strtotime('-5 days +2 minutes'))]);
        $this->add('email_events', ['organization_id' => $org, 'outbound_email_id' => $message, 'event_type' => 'OPEN', 'occurred_at' => date('Y-m-d H:i:s', strtotime('-4 days'))]);
        $this->add('email_tracking', ['organization_id' => $org, 'document_id' => $documents['sales_invoice'], 'recipient' => 'amministrazione@cliente-demo.local', 'subject' => 'Fattura demo disponibile', 'tracking_token' => hash('sha256', 'legacy-demo-track-' . $org), 'sent_at' => date('Y-m-d H:i:s', strtotime('-5 days')), 'first_opened_at' => date('Y-m-d H:i:s', strtotime('-4 days')), 'last_opened_at' => date('Y-m-d H:i:s', strtotime('-3 days')), 'opens_count' => 3]);
        $this->add('background_jobs', ['organization_id' => $org, 'queue' => 'demo', 'job_type' => 'DEMO_HEALTH_CHECK', 'payload_json' => $this->json(['organization_id' => $org]), 'status' => 'COMPLETED', 'attempts' => 1, 'available_at' => date('Y-m-d H:i:s', strtotime('-1 day')), 'started_at' => date('Y-m-d H:i:s', strtotime('-1 day')), 'completed_at' => date('Y-m-d H:i:s', strtotime('-1 day +1 minute'))]);
    }

    private function seedProfessionalWorkspace(int $org, array $users, array $accounts): void
    {
        $year = (int) date('Y');
        $assetCategory = $this->add('fixed_asset_categories', ['organization_id' => $org, 'code' => 'HW', 'name' => 'Hardware e macchine elettroniche', 'civil_rate' => 20, 'tax_rate' => 20, 'active' => 1]);
        $asset = $this->add('fixed_assets', ['organization_id' => $org, 'asset_code' => 'CESP-DEMO-001', 'description' => 'Server e infrastruttura aziendale', 'category' => 'Hardware', 'category_id' => $assetCategory, 'purchase_date' => date('Y-m-d', strtotime('-20 months')), 'purchase_cost' => 18000, 'depreciation_rate' => 20, 'accumulated_depreciation' => 7200, 'net_book_value' => 10800, 'status' => 'ACTIVE', 'created_by' => $users['accountant']]);
        $this->add('depreciation_entries', ['organization_id' => $org, 'fixed_asset_id' => $asset, 'fiscal_year' => $year - 1, 'amount' => 3600, 'status' => 'POSTED']);
        foreach ([['VAT','Versamento IVA mensile',date('Y-m-16', strtotime('+1 month')),1280],['F24','F24 contributi e ritenute',date('Y-m-16', strtotime('+1 month')),3650],['LIPE','Comunicazione LIPE trimestrale',date('Y-m-t', strtotime('+2 months')),0],['CU','Certificazione Unica',($year + 1) . '-03-16',0]] as [$type,$description,$due,$amount]) {
            $this->add('tax_deadlines', ['organization_id' => $org, 'due_date' => $due, 'deadline_type' => $type, 'description' => $description, 'reference_period' => date('m/Y'), 'amount' => $amount, 'status' => 'OPEN']);
        }
        $this->add('withholding_records', ['organization_id' => $org, 'record_date' => date('Y-m-d', strtotime('-20 days')), 'due_date' => date('Y-m-16', strtotime('+1 month')), 'withholding_type' => 'IRPEF', 'gross_amount' => 1500, 'taxable_percent' => 100, 'rate_percent' => 20, 'withholding_amount' => 300, 'net_amount' => 1200, 'status' => 'WITHHELD', 'created_by' => $users['accountant']]);
        $this->add('vat_adjustments', ['organization_id' => $org, 'fiscal_year' => $year, 'adjustment_type' => 'PRO_RATA', 'description' => 'Rettifica IVA annuale dimostrativa', 'vat_debit_delta' => 125, 'vat_credit_delta' => 0, 'status' => 'DRAFT']);
        $this->add('lipe_communications', ['organization_id' => $org, 'fiscal_year' => $year, 'quarter_number' => 1, 'schema_version' => '1.0-DEMO', 'payload_json' => $this->json(['vp2' => 18500, 'vp3' => 6400]), 'status' => 'REVIEWED']);
        $this->add('vat_annual_summaries', ['organization_id' => $org, 'fiscal_year' => $year - 1, 'schema_version' => 'DEMO-1', 'detail_json' => $this->json(['sales' => 285000, 'purchases' => 132000]), 'status' => 'REVIEWED']);
        $this->add('accounting_period_locks', ['organization_id' => $org, 'scope' => 'VAT', 'starts_on' => ($year - 1) . '-01-01', 'ends_on' => ($year - 1) . '-12-31', 'reason' => 'Periodo IVA chiuso e validato', 'locked_by' => $users['accountant']]);
        $this->add('accounting_closing_runs', ['organization_id' => $org, 'fiscal_year' => $year - 1, 'status' => 'POSTED', 'snapshot_json' => $this->json(['assets' => 168000, 'liabilities' => 82000, 'result' => 24000]), 'created_by' => $users['accountant']]);
        $this->add('accounting_adjustment_schedules', ['organization_id' => $org, 'adjustment_type' => 'PREPAID', 'description' => 'Risconto assicurazione annuale', 'source_account_id' => $accounts['costs'], 'counterpart_account_id' => $accounts['assets'], 'amount' => 1200, 'competence_from' => date('Y-07-01'), 'competence_to' => date('Y-06-30', strtotime('+1 year')), 'posting_date' => date('Y-12-31'), 'status' => 'DRAFT']);

        $import = $this->add('import_batches', ['organization_id' => $org, 'uuid' => $this->uuid(), 'source_system' => 'DATEV_KOINOS', 'import_type' => 'CUSTOMERS', 'status' => 'COMPLETED', 'original_filename' => 'clienti-koinos-demo.csv', 'checksum_sha256' => hash('sha256', 'demo-import'), 'file_size' => 2048, 'total_rows' => 25, 'valid_rows' => 25, 'imported_rows' => 25, 'created_by' => $users['owner'], 'completed_at' => date('Y-m-d H:i:s', strtotime('-60 days'))]);
        $file = $this->add('import_files', ['organization_id' => $org, 'batch_id' => $import, 'filename' => 'clienti-koinos-demo.csv', 'media_type' => 'text/csv', 'checksum_sha256' => hash('sha256', 'demo-import-file'), 'file_size' => 2048]);
        $this->add('import_rows', ['organization_id' => $org, 'batch_id' => $import, 'source_file_id' => $file, 'source_row_number' => 2, 'raw_data_json' => $this->json(['RagioneSociale' => 'Cliente storico demo']), 'normalized_data_json' => $this->json(['business_name' => 'Cliente storico demo']), 'status' => 'IMPORTED']);
        $this->add('import_records', ['organization_id' => $org, 'batch_id' => $import, 'entity_type' => 'customers', 'entity_id' => 1, 'operation' => 'CREATE']);
        $this->add('import_mappings', ['organization_id' => $org, 'source_system' => 'DATEV_KOINOS', 'import_type' => 'CUSTOMERS', 'name' => 'Mappatura clienti demo', 'mapping_json' => $this->json(['RagioneSociale' => 'business_name']), 'active' => 1]);

        $this->add('report_presets', ['organization_id' => $org, 'report_key' => 'MANAGEMENT_OVERVIEW', 'name' => 'Cruscotto direzionale mensile', 'filters_json' => $this->json(['period' => 'YTD']), 'columns_json' => $this->json(['revenue','margin','cash']), 'created_by' => $users['owner']]);
        $this->add('report_exports', ['organization_id' => $org, 'report_key' => 'MANAGEMENT_OVERVIEW', 'period_from' => date('Y-01-01'), 'period_to' => date('Y-m-d'), 'parameters_json' => $this->json(['demo' => true]), 'filename' => 'report-direzionale-demo.xlsx', 'checksum_sha256' => hash('sha256', 'demo-report'), 'row_count' => 24, 'generated_by' => $users['owner'], 'generated_at' => date('Y-m-d H:i:s', strtotime('-2 days'))]);
        $this->add('official_print_runs', ['organization_id' => $org, 'print_type' => 'JOURNAL', 'sequence_number' => 1, 'title' => 'Libro giornale demo', 'period_start' => date('Y-01-01'), 'period_end' => date('Y-m-d'), 'status' => 'VALIDATED', 'row_count' => 120, 'professional_validation_reference' => 'VALID-DEMO-001', 'generated_by' => $users['accountant'], 'validated_by' => $users['owner'], 'validated_at' => date('Y-m-d H:i:s', strtotime('-1 day'))]);
        $this->add('compliance_filing_runs', ['organization_id' => $org, 'filing_type' => 'LIPE', 'period_year' => $year, 'period_code' => 'Q1', 'status' => 'READY', 'schema_version' => 'DEMO-1', 'data_json' => $this->json(['quarter' => 1]), 'anomaly_count' => 0, 'professional_validation_reference' => 'LIPE-DEMO-Q1', 'created_by' => $users['accountant'], 'validated_by' => $users['owner'], 'validated_at' => date('Y-m-d H:i:s')]);
        $statement = $this->add('bank_statement_imports', ['organization_id' => $org, 'bank_account_id' => $accounts['bank_id'], 'source_format' => 'CSV', 'original_filename' => 'estratto-conto-demo.csv', 'stored_path' => 'storage/imports/estratto-conto-demo.csv', 'checksum_sha256' => hash('sha256', 'demo-statement'), 'status' => 'IMPORTED', 'rows_count' => 48, 'imported_count' => 48, 'duplicate_count' => 0, 'error_count' => 0, 'imported_by' => $users['accountant'], 'imported_at' => date('Y-m-d H:i:s', strtotime('-3 days'))]);

        foreach ($users as $userId) {
            $this->add('user_preferences', ['organization_id' => $org, 'user_id' => $userId, 'preference_key' => 'dashboard.compact', 'preference_json' => $this->json(['enabled' => false])]);
        }
        $this->add('saved_views', ['organization_id' => $org, 'user_id' => $users['accountant'], 'module_key' => 'customers', 'name' => 'Clienti attivi Milano', 'query_json' => $this->json(['filters' => ['city' => 'Milano', 'active' => 1]]), 'is_default' => 1]);
        foreach ([['ACCOUNTING','Liquidazione IVA da verificare','La liquidazione del mese corrente è ancora in bozza.'],['SALES','Preventivo in scadenza','Il preventivo demo richiede un follow-up.'],['INVENTORY','Scorta minima','Due articoli sono vicini alla soglia minima.'],['HR','Ferie da approvare','È presente una richiesta ferie in attesa.']] as [$category,$title,$message]) {
            $this->add('workspace_notifications', ['organization_id' => $org, 'category' => $category, 'title' => $title, 'message' => $message, 'severity' => 'INFO', 'action_url' => '/dashboard']);
        }
        foreach (['company','users','modules','accounting','bank','imports'] as $step) {
            $this->add('onboarding_progress', ['organization_id' => $org, 'step_key' => $step, 'completed_by' => $users['owner'], 'completed_at' => date('Y-m-d H:i:s', strtotime('-30 days'))]);
        }
        $this->add('bulk_operations', ['organization_id' => $org, 'user_id' => $users['owner'], 'module_key' => 'customers', 'operation' => 'EXPORT', 'selection_json' => $this->json(['all_filtered' => true]), 'status' => 'COMPLETED', 'processed_count' => 8, 'failed_count' => 0, 'result_json' => $this->json(['filename' => 'clienti-demo.xlsx']), 'completed_at' => date('Y-m-d H:i:s', strtotime('-1 day'))]);
    }

    private function findDemoOrganization(): int
    {
        $statement = $this->db->prepare('SELECT id FROM organizations WHERE tax_code = ? ORDER BY id LIMIT 1');
        $statement->execute([self::DEMO_TAX_CODE]);
        return (int) ($statement->fetchColumn() ?: 0);
    }

    /** @param list<string> $tables */
    private function requireTables(array $tables): void
    {
        $missing = array_values(array_filter($tables, fn (string $table): bool => !$this->tableExists($table)));
        if ($missing !== []) {
            throw new RuntimeException('Database non aggiornato. Tabelle mancanti: ' . implode(', ', $missing) . '. Esegui prima bin/luna migrate.');
        }
    }

    /** @param array<string,mixed> $data */
    private function add(string $table, array $data): int
    {
        if (!$this->tableExists($table)) {
            throw new RuntimeException("Tabella {$table} mancante: esegui prima bin/luna migrate.");
        }
        $available = $this->tableColumns($table);
        $data = array_filter($data, static fn (mixed $_, string $column): bool => isset($available[$column]), ARRAY_FILTER_USE_BOTH);
        if ($data === []) {
            return 0;
        }
        $columns = array_keys($data);
        $quoted = implode(', ', array_map(static fn (string $column): string => '`' . $column . '`', $columns));
        $placeholders = implode(', ', array_fill(0, count($columns), '?'));
        $statement = $this->db->prepare("INSERT INTO `{$table}` ({$quoted}) VALUES ({$placeholders})");
        $statement->execute(array_values($data));
        $this->counts[$table] = ($this->counts[$table] ?? 0) + 1;
        $id = (int) $this->db->lastInsertId();
        return $id > 0 ? $id : (int) ($data['organization_id'] ?? 0);
    }

    /** @return array<string,true> */
    private function tableColumns(string $table): array
    {
        if (isset($this->columns[$table])) {
            return $this->columns[$table];
        }
        if (!preg_match('/^[a-z0-9_]+$/', $table)) {
            throw new RuntimeException('Nome tabella demo non valido.');
        }
        $columns = [];
        foreach ($this->db->query("SHOW COLUMNS FROM `{$table}`")->fetchAll(PDO::FETCH_ASSOC) as $column) {
            $columns[(string) $column['Field']] = true;
        }
        return $this->columns[$table] = $columns;
    }

    private function tableExists(string $table): bool
    {
        $statement = $this->db->prepare(
            'SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?'
        );
        $statement->execute([$table]);
        return (bool) $statement->fetchColumn();
    }

    private function value(string $sql, array $parameters = []): int
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($parameters);
        return (int) ($statement->fetchColumn() ?: 0);
    }

    private function json(array $value): string
    {
        return (string) json_encode($value, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }

    private function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($bytes), 4));
    }
}
