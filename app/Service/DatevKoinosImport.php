<?php
declare(strict_types=1);
namespace Luna\Service;

use PDO;
use InvalidArgumentException;
use Throwable;

/** Applies only complete master data; keeps incomplete financial sources explicitly separate. */
final class DatevKoinosImport
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId) {}

    public function commit(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0; $skipped = 0; $applied = 0; $pending = 0; $references = 0;
        $decoded = [];
        foreach ($rows as $staged) {
            $record = json_decode($staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $decoded[] = compact('staged', 'record');
        }
        $priority = ['accounts'=>10,'customers'=>20,'suppliers'=>20,'invoice_history'=>25,'asset_categories'=>30,'fixed_asset_master'=>40,'asset_progressives'=>50,'asset_movements'=>60];
        usort($decoded, static fn (array $a, array $b): int => ($priority[$a['record']['kind']] ?? 100) <=> ($priority[$b['record']['kind']] ?? 100));
        foreach ($decoded as $item) {
            $staged = $item['staged'];
            $this->db->exec('SAVEPOINT datev_record');
            try {
                $record = $item['record'];
                $kind = $record['kind']; $data = $record['data']; $key = (string) $record['key'];
                if (!isset(DatevKoinosReader::LABELS[$kind]) || $key === '' || strlen($key) > 255) throw new InvalidArgumentException('Identificativo origine non valido.');
                $json = json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
                $hash = hash('sha256', $json);
                $find = $this->db->prepare('SELECT id, content_sha256 FROM datev_reference_records WHERE organization_id = ? AND record_kind = ? AND source_key = ? ORDER BY id DESC');
                $find->execute([$this->organizationId, $kind, $key]); $versions = $find->fetchAll();
                $identical = array_values(array_filter($versions, static fn ($v) => $v['content_sha256'] === $hash));
                if ($identical) {
                    $existingReference = $identical[0];
                    if ($kind === 'invoice_history') {
                        [$entityId, $note, $state, $wasCreated] = $this->historicalInvoice($key, $data, $batchId);
                        if ($entityId !== null) {
                            $this->db->prepare('UPDATE datev_reference_records SET application_status=?,application_note=?,entity_type=\'documents\',entity_id=? WHERE id=? AND organization_id=?')
                                ->execute([$state,$note,$entityId,$existingReference['id'],$this->organizationId]);
                            $this->mark((int)$staged['id'],$wasCreated ? 'IMPORTED' : 'SKIPPED',$note,(int)$existingReference['id']);
                            if ($wasCreated) { $imported++; $state === 'APPLIED' ? $applied++ : $pending++; } else { $skipped++; }
                            continue;
                        }
                    }
                    $this->mark((int) $staged['id'], 'SKIPPED', 'Dato già acquisito: nessuna duplicazione.', (int) $existingReference['id']); $skipped++; continue;
                }
                $state = empty($record['issue']) ? 'REFERENCE' : 'NEEDS_DATA';
                $note = $record['issue'] ?? 'Dato di riferimento acquisito. Non genera movimenti contabili.';
                $entity = null; $entityId = null;
                if ($versions) {
                    $state = 'CONFLICT'; $note = 'Esiste una versione differente della stessa chiave Koinos. Nessun dato operativo è stato sovrascritto.';
                } elseif ($kind === 'accounts' && !empty($data['account_type'])) {
                    [$entityId, $note, $state] = $this->account($data, $batchId); $entity = 'chart_of_accounts';
                } elseif (in_array($kind, ['customers','suppliers'], true)) {
                    [$entityId, $note, $state] = $this->party($kind, $data, $batchId); $entity = $kind;
                } elseif ($kind === 'invoice_history') {
                    [$entityId, $note, $state] = $this->historicalInvoice($key, $data, $batchId); $entity = 'documents';
                } elseif ($kind === 'causes') {
                    [$entityId, $note, $state] = $this->cause($data); $entity = 'accounting_causes';
                } elseif ($kind === 'asset_categories') {
                    [$entityId, $note, $state] = $this->assetCategory($data); $entity = 'fixed_asset_categories';
                } elseif ($kind === 'fixed_asset_master') {
                    [$entityId, $note, $state] = $this->fixedAsset($data, $batchId); $entity = 'fixed_assets';
                } elseif ($kind === 'asset_progressives') {
                    [$entityId, $note, $state] = $this->assetProgressive($key, $data, $batchId); $entity = 'datev_fixed_asset_progressives';
                } elseif ($kind === 'asset_movements') {
                    [$entityId, $note, $state] = $this->assetMovement($key, $data, $batchId); $entity = 'datev_fixed_asset_movements';
                }
                $description = (string) ($data['business_name'] ?? $data['name'] ?? $data['description'] ?? $data['Descrizione'] ?? $data['number'] ?? $data['filename'] ?? $key);
                $insert = $this->db->prepare('INSERT INTO datev_reference_records (organization_id,batch_id,source_file_id,source_row_id,record_kind,source_key,description,payload_json,content_sha256,application_status,application_note,entity_type,entity_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)');
                $insert->execute([$this->organizationId,$batchId,$staged['source_file_id'],$staged['id'],$kind,$key,mb_substr($description,0,500),$json,$hash,$state,$note,$entity,$entityId]);
                $id = (int) $this->db->lastInsertId();
                $this->mark((int) $staged['id'], 'IMPORTED', $note, $id);
                $imported++;
                if ($state === 'APPLIED') $applied++;
                elseif (in_array($state, ['NEEDS_DATA','CONFLICT'], true)) $pending++;
                else $references++;
            } catch (Throwable $e) {
                $this->db->exec('ROLLBACK TO SAVEPOINT datev_record');
                $this->mark((int) $staged['id'], 'ERROR', $e->getMessage()); $errors++;
            }
        }
        // Parent links only for newly created accounts in this batch, never for existing accounts.
        $q = $this->db->prepare("SELECT d.entity_id,d.payload_json FROM datev_reference_records d JOIN chart_of_accounts a ON a.id=d.entity_id AND a.organization_id=d.organization_id WHERE d.organization_id=? AND d.batch_id=? AND d.record_kind='accounts' AND d.application_status='APPLIED' AND a.source_import_batch_id=?");
        $q->execute([$this->organizationId,$batchId,$batchId]);
        foreach ($q->fetchAll() as $row) {
            $data = json_decode($row['payload_json'],true,512,JSON_THROW_ON_ERROR);
            if (!empty($data['parent_code'])) {
                $parent = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE organization_id=? AND code=?');
                $parent->execute([$this->organizationId,$data['parent_code']]); $parentId = $parent->fetchColumn();
                if ($parentId && (int) $parentId !== (int) $row['entity_id']) $this->db->prepare('UPDATE chart_of_accounts SET parent_id=? WHERE id=? AND organization_id=?')->execute([$parentId,$row['entity_id'],$this->organizationId]);
            }
        }
        return compact('imported','errors','skipped','applied','pending','references');
    }

    /** Materializes XML invoices already archived by older DATEV imports. */
    public function materializeHistoricalInvoices(?int $batchId = null): array
    {
        $sql = "SELECT id,batch_id,source_key,payload_json FROM datev_reference_records WHERE organization_id=? AND record_kind='invoice_history'";
        $params = [$this->organizationId];
        if ($batchId !== null) { $sql .= ' AND batch_id=?'; $params[] = $batchId; }
        $sql .= ' ORDER BY id';
        $query = $this->db->prepare($sql); $query->execute($params);
        $created = 0; $linked = 0; $pending = 0; $errors = 0;
        $started = !$this->db->inTransaction();
        if ($started) $this->db->beginTransaction();
        try {
            foreach ($query->fetchAll() as $reference) {
                $this->db->exec('SAVEPOINT datev_invoice');
                try {
                    $data = json_decode((string)$reference['payload_json'],true,512,JSON_THROW_ON_ERROR);
                    [$documentId,$note,$state,$wasCreated] = $this->historicalInvoice((string)$reference['source_key'],$data,(int)$reference['batch_id']);
                    $this->db->prepare("UPDATE datev_reference_records SET application_status=?,application_note=?,entity_type=?,entity_id=? WHERE id=? AND organization_id=?")
                        ->execute([$state,$note,$documentId ? 'documents' : null,$documentId,$reference['id'],$this->organizationId]);
                    if ($documentId) $wasCreated ? $created++ : $linked++; else $pending++;
                } catch (Throwable $exception) {
                    $this->db->exec('ROLLBACK TO SAVEPOINT datev_invoice');
                    $this->db->prepare("UPDATE datev_reference_records SET application_status='NEEDS_DATA',application_note=? WHERE id=? AND organization_id=?")
                        ->execute([mb_substr('Materializzazione non riuscita: '.$exception->getMessage(),0,2000),$reference['id'],$this->organizationId]);
                    $errors++;
                }
            }
            if ($started) $this->db->commit();
        } catch (Throwable $exception) {
            if ($started && $this->db->inTransaction()) $this->db->rollBack();
            throw $exception;
        }
        return compact('created','linked','pending','errors');
    }

    /** Creates a read-only document shell: it deliberately creates no journal, VAT or open-item rows. */
    private function historicalInvoice(string $sourceKey, array $data, int $batchId): array
    {
        $type = (string)($data['document_type'] ?? '');
        $number = trim((string)($data['number'] ?? ''));
        $date = $this->date($data['document_date'] ?? null);
        $name = trim((string)($data['counterparty_name'] ?? ''));
        if (!in_array($type,['SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE'],true) || $number === '' || !$date || $name === '') {
            return [null,'XML storico privo dei dati minimi per creare il documento consultabile.','NEEDS_DATA',false];
        }
        $externalKey = strlen($sourceKey) === 64 ? $sourceKey : hash('sha256',$sourceKey);
        $find = $this->db->prepare('SELECT id FROM documents WHERE organization_id=? AND external_key=? LIMIT 1');
        $find->execute([$this->organizationId,$externalKey]);
        if ($id = $find->fetchColumn()) return [(int)$id,'Fattura storica già consultabile: nessuna duplicazione.','APPLIED',false];

        $partyTable = $type === 'PURCHASE_INVOICE' ? 'suppliers' : 'customers';
        $partyType = $partyTable === 'suppliers' ? 'SUPPLIER' : 'CUSTOMER';
        $country = strtoupper(trim((string)($data['counterparty_country'] ?? 'IT'))) ?: 'IT';
        $vat = PartyAutomationService::normalizeVat($data['counterparty_vat'] ?? null,$country);
        $tax = trim((string)($data['counterparty_tax_code'] ?? ''));
        $party = $this->db->prepare("SELECT id FROM {$partyTable} WHERE organization_id=? AND ((?<>'' AND vat_number=?) OR (?<>'' AND tax_code=?) OR business_name=?) ORDER BY CASE WHEN vat_number=? THEN 0 WHEN tax_code=? THEN 1 ELSE 2 END,id LIMIT 1");
        $party->execute([$this->organizationId,$vat,$vat,$tax,$tax,$name,$vat,$tax]);
        $partyId = (int)$party->fetchColumn();
        if (!$partyId) {
            $insertParty = $this->db->prepare("INSERT INTO {$partyTable} (organization_id,business_name,vat_number,tax_code,address,postal_code,city,province,country_code,iban,bank_name,bank_abi,bank_cab,payment_method_code,active,source_import_batch_id,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1,?,?,?,NOW(),NOW())");
            $insertParty->execute([$this->organizationId,$name,$vat ?: null,$tax ?: null,($data['counterparty_address']??'') ?: null,($data['counterparty_postal_code']??'') ?: null,($data['counterparty_city']??'') ?: null,($data['counterparty_province']??'') ?: null,$country,($data['bank_iban']??'') ?: null,($data['bank_name']??'') ?: null,($data['bank_abi']??'') ?: null,($data['bank_cab']??'') ?: null,($data['payment_method_code']??'') ?: null,$batchId,$this->userId ?: null,$this->userId ?: null]);
            $partyId = (int)$this->db->lastInsertId();
        }

        $sameNumber = $this->db->prepare('SELECT id,document_date,total FROM documents WHERE organization_id=? AND document_type=? AND fiscal_year=? AND number=? AND counterparty_id=? LIMIT 1');
        $sameNumber->execute([$this->organizationId,$type,(int)substr($date,0,4),$number,$partyId]);
        if ($existing = $sameNumber->fetch()) {
            $expectedTotal = round((float)($data['total'] ?? 0),2);
            if ($existing['document_date'] === $date && ($expectedTotal <= 0 || abs((float)$existing['total']-$expectedTotal)<.01)) {
                $this->db->prepare('UPDATE documents SET external_key=COALESCE(external_key,?) WHERE id=? AND organization_id=?')->execute([$externalKey,$existing['id'],$this->organizationId]);
                return [(int)$existing['id'],'Fattura storica raccordata al documento già presente.','APPLIED',false];
            }
            return [null,'Numero documento già presente con data o totale differenti: raccordo manuale necessario.','CONFLICT',false];
        }

        $lines = is_array($data['lines'] ?? null) ? $data['lines'] : [];
        $summaries = is_array($data['vat_summaries'] ?? null) ? $data['vat_summaries'] : [];
        $taxable = $summaries ? round(array_sum(array_map(fn(array $row): float => $this->decimal($row['taxable'] ?? 0),$summaries)),2) : round(array_sum(array_column($lines,'taxable_amount')),2);
        $vatTotal = $summaries ? round(array_sum(array_map(fn(array $row): float => $this->decimal($row['vat'] ?? 0),$summaries)),2) : round(array_sum(array_column($lines,'vat_amount')),2);
        $total = round((float)($data['total'] ?? 0),2); if ($total <= 0) $total = round($taxable+$vatTotal,2);
        $note = 'Documento storico importato dall’XML originale DATEV. Consultazione documentale: non genera prima nota, movimenti IVA o partite aperte.';
        $insert = $this->db->prepare('INSERT INTO documents (organization_id,document_type,number,fiscal_year,document_date,due_date,counterparty_type,counterparty_id,counterparty_name,subject,currency,taxable_total,vat_total,withholding_total,withholding_type,withholding_rate,withholding_taxable_percent,withholding_cause,total,balance_due,status,fatturapa_type,payment_method_code,bank_name,bank_abi,bank_cab,bank_iban,notes,external_key,source_import_batch_id,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,\'HISTORICAL\',?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())');
        $insert->execute([$this->organizationId,$type,$number,(int)substr($date,0,4),$date,($data['due_date']??'') ?: null,$partyType,$partyId,$name,'Storico XML FatturaPA',($data['currency']??'EUR') ?: 'EUR',$taxable,$vatTotal,(float)($data['withholding_amount']??0),($data['withholding_type']??'') ?: null,(float)($data['withholding_rate']??0),100,($data['withholding_cause']??'') ?: null,$total,0,($data['fatturapa_type']??'') ?: null,($data['payment_method_code']??'') ?: null,($data['bank_name']??'') ?: null,($data['bank_abi']??'') ?: null,($data['bank_cab']??'') ?: null,($data['bank_iban']??'') ?: null,$note,$externalKey,$batchId,$this->userId ?: null,$this->userId ?: null]);
        $documentId = (int)$this->db->lastInsertId();
        $insertLine = $this->db->prepare('INSERT INTO document_lines (organization_id,document_id,line_number,description,quantity,unit,unit_price,discount_percent,taxable_amount,vat_rate,vat_nature,vat_amount,total_amount,created_at,updated_at) VALUES (?,?,?,?,?,?,?,0,?,?,?,?,?,NOW(),NOW())');
        foreach ($lines as $index => $line) {
            $lineTaxable=round((float)($line['taxable_amount']??0),2); $lineVat=round((float)($line['vat_amount']??0),2);
            $insertLine->execute([$this->organizationId,$documentId,$index+1,trim((string)($line['description']??'')) ?: 'Riga FatturaPA',(float)($line['quantity']??1),($line['unit']??'NR') ?: 'NR',(float)($line['unit_price']??0),$lineTaxable,(float)($line['vat_rate']??0),($line['vat_nature']??'') ?: null,$lineVat,round($lineTaxable+$lineVat,2)]);
        }
        return [$documentId,'Fattura XML storica resa consultabile; contabilità, IVA e scadenziario non duplicati.','APPLIED',true];
    }

    private function account(array $data, int $batch): array
    {
        if (!in_array($data['account_type'], ['ASSET','LIABILITY','EQUITY','EXPENSE','REVENUE'],true)) throw new InvalidArgumentException('Classificazione del conto non riconosciuta.');
        $q=$this->db->prepare('SELECT * FROM chart_of_accounts WHERE organization_id=? AND code=?');
        $q->execute([$this->organizationId,$data['code']]); $existing=$q->fetch();
        if ($existing) {
            $same=$existing['name']===$data['name'] && $existing['account_type']===$data['account_type'] && (bool)$existing['is_postable']===(bool)$data['is_postable'];
            return [(int)$existing['id'], $same ? 'Conto già presente: dati operativi mantenuti.' : 'Codice conto già presente con dati differenti: verificare il raccordo. Nessuna sovrascrittura.', $same ? 'APPLIED' : 'CONFLICT'];
        }
        $normal=in_array($data['account_type'],['LIABILITY','EQUITY','REVENUE'],true)?'CREDIT':'DEBIT';
        $q=$this->db->prepare('INSERT INTO chart_of_accounts (organization_id,code,name,account_type,normal_balance,is_postable,active,source_import_batch_id,created_by,updated_by) VALUES (?,?,?,?,?,?,1,?,?,?)');
        $q->execute([$this->organizationId,$data['code'],$data['name'],$data['account_type'],$normal,(int)$data['is_postable'],$batch,$this->userId,$this->userId]);
        return [(int)$this->db->lastInsertId(),'Conto importato; verificare i collegamenti agli automatismi contabili.','APPLIED'];
    }

    private function party(string $table, array $data, int $batch): array
    {
        $vat=trim((string)($data['vat_number']??'')); $tax=trim((string)($data['tax_code']??''));
        $q=$this->db->prepare("SELECT * FROM {$table} WHERE organization_id=? AND (code=? OR (?<>'' AND vat_number=?) OR (?<>'' AND tax_code=?))");
        $q->execute([$this->organizationId,$data['code'],$vat,$vat,$tax,$tax]); $matches=$q->fetchAll();
        if ($matches) {
            $same=count($matches)===1 && $matches[0]['business_name']===$data['business_name']
                && (($vat !== '' && $matches[0]['vat_number']===$vat) || ($tax !== '' && $matches[0]['tax_code']===$tax) || ($matches[0]['code']===$data['code'] && $vat==='' && $tax===''));
            return [$same?(int)$matches[0]['id']:null,$same?'Anagrafica già presente: conservati recapiti, banca e condizioni di pagamento.':'Possibile doppione o codice già utilizzato: verificare il raccordo. Nessun dato esistente è stato modificato.',$same?'APPLIED':'CONFLICT'];
        }
        if (empty($data['country_code'])) return [null,'Paese e indirizzo da verificare: anagrafica conservata nell’archivio, non creata con un paese presunto.','NEEDS_DATA'];
        $columns=['code','business_name','vat_number','tax_code','address','postal_code','city','province'];
        $values=array_map(static fn($key)=>($data[$key]??'')!==''?$data[$key]:null,$columns);
        $q=$this->db->prepare("INSERT INTO {$table} (organization_id,".implode(',',$columns).",country_code,active,source_import_batch_id,created_by,updated_by) VALUES (?,".implode(',',array_fill(0,count($columns),'?')).",?,1,?,?,?)");
        $q->execute(array_merge([$this->organizationId],$values,[$data['country_code']??null,$batch,$this->userId,$this->userId]));
        return [(int)$this->db->lastInsertId(),'Anagrafica importata. Recapiti, banca e condizioni non presenti nella fonte restano da completare.','APPLIED'];
    }

    private function cause(array $data): array
    {
        $q=$this->db->prepare('SELECT id,name FROM accounting_causes WHERE organization_id=? AND code=?');
        $q->execute([$this->organizationId,$data['code']]); $existing=$q->fetch();
        if ($existing) return [(int)$existing['id'],'Causale già presente: mantenuta la configurazione attuale. Confrontare con la fonte.','REFERENCE'];
        $q=$this->db->prepare("INSERT INTO accounting_causes (organization_id,code,name,category,default_description,automatic,active,created_by,updated_by) VALUES (?,?,?,'GENERAL',?,0,0,?,?)");
        $q->execute([$this->organizationId,$data['code'],$data['name'],$data['name'],$this->userId,$this->userId]);
        return [(int)$this->db->lastInsertId(),'Causale acquisita e disattivata: verificare conti, sezionale e automatismi prima di abilitarla.','NEEDS_DATA'];
    }

    private function assetCategory(array $data): array
    {
        $code = trim((string)($data['code'] ?? ''));
        $name = trim((string)($data['name'] ?? ''));
        if ($code === '' || $name === '') throw new InvalidArgumentException('Categoria cespite priva di codice o descrizione.');
        $civilRate = $this->decimal($data['ordinary_rate'] ?? 0);
        $taxRate = $civilRate;
        $q = $this->db->prepare('SELECT id,name,civil_rate,tax_rate FROM fixed_asset_categories WHERE organization_id=? AND code=?');
        $q->execute([$this->organizationId,$code]); $existing = $q->fetch();
        if ($existing) {
            $same = $existing['name'] === $name && abs((float)$existing['civil_rate'] - $civilRate) < .0001;
            return [(int)$existing['id'],$same ? 'Categoria cespite già presente.' : 'Categoria già presente con configurazione diversa: mantenuti i valori operativi esistenti.',$same ? 'APPLIED' : 'CONFLICT'];
        }
        $q = $this->db->prepare('INSERT INTO fixed_asset_categories (organization_id,code,name,civil_rate,tax_rate,first_year_percent,active) VALUES (?,?,?,?,?,50,1)');
        $q->execute([$this->organizationId,$code,$name,$civilRate,$taxRate]);
        return [(int)$this->db->lastInsertId(),'Categoria cespite importata; raccordare i conti patrimoniali e di ammortamento.','NEEDS_DATA'];
    }

    private function fixedAsset(array $data, int $batchId): array
    {
        $code = trim((string)($data['asset_code'] ?? ''));
        $description = trim((string)($data['description'] ?? ''));
        $purchaseDate = $this->date($data['purchase_date'] ?? null);
        $inServiceDate = $this->date($data['in_service_date'] ?? null) ?: $purchaseDate;
        $cost = $this->decimal($data['purchase_cost'] ?? 0);
        if ($code === '' || $description === '' || !$purchaseDate || $cost < 0) throw new InvalidArgumentException('Anagrafica cespite incompleta o non valida.');
        $categoryId = null;
        if (!empty($data['category_code'])) {
            $q = $this->db->prepare('SELECT id FROM fixed_asset_categories WHERE organization_id=? AND code=?');
            $q->execute([$this->organizationId,$data['category_code']]); $categoryId = $q->fetchColumn() ?: null;
        }
        $civilRate = $this->decimal($data['civil_depreciation_rate'] ?? 0);
        $taxRate = $this->decimal($data['tax_depreciation_rate'] ?? $civilRate);
        $disposalDate = $this->date($data['disposal_date'] ?? null);
        $status = $disposalDate ? 'DISPOSED' : 'ACTIVE';
        $q = $this->db->prepare('SELECT * FROM fixed_assets WHERE organization_id=? AND asset_code=?');
        $q->execute([$this->organizationId,$code]); $existing = $q->fetch();
        if ($existing) {
            $same = $existing['description'] === $description && $existing['purchase_date'] === $purchaseDate && abs((float)$existing['purchase_cost'] - $cost) < .01;
            return [(int)$existing['id'],$same ? 'Cespite già presente: i progressivi storici saranno raccordati.' : 'Codice cespite già presente con dati differenti: nessuna sovrascrittura.',$same ? 'APPLIED' : 'CONFLICT'];
        }
        $q = $this->db->prepare('INSERT INTO fixed_assets (organization_id,asset_code,description,category,category_id,purchase_date,in_service_date,purchase_cost,residual_value,depreciation_rate,civil_depreciation_rate,tax_depreciation_rate,first_year_percent,accumulated_depreciation,net_book_value,tax_accumulated_depreciation,tax_net_value,status,disposal_date,source_import_batch_id,created_by,updated_by) VALUES (?,?,?,?,?,?,?,?,0,?,?,?,?,0,?,0,?,?,?, ?,?,?)');
        $q->execute([$this->organizationId,$code,$description,$data['category_name'] ?? $data['category_code'] ?? null,$categoryId,$purchaseDate,$inServiceDate,$cost,$civilRate,$civilRate,$taxRate,50,$cost,$cost,$status,$disposalDate,$batchId,$this->userId,$this->userId]);
        return [(int)$this->db->lastInsertId(),'Cespite importato; saldi e movimenti vengono raccordati dai progressivi DATEV.','APPLIED'];
    }

    private function assetProgressive(string $sourceKey, array $data, int $batchId): array
    {
        $asset = $this->findAsset((string)($data['asset_code'] ?? ''));
        if (!$asset) return [null,'Cespite non trovato per il progressivo: acquisito come anomalia da riconciliare.','NEEDS_DATA'];
        $date = $this->date($data['as_of_date'] ?? null);
        if (!$date) throw new InvalidArgumentException('Data finale del progressivo cespite non valida.');
        $values = [
            $this->decimal($data['historical_cost'] ?? 0), $this->decimal($data['current_value'] ?? 0),
            $this->decimal($data['ordinary_fund'] ?? 0), $this->decimal($data['accelerated_fund'] ?? 0),
            $this->decimal($data['depreciable_residual'] ?? 0), $this->decimal($data['alienation_amount'] ?? 0),
            $this->decimal($data['elimination_amount'] ?? 0),
        ];
        $q = $this->db->prepare('SELECT id FROM datev_fixed_asset_progressives WHERE organization_id=? AND source_key=?');
        $q->execute([$this->organizationId,$sourceKey]);
        if ($id = $q->fetchColumn()) return [(int)$id,'Progressivo cespite già presente.','APPLIED'];
        $q = $this->db->prepare('INSERT INTO datev_fixed_asset_progressives (organization_id,fixed_asset_id,batch_id,source_key,as_of_date,historical_cost,current_value,ordinary_fund,accelerated_fund,depreciable_residual,alienation_amount,elimination_amount) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)');
        $q->execute(array_merge([$this->organizationId,$asset['id'],$batchId,$sourceKey,$date],$values));
        $id = (int)$this->db->lastInsertId();
        $latest = $this->db->prepare('SELECT MAX(as_of_date) FROM datev_fixed_asset_progressives WHERE organization_id=? AND fixed_asset_id=?');
        $latest->execute([$this->organizationId,$asset['id']]);
        if ($latest->fetchColumn() === $date) {
            $fund = max(0,$values[2] + $values[3]); $net = max(0,$values[4]);
            $this->db->prepare('UPDATE fixed_assets SET accumulated_depreciation=?,net_book_value=?,updated_by=?,updated_at=NOW() WHERE id=? AND organization_id=?')
                ->execute([$fund,$net,$this->userId,$asset['id'],$this->organizationId]);
        }
        return [$id,'Progressivo storico importato e saldo civile aggiornato alla data più recente.','APPLIED'];
    }

    private function assetMovement(string $sourceKey, array $data, int $batchId): array
    {
        $asset = $this->findAsset((string)($data['asset_code'] ?? ''));
        if (!$asset) return [null,'Cespite non trovato per il movimento: acquisito come anomalia da riconciliare.','NEEDS_DATA'];
        $q = $this->db->prepare('SELECT id FROM datev_fixed_asset_movements WHERE organization_id=? AND source_movement_id=?');
        $q->execute([$this->organizationId,$sourceKey]);
        if ($id = $q->fetchColumn()) return [(int)$id,'Movimento cespite già presente.','APPLIED'];
        $q = $this->db->prepare('INSERT INTO datev_fixed_asset_movements (organization_id,fixed_asset_id,batch_id,source_movement_id,cause_code,movement_date,valid_from,tax_valid_from,document_number,description,party_code,party_name) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)');
        $q->execute([$this->organizationId,$asset['id'],$batchId,$sourceKey,$data['cause_code'] ?? null,$this->date($data['document_date'] ?? null),$this->date($data['valid_from'] ?? null),$this->date($data['tax_valid_from'] ?? null),$data['document_number'] ?? null,$data['description'] ?? null,$data['party_code'] ?? null,$data['party_name'] ?? null]);
        return [(int)$this->db->lastInsertId(),'Movimento cespite importato e collegato all’anagrafica.','APPLIED'];
    }

    private function findAsset(string $code): ?array
    {
        $q = $this->db->prepare('SELECT * FROM fixed_assets WHERE organization_id=? AND asset_code=?');
        $q->execute([$this->organizationId,$code]);
        return $q->fetch() ?: null;
    }

    private function decimal(mixed $value): float
    {
        $text = trim((string)$value);
        if ($text === '') return 0.0;
        if (str_contains($text, ',')) $text = str_replace(',', '.', str_replace('.', '', $text));
        if (!is_numeric($text)) throw new InvalidArgumentException('Importo DATEV non valido: ' . $value);
        return round((float)$text, 2);
    }

    private function date(mixed $value): ?string
    {
        $text = trim((string)$value);
        if ($text === '') return null;
        foreach (['!Y-m-d','!d/m/Y'] as $format) {
            $date = \DateTimeImmutable::createFromFormat($format,$text);
            if ($date && $date->format(substr($format,1)) === $text) return $date->format('Y-m-d');
        }
        throw new InvalidArgumentException('Data DATEV non valida: ' . $text);
    }

    private function mark(int $row, string $status, string $note, ?int $entity=null): void
    {
        $this->db->prepare('UPDATE import_rows SET status=?,error_message=?,imported_entity_id=?,updated_at=NOW() WHERE id=? AND organization_id=?')->execute([$status,mb_substr($note,0,2000),$entity,$row,$this->organizationId]);
    }
}
