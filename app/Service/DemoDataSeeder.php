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
                "L'azienda demo esiste giÃ  (ID {$existing}). Usa demo:seed --reset per rigenerarla integralmente."
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
                'address' => 'Via dellâ€™Innovazione 24',
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
            'active' =×~½ÞÚ$z{-®éÜj×GF†—2ÓæFB‚w6F•öæ÷F–f–6F–öç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw6F•÷G&ç6Ö—76–öåö–BrÓâGG&ç6Ö—76–öâÂvæ÷F–f–6F–öå÷G—RrÓâu$2rÂvf–ÆVæÖRrÓâu$5ôDTÔóç†ÖÂrÂv6†V6·7VÕ÷6†#SbrÓâ†6‚‚w6†#SbrÂvFVÖò×&V6V—Br’Âw–ÆöE÷†ÖÂrÓâsÅ&–6WgWF6öç6VvæFVÖóÒ'G'VR"óârÂw&V6V—fVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ’F—2r’’Âw&ö6W76VEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ’F—2r’•Ò“° ¢GF†—2ÓæFB‚v6öÖ×Væ–6F–öå÷6WGF–æw2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwG&ç7÷'BrÓâtD•4$ÄTBrÂvg&öÕöVÖ–ÂrÓâvæ÷&WÇ”FVÖòæÇVææÆö6ÂrÂvg&öÕöæÖRrÓâtÇVæFVÖòrÂwG&6¶–æuö&6U÷W&ÂrÓâv‡GG3¢òöFVÖòæÇVææÆö6ÂrÂwG&6¶–æuöVæ&ÆVBrÓâÂvÖ…öGFV×G2rÓâUÒ“°¢FÖW76vRÒGF†—2ÓæFB‚v÷WF&÷VæEöVÖ–Ç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvÖW76vU÷WV–BrÓâGF†—2ÓçWV–B‚’ÂvFö7VÖVçEö–BrÓâFFö7VÖVçG5²w6ÆW5ö–çfö–6RuÒÂw&V6—–VçBrÓâvÖÖ–æ—7G&¦–öæT6Æ–VçFRÖFVÖòæÆö6ÂrÂw7V&¦V7BrÓâtfGGW&FVÖòF—7öæ–&–ÆRrÂv‡FÖÅö&öG’rÓâsÇävVçF–ÆR6Æ–VçFRÂÆfGGW&FVÖò:‚F—7öæ–&–ÆRãÂ÷ârÂwFW‡Eö&öG’rÓâtÆfGGW&FVÖò:‚F—7öæ–&–ÆRârÂvGF6†ÖVçEö§6öârÓâGF†—2Óæ§6öâ…²vFö7VÖVçEö–BrÓâFFö7VÖVçG5²w6ÆW5ö–çfö–6RuÕÒ’ÂwG&6¶–æu÷Fö¶VârÓâ†6‚‚w6†#SbrÂvFVÖò×G&6²ÒrâF÷&r’Âw7FGW2rÓâu4TåBrÂvGFV×G2rÓâÂw6VçEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓRF—2r’’ÂvÖW76vUö–BrÓâsÆFVÖòÒrâF÷&râtÇVææÆö6ÃârÂv7&VFVEö'’rÓâGW6W'5²v66÷VçFçBuÕÒ“°¢GF†—2ÓæFB‚vVÖ–ÅöWfVçG2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv÷WF&÷VæEöVÖ–Åö–BrÓâFÖW76vRÂvWfVçE÷G—RrÓâtDTÄ•dU$TBrÂvö67W'&VEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓRF—2³"Ö–çWFW2r’•Ò“°¢GF†—2ÓæFB‚vVÖ–ÅöWfVçG2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv÷WF&÷VæEöVÖ–Åö–BrÓâFÖW76vRÂvWfVçE÷G—RrÓâtõTârÂvö67W'&VEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓBF—2r’•Ò“°¢GF†—2ÓæFB‚vVÖ–Å÷G&6¶–ærrÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvFö7VÖVçEö–BrÓâFFö7VÖVçG5²w6ÆW5ö–çfö–6RuÒÂw&V6—–VçBrÓâvÖÖ–æ—7G&¦–öæT6Æ–VçFRÖFVÖòæÆö6ÂrÂw7V&¦V7BrÓâtfGGW&FVÖòF—7öæ–&–ÆRrÂwG&6¶–æu÷Fö¶VârÓâ†6‚‚w6†#SbrÂvÆVv7’ÖFVÖò×G&6²ÒrâF÷&r’Âw6VçEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓRF—2r’’Âvf—'7Eö÷VæVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓBF—2r’’ÂvÆ7Eö÷VæVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ2F—2r’’Âv÷Vç5ö6÷VçBrÓâ5Ò“°¢GF†—2ÓæFB‚v&6¶w&÷VæEö¦ö'2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwVWVRrÓâvFVÖòrÂv¦ö%÷G—RrÓâtDTÔõô„TÅD…ô4„T4²rÂw–ÆöEö§6öârÓâGF†—2Óæ§6öâ…²v÷&væ—¦F–öåö–BrÓâF÷&uÒ’Âw7FGW2rÓât4ôÕÄUDTBrÂvGFV×G2rÓâÂvf–Æ&ÆUöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓF’r’’Âw7F'FVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓF’r’’Âv6ö×ÆWFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓF’³Ö–çWFRr’•Ò“°¢Ð ¢&—fFRgVæ7F–öâ6VVE&öfW76–öæÅv÷&·76R†–çBF÷&rÂ'&’GW6W'2Â'&’F66÷VçG2“¢fö–@¢°¢G–V"Ò†–çB’FFR‚u’r“°¢F76WD6FVv÷'’ÒGF†—2ÓæFB‚vf—†VEö76WEö6FVv÷&–W2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv6öFRrÓât…rrÂvæÖRrÓât†&Gv&RRÖ66†–æRVÆWGG&öæ–6†RrÂv6—f–Å÷&FRrÓâ#ÂwF…÷&FRrÓâ#Âv7F—fRrÓâÒ“°¢F76WBÒGF†—2ÓæFB‚vf—†VEö76WG2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv76WEö6öFRrÓât4U5ÔDTÔòÓrÂvFW67&—F–öârÓâu6W'fW"R–æg&7G'WGGW&¦–VæFÆRrÂv6FVv÷'’rÓât†&Gv&RrÂv6FVv÷'•ö–BrÓâF76WD6FVv÷'’ÂwW&6†6UöFFRrÓâFFR‚u’ÖÒÖBrÂ7G'F÷F–ÖR‚rÓ#ÖöçF‡2r’’ÂwW&6†6Uö6÷7BrÓâƒÂvFW&V6–F–öå÷&FRrÓâ#Âv67V×VÆFVEöFW&V6–F–öârÓâs#ÂvæWEö&ööµ÷fÇVRrÓâƒÂw7FGW2rÓât5D•dRrÂv7&VFVEö'’rÓâGW6W'5²v66÷VçFçBuÕÒ“°¢GF†—2ÓæFB‚vFW&V6–F–öåöVçG&–W2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf—†VEö76WEö–BrÓâF76WBÂvf—66Å÷–V"rÓâG–V"ÒÂvÖ÷VçBrÓâ3cÂw7FGW2rÓâuõ5DTBuÒ“°¢f÷&V6‚…µ²udBrÂufW'6ÖVçFò•dÖVç6–ÆRrÆFFR‚u’ÖÒÓbrÂ7G'F÷F–ÖR‚r³ÖöçF‚r’’Ã#ƒÒÅ²tc#BrÂtc#B6öçG&–'WF’R&—FVçWFRrÆFFR‚u’ÖÒÓbrÂ7G'F÷F–ÖR‚r³ÖöçF‚r’’Ã3cSÒÅ²tÄ•RrÂt6ö×Væ–6¦–öæRÄ•RG&–ÖW7G&ÆRrÆFFR‚u’ÖÒ×BrÂ7G'F÷F–ÖR‚r³"ÖöçF‡2r’’ÃÒÅ²t5RrÂt6W'F–f–6¦–öæRVæ–6rÂ‚G–V"²’ârÓ2ÓbrÃÕÒ2²GG—RÂFFW67&—F–öâÂFGVRÂFÖ÷VçEÒ’°¢GF†—2ÓæFB‚wF…öFVFÆ–æW2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvGVUöFFRrÓâFGVRÂvFVFÆ–æU÷G—RrÓâGG—RÂvFW67&—F–öârÓâFFW67&—F–öâÂw&VfW&Væ6U÷W&–öBrÓâFFR‚vÒõ’r’ÂvÖ÷VçBrÓâFÖ÷VçBÂw7FGW2rÓâtõTâuÒ“°¢Ð¢GF†—2ÓæFB‚wv—F††öÆF–æu÷&V6÷&G2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw&V6÷&EöFFRrÓâFFR‚u’ÖÒÖBrÂ7G'F÷F–ÖR‚rÓ#F—2r’’ÂvGVUöFFRrÓâFFR‚u’ÖÒÓbrÂ7G'F÷F–ÖR‚r³ÖöçF‚r’’Âwv—F††öÆF–æu÷G—RrÓât•%TbrÂvw&÷75öÖ÷VçBrÓâSÂwF†&ÆU÷W&6VçBrÓâÂw&FU÷W&6VçBrÓâ#Âwv—F††öÆF–æuöÖ÷VçBrÓâ3ÂvæWEöÖ÷VçBrÓâ#Âw7FGW2rÓâut•D„„TÄBrÂv7&VFVEö'’rÓâGW6W'5²v66÷VçFçBuÕÒ“°¢GF†—2ÓæFB‚wfEöF§W7FÖVçG2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf—66Å÷–V"rÓâG–V"ÂvF§W7FÖVçE÷G—RrÓâu$õõ$DrÂvFW67&—F–öârÓâu&WGF–f–6•dæçVÆRF–Ö÷7G&F—frÂwfEöFV&—EöFVÇFrÓâ#RÂwfEö7&VF—EöFVÇFrÓâÂw7FGW2rÓâtE$eBuÒ“°¢GF†—2ÓæFB‚vÆ—Uö6öÖ×Væ–6F–öç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf—66Å÷–V"rÓâG–V"ÂwV'FW%öçVÖ&W"rÓâÂw66†VÖ÷fW'6–öârÓâsãÔDTÔòrÂw–ÆöEö§6öârÓâGF†—2Óæ§6öâ…²wg"rÓâƒSÂwg2rÓâcCÒ’Âw7FGW2rÓâu$Ud”UtTBuÒ“°¢GF†—2ÓæFB‚wfEöæçVÅ÷7VÖÖ&–W2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf—66Å÷–V"rÓâG–V"ÒÂw66†VÖ÷fW'6–öârÓâtDTÔòÓrÂvFWF–Åö§6öârÓâGF†—2Óæ§6öâ…²w6ÆW2rÓâ#ƒSÂwW&6†6W2rÓâ3#Ò’Âw7FGW2rÓâu$Ud”UtTBuÒ“°¢GF†—2ÓæFB‚v66÷VçF–æu÷W&–öEöÆö6·2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw66÷RrÓâudBrÂw7F'G5ööârÓâ‚G–V"Ò’ârÓÓrÂvVæG5ööârÓâ‚G–V"Ò’ârÓ"Ó3rÂw&V6öârÓâuW&–öFò•d6†—W6òRfÆ–FFòrÂvÆö6¶VEö'’rÓâGW6W'5²v66÷VçFçBuÕÒ“°¢GF†—2ÓæFB‚v66÷VçF–æuö6Æ÷6–æu÷'Vç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf—66Å÷–V"rÓâG–V"ÒÂw7FGW2rÓâuõ5DTBrÂw6æ6†÷Eö§6öârÓâGF†—2Óæ§6öâ…²v76WG2rÓâcƒÂvÆ–&–Æ—F–W2rÓâƒ#Âw&W7VÇBrÓâ#CÒ’Âv7&VFVEö'’rÓâGW6W'5²v66÷VçFçBuÕÒ“°¢GF†—2ÓæFB‚v66÷VçF–æuöF§W7FÖVçE÷66†VGVÆW2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvF§W7FÖVçE÷G—RrÓâu$U”BrÂvFW67&—F–öârÓâu&—66öçFò76–7W&¦–öæRæçVÆRrÂw6÷W&6Uö66÷VçEö–BrÓâF66÷VçG5²v6÷7G2uÒÂv6÷VçFW''Eö66÷VçEö–BrÓâF66÷VçG5²v76WG2uÒÂvÖ÷VçBrÓâ#Âv6ö×WFVæ6Uög&öÒrÓâFFR‚u’ÓrÓr’Âv6ö×WFVæ6U÷FòrÓâFFR‚u’ÓbÓ3rÂ7G'F÷F–ÖR‚r³–V"r’’Âw÷7F–æuöFFRrÓâFFR‚u’Ó"Ó3r’Âw7FGW2rÓâtE$eBuÒ“° ¢F–×÷'BÒGF†—2ÓæFB‚v–×÷'Eö&F6†W2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwWV–BrÓâGF†—2ÓçWV–B‚’Âw6÷W&6U÷7—7FVÒrÓâtDDUeô´ô”äõ2rÂv–×÷'E÷G—RrÓât5U5DôÔU%2rÂw7FGW2rÓât4ôÕÄUDTBrÂv÷&–v–æÅöf–ÆVæÖRrÓâv6Æ–VçF’Ö¶ö–æ÷2ÖFVÖòæ77brÂv6†V6·7VÕ÷6†#SbrÓâ†6‚‚w6†#SbrÂvFVÖòÖ–×÷'Br’Âvf–ÆU÷6—¦RrÓâ#C‚ÂwF÷FÅ÷&÷w2rÓâ#RÂwfÆ–E÷&÷w2rÓâ#RÂv–×÷'FVE÷&÷w2rÓâ#RÂv7&VFVEö'’rÓâGW6W'5²v÷væW"uÒÂv6ö×ÆWFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓcF—2r’•Ò“°¢Ff–ÆRÒGF†—2ÓæFB‚v–×÷'Eöf–ÆW2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv&F6…ö–BrÓâF–×÷'BÂvf–ÆVæÖRrÓâv6Æ–VçF’Ö¶ö–æ÷2ÖFVÖòæ77brÂvÖVF–÷G—RrÓâwFW‡Bö77brÂv6†V6·7VÕ÷6†#SbrÓâ†6‚‚w6†#SbrÂvFVÖòÖ–×÷'BÖf–ÆRr’Âvf–ÆU÷6—¦RrÓâ#C…Ò“°¢GF†—2ÓæFB‚v–×÷'E÷&÷w2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv&F6…ö–BrÓâF–×÷'BÂw6÷W&6Uöf–ÆUö–BrÓâFf–ÆRÂw6÷W&6U÷&÷uöçVÖ&W"rÓâ"Âw&uöFFö§6öârÓâGF†—2Óæ§6öâ…²u&v–öæU6ö6–ÆRrÓât6Æ–VçFR7F÷&–6òFVÖòuÒ’Âvæ÷&ÖÆ—¦VEöFFö§6öârÓâGF†—2Óæ§6öâ…²v'W6–æW75öæÖRrÓât6Æ–VçFR7F÷&–6òFVÖòuÒ’Âw7FGW2rÓât”Õõ%DTBuÒ“°¢GF†—2ÓæFB‚v–×÷'E÷&V6÷&G2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv&F6…ö–BrÓâF–×÷'BÂvVçF—G•÷G—RrÓâv7W7FöÖW'2rÂvVçF—G•ö–BrÓâÂv÷W&F–öârÓât5$TDRuÒ“°¢GF†—2ÓæFB‚v–×÷'EöÖ–æw2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw6÷W&6U÷7—7FVÒrÓâtDDUeô´ô”äõ2rÂv–×÷'E÷G—RrÓât5U5DôÔU%2rÂvæÖRrÓâtÖGW&6Æ–VçF’FVÖòrÂvÖ–æuö§6öârÓâGF†—2Óæ§6öâ…²u&v–öæU6ö6–ÆRrÓâv'W6–æW75öæÖRuÒ’Âv7F—fRrÓâÒ“° ¢GF†—2ÓæFB‚w&W÷'E÷&W6WG2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw&W÷'Eö¶W’rÓâtÔätTÔTåEôõdU%d”UrrÂvæÖRrÓât7'W66÷GFòF—&W¦–öæÆRÖVç6–ÆRrÂvf–ÇFW'5ö§6öârÓâGF†—2Óæ§6öâ…²wW&–öBrÓâu•DBuÒ’Âv6öÇVÖç5ö§6öârÓâGF†—2Óæ§6öâ…²w&WfVçVRrÂvÖ&v–ârÂv66‚uÒ’Âv7&VFVEö'’rÓâGW6W'5²v÷væW"uÕÒ“°¢GF†—2ÓæFB‚w&W÷'EöW‡÷'G2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw&W÷'Eö¶W’rÓâtÔätTÔTåEôõdU%d”UrrÂwW&–öEög&öÒrÓâFFR‚u’ÓÓr’ÂwW&–öE÷FòrÓâFFR‚u’ÖÒÖBr’Âw&ÖWFW'5ö§6öârÓâGF†—2Óæ§6öâ…²vFVÖòrÓâG'VUÒ’Âvf–ÆVæÖRrÓâw&W÷'BÖF—&W¦–öæÆRÖFVÖòç†Ç7‚rÂv6†V6·7VÕ÷6†#SbrÓâ†6‚‚w6†#SbrÂvFVÖò×&W÷'Br’Âw&÷uö6÷VçBrÓâ#BÂvvVæW&FVEö'’rÓâGW6W'5²v÷væW"uÒÂvvVæW&FVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ"F—2r’•Ò“°¢GF†—2ÓæFB‚vöff–6–Å÷&–çE÷'Vç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw&–çE÷G—RrÓât¤õU$äÂrÂw6WVVæ6UöçVÖ&W"rÓâÂwF—FÆRrÓâtÆ–'&òv–÷&æÆRFVÖòrÂwW&–öE÷7F'BrÓâFFR‚u’ÓÓr’ÂwW&–öEöVæBrÓâFFR‚u’ÖÒÖBr’Âw7FGW2rÓâudÄ”DDTBrÂw&÷uö6÷VçBrÓâ#Âw&öfW76–öæÅ÷fÆ–FF–öå÷&VfW&Væ6RrÓâudÄ”BÔDTÔòÓrÂvvVæW&FVEö'’rÓâGW6W'5²v66÷VçFçBuÒÂwfÆ–FFVEö'’rÓâGW6W'5²v÷væW"uÒÂwfÆ–FFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓF’r’•Ò“°¢GF†—2ÓæFB‚v6ö×Æ–æ6Uöf–Æ–æu÷'Vç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂvf–Æ–æu÷G—RrÓâtÄ•RrÂwW&–öE÷–V"rÓâG–V"ÂwW&–öEö6öFRrÓâurÂw7FGW2rÓâu$TE’rÂw66†VÖ÷fW'6–öârÓâtDTÔòÓrÂvFFö§6öârÓâGF†—2Óæ§6öâ…²wV'FW"rÓâÒ’ÂvæöÖÇ•ö6÷VçBrÓâÂw&öfW76–öæÅ÷fÆ–FF–öå÷&VfW&Væ6RrÓâtÄ•RÔDTÔòÕrÂv7&VFVEö'’rÓâGW6W'5²v66÷VçFçBuÒÂwfÆ–FFVEö'’rÓâGW6W'5²v÷væW"uÒÂwfÆ–FFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2r•Ò“°¢G7FFVÖVçBÒGF†—2ÓæFB‚v&æµ÷7FFVÖVçEö–×÷'G2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv&æµö66÷VçEö–BrÓâF66÷VçG5²v&æµö–BuÒÂw6÷W&6Uöf÷&ÖBrÓât55brÂv÷&–v–æÅöf–ÆVæÖRrÓâvW7G&GFòÖ6öçFòÖFVÖòæ77brÂw7F÷&VE÷F‚rÓâw7F÷&vRö–×÷'G2öW7G&GFòÖ6öçFòÖFVÖòæ77brÂv6†V6·7VÕ÷6†#SbrÓâ†6‚‚w6†#SbrÂvFVÖò×7FFVÖVçBr’Âw7FGW2rÓât”Õõ%DTBrÂw&÷w5ö6÷VçBrÓâC‚Âv–×÷'FVEö6÷VçBrÓâC‚ÂvGWÆ–6FUö6÷VçBrÓâÂvW'&÷%ö6÷VçBrÓâÂv–×÷'FVEö'’rÓâGW6W'5²v66÷VçFçBuÒÂv–×÷'FVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ2F—2r’•Ò“° ¢f÷&V6‚‚GW6W'22GW6W$–B’°¢GF†—2ÓæFB‚wW6W%÷&VfW&Væ6W2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwW6W%ö–BrÓâGW6W$–BÂw&VfW&Væ6Uö¶W’rÓâvF6†&ö&Bæ6ö×7BrÂw&VfW&Væ6Uö§6öârÓâGF†—2Óæ§6öâ…²vVæ&ÆVBrÓâfÇ6UÒ•Ò“°¢Ð¢GF†—2ÓæFB‚w6fVE÷f–Ww2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwW6W%ö–BrÓâGW6W'5²v66÷VçFçBuÒÂvÖöGVÆUö¶W’rÓâv7W7FöÖW'2rÂvæÖRrÓât6Æ–VçF’GF—f’Ö–ÆæòrÂwVW'•ö§6öârÓâGF†—2Óæ§6öâ…²vf–ÇFW'2rÓâ²v6—G’rÓâtÖ–ÆæòrÂv7F—fRrÓâÕÒ’Âv—5öFVfVÇBrÓâÒ“°¢f÷&V6‚…µ²t44õTåD”ärrÂtÆ—V–F¦–öæR•dFfW&–f–6&RrÂtÆÆ—V–F¦–öæRFVÂÖW6R6÷'&VçFR:‚æ6÷&–â&÷§¦âuÒÅ²u4ÄU2rÂu&WfVçF—fò–â66FVç¦rÂt–Â&WfVçF—fòFVÖò&–6†–VFRVâföÆÆ÷r×WâuÒÅ²t”ådTåDõ%’rÂu66÷'FÖ–æ–ÖrÂtGVR'F–6öÆ’6öæòf–6–æ’ÆÆ6övÆ–Ö–æ–ÖâuÒÅ²t…"rÂtfW&–RF&÷f&RrÂ|8‚&W6VçFRVæ&–6†–W7FfW&–R–âGFW6âuÕÒ2²F6FVv÷'’ÂGF—FÆRÂFÖW76vUÒ’°¢GF†—2ÓæFB‚wv÷&·76Uöæ÷F–f–6F–öç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂv6FVv÷'’rÓâF6FVv÷'’ÂwF—FÆRrÓâGF—FÆRÂvÖW76vRrÓâFÖW76vRÂw6WfW&—G’rÓât”ädòrÂv7F–öå÷W&ÂrÓâröF6†&ö&BuÒ“°¢Ð¢f÷&V6‚…²v6ö×ç’rÂwW6W'2rÂvÖöGVÆW2rÂv66÷VçF–ærrÂv&æ²rÂv–×÷'G2uÒ2G7FW’°¢GF†—2ÓæFB‚vöæ&ö&F–æu÷&öw&W72rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂw7FWö¶W’rÓâG7FWÂv6ö×ÆWFVEö'’rÓâGW6W'5²v÷væW"uÒÂv6ö×ÆWFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓ3F—2r’•Ò“°¢Ð¢GF†—2ÓæFB‚v'VÆµö÷W&F–öç2rÂ²v÷&væ—¦F–öåö–BrÓâF÷&rÂwW6W%ö–BrÓâGW6W'5²v÷væW"uÒÂvÖöGVÆUö¶W’rÓâv7W7FöÖW'2rÂv÷W&F–öârÓâtU…õ%BrÂw6VÆV7F–öåö§6öârÓâGF†—2Óæ§6öâ…²vÆÅöf–ÇFW&VBrÓâG'VUÒ’Âw7FGW2rÓât4ôÕÄUDTBrÂw&ö6W76VEö6÷VçBrÓâ‚Âvf–ÆVEö6÷VçBrÓâÂw&W7VÇEö§6öârÓâGF†—2Óæ§6öâ…²vf–ÆVæÖRrÓâv6Æ–VçF’ÖFVÖòç†Ç7‚uÒ’Âv6ö×ÆWFVEöBrÓâFFR‚u’ÖÒÖBƒ¦“§2rÂ7G'F÷F–ÖR‚rÓF’r’•Ò“°¢Ð ¢&—fFRgVæ7F–öâf–æDFVÖô÷&væ—¦F–öâ‚“¢–ç@¢°¢G7FFVÖVçBÒGF†—2ÓæF"Óç&W&R‚u4TÄT5B–Be$ôÒ÷&væ—¦F–öç2t„U$RF…ö6öFRÒòõ$DU"%’–BÄ”Ô•Br“°¢G7FFVÖVçBÓæW†V7WFR…·6VÆc£¤DTÔõõD…ô4ôDUÒ“°¢&WGW&â†–çB’‚G7FFVÖVçBÓæfWF6„6öÇVÖâ‚’ó¢“°¢Ð ¢ò¢¢&ÒÆ—7CÇ7G&–æsâGF&ÆW2¢ð¢&—fFRgVæ7F–öâ&WV—&UF&ÆW2†'&’GF&ÆW2“¢fö–@¢°¢FÖ—76–ærÒ'&•÷fÇVW2†'&•öf–ÇFW"‚GF&ÆW2Âfâ‡7G&–ærGF&ÆR“¢&ööÂÓâGF†—2ÓçF&ÆTW†—7G2‚GF&ÆR’’“°¢–b‚FÖ—76–ærÓÒµÒ’°¢F‡&÷ræWr'VçF–ÖTW†6WF–öâ‚tFF&6Ræöâvv–÷&æFòâF&VÆÆRÖæ6çF“¢râ–×ÆöFR‚rÂrÂFÖ—76–ær’ârâW6VwV’&–Ö&–âöÇVæÖ–w&FRâr“°¢Ð¢Ð ¢ò¢¢&Ò'&“Ç7G&–ærÆÖ—†VCâFFF¢ð¢&—fFRgVæ7F–öâFB‡7G&–ærGF&ÆRÂ'&’FFF“¢–ç@¢°¢–b‚GF†—2ÓçF&ÆTW†—7G2‚GF&ÆR’’°¢F‡&÷ræWr'VçF–ÖTW†6WF–öâ‚%F&VÆÆ²GF&ÆWÒÖæ6çFS¢W6VwV’&–Ö&–âöÇVæÖ–w&FRâ"“°¢Ð¢Ff–Æ&ÆRÒGF†—2ÓçF&ÆT6öÇVÖç2‚GF&ÆR“°¢FFFÒ'&•öf–ÇFW"‚FFFÂ7FF–2fâ†Ö—†VBEòÂ7G&–ærF6öÇVÖâ“¢&ööÂÓâ—76WB‚Ff–Æ&ÆU²F6öÇVÖåÒ’Â%$•ôd”ÅDU%õU4Uô$õD‚“°¢–b‚FFFÓÓÒµÒ’°¢&WGW&â°¢Ð¢F6öÇVÖç2Ò'&•ö¶W—2‚FFF“°¢GV÷FVBÒ–×ÆöFR‚rÂrÂ'&•öÖ‡7FF–2fâ‡7G&–ærF6öÇVÖâ“¢7G&–ærÓâvrâF6öÇVÖââvrÂF6öÇVÖç2’“°¢GÆ6V†öÆFW'2Ò–×ÆöFR‚rÂrÂ'&•öf–ÆÂƒÂ6÷VçB‚F6öÇVÖç2’Âsòr’“°¢G7FFVÖVçBÒGF†—2ÓæF"Óç&W&R‚$”å4U%B”åDò²GF&ÆWÖ‡²GV÷FVGÒ’dÅTU2‡²GÆ6V†öÆFW'7Ò’"“°¢G7FFVÖVçBÓæW†V7WFR†'&•÷fÇVW2‚FFF’“°¢GF†—2Óæ6÷VçG5²GF&ÆUÒÒ‚GF†—2Óæ6÷VçG5²GF&ÆUÒóò’²°¢F–BÒ†–çB’GF†—2ÓæF"ÓæÆ7D–ç6W'D–B‚“°¢&WGW&âF–BâòF–B¢†–çB’‚FFF²v÷&væ—¦F–öåö–BuÒóò“°¢Ð ¢ò¢¢&WGW&â'&“Ç7G&–ærÇG'VSâ¢ð¢&—fFRgVæ7F–öâF&ÆT6öÇVÖç2‡7G&–ærGF&ÆR“¢'&¢°¢–b†—76WB‚GF†—2Óæ6öÇVÖç5²GF&ÆUÒ’’°¢&WGW&âGF†—2Óæ6öÇVÖç5²GF&ÆUÓ°¢Ð¢–b‚&VuöÖF6‚‚rõå¶×£Ó•õÒ²BòrÂGF&ÆR’’°¢F‡&÷ræWr'VçF–ÖTW†6WF–öâ‚tæöÖRF&VÆÆFVÖòæöâfÆ–Fòâr“°¢Ð¢F6öÇVÖç2ÒµÓ°¢f÷&V6‚‚GF†—2ÓæF"ÓçVW'’‚%4„õr4ôÅTÔå2e$ôÒ²GF&ÆWÖ"’ÓæfWF6„ÆÂ…Dó£¤dUD4…ô54ô2’2F6öÇVÖâ’°¢F6öÇVÖç5²‡7G&–ær’F6öÇVÖå²tf–VÆBuÕÒÒG'VS°¢Ð¢&WGW&âGF†—2Óæ6öÇVÖç5²GF&ÆUÒÒF6öÇVÖç3°¢Ð ¢&—fFRgVæ7F–öâF&ÆTW†—7G2‡7G&–ærGF&ÆR“¢&ööÀ¢°¢G7FFVÖVçBÒGF†—2ÓæF"Óç&W&R€¢u4TÄT5Be$ôÒ–æf÷&ÖF–öå÷66†VÖåD$ÄU2t„U$RD$ÄUõ44„TÔÒDD$4R‚’äBD$ÄUôäÔRÒòp¢“°¢G7FFVÖVçBÓæW†V7WFR…²GF&ÆUÒ“°¢&WGW&â†&ööÂ’G7FFVÖVçBÓæfWF6„6öÇVÖâ‚“°¢Ð ¢&—fFRgVæ7F–öâfÇVR‡7G&–ærG7ÂÂ'&’G&ÖWFW'2ÒµÒ“¢–ç@¢°¢G7FFVÖVçBÒGF†—2ÓæF"Óç&W&R‚G7Â“°¢G7FFVÖVçBÓæW†V7WFR‚G&ÖWFW'2“°¢&WGW&â†–çB’‚G7FFVÖVçBÓæfWF6„6öÇVÖâ‚’ó¢“°¢Ð ¢&—fFRgVæ7F–öâ§6öâ†'&’GfÇVR“¢7G&–æp¢°¢&WGW&â‡7G&–ær’§6öåöVæ6öFR‚GfÇVRÂ¥4ôåõTäU44TEõTä”4ôDRÂ¥4ôåõTäU44TEõ4Ä4„U2“°¢Ð ¢&—fFRgVæ7F–öâWV–B‚“¢7G&–æp¢°¢F'—FW2Ò&æFöÕö'—FW2ƒb“°¢F'—FW5³eÒÒ6‡"‚†÷&B‚F'—FW5³eÒ’bƒb’ÂƒC“°¢F'—FW5³…ÒÒ6‡"‚†÷&B‚F'—FW5³…Ò’bƒ6b’Âƒƒ“°¢&WGW&âg7&–çFb‚rW2W2ÒW2ÒW2ÒW2ÒW2W2W2rÂ7G%÷7Æ—B†&–ã&†W‚‚F'—FW2’ÂB’“°¢Ð§Ð 