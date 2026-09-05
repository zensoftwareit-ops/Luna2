<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use PhpOffice\PhpSpreadsheet\IOFactory;
use RuntimeException;
use SimpleXMLElement;
use Throwable;
use ZipArchive;

final class ImportService
{
    private const TARGETS = [
        'customers', 'suppliers', 'chart_of_accounts', 'journal_entries', 'payments', 'open_items',
        'vat_movements', 'fixed_assets', 'bank_transactions', 'fatturapa',
    ];
    private const EXTENSIONS = ['csv', 'txt', 'xlsx', 'xml', 'p7m', 'zip'];

    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
        private readonly string $storagePath,
    ) {
    }

    public function upload(array $file, string $target, int $maxBytes): int
    {
        if (!in_array($target, self::TARGETS, true)) {
            throw new InvalidArgumentException('Tipo di importazione non supportato.');
        }
        if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file((string) ($file['tmp_name'] ?? ''))) {
            throw new InvalidArgumentException('Caricamento file non riuscito.');
        }
        if ((int) ($file['size'] ?? 0) <= 0 || (int) $file['size'] > $maxBytes) {
            throw new InvalidArgumentException('Dimensione del file non ammessa.');
        }
        $originalName = basename((string) ($file['name'] ?? 'import'));
        $extension = mb_strtolower(pathinfo($originalName, PATHINFO_EXTENSION));
        if (!in_array($extension, self::EXTENSIONS, true)) {
            throw new InvalidArgumentException('Formato non ammesso. Usa CSV, XLSX, XML FatturaPA o ZIP.');
        }

        $uuid = $this->uuid();
        $directory = $this->storagePath . '/imports/' . $uuid;
        if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
            throw new RuntimeException('Impossibile creare la cartella di importazione.');
        }
        $storedPath = $directory . '/original.' . $extension;
        if (!move_uploaded_file((string) $file['tmp_name'], $storedPath)) {
            throw new RuntimeException('Impossibile archiviare il file caricato.');
        }

        $statement = $this->db->prepare(
            "INSERT INTO import_batches
             (organization_id, uuid, source_system, import_type, status, original_filename, checksum_sha256, file_size,
              total_rows, valid_rows, error_rows, created_by, created_at, updated_at)
             VALUES (?, ?, 'DATEV_KOINOS', ?, 'STAGING', ?, ?, ?, 0, 0, 0, ?, NOW(), NOW())"
        );
        $statement->execute([
            $this->organizationId, $uuid, $target, $originalName, hash_file('sha256', $storedPath), filesize($storedPath), $this->userId,
        ]);
        $batchId = (int) $this->db->lastInsertId();

        try {
            $this->stageFile($batchId, $storedPath, $originalName, $target, $maxBytes, 0);
            $this->refreshCounters($batchId, 'READY');
        } catch (Throwable $exception) {
            $this->db->prepare("UPDATE import_batches SET status = 'ERROR', error_message = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([mb_substr($exception->getMessage(), 0, 2000), $batchId, $this->organizationId]);
            throw $exception;
        }
        return $batchId;
    }

    public function commit(int $batchId): array
    {
        $batch = $this->batch($batchId, true);
        if (!in_array($batch['status'], ['READY', 'ERROR'], true)) {
            throw new InvalidArgumentException('Il lotto non è pronto per l’importazione.');
        }

        $this->db->beginTransaction();
        try {
            $this->db->prepare("UPDATE import_batches SET status = 'IMPORTING', updated_at = NOW() WHERE id = ?")->execute([$batchId]);
            $statement = $this->db->prepare("SELECT * FROM import_rows WHERE batch_id = ? AND organization_id = ? AND status IN ('STAGED','VALID') ORDER BY source_file_id, source_row_number");
            $statement->execute([$batchId, $this->organizationId]);
            $rows = $statement->fetchAll();
            $result = match ($batch['import_type']) {
                'customers' => $this->commitParties($batchId, $rows, 'customers'),
                'suppliers' => $this->commitParties($batchId, $rows, 'suppliers'),
                'chart_of_accounts' => $this->commitAccounts($batchId, $rows),
                'journal_entries' => $this->commitJournal($batchId, $rows),
                'payments' => $this->commitPayments($batchId, $rows),
                'open_items' => $this->commitOpenItems($batchId, $rows),
                'vat_movements' => $this->commitVatMovements($batchId, $rows),
                'fixed_assets' => $this->commitFixedAssets($batchId, $rows),
                'bank_transactions' => $this->commitBankTransactions($batchId, $rows),
                'fatturapa' => $this->commitFatturaPa($batchId, $rows),
                default => throw new InvalidArgumentException('Importazione non gestita.'),
            };
            $status = $result['errors'] > 0 ? 'COMPLETED_WITH_ERRORS' : 'COMPLETED';
            $this->db->prepare('UPDATE import_batches SET status = ?, imported_rows = ?, error_rows = ?, completed_at = NOW(), updated_at = NOW() WHERE id = ?')
                ->execute([$status, $result['imported'], $result['errors'], $batchId]);
            $this->db->commit();
            return $result;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            $this->db->prepare("UPDATE import_batches SET status = 'ERROR', error_message = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([mb_substr($exception->getMessage(), 0, 2000), $batchId, $this->organizationId]);
            throw $exception;
        }
    }

    public function ingestRemoteInvoices(array $files, int $maxBytes): int
    {
        $uuid=$this->uuid();$directory=$this->storagePath.'/imports/'.$uuid;
        if(!is_dir($directory)&&!mkdir($directory,0750,true)&&!is_dir($directory)){throw new RuntimeException('Impossibile creare la cartella di importazione.');}
        $totalBytes=array_sum(array_map(static fn(array $file):int=>strlen((string)($file['content']??'')),$files));
        if($totalBytes<=0||$totalBytes>$maxBytes){throw new InvalidArgumentException('Dimensione complessiva delle fatture non ammessa.');}
        $this->db->prepare("INSERT INTO import_batches (organization_id,uuid,source_system,import_type,status,original_filename,checksum_sha256,file_size,total_rows,valid_rows,error_rows,created_by,created_at,updated_at) VALUES (?,?,'SDI_API','fatturapa','STAGING','Ricezione endpoint SDI',?,?,0,0,0,?,NOW(),NOW())")
            ->execute([$this->organizationId,$uuid,hash('sha256',implode('',array_column($files,'content'))),$totalBytes,$this->userId]);
        $batchId=(int)$this->db->lastInsertId();
        try{foreach($files as $index=>$file){$name=basename((string)($file['name']??('fattura-'.($index+1).'.xml')));if(mb_strtolower(pathinfo($name,PATHINFO_EXTENSION))!=='xml')continue;$path=$directory.'/'.($index+1).'.xml';if(file_put_contents($path,(string)$file['content'],LOCK_EX)===false){throw new RuntimeException('Impossibile archiviare una fattura ricevuta.');}$this->stageFile($batchId,$path,$name,'fatturapa',$maxBytes,0);} $this->refreshCounters($batchId,'READY');return $batchId;}
        catch(Throwable $exception){$this->db->prepare("UPDATE import_batches SET status='ERROR',error_message=?,updated_at=NOW() WHERE id=?")->execute([mb_substr($exception->getMessage(),0,2000),$batchId]);throw $exception;}
    }

    public function rollback(int $batchId): int
    {
        $batch = $this->batch($batchId, true);
        if (!in_array($batch['status'], ['COMPLETED', 'COMPLETED_WITH_ERRORS'], true)) {
            throw new InvalidArgumentException('Questo lotto non può essere annullato.');
        }
        $allowedTables = [
            'customers', 'suppliers', 'chart_of_accounts', 'journal_entries', 'payments', 'documents',
            'accounting_open_items', 'vat_movements', 'fixed_assets', 'bank_transactions',
        ];
        $statement = $this->db->prepare('SELECT * FROM import_records WHERE batch_id = ? AND organization_id = ? ORDER BY id DESC');
        $statement->execute([$batchId, $this->organizationId]);
        $records = $statement->fetchAll();
        $count = 0;

        $this->db->beginTransaction();
        try {
            foreach ($records as $record) {
                $table = (string) $record['entity_type'];
                if (!in_array($table, $allowedTables, true)) {
                    continue;
                }
                if ($record['operation'] === 'CREATE') {
                    $delete = $this->db->prepare("DELETE FROM `{$table}` WHERE id = ? AND organization_id = ?");
                    $delete->execute([$record['entity_id'], $this->organizationId]);
                    $count += $delete->rowCount();
                } elseif ($record['operation'] === 'UPDATE' && $record['before_data_json']) {
                    $before = json_decode((string) $record['before_data_json'], true, 512, JSON_THROW_ON_ERROR);
                    $allowed = $this->restorableColumns($table);
                    $values = array_intersect_key($before, array_flip($allowed));
                    if ($values === []) {
                        continue;
                    }
                    $sets = implode(', ', array_map(static fn (string $column): string => "`{$column}` = ?", array_keys($values)));
                    $update = $this->db->prepare("UPDATE `{$table}` SET {$sets}, updated_at = NOW() WHERE id = ? AND organization_id = ?");
                    $update->execute(array_merge(array_values($values), [$record['entity_id'], $this->organizationId]));
                    $count += $update->rowCount();
                }
            }
            $this->db->prepare("UPDATE import_batches SET status = 'ROLLED_BACK', rolled_back_at = NOW(), updated_at = NOW() WHERE id = ?")->execute([$batchId]);
            $this->db->commit();
            return $count;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function stageFile(int $batchId, string $path, string $name, string $target, int $maxBytes, int $depth): void
    {
        if ($depth > 2) {
            throw new InvalidArgumentException('Archivio ZIP annidato oltre il limite.');
        }
        $extension = mb_strtolower(pathinfo($name, PATHINFO_EXTENSION));
        $fileStatement = $this->db->prepare(
            'INSERT INTO import_files (organization_id, batch_id, filename, media_type, checksum_sha256, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())'
        );
        $fileStatement->execute([$this->organizationId, $batchId, basename($name), $extension, hash_file('sha256', $path), filesize($path)]);
        $fileId = (int) $this->db->lastInsertId();

        if ($extension === 'zip') {
            $zip = new ZipArchive();
            if ($zip->open($path) !== true) {
                throw new InvalidArgumentException('Archivio ZIP non leggibile.');
            }
            $total = 0;
            for ($index = 0; $index < $zip->numFiles; $index++) {
                $stat = $zip->statIndex($index);
                $entryName = (string) ($stat['name'] ?? '');
                if ($entryName === '' || str_ends_with($entryName, '/') || str_contains($entryName, '..') || str_starts_with($entryName, '/') || preg_match('/^[A-Za-z]:/', $entryName)) {
                    continue;
                }
                $total += (int) ($stat['size'] ?? 0);
                if ($total > $maxBytes * 4) {
                    $zip->close();
                    throw new InvalidArgumentException('Archivio ZIP troppo grande dopo la decompressione.');
                }
                $entryExtension = mb_strtolower(pathinfo($entryName, PATHINFO_EXTENSION));
                if (!in_array($entryExtension, self::EXTENSIONS, true)) {
                    continue;
                }
                $content = $zip->getFromIndex($index);
                if ($content === false) {
                    continue;
                }
                $childPath = dirname($path) . '/entry-' . $index . '.' . $entryExtension;
                file_put_contents($childPath, $content, LOCK_EX);
                $this->stageFile($batchId, $childPath, basename($entryName), $target, $maxBytes, $depth + 1);
            }
            $zip->close();
            return;
        }

        if ($extension === 'xml') {
            $this->stageFatturaPa($batchId, $fileId, $path);
            return;
        }
        if ($extension === 'p7m') {
            $xml = $this->extractXmlFromP7m((string) file_get_contents($path));
            if ($xml === null) {
                throw new InvalidArgumentException('Contenuto XML non estraibile dal file P7M. Verifica che sia una FatturaPA firmata CAdES.');
            }
            $xmlPath = dirname($path) . '/extracted-' . $fileId . '.xml';
            if (file_put_contents($xmlPath, $xml, LOCK_EX) === false) { throw new RuntimeException('Impossibile archiviare l’XML estratto.'); }
            $this->stageFatturaPa($batchId, $fileId, $xmlPath);
            return;
        }
        if ($extension === 'xlsx') {
            $this->stageSpreadsheet($batchId, $fileId, $path);
            return;
        }
        $this->stageCsv($batchId, $fileId, $path);
    }

    private function stageCsv(int $batchId, int $fileId, string $path): void
    {
        $handle = fopen($path, 'rb');
        if (!$handle) {
            throw new RuntimeException('File CSV non leggibile.');
        }
        $sample = (string) fgets($handle);
        rewind($handle);
        $delimiter = $this->detectDelimiter($sample);
        $headers = fgetcsv($handle, 0, $delimiter);
        if (!$headers) {
            fclose($handle);
            throw new InvalidArgumentException('Intestazione CSV assente.');
        }
        $headers = array_map([$this, 'normalizeHeader'], $headers);
        $rowNumber = 1;
        while (($values = fgetcsv($handle, 0, $delimiter)) !== false) {
            $rowNumber++;
            if ($rowNumber > 200001) {
                fclose($handle);
                throw new InvalidArgumentException('Il singolo file supera 200.000 righe.');
            }
            $values = array_pad($values, count($headers), null);
            $row = array_combine($headers, array_slice($values, 0, count($headers))) ?: [];
            if (count(array_filter($row, static fn (mixed $value): bool => trim((string) $value) !== '')) === 0) {
                continue;
            }
            $this->insertStagedRow($batchId, $fileId, $rowNumber, $row, $row);
        }
        fclose($handle);
    }

    private function extractXmlFromP7m(string $payload): ?string
    {
        $start = strpos($payload, '<?xml');
        if ($start === false) { $start = strpos($payload, '<p:FatturaElettronica'); }
        if ($start === false) { $start = strpos($payload, '<FatturaElettronica'); }
        if ($start === false) { return null; }
        foreach (['</p:FatturaElettronica>', '</FatturaElettronica>'] as $closing) {
            $end = strpos($payload, $closing, $start);
            if ($end !== false) { return substr($payload, $start, $end + strlen($closing) - $start); }
        }
        return null;
    }

    private function stageSpreadsheet(int $batchId, int $fileId, string $path): void
    {
        $reader = IOFactory::createReaderForFile($path);
        $reader->setReadDataOnly(true);
        $spreadsheet = $reader->load($path);
        foreach ($spreadsheet->getWorksheetIterator() as $sheet) {
            $rows = $sheet->toArray(null, true, true, false);
            if ($rows === []) {
                continue;
            }
            $headers = array_map([$this, 'normalizeHeader'], array_shift($rows));
            foreach ($rows as $index => $values) {
                $values = array_pad($values, count($headers), null);
                $row = array_combine($headers, array_slice($values, 0, count($headers))) ?: [];
                if (count(array_filter($row, static fn (mixed $value): bool => trim((string) $value) !== '')) === 0) {
                    continue;
                }
                $row['_sheet'] = $sheet->getTitle();
                $this->insertStagedRow($batchId, $fileId, $index + 2, $row, $row);
            }
        }
        $spreadsheet->disconnectWorksheets();
    }

    private function stageFatturaPa(int $batchId, int $fileId, string $path): void
    {
        libxml_use_internal_errors(true);
        $xml = simplexml_load_file($path, SimpleXMLElement::class, LIBXML_NONET | LIBXML_NOBLANKS);
        if (!$xml) {
            throw new InvalidArgumentException('XML non valido: ' . basename($path));
        }
        $header = $this->xpathOne($xml, '//*[local-name()="FatturaElettronicaHeader"]');
        if (!$header) {
            throw new InvalidArgumentException('Il file XML non è una FatturaPA riconosciuta.');
        }
        $issuerVat = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]//*[local-name()="IdFiscaleIVA"]/*[local-name()="IdCodice"]');
        $issuerName = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]//*[local-name()="Anagrafica"]/*[local-name()="Denominazione"]');
        if ($issuerName === '') {
            $issuerName = trim($this->xpathText($header, './/*[local-name()="CedentePrestatore"]//*[local-name()="Anagrafica"]/*[local-name()="Nome"]') . ' ' . $this->xpathText($header, './/*[local-name()="CedentePrestatore"]//*[local-name()="Anagrafica"]/*[local-name()="Cognome"]'));
        }
        $issuerTaxCode = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]//*[local-name()="DatiAnagrafici"]/*[local-name()="CodiceFiscale"]');
        $issuerAddress = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]/*[local-name()="Sede"]/*[local-name()="Indirizzo"]');
        $issuerPostalCode = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]/*[local-name()="Sede"]/*[local-name()="CAP"]');
        $issuerCity = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]/*[local-name()="Sede"]/*[local-name()="Comune"]');
        $issuerProvince = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]/*[local-name()="Sede"]/*[local-name()="Provincia"]');
        $issuerCountry = $this->xpathText($header, './/*[local-name()="CedentePrestatore"]/*[local-name()="Sede"]/*[local-name()="Nazione"]') ?: 'IT';
        $recipientVat = $this->xpathText($header, './/*[local-name()="CessionarioCommittente"]//*[local-name()="IdFiscaleIVA"]/*[local-name()="IdCodice"]');
        $recipientName = $this->xpathText($header, './/*[local-name()="CessionarioCommittente"]//*[local-name()="Anagrafica"]/*[local-name()="Denominazione"]');
        $organizationVat = (string) $this->db->query('SELECT vat_number FROM organizations WHERE id = ' . (int) $this->organizationId)->fetchColumn();
        $direction = preg_replace('/\D/', '', $issuerVat) === preg_replace('/\D/', '', $organizationVat) ? 'SALES_INVOICE' : 'PURCHASE_INVOICE';
        $bodies = $xml->xpath('//*[local-name()="FatturaElettronicaBody"]') ?: [];
        foreach ($bodies as $index => $body) {
            $general = $this->xpathOne($body, './/*[local-name()="DatiGeneraliDocumento"]');
            $number = $this->xpathText($general, './*[local-name()="Numero"]');
            $date = $this->xpathText($general, './*[local-name()="Data"]');
            $type = $this->xpathText($general, './*[local-name()="TipoDocumento"]');
            $total = $this->decimal($this->xpathText($general, './*[local-name()="ImportoTotaleDocumento"]'));
            $withholdingNode = $this->xpathOne($general, './*[local-name()="DatiRitenuta"]');
            $paymentNode = $this->xpathOne($body, './/*[local-name()="DatiPagamento"]/*[local-name()="DettaglioPagamento"]');
            $lines = [];
            foreach (($body->xpath('.//*[local-name()="DettaglioLinee"]') ?: []) as $line) {
                $quantity = $this->decimal($this->xpathText($line, './*[local-name()="Quantita"]') ?: '1');
                $unitPrice = $this->decimal($this->xpathText($line, './*[local-name()="PrezzoUnitario"]'));
                $taxable = $this->decimal($this->xpathText($line, './*[local-name()="PrezzoTotale"]'));
                $vatRate = $this->decimal($this->xpathText($line, './*[local-name()="AliquotaIVA"]'));
                $lines[] = [
                    'description' => $this->xpathText($line, './*[local-name()="Descrizione"]'),
                    'quantity' => $quantity, 'unit' => $this->xpathText($line, './*[local-name()="UnitaMisura"]') ?: 'NR',
                    'unit_price' => $unitPrice, 'taxable_amount' => $taxable, 'vat_rate' => $vatRate,
                    'vat_nature' => $this->xpathText($line, './*[local-name()="Natura"]') ?: null,
                    'vat_amount' => round($taxable * $vatRate / 100, 2),
                ];
            }
            $normalized = [
                'document_type' => in_array($type, ['TD04', 'TD08'], true) && $direction === 'SALES_INVOICE' ? 'CREDIT_NOTE' : $direction,
                'fatturapa_type' => $type, 'number' => $number, 'document_date' => $date,
                'currency' => $this->xpathText($general, './*[local-name()="Divisa"]') ?: 'EUR', 'total' => $total,
                'issuer_vat' => $issuerVat, 'issuer_name' => $issuerName, 'recipient_vat' => $recipientVat, 'recipient_name' => $recipientName,
                'counterparty_vat' => $direction === 'SALES_INVOICE' ? $recipientVat : $issuerVat,
                'counterparty_name' => $direction === 'SALES_INVOICE' ? $recipientName : $issuerName,
                'counterparty_tax_code' => $direction === 'PURCHASE_INVOICE' ? $issuerTaxCode : null,
                'counterparty_address' => $direction === 'PURCHASE_INVOICE' ? $issuerAddress : null,
                'counterparty_postal_code' => $direction === 'PURCHASE_INVOICE' ? $issuerPostalCode : null,
                'counterparty_city' => $direction === 'PURCHASE_INVOICE' ? $issuerCity : null,
                'counterparty_province' => $direction === 'PURCHASE_INVOICE' ? $issuerProvince : null,
                'counterparty_country' => $direction === 'PURCHASE_INVOICE' ? $issuerCountry : 'IT',
                'due_date' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="DataScadenzaPagamento"]') ?: null) : null,
                'payment_method_code' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="ModalitaPagamento"]') ?: null) : null,
                'bank_name' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="IstitutoFinanziario"]') ?: null) : null,
                'bank_iban' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="IBAN"]') ?: null) : null,
                'bank_abi' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="ABI"]') ?: null) : null,
                'bank_cab' => $paymentNode ? ($this->xpathText($paymentNode, './*[local-name()="CAB"]') ?: null) : null,
                'withholding_type' => $withholdingNode ? ($this->xpathText($withholdingNode, './*[local-name()="TipoRitenuta"]') ?: null) : null,
                'withholding_amount' => $withholdingNode ? $this->decimal($this->xpathText($withholdingNode, './*[local-name()="ImportoRitenuta"]')) : 0,
                'withholding_rate' => $withholdingNode ? $this->decimal($this->xpathText($withholdingNode, './*[local-name()="AliquotaRitenuta"]')) : 0,
                'withholding_cause' => $withholdingNode ? ($this->xpathText($withholdingNode, './*[local-name()="CausalePagamento"]') ?: null) : null,
                'lines' => $lines,
            ];
            $this->insertStagedRow($batchId, $fileId, $index + 1, ['xml_file' => basename($path)], $normalized);
        }
    }

    private function commitParties(int $batchId, array $rows, string $table): array
    {
        $imported = 0; $errors = 0;
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $data = [
                'code' => $this->pick($row, ['code', 'codice', 'codice_cliente', 'codice_fornitore']),
                'business_name' => $this->pick($row, ['business_name', 'ragione_sociale', 'denominazione', 'nominativo', 'nome']),
                'vat_number' => $this->pick($row, ['vat_number', 'partita_iva', 'piva', 'partitaiva']),
                'tax_code' => $this->pick($row, ['tax_code', 'codice_fiscale', 'cf']),
                'sdi_code' => $this->pick($row, ['sdi_code', 'codice_sdi', 'codice_destinatario']),
                'pec' => $this->pick($row, ['pec']), 'email' => $this->pick($row, ['email', 'mail']),
                'phone' => $this->pick($row, ['phone', 'telefono', 'tel']), 'address' => $this->pick($row, ['address', 'indirizzo']),
                'postal_code' => $this->pick($row, ['postal_code', 'cap']), 'city' => $this->pick($row, ['city', 'citta', 'comune']),
                'province' => $this->pick($row, ['province', 'provincia', 'prov']), 'country_code' => $this->pick($row, ['country_code', 'paese', 'nazione']) ?: 'IT',
                'iban' => $this->pick($row, ['iban']), 'payment_terms' => $this->pick($row, ['payment_terms', 'condizioni_pagamento']),
            ];
            $data['country_code'] = strtoupper((string) ($data['country_code'] ?: 'IT'));
            $data['vat_number'] = PartyAutomationService::normalizeVat($data['vat_number'], $data['country_code']);
            if (!$data['business_name']) {
                $this->markRow($staged['id'], 'ERROR', 'Ragione sociale mancante.'); $errors++; continue;
            }
            $existing = null;
            if ($data['vat_number']) {
                $find = $this->db->prepare("SELECT * FROM {$table} WHERE organization_id = ? AND vat_number = ? LIMIT 1");
                $find->execute([$this->organizationId, $data['vat_number']]);
                $existing = $find->fetch();
            }
            if (!$existing && $data['code']) {
                $find = $this->db->prepare("SELECT * FROM {$table} WHERE organization_id = ? AND code = ? LIMIT 1");
                $find->execute([$this->organizationId, $data['code']]);
                $existing = $find->fetch();
            }
            if ($existing) {
                $columns = array_keys($data);
                $sets = implode(', ', array_map(static fn (string $column): string => "`{$column}` = ?", $columns));
                $update = $this->db->prepare("UPDATE {$table} SET {$sets}, active = 1, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?");
                $update->execute(array_merge(array_values($data), [$this->userId, $existing['id'], $this->organizationId]));
                $entityId = (int) $existing['id'];
                $this->recordImport($batchId, $table, $entityId, 'UPDATE', $existing);
            } else {
                $columns = array_keys($data);
                $insert = $this->db->prepare("INSERT INTO {$table} (organization_id, " . implode(',', array_map(static fn ($c) => "`{$c}`", $columns)) . ', active, source_import_batch_id, created_by, updated_by, created_at, updated_at) VALUES (?, ' . implode(',', array_fill(0, count($columns), '?')) . ', 1, ?, ?, ?, NOW(), NOW())');
                $insert->execute(array_merge([$this->organizationId], array_values($data), [$batchId, $this->userId, $this->userId]));
                $entityId = (int) $this->db->lastInsertId();
                $this->recordImport($batchId, $table, $entityId, 'CREATE', null);
            }
            $this->markRow($staged['id'], 'IMPORTED', null, $entityId); $imported++;
        }
        return compact('imported', 'errors');
    }

    private function commitAccounts(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        $parents = [];
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $code = $this->pick($row, ['code', 'codice', 'codice_conto', 'conto']);
            $name = $this->pick($row, ['name', 'descrizione', 'descrizione_conto', 'denominazione']);
            if (!$code || !$name) {
                $this->markRow($staged['id'], 'ERROR', 'Codice o descrizione conto mancanti.'); $errors++; continue;
            }
            $typeRaw = mb_strtoupper($this->pick($row, ['account_type', 'tipo', 'natura']) ?: 'EXPENSE');
            $type = match (true) {
                str_contains($typeRaw, 'ATTIV') || str_contains($typeRaw, 'ASSET') => 'ASSET',
                str_contains($typeRaw, 'PASSIV') || str_contains($typeRaw, 'LIABIL') => 'LIABILITY',
                str_contains($typeRaw, 'PATRIM') || str_contains($typeRaw, 'EQUIT') => 'EQUITY',
                str_contains($typeRaw, 'RICAV') || str_contains($typeRaw, 'REVEN') => 'REVENUE',
                default => 'EXPENSE',
            };
            $normalRaw = mb_strtoupper($this->pick($row, ['normal_balance', 'saldo_naturale', 'segno']) ?: '');
            $normal = str_contains($normalRaw, 'AVER') || str_contains($normalRaw, 'CREDIT')
                ? 'CREDIT'
                : (str_contains($normalRaw, 'DAR') || str_contains($normalRaw, 'DEBIT')
                    ? 'DEBIT' : (in_array($type, ['LIABILITY','EQUITY','REVENUE'], true) ? 'CREDIT' : 'DEBIT'));
            $classification = $this->pick($row, ['classification_code', 'classificazione', 'codice_classificazione']);
            $section = $this->pick($row, ['statement_section', 'sezione_bilancio', 'bilancio']);
            $taxMapping = $this->pick($row, ['tax_mapping_code', 'codice_fiscale', 'rigo_dichiarazione']);
            $postableRaw = mb_strtoupper($this->pick($row, ['is_postable', 'movimentabile', 'dettaglio']) ?: '1');
            $postable = in_array($postableRaw, ['0','NO','N','FALSE','RAGGRUPPAMENTO'], true) ? 0 : 1;
            $find = $this->db->prepare('SELECT * FROM chart_of_accounts WHERE organization_id = ? AND code = ?');
            $find->execute([$this->organizationId, $code]);
            $existing = $find->fetch();
            if ($existing && !empty($existing['locked'])) {
                $this->markRow($staged['id'], 'SKIPPED', 'Conto di sistema protetto.', (int) $existing['id']);
                continue;
            }
            if ($existing) {
                $this->db->prepare('UPDATE chart_of_accounts SET name = ?, account_type = ?, normal_balance = ?, classification_code = ?, statement_section = ?, tax_mapping_code = ?, is_postable = ?, updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                    ->execute([$name, $type, $normal, $classification, $section, $taxMapping, $postable, $this->userId, $existing['id'], $this->organizationId]);
                $entityId = (int) $existing['id'];
                $this->recordImport($batchId, 'chart_of_accounts', $entityId, 'UPDATE', $existing);
            } else {
                $this->db->prepare('INSERT INTO chart_of_accounts (organization_id, code, name, account_type, normal_balance, classification_code, statement_section, tax_mapping_code, is_postable, active, source_import_batch_id, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, NOW(), NOW())')
                    ->execute([$this->organizationId, $code, $name, $type, $normal, $classification, $section, $taxMapping, $postable, $batchId, $this->userId, $this->userId]);
                $entityId = (int) $this->db->lastInsertId();
                $this->recordImport($batchId, 'chart_of_accounts', $entityId, 'CREATE', null);
            }
            $parentCode = $this->pick($row, ['parent_code', 'codice_padre', 'conto_padre', 'raggruppamento']);
            if ($parentCode) {
                $parents[$entityId] = $parentCode;
            }
            $this->markRow($staged['id'], 'IMPORTED', null, $entityId); $imported++;
        }
        foreach ($parents as $accountId => $parentCode) {
            $find = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE organization_id = ? AND code = ?');
            $find->execute([$this->organizationId, $parentCode]);
            $parentId = (int) $find->fetchColumn();
            if ($parentId > 0 && $parentId !== $accountId) {
                $this->db->prepare('UPDATE chart_of_accounts SET parent_id = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
                    ->execute([$parentId, $accountId, $this->organizationId]);
            }
        }
        return compact('imported', 'errors');
    }

    private function commitJournal(int $batchId, array $rows): array
    {
        $groups = [];
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $protocol = $this->pick($row, ['protocol', 'protocollo', 'numero_registrazione', 'registrazione']);
            if (!$protocol) {
                $this->markRow($staged['id'], 'ERROR', 'Protocollo registrazione mancante.');
                continue;
            }
            $groups[$protocol][] = [$staged, $row];
        }
        $imported = 0; $errors = count($rows) - array_sum(array_map('count', $groups));
        $service = new AccountingService($this->db, $this->organizationId, $this->userId);
        foreach ($groups as $protocol => $items) {
            $this->db->exec('SAVEPOINT import_journal_group');
            $lines = [];
            $groupError = null;
            foreach ($items as [$staged, $row]) {
                $accountCode = $this->pick($row, ['account_code', 'codice_conto', 'conto']);
                $find = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE organization_id = ? AND code = ?');
                $find->execute([$this->organizationId, $accountCode]);
                $accountId = (int) $find->fetchColumn();
                if (!$accountId) {
                    $groupError = 'Conto non trovato: ' . $accountCode;
                    break;
                }
                $lines[] = [
                    'account_id' => $accountId,
                    'debit' => $this->decimal($this->pick($row, ['debit', 'dare', 'importo_dare']) ?: 0),
                    'credit' => $this->decimal($this->pick($row, ['credit', 'avere', 'importo_avere']) ?: 0),
                    'description' => $this->pick($row, ['line_description', 'descrizione_riga', 'descrizione']),
                ];
            }
            if ($groupError !== null) {
                foreach ($items as [$staged]) {
                    $this->markRow($staged['id'], 'ERROR', $groupError);
                    $errors++;
                }
                continue;
            }
            $first = $items[0][1];
            try {
                $entryId = $service->postManual([
                    'entry_date' => $this->dateValue($this->pick($first, ['entry_date', 'data', 'data_registrazione'])),
                    'competence_date' => $this->dateValue($this->pick($first, ['competence_date', 'data_competenza'])),
                    'entry_type' => 'DATEV_IMPORT',
                    'description' => $this->pick($first, ['entry_description', 'causale', 'descrizione']) ?: 'Import DATEV ' . $protocol,
                    'document_number' => $this->pick($first, ['document_number', 'numero_documento']),
                    'source_type' => 'DATEV_IMPORT', 'source_id' => null,
                    'counterparty' => $this->pick($first, ['counterparty', 'controparte', 'nominativo']), 'notes' => 'Protocollo origine: ' . $protocol,
                ], $lines);
                $this->recordImport($batchId, 'journal_entries', $entryId, 'CREATE', null);
                foreach ($items as [$staged]) {
                    $this->markRow($staged['id'], 'IMPORTED', null, $entryId); $imported++;
                }
            } catch (Throwable $exception) {
                $this->db->exec('ROLLBACK TO SAVEPOINT import_journal_group');
                foreach ($items as [$staged]) {
                    $this->markRow($staged['id'], 'ERROR', $exception->getMessage()); $errors++;
                }
            }
        }
        return compact('imported', 'errors');
    }

    private function commitPayments(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $documentNumber = $this->pick($row, ['document_number', 'numero_documento', 'fattura']);
            $amount = $this->decimal($this->pick($row, ['amount', 'importo']) ?: 0);
            $date = $this->dateValue($this->pick($row, ['payment_date', 'data_pagamento', 'data']));
            if ($amount <= 0 || !$date) {
                $this->markRow($staged['id'], 'ERROR', 'Data o importo del pagamento non validi.'); $errors++; continue;
            }
            $documentId = null;
            if ($documentNumber) {
                $find = $this->db->prepare('SELECT id FROM documents WHERE organization_id = ? AND number = ? ORDER BY id DESC LIMIT 1');
                $find->execute([$this->organizationId, $documentNumber]);
                $documentId = $find->fetchColumn() ?: null;
            }
            $direction = mb_strtoupper($this->pick($row, ['payment_type', 'tipo', 'direzione']) ?: 'RECEIPT');
            $paymentType = str_contains($direction, 'PAG') || str_contains($direction, 'OUT') ? 'PAYMENT' : 'RECEIPT';
            $this->db->prepare('INSERT INTO payments (organization_id, document_id, payment_type, payment_date, amount, currency, bank_amount, method, status, reference_number, bank_name, description, reconciled, source_import_batch_id, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, \'POSTED\', ?, ?, ?, 0, ?, ?, ?, NOW(), NOW())')
                ->execute([$this->organizationId, $documentId, $paymentType, $date, $amount, mb_strtoupper($this->pick($row, ['currency', 'valuta']) ?: 'EUR'), $amount, $this->pick($row, ['method', 'metodo', 'modalita']) ?: 'OTHER', $this->pick($row, ['reference', 'riferimento', 'cro']) ?: $documentNumber, $this->pick($row, ['bank', 'banca']), $this->pick($row, ['description', 'descrizione', 'causale']), $batchId, $this->userId, $this->userId]);
            $entityId = (int) $this->db->lastInsertId();
            $this->recordImport($batchId, 'payments', $entityId, 'CREATE', null);
            $this->markRow($staged['id'], 'IMPORTED', null, $entityId); $imported++;
        }
        return compact('imported', 'errors');
    }

    private function commitOpenItems(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $directionRaw = mb_strtoupper($this->pick($row, ['direction', 'tipo', 'segno', 'partita_tipo']) ?: 'RECEIVABLE');
            $direction = str_contains($directionRaw, 'DEB') || str_contains($directionRaw, 'PAY') || str_contains($directionRaw, 'FORN') ? 'PAYABLE' : 'RECEIVABLE';
            $partyName = $this->pick($row, ['party_name', 'controparte', 'nominativo', 'ragione_sociale', 'cliente_fornitore']);
            $issueDate = $this->dateValue($this->pick($row, ['issue_date', 'data_documento', 'data']));
            $dueDate = $this->dateValue($this->pick($row, ['due_date', 'scadenza', 'data_scadenza'])) ?: $issueDate;
            $original = abs($this->decimal($this->pick($row, ['original_amount', 'importo_originario', 'importo']) ?: 0));
            $settled = min($original, abs($this->decimal($this->pick($row, ['settled_amount', 'pagato', 'incassato']) ?: 0)));
            $accountCode = $this->pick($row, ['account_code', 'codice_conto', 'conto']);
            $account = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE organization_id = ? AND (code = ? OR system_key = ?) ORDER BY system_key IS NOT NULL DESC LIMIT 1');
            $account->execute([$this->organizationId, $accountCode, $direction === 'RECEIVABLE' ? 'TRADE_RECEIVABLES' : 'TRADE_PAYABLES']);
            $accountId = (int) $account->fetchColumn();
            if (!$partyName || !$issueDate || !$dueDate || $original <= 0 || !$accountId) {
                $this->markRow($staged['id'], 'ERROR', 'Controparte, date, importo o conto della partita non validi.'); $errors++; continue;
            }
            $status = $settled >= $original - .005 ? 'SETTLED' : ($settled > .005 ? 'PARTIAL' : ($dueDate < date('Y-m-d') ? 'OVERDUE' : 'OPEN'));
            $statement = $this->db->prepare(
                'INSERT INTO accounting_open_items
                 (organization_id, direction, party_type, party_name, account_id, reference, issue_date, due_date,
                  original_amount, settled_amount, currency, status, source_import_batch_id, created_by, updated_by, created_at, updated_at)
                 VALUES (?, ?, \'OTHER\', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            );
            $statement->execute([$this->organizationId, $direction, $partyName, $accountId, $this->pick($row, ['reference', 'riferimento', 'numero_documento', 'fattura']), $issueDate, $dueDate, $original, $settled, mb_strtoupper($this->pick($row, ['currency', 'valuta']) ?: 'EUR'), $status, $batchId, $this->userId, $this->userId]);
            $id = (int) $this->db->lastInsertId();
            $this->recordImport($batchId, 'accounting_open_items', $id, 'CREATE', null);
            $this->markRow($staged['id'], 'IMPORTED', null, $id); $imported++;
        }
        return compact('imported', 'errors');
    }

    private function commitVatMovements(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        $service = new VatService($this->db, $this->organizationId, $this->userId);
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $registerRaw = mb_strtoupper($this->pick($row, ['register_type', 'registro', 'tipo_registro']) ?: 'SALES');
            $register = str_contains($registerRaw, 'ACQ') || str_contains($registerRaw, 'PUR') ? 'PURCHASES' : (str_contains($registerRaw, 'CORR') ? 'CORRISPETTIVI' : 'SALES');
            $data = [
                'register_type' => $register,
                'movement_date' => $this->dateValue($this->pick($row, ['movement_date', 'data_registrazione', 'data'])),
                'protocol_number' => $this->pick($row, ['protocol_number', 'protocollo', 'numero_registrazione']),
                'counterparty_name' => $this->pick($row, ['counterparty_name', 'controparte', 'nominativo']),
                'description' => $this->pick($row, ['description', 'descrizione', 'causale']),
                'vat_code' => $this->pick($row, ['vat_code', 'codice_iva', 'aliquota']) ?: 'N/D',
                'vat_rate' => $this->pick($row, ['vat_rate', 'percentuale_iva', 'aliquota_percentuale', 'aliquota']),
                'vat_nature' => $this->pick($row, ['vat_nature', 'natura_iva', 'natura']),
                'taxable_amount' => $this->pick($row, ['taxable_amount', 'imponibile']) ?: 0,
                'vat_amount' => $this->pick($row, ['vat_amount', 'imposta', 'iva']) ?: 0,
                'vat_due_amount' => $this->pick($row, ['vat_due_amount', 'iva_dovuta']),
                'deductible_vat' => $this->pick($row, ['deductible_vat', 'iva_detraibile']),
                'deductibility_percent' => $this->pick($row, ['deductibility_percent', 'percentuale_detraibilita']) ?: 100,
                'operation_type' => mb_strtoupper($this->pick($row, ['operation_type', 'tipo_operazione']) ?: 'DOMESTIC'),
                'collectability' => mb_strtoupper($this->pick($row, ['collectability', 'esigibilita']) ?: 'IMMEDIATE'),
            ];
            try {
                $id = $service->saveManual($data);
                $this->db->prepare("UPDATE vat_movements SET source_type = 'IMPORT', source_import_batch_id = ? WHERE id = ? AND organization_id = ?")
                    ->execute([$batchId, $id, $this->organizationId]);
                $this->recordImport($batchId, 'vat_movements', $id, 'CREATE', null);
                $this->markRow($staged['id'], 'IMPORTED', null, $id); $imported++;
            } catch (Throwable $exception) {
                $this->markRow($staged['id'], 'ERROR', $exception->getMessage()); $errors++;
            }
        }
        return compact('imported', 'errors');
    }

    private function commitFixedAssets(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        $service = new AssetService($this->db, $this->organizationId, $this->userId);
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $this->db->exec('SAVEPOINT import_fixed_asset_row');
            try {
                $id = $service->saveAsset([
                    'asset_code' => $this->pick($row, ['asset_code', 'codice_cespite', 'codice']),
                    'description' => $this->pick($row, ['description', 'descrizione']),
                    'category' => $this->pick($row, ['category', 'categoria']),
                    'purchase_date' => $this->dateValue($this->pick($row, ['purchase_date', 'data_acquisto', 'data'])),
                    'in_service_date' => $this->dateValue($this->pick($row, ['in_service_date', 'entrata_in_funzione'])),
                    'purchase_cost' => $this->pick($row, ['purchase_cost', 'costo_storico', 'costo']),
                    'residual_value' => $this->pick($row, ['residual_value', 'valore_residuo']) ?: 0,
                    'civil_depreciation_rate' => $this->pick($row, ['civil_depreciation_rate', 'aliquota_civile', 'aliquota']),
                    'tax_depreciation_rate' => $this->pick($row, ['tax_depreciation_rate', 'aliquota_fiscale', 'aliquota']),
                    'first_year_percent' => $this->pick($row, ['first_year_percent', 'percentuale_primo_anno']) ?: 50,
                    'status' => 'ACTIVE',
                ]);
                $cost = $this->decimal($this->pick($row, ['purchase_cost', 'costo_storico', 'costo']) ?: 0);
                $civilFund = max(0, $this->decimal($this->pick($row, ['accumulated_depreciation', 'fondo_ammortamento', 'fondo_civile']) ?: 0));
                $taxFund = max(0, $this->decimal($this->pick($row, ['tax_accumulated_depreciation', 'fondo_ammortamento_fiscale', 'fondo_fiscale']) ?: $civilFund));
                $civilNetSource = $this->pick($row, ['net_book_value', 'valore_netto_contabile', 'residuo_civile']);
                $taxNetSource = $this->pick($row, ['tax_net_value', 'valore_netto_fiscale', 'residuo_fiscale']);
                $civilNet = $civilNetSource !== null && $civilNetSource !== '' ? $this->decimal($civilNetSource) : $cost - $civilFund;
                $taxNet = $taxNetSource !== null && $taxNetSource !== '' ? $this->decimal($taxNetSource) : $cost - $taxFund;
                if ($civilFund > $cost + .005 || $taxFund > $cost + .005 || $civilNet < -.005 || $taxNet < -.005
                    || abs(($civilFund + $civilNet) - $cost) > .02 || abs(($taxFund + $taxNet) - $cost) > .02) {
                    throw new InvalidArgumentException('Fondi ammortamento e valori netti del cespite non sono coerenti con il costo storico.');
                }
                $this->db->prepare(
                    'UPDATE fixed_assets SET accumulated_depreciation = ?, net_book_value = ?,
                     tax_accumulated_depreciation = ?, tax_net_value = ?, source_import_batch_id = ?, updated_at = NOW()
                     WHERE id = ? AND organization_id = ?'
                )->execute([$civilFund, $civilNet, $taxFund, $taxNet, $batchId, $id, $this->organizationId]);
                $this->recordImport($batchId, 'fixed_assets', $id, 'CREATE', null);
                $this->markRow($staged['id'], 'IMPORTED', null, $id); $imported++;
            } catch (Throwable $exception) {
                $this->db->exec('ROLLBACK TO SAVEPOINT import_fixed_asset_row');
                $this->markRow($staged['id'], 'ERROR', $exception->getMessage()); $errors++;
            }
        }
        return compact('imported', 'errors');
    }

    private function commitBankTransactions(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        $service = new BankingService($this->db, $this->organizationId, $this->userId);
        foreach ($rows as $staged) {
            $row = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            $bankName = trim((string) ($this->pick($row, ['bank_account', 'conto_bancario', 'banca']) ?: ''));
            $bankKey = strtoupper(str_replace(' ', '', (string) ($this->pick($row, ['iban']) ?: $bankName)));
            $find = $this->db->prepare('SELECT id FROM bank_accounts WHERE organization_id = ? AND (REPLACE(iban, \' \', \'\') = ? OR UPPER(name) = ?) LIMIT 1');
            $find->execute([$this->organizationId, $bankKey, mb_strtoupper($bankName)]);
            $bankId = (int) $find->fetchColumn();
            try {
                if (!$bankId) {
                    throw new InvalidArgumentException('Conto bancario non trovato: crearlo prima dell’import.');
                }
                $id = $service->saveTransaction([
                    'bank_account_id' => $bankId,
                    'booking_date' => $this->dateValue($this->pick($row, ['booking_date', 'data_contabile', 'data'])),
                    'value_date' => $this->dateValue($this->pick($row, ['value_date', 'data_valuta'])),
                    'amount' => $this->pick($row, ['amount', 'importo']),
                    'description' => $this->pick($row, ['description', 'descrizione', 'causale']),
                    'counterparty' => $this->pick($row, ['counterparty', 'controparte', 'nominativo']),
                    'reference' => $this->pick($row, ['reference', 'riferimento', 'cro']),
                    'external_id' => $this->pick($row, ['external_id', 'id_movimento']),
                ]);
                $this->db->prepare('UPDATE bank_transactions SET source_import_batch_id = ? WHERE id = ? AND organization_id = ?')->execute([$batchId, $id, $this->organizationId]);
                $this->recordImport($batchId, 'bank_transactions', $id, 'CREATE', null);
                $this->markRow($staged['id'], 'IMPORTED', null, $id); $imported++;
            } catch (Throwable $exception) {
                $this->markRow($staged['id'], 'ERROR', $exception->getMessage()); $errors++;
            }
        }
        return compact('imported', 'errors');
    }

    private function commitFatturaPa(int $batchId, array $rows): array
    {
        $imported = 0; $errors = 0;
        foreach ($rows as $staged) {
            $data = json_decode((string) $staged['normalized_data_json'], true, 512, JSON_THROW_ON_ERROR);
            if (empty($data['number']) || empty($data['document_date']) || empty($data['counterparty_name'])) {
                $this->markRow($staged['id'], 'ERROR', 'Dati minimi FatturaPA mancanti.'); $errors++; continue;
            }
            $data['counterparty_vat'] = PartyAutomationService::normalizeVat($data['counterparty_vat'] ?? null, (string) ($data['counterparty_country'] ?? 'IT'));
            $partyTable = $data['document_type'] === 'PURCHASE_INVOICE' ? 'suppliers' : 'customers';
            $find = $this->db->prepare("SELECT id FROM {$partyTable} WHERE organization_id = ? AND vat_number = ? LIMIT 1");
            $find->execute([$this->organizationId, $data['counterparty_vat']]);
            $partyId = (int) $find->fetchColumn();
            if (!$partyId) {
                $this->db->prepare("INSERT INTO {$partyTable} (organization_id, business_name, vat_number, tax_code, address, postal_code, city, province, country_code, iban, bank_name, bank_abi, bank_cab, payment_method_code, active, source_import_batch_id, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, NOW(), NOW())")
                    ->execute([$this->organizationId, $data['counterparty_name'], $data['counterparty_vat'] ?: null,
                        $data['counterparty_tax_code'] ?: null, $data['counterparty_address'] ?: null, $data['counterparty_postal_code'] ?: null,
                        $data['counterparty_city'] ?: null, $data['counterparty_province'] ?: null, $data['counterparty_country'] ?: 'IT',
                        $data['bank_iban'] ?: null, $data['bank_name'] ?: null, $data['bank_abi'] ?: null, $data['bank_cab'] ?: null,
                        $data['payment_method_code'] ?: null, $batchId, $this->userId, $this->userId]);
                $partyId = (int) $this->db->lastInsertId();
                $this->recordImport($batchId, $partyTable, $partyId, 'CREATE', null);
            }
            $externalKey = hash('sha256', implode('|', [$data['issuer_vat'], $data['recipient_vat'], $data['document_date'], $data['number'], $data['fatturapa_type'], $data['total']]));
            $find = $this->db->prepare('SELECT id FROM documents WHERE organization_id = ? AND external_key = ? LIMIT 1');
            $find->execute([$this->organizationId, $externalKey]);
            if ($existingId = $find->fetchColumn()) {
                $this->markRow($staged['id'], 'SKIPPED', 'Documento già importato.', (int) $existingId); continue;
            }
            $taxable = round(array_sum(array_column($data['lines'], 'taxable_amount')), 2);
            $vat = round(array_sum(array_column($data['lines'], 'vat_amount')), 2);
            $total = $data['total'] > 0 ? round((float) $data['total'], 2) : round($taxable + $vat, 2);
            $status = $data['document_type'] === 'PURCHASE_INVOICE' ? 'RECEIVED' : 'ISSUED';
            $this->db->prepare(
                'INSERT INTO documents (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type, counterparty_id, counterparty_name, currency, taxable_total, vat_total, withholding_total, withholding_type, withholding_rate, withholding_taxable_percent, withholding_cause, total, balance_due, status, fatturapa_type, payment_method_code, bank_name, bank_abi, bank_cab, bank_iban, external_key, source_import_batch_id, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 100, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
            )->execute([$this->organizationId, $data['document_type'], $data['number'], (int) substr($data['document_date'], 0, 4), $data['document_date'], $data['due_date'] ?: null, $partyTable === 'suppliers' ? 'SUPPLIER' : 'CUSTOMER', $partyId, $data['counterparty_name'], $data['currency'], $taxable, $vat,
                $data['withholding_amount'] ?: 0, $data['withholding_type'] ?: null, $data['withholding_rate'] ?: 0, $data['withholding_cause'] ?: null,
                $total, round($total - (float) ($data['withholding_amount'] ?? 0), 2), $status, $data['fatturapa_type'], $data['payment_method_code'] ?: null,
                $data['bank_name'] ?: null, $data['bank_abi'] ?: null, $data['bank_cab'] ?: null, $data['bank_iban'] ?: null,
                $externalKey, $batchId, $this->userId, $this->userId]);
            $documentId = (int) $this->db->lastInsertId();
            $insert = $this->db->prepare('INSERT INTO document_lines (organization_id, document_id, line_number, description, quantity, unit, unit_price, discount_percent, taxable_amount, vat_rate, vat_nature, vat_amount, total_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, NOW(), NOW())');
            foreach ($data['lines'] as $index => $line) {
                $insert->execute([$this->organizationId, $documentId, $index + 1, $line['description'], $line['quantity'], $line['unit'], $line['unit_price'], $line['taxable_amount'], $line['vat_rate'], $line['vat_nature'], $line['vat_amount'], round($line['taxable_amount'] + $line['vat_amount'], 2)]);
            }
            (new AccountingService($this->db, $this->organizationId, $this->userId))->postDocument($documentId);
            (new ReceivablesService($this->db, $this->organizationId, $this->userId))->syncDocumentById($documentId);
            $this->recordImport($batchId, 'documents', $documentId, 'CREATE', null);
            $this->markRow($staged['id'], 'IMPORTED', null, $documentId); $imported++;
        }
        return compact('imported', 'errors');
    }

    private function insertStagedRow(int $batchId, int $fileId, int $rowNumber, array $raw, array $normalized): void
    {
        $statement = $this->db->prepare('INSERT INTO import_rows (organization_id, batch_id, source_file_id, source_row_number, raw_data_json, normalized_data_json, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, \'STAGED\', NOW(), NOW())');
        $statement->execute([$this->organizationId, $batchId, $fileId, $rowNumber, json_encode($raw, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), json_encode($normalized, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)]);
    }

    private function refreshCounters(int $batchId, string $status): void
    {
        $statement = $this->db->prepare('SELECT COUNT(*) FROM import_rows WHERE batch_id = ? AND organization_id = ?');
        $statement->execute([$batchId, $this->organizationId]);
        $count = (int) $statement->fetchColumn();
        $this->db->prepare('UPDATE import_batches SET status = ?, total_rows = ?, valid_rows = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
            ->execute([$status, $count, $count, $batchId, $this->organizationId]);
    }

    private function batch(int $batchId, bool $forUpdate): array
    {
        $statement = $this->db->prepare('SELECT * FROM import_batches WHERE id = ? AND organization_id = ?' . ($forUpdate ? ' FOR UPDATE' : ''));
        $statement->execute([$batchId, $this->organizationId]);
        $batch = $statement->fetch();
        return $batch ?: throw new InvalidArgumentException('Lotto di importazione non trovato.');
    }

    private function markRow(int $rowId, string $status, ?string $error = null, ?int $entityId = null): void
    {
        $this->db->prepare('UPDATE import_rows SET status = ?, error_message = ?, imported_entity_id = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?')
            ->execute([$status, $error ? mb_substr($error, 0, 2000) : null, $entityId, $rowId, $this->organizationId]);
    }

    private function recordImport(int $batchId, string $table, int $entityId, string $operation, ?array $before): void
    {
        $this->db->prepare('INSERT INTO import_records (organization_id, batch_id, entity_type, entity_id, operation, before_data_json, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())')
            ->execute([$this->organizationId, $batchId, $table, $entityId, $operation, $before ? json_encode($before, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) : null]);
    }

    private function pick(array $row, array $keys): ?string
    {
        foreach ($keys as $key) {
            if (array_key_exists($key, $row) && trim((string) $row[$key]) !== '') {
                return trim((string) $row[$key]);
            }
        }
        return null;
    }

    private function restorableColumns(string $table): array
    {
        return match ($table) {
            'customers', 'suppliers' => ['code', 'business_name', 'vat_number', 'tax_code', 'sdi_code', 'pec', 'email', 'phone', 'address', 'postal_code', 'city', 'province', 'country_code', 'iban', 'bank_name', 'bank_abi', 'bank_cab', 'payment_terms', 'payment_days', 'payment_month_end', 'payment_method_code', 'active'],
            'chart_of_accounts' => ['code', 'name', 'account_type', 'normal_balance', 'parent_id', 'classification_code', 'statement_section', 'tax_mapping_code', 'is_postable', 'active'],
            default => [],
        };
    }

    private function detectDelimiter(string $sample): string
    {
        $counts = [';' => substr_count($sample, ';'), ',' => substr_count($sample, ','), "\t" => substr_count($sample, "\t"), '|' => substr_count($sample, '|')];
        arsort($counts);
        return (string) array_key_first($counts);
    }

    private function normalizeHeader(mixed $header): string
    {
        $header = mb_strtolower(trim((string) $header));
        $header = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $header) ?: $header;
        $header = preg_replace('/[^a-z0-9]+/', '_', $header) ?: '';
        return trim($header, '_') ?: 'colonna';
    }

    private function xpathOne(SimpleXMLElement $xml, string $path): ?SimpleXMLElement
    {
        $result = $xml->xpath($path) ?: [];
        return $result[0] ?? null;
    }

    private function xpathText(?SimpleXMLElement $xml, string $path): string
    {
        if (!$xml) {
            return '';
        }
        $result = $xml->xpath($path) ?: [];
        return isset($result[0]) ? trim((string) $result[0]) : '';
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

    private function dateValue(?string $value): ?string
    {
        if (!$value) {
            return null;
        }
        foreach (['Y-m-d', 'd/m/Y', 'd-m-Y', 'Ymd'] as $format) {
            $date = \DateTimeImmutable::createFromFormat('!' . $format, $value);
            if ($date) {
                return $date->format('Y-m-d');
            }
        }
        return null;
    }

    private function uuid(): string
    {
        $data = random_bytes(16);
        $data[6] = chr((ord($data[6]) & 0x0f) | 0x40);
        $data[8] = chr((ord($data[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($data), 4));
    }
}
