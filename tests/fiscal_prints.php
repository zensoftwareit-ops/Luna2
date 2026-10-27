<?php
declare(strict_types=1);

// Dedicated local database only. Never use the application's .env here.
require dirname(__DIR__) . '/vendor/autoload.php';
use Luna\Service\OfficialPrintService;
use Luna\Service\LedgerReportService;
use Luna\Service\TrialBalanceService;
use Luna\Service\AssetService;
use Luna\Service\ImportService;
use Luna\Service\PartyLedgerService;
use Luna\Service\TabularExportService;
use Luna\Service\VatService;

$db = new PDO('mysql:host=127.0.0.1;port=33317;charset=utf8mb4', 'root', '', [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES => false,
]);
$name = 'luna_fiscal_test_' . bin2hex(random_bytes(5));
$db->exec('CREATE DATABASE `' . $name . '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
$db->exec('USE `' . $name . '`');
$base = dirname(__DIR__);
$checks = 0;
$assert = static function (bool $pass, string $label) use (&$checks): void {
    if (!$pass) { throw new RuntimeException($label); }
    $checks++;
};
$add = static function (string $table, array $row) use ($db): int {
    $s = $db->prepare('INSERT INTO ' . $table . ' (`' . implode('`,`', array_keys($row)) . '`) VALUES (' . implode(',', array_fill(0, count($row), '?')) . ')');
    $s->execute(array_values($row));
    return (int) $db->lastInsertId();
};
foreach (glob($base . '/database/migrations/*.sql') as $file) {
    foreach (preg_split('/;\s*(?:\r?\n|$)/', file_get_contents($file)) as $sql) {
        if (trim($sql) !== '') { $db->exec($sql); }
    }
}
$assert(true, 'All migrations on MariaDB');
$org = $add('organizations', ['business_name' => 'Società test stampe fiscali S.r.l.', 'vat_number' => '01234567890',
    'tax_code' => '01234567890', 'address' => 'Via dei Controlli 15', 'postal_code' => '20100', 'city' => 'Milano', 'province' => 'MI']);
$other = $add('organizations', ['business_name' => 'OTHER TENANT']);
$user = $add('users', ['organization_id' => $org, 'name' => 'Verificatore', 'email' => 'fiscal-test@example.invalid', 'password_hash' => 'test-only', 'role' => 'OWNER']);
$account = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '1001', 'name' => 'Banca test', 'account_type' => 'ASSET']);
$offset = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '4001', 'name' => 'Ricavi test', 'account_type' => 'REVENUE']);
$add('accounting_settings', ['organization_id' => $org]);
for ($i = 0; $i < 90; $i++) {
    $date = $i === 0 ? '2025-12-31' : sprintf('2026-01-%02d', 1 + ($i % 28));
    $entry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'PN-' . $i, 'entry_date' => $date,
        'competence_date' => $date, 'entry_type' => 'MANUAL', 'status' => 'POSTED', 'description' => 'Registrazione contabile di collaudo ' . $i,
        'document_number' => 'FA-' . $i, 'counterparty' => 'Cliente collaudo', 'total_debit' => 100, 'total_credit' => 100]);
    $add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $entry, 'line_number' => 1, 'account_id' => $account, 'debit' => 100, 'credit' => 0]);
    $add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $entry, 'line_number' => 2, 'account_id' => $offset, 'debit' => 0, 'credit' => 100]);
}
$ledger = new LedgerReportService($db, $org);
[$a, $lines] = $ledger->dataset($account, '2026-01-01', '2026-12-31');
[$filtered, $found] = $ledger->dataset($account, '2026-01-01', '2026-12-31', 'FA-89');
$assert($a['opening_balance'] == 100 && $a['closing_balance'] == 9000, 'Opening and closing balances');
$assert($a['closing_balance'] === $filtered['closing_balance'] && count($found) === 1, 'Search does not change balances');
$expected = array_values(array_filter($lines, static fn ($l) => $l['document_number'] === 'FA-89'))[0];
$assert($found[0]['running_balance'] === $expected['running_balance'], 'Filtered running balance remains actual');
[$empty] = $ledger->dataset($account, '2027-01-01', '2027-12-31', 'absent');
$assert($empty['opening_balance'] == 9000 && $empty['closing_balance'] == 9000, 'No movement carry forward');
try { (new LedgerReportService($db, $other))->dataset($account, '2026-01-01', '2026-12-31'); $assert(false, 'Tenant isolation'); }
catch (InvalidArgumentException $e) { $assert(true, 'Tenant isolation'); }

$closing = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'CLOSE-2026', 'entry_date' => '2026-12-31',
    'competence_date' => '2026-12-31', 'entry_type' => 'CLOSING', 'status' => 'POSTED', 'description' => 'Chiusura esercizio 2026',
    'total_debit' => 9000, 'total_credit' => 9000]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $closing, 'line_number' => 1, 'account_id' => $account, 'debit' => 0, 'credit' => 9000]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $closing, 'line_number' => 2, 'account_id' => $offset, 'debit' => 9000, 'credit' => 0]);
[$beforeClosing] = $ledger->dataset($account, '2026-01-01', '2026-12-31');
[$afterClosing] = $ledger->dataset($account, '2026-01-01', '2026-12-31', '', true);
$assert($beforeClosing['closing_balance'] == 9000 && $afterClosing['closing_balance'] == 0,
    'Ledger excludes closing entries by default and includes them on request');
$trialBalance = new TrialBalanceService($db, $org);
$statementBeforeClosing = $trialBalance->dataset('2026-01-01', '2026-12-31');
$statementAfterClosing = $trialBalance->dataset('2026-01-01', '2026-12-31', '', '', true);
$assert($statementBeforeClosing['totals']['assets'] == 9000 && $statementAfterClosing['totals']['assets'] == 0,
    'Trial balance excludes closing entries by default and includes them on request');

$openingEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'OPEN-2027', 'entry_date' => '2027-01-01',
    'competence_date' => '2027-01-01', 'entry_type' => 'OPENING', 'status' => 'POSTED', 'description' => 'Apertura esercizio 2027',
    'total_debit' => 9000, 'total_credit' => 9000]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $openingEntry, 'line_number' => 1, 'account_id' => $account, 'debit' => 9000, 'credit' => 0]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $openingEntry, 'line_number' => 2, 'account_id' => $offset, 'debit' => 0, 'credit' => 9000]);
[$opened] = $ledger->dataset($account, '2027-01-01', '2027-12-31');
$assert($opened['opening_balance'] == 0 && $opened['closing_balance'] == 9000,
    'A posted opening replaces, rather than duplicates, prior balance-sheet carry-forward');
$openedStatement = $trialBalance->dataset('2027-01-01', '2027-12-31');
$assert($openedStatement['totals']['assets'] == 9000,
    'Trial balance does not duplicate prior balances when a posted opening exists');

$assetParent = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '05025', 'name' => 'Immobilizzazioni immateriali', 'account_type' => 'ASSET', 'is_postable' => 0]);
$assetCost = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '050251015', 'name' => 'Licenze', 'account_type' => 'ASSET', 'parent_id' => $assetParent]);
$assetFund = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '050251510', 'name' => 'Fondo ammortamento licenze', 'account_type' => 'ASSET', 'parent_id' => $assetParent]);
$offsettingEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'OPEN-2028-ASSET', 'entry_date' => '2028-01-01',
    'competence_date' => '2028-01-01', 'entry_type' => 'OPENING', 'status' => 'POSTED', 'description' => 'Costo e fondo interamente compensati',
    'total_debit' => 1081.97, 'total_credit' => 1081.97]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $offsettingEntry, 'line_number' => 1, 'account_id' => $assetCost, 'debit' => 1081.97, 'credit' => 0]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $offsettingEntry, 'line_number' => 2, 'account_id' => $assetFund, 'debit' => 0, 'credit' => 1081.97]);
$offsettingStatement = $trialBalance->dataset('2028-01-01', '2028-12-31');
$visibleAssetCodes = array_column($offsettingStatement['sections']['ASSET'], 'code');
$assert(in_array('050251015', $visibleAssetCodes, true) && in_array('050251510', $visibleAssetCodes, true),
    'Offsetting asset cost and depreciation fund remain visible as individual ledgers');

$vat = new VatService($db, $org, $user);
foreach (['SALES', 'PURCHASES'] as $register) {
    for ($i = 0; $i < 35; $i++) {
        $vat->saveManual(['register_type' => $register, 'movement_date' => '2026-01-15', 'vat_code' => '22%', 'vat_rate' => 22,
            'taxable_amount' => 100, 'vat_amount' => 22, 'counterparty_name' => 'Controparte test con denominazione estesa',
            'protocol_number' => $register . '-' . $i, 'document_reference' => 'INV-' . $i, 'document_reference_date' => '2026-01-10']);
    }
}
$settlement = $vat->calculateSettlement(['period_type' => 'MONTHLY', 'period_year' => 2026, 'period_number' => 1,
    'payment_due_date' => '2026-02-16']);
$vat->updateSettlementStatus($settlement, 'SUBMITTED');
$vat->updateSettlementStatus($settlement, 'PAID', '2026-02-16', 'F24-TEST-123');
$add('vat_movements', ['organization_id' => $org, 'source_type' => 'DATEV_PDF', 'document_fiscal_type' => 'TD01', 'register_type' => 'PURCHASES',
    'movement_date' => '2026-02-10', 'vat_code' => '22%', 'vat_rate' => 22, 'taxable_amount' => 100, 'vat_amount' => 22,
    'counterparty_name' => 'Fornitore DATEV', 'document_reference' => 'FT-2026-01', 'document_reference_date' => '2026-02-10',
    'deductible_vat' => 22, 'period_year' => 2026, 'period_month' => 2]);
$creditNote = $add('vat_movements', ['organization_id' => $org, 'source_type' => 'DATEV_PDF', 'document_fiscal_type' => 'TD04', 'register_type' => 'PURCHASES',
    'movement_date' => '2026-02-11', 'vat_code' => '22%', 'vat_rate' => 22, 'taxable_amount' => 50, 'vat_amount' => 11,
    'counterparty_name' => 'Fornitore DATEV', 'document_reference' => 'NC-2026-01', 'document_reference_date' => '2026-02-11',
    'deductible_vat' => 11, 'deductibility_percent' => 100, 'period_year' => 2026, 'period_month' => 2]);
$add('vat_movements', ['organization_id' => $org, 'source_type' => 'DOCUMENT', 'document_fiscal_type' => 'TD01', 'register_type' => 'PURCHASES',
    'movement_date' => '2026-02-10', 'vat_code' => '22%', 'vat_rate' => 22, 'taxable_amount' => 100, 'vat_amount' => 22,
    'counterparty_name' => 'Fornitore XML', 'document_reference' => 'FT-XML-01', 'document_reference_date' => '2026-02-10',
    'deductible_vat' => 22, 'period_year' => 2026, 'period_month' => 2]);
$reconciledSettlement = $vat->calculateSettlement(['period_type' => 'MONTHLY', 'period_year' => 2026, 'period_number' => 2]);
$statement = $db->prepare('SELECT vat_credit, source_reconciliation_mode, excluded_document_rows, excluded_document_purchase_vat FROM vat_settlements WHERE id = ?');
$statement->execute([$reconciledSettlement]);
$reconciled = $statement->fetch();
$assert((float) $reconciled['vat_credit'] === 11.0, 'DATEV register takes precedence over duplicate XML VAT');
$statement = $db->prepare('SELECT previous_credit, previous_credit_mode, previous_credit_source_settlement_id FROM vat_settlements WHERE id = ?');
$statement->execute([$reconciledSettlement]);
$carriedCredit = $statement->fetch();
$assert($carriedCredit['previous_credit_mode'] === 'AUTO' && (int) $carriedCredit['previous_credit_source_settlement_id'] === $settlement,
    'Previous VAT settlement is traced automatically');
$assert($reconciled['source_reconciliation_mode'] === 'DATEV_PRIORITY' && (int) $reconciled['excluded_document_rows'] === 1
    && (float) $reconciled['excluded_document_purchase_vat'] === 22.0, 'VAT source reconciliation is auditable');
$statement = $db->prepare('SELECT document_fiscal_type, taxable_amount, vat_amount, deductible_vat FROM vat_movements WHERE id = ?');
$statement->execute([$creditNote]);
$storedCreditNote = $statement->fetch();
$assert($storedCreditNote['document_fiscal_type'] === 'TD04'
    && (float) $storedCreditNote['taxable_amount'] === 50.0
    && (float) $storedCreditNote['vat_amount'] === 11.0
    && (float) $storedCreditNote['deductible_vat'] === 11.0,
    'Credit note amounts stay positive and TD04 determines their accounting sign');
$scheduleSupplier = $add('suppliers', ['organization_id' => $org, 'business_name' => 'Fornitore scadenze test', 'active' => 1]);
$scheduleDocument = $add('documents', ['organization_id' => $org, 'document_type' => 'PURCHASE_INVOICE', 'number' => 'RATE-TEST-1',
    'fiscal_year' => 2026, 'document_date' => '2026-10-01', 'counterparty_type' => 'SUPPLIER', 'counterparty_id' => $scheduleSupplier,
    'counterparty_name' => 'Fornitore scadenze test', 'total' => 100, 'balance_due' => 100, 'status' => 'RECEIVED']);
$importService = new ImportService($db, $org, $user, $base . '/storage/private/fiscal-tests/' . $name);
$scheduleMethod = new ReflectionMethod($importService, 'replaceFatturaPaSchedules');
$scheduleMethod->setAccessible(true);
$scheduleMethod->invoke($importService, $scheduleDocument, [
    ['due_date' => '2026-10-31', 'amount' => 60, 'payment_method_code' => 'MP05', 'iban' => 'IT00TEST'],
    ['due_date' => '2026-11-30', 'amount' => 40, 'payment_method_code' => 'MP05', 'iban' => 'IT00TEST'],
], 100.0, '2026-10-01', 'MP05', 'IT00TEST');
$scheduleRows = $db->query('SELECT due_date, amount FROM payment_schedules WHERE document_id = ' . (int) $scheduleDocument . ' ORDER BY installment_number')->fetchAll();
$assert(count($scheduleRows) === 2 && (float) $scheduleRows[0]['amount'] === 60.0 && (float) $scheduleRows[1]['amount'] === 40.0,
    'Matching FatturaPA installments are preserved');
$scheduleMethod->invoke($importService, $scheduleDocument, [
    ['due_date' => '2026-10-31', 'amount' => 100, 'payment_method_code' => 'MP05', 'iban' => 'IT00TEST'],
], 80.0, '2026-10-01', 'MP05', 'IT00TEST');
$recalculatedSchedule = $db->query('SELECT due_date, amount FROM payment_schedules WHERE document_id = ' . (int) $scheduleDocument)->fetchAll();
$assert(count($recalculatedSchedule) === 1 && (float) $recalculatedSchedule[0]['amount'] === 80.0,
    'Mismatching FatturaPA installments are recalculated to net payable');
$partySupplier = $add('suppliers', ['organization_id' => $org, 'code' => 'FOR-TEST-001',
    'business_name' => 'Magica Luce Societa a responsabilita limitata', 'vat_number' => '09418840964',
    'tax_code' => '09418840964', 'address' => 'Via Isonzo 24', 'postal_code' => '20100', 'city' => 'Milano',
    'province' => 'MI', 'active' => 1]);
$partyCustomerEquivalent = $add('customers', ['organization_id' => $org, 'code' => 'CLI-TEST-001',
    'business_name' => 'Magica Luce S.r.l.', 'vat_number' => '09418840964',
    'tax_code' => '09418840964', 'active' => 1]);
$correctDatevSupplier = $add('suppliers', ['organization_id' => $org, 'code' => 'FOR-TEST-002',
    'business_name' => 'Fornitore corretto S.r.l.', 'active' => 1]);
$payableAccount = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '3901010',
    'name' => 'Fornitori beni e servizi entro esercizio successivo', 'account_type' => 'LIABILITY']);
$add('accounting_account_mappings', ['organization_id' => $org, 'mapping_key' => 'TRADE_PAYABLES',
    'account_id' => $payableAccount, 'description' => 'Debiti verso fornitori']);
$receivableAccount = $add('chart_of_accounts', ['organization_id' => $org, 'code' => '0501010',
    'name' => 'Crediti verso clienti', 'account_type' => 'ASSET']);
$add('accounting_account_mappings', ['organization_id' => $org, 'mapping_key' => 'TRADE_RECEIVABLES',
    'account_id' => $receivableAccount, 'description' => 'Crediti verso clienti']);
$openingEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'OPEN-SUP-001',
    'entry_date' => '2025-12-31', 'competence_date' => '2025-12-31', 'entry_type' => 'OPENING', 'status' => 'POSTED',
    'description' => 'Progressivi fornitore', 'counterparty' => 'Magica Luce Societa a responsabilita limitata',
    'total_debit' => 50, 'total_credit' => 50]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $openingEntry, 'line_number' => 1,
    'account_id' => $offset, 'debit' => 50, 'credit' => 0, 'description' => 'Apertura']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $openingEntry, 'line_number' => 2,
    'account_id' => $payableAccount, 'debit' => 0, 'credit' => 50, 'description' => 'Apertura fornitore',
    'supplier_id' => $partySupplier]);
$partyDocument = $add('documents', ['organization_id' => $org, 'document_type' => 'PURCHASE_INVOICE',
    'number' => '100/E', 'fiscal_year' => 2026, 'document_date' => '2026-03-10', 'counterparty_type' => 'SUPPLIER',
    'counterparty_id' => $partySupplier, 'counterparty_name' => 'Magica Luce Societa a responsabilita limitata',
    'taxable_total' => 100, 'vat_total' => 22, 'total' => 122, 'balance_due' => 122, 'status' => 'RECEIVED']);
$partyEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'GEN-2026-PARTY-001',
    'entry_date' => '2026-03-31', 'competence_date' => '2026-03-10', 'entry_type' => 'PURCHASE_INVOICE',
    'status' => 'POSTED', 'description' => 'Fattura acquisti 100/E', 'document_number' => '100/E',
    'source_protocol' => '137', 'source_type' => 'DOCUMENT', 'source_id' => $partyDocument,
    'counterparty' => 'Magica Luce Societa a responsabilita limitata', 'total_debit' => 122, 'total_credit' => 122]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $partyEntry, 'line_number' => 1,
    'account_id' => $offset, 'debit' => 122, 'credit' => 0, 'description' => 'Costo acquisto']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $partyEntry, 'line_number' => 2,
    'account_id' => $payableAccount, 'debit' => 0, 'credit' => 122, 'description' => 'Debito fattura 100/E',
    'supplier_id' => $partySupplier]);
$partyLedger = new PartyLedgerService($db, $org);
$assert($partyLedger->historyStartDate() === '2025-12-31',
    'Customer and supplier account sheets default to the complete accounting history');
$partyDetail = $partyLedger->detail('SUPPLIER', $partySupplier, '2026-01-01', '2026-07-31');
$assert((int) ($partyDetail['opposite_party']['id'] ?? 0) === $partyCustomerEquivalent
    && ($partyDetail['opposite_party']['party_type'] ?? '') === 'CUSTOMER',
    'Supplier account sheets expose the matching customer role by VAT number');
$assert((float) $partyDetail['totals']['opening_credit'] === 50.0
    && (float) $partyDetail['totals']['progressive_credit'] === 172.0
    && (float) $partyDetail['totals']['accounting_closing'] === -172.0,
    'Supplier accounting sheet calculates DATEV-style progressives and accounting balance');
$assert(count($partyDetail['rows']) === 1
    && $partyDetail['rows'][0]['document_date'] === '2026-03-10'
    && $partyDetail['rows'][0]['source_protocol'] === '137',
    'Supplier accounting sheet includes document date and source protocol');

// Le prime note DATEV conservano il nominativo del sottoconto nel nome conto,
// non necessariamente nella controparte di testata. La sincronizzazione deve
// quindi recuperarlo dalla riga sorgente archiviata e quadrarne Dare/Avere.
$datevImportBatch = $add('import_batches', ['organization_id' => $org, 'uuid' => '00000000-0000-4000-8000-000000000123',
    'source_system' => 'DATEV_KOINOS', 'import_type' => 'journal_entries', 'status' => 'COMPLETED',
    'original_filename' => 'prima-nota-datev.csv', 'checksum_sha256' => str_repeat('a', 64), 'file_size' => 100,
    'total_rows' => 6, 'valid_rows' => 6, 'imported_rows' => 6, 'error_rows' => 0]);
$datevImportFile = $add('import_files', ['organization_id' => $org, 'batch_id' => $datevImportBatch,
    'filename' => 'prima-nota-datev.csv', 'media_type' => 'text/csv', 'checksum_sha256' => str_repeat('b', 64), 'file_size' => 100]);
$duplicateDatevSupplier = $add('suppliers', ['organization_id' => $org, 'code' => 'FOR-DATEV-DUPLICATE',
    'business_name' => 'Magica Luce S.r.l.', 'source_import_batch_id' => $datevImportBatch, 'active' => 1]);
$inactiveHistoricalName = 'NUOVO FORNITORE STORICO';
$inactiveHistoricalSupplier = $add('suppliers', ['organization_id' => $org,
    'code' => 'FOR-DATEV-' . strtoupper(substr(hash('sha256', $inactiveHistoricalName), 0, 12)),
    'business_name' => $inactiveHistoricalName, 'source_import_batch_id' => $datevImportBatch, 'active' => 0]);
$datevEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'GEN-2026-DATEV-001',
    'entry_date' => '2026-04-01', 'competence_date' => '2026-04-01', 'entry_type' => 'DATEV_IMPORT',
    'status' => 'POSTED', 'description' => 'Causale che non contiene il fornitore', 'source_protocol' => 'KOINOS-001',
    'source_type' => 'MANUAL', 'total_debit' => 75, 'total_credit' => 75]);
$datevPayableLine = $add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $datevEntry,
    'line_number' => 1, 'account_id' => $payableAccount, 'debit' => 0, 'credit' => 75, 'description' => 'Causale DATEV']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $datevEntry,
    'line_number' => 2, 'account_id' => $offset, 'debit' => 75, 'credit' => 0, 'description' => 'Costo']);
// Le correzioni successive all'importazione non devono impedire il recupero:
// il numero di righe ora differisce da quello del CSV sorgente.
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $datevEntry,
    'line_number' => 3, 'account_id' => $offset, 'debit' => 1, 'credit' => 0, 'description' => 'Riga aggiunta']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $datevEntry,
    'line_number' => 4, 'account_id' => $offset, 'debit' => 0, 'credit' => 1, 'description' => 'Riga aggiunta']);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 2, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '3901010', 'account_name' => 'Magica Luce S.r.l.']),
    'status' => 'IMPORTED', 'imported_entity_id' => $datevEntry]);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 3, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '4001', 'account_name' => 'Costo di prova']),
    'status' => 'IMPORTED', 'imported_entity_id' => $datevEntry]);
$add('import_records', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'entity_type' => 'journal_entries',
    'entity_id' => $datevEntry, 'operation' => 'CREATE']);
$missingPartyEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'GEN-2026-DATEV-002',
    'entry_date' => '2026-04-02', 'competence_date' => '2026-04-02', 'entry_type' => 'DATEV_IMPORT',
    'status' => 'POSTED', 'description' => 'Fornitore storico non ancora in anagrafica', 'source_protocol' => 'KOINOS-002',
    'source_type' => 'MANUAL', 'total_debit' => 90, 'total_credit' => 90]);
$missingPartyLine = $add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $missingPartyEntry,
    'line_number' => 1, 'account_id' => $payableAccount, 'debit' => 0, 'credit' => 90, 'description' => 'Debito storico']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $missingPartyEntry,
    'line_number' => 2, 'account_id' => $offset, 'debit' => 90, 'credit' => 0, 'description' => 'Costo storico']);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 4, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '3901010', 'account_name' => 'NUOVO FORNITORE STORICO']),
    'status' => 'IMPORTED', 'imported_entity_id' => $missingPartyEntry]);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 5, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '4001', 'account_name' => 'Costo di prova']),
    'status' => 'IMPORTED', 'imported_entity_id' => $missingPartyEntry]);
$add('import_records', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'entity_type' => 'journal_entries',
    'entity_id' => $missingPartyEntry, 'operation' => 'CREATE']);
$wrongPartyEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'GEN-2026-DATEV-003',
    'entry_date' => '2026-04-03', 'competence_date' => '2026-04-03', 'entry_type' => 'DATEV_IMPORT',
    'status' => 'POSTED', 'description' => 'Movimento precedentemente associato al soggetto errato',
    'source_protocol' => 'KOINOS-003', 'source_type' => 'MANUAL', 'total_debit' => 110, 'total_credit' => 110]);
$wrongPartyLine = $add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $wrongPartyEntry,
    'line_number' => 1, 'account_id' => $payableAccount, 'debit' => 0, 'credit' => 110,
    'description' => 'Debito storico', 'supplier_id' => $partySupplier]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $wrongPartyEntry,
    'line_number' => 2, 'account_id' => $offset, 'debit' => 110, 'credit' => 0, 'description' => 'Costo storico']);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 6, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '3901010', 'account_name' => 'FORNITORE CORRETTO SRL']),
    'status' => 'IMPORTED', 'imported_entity_id' => $wrongPartyEntry]);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'source_file_id' => $datevImportFile,
    'source_row_number' => 7, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '4001', 'account_name' => 'Costo di prova']),
    'status' => 'IMPORTED', 'imported_entity_id' => $wrongPartyEntry]);
$add('import_records', ['organization_id' => $org, 'batch_id' => $datevImportBatch, 'entity_type' => 'journal_entries',
    'entity_id' => $wrongPartyEntry, 'operation' => 'CREATE']);
$partyLedger->synchronize();
$linkedDatevLine = $db->query('SELECT supplier_id FROM journal_entry_lines WHERE id = ' . (int) $datevPayableLine)->fetchColumn();
$assert((int) $linkedDatevLine === $partySupplier,
    'DATEV source account name deterministically links historical collective-account lines to supplier subledgers');
$duplicateDatevActive = $db->query('SELECT active FROM suppliers WHERE id = ' . (int) $duplicateDatevSupplier)->fetchColumn();
$assert((int) $duplicateDatevActive === 0,
    'Empty DATEV-generated duplicate parties are deactivated after authoritative reconciliation');
$createdHistoricalSupplier = $db->query("SELECT id FROM suppliers WHERE organization_id = {$org} AND business_name = 'NUOVO FORNITORE STORICO'")->fetchColumn();
$linkedMissingPartyLine = $db->query('SELECT supplier_id FROM journal_entry_lines WHERE id = ' . (int) $missingPartyLine)->fetchColumn();
$reactivatedHistoricalSupplier = $db->query('SELECT active FROM suppliers WHERE id = ' . (int) $inactiveHistoricalSupplier)->fetchColumn();
$assert((int) $createdHistoricalSupplier === $inactiveHistoricalSupplier
    && (int) $linkedMissingPartyLine === $inactiveHistoricalSupplier
    && (int) $reactivatedHistoricalSupplier === 1,
    'DATEV reconciliation restores inactive historical parties and assigns their collective-account lines automatically');
$repairedWrongPartyLine = $db->query('SELECT supplier_id FROM journal_entry_lines WHERE id = ' . (int) $wrongPartyLine)->fetchColumn();
$assert((int) $repairedWrongPartyLine === $correctDatevSupplier,
    'One-time DATEV reconciliation replaces incorrect pre-existing subaccount associations with the authoritative source party');
$partyIntegrity = $partyLedger->integrity('SUPPLIER');
$assert($partyIntegrity['is_balanced'] === true
    && abs($partyIntegrity['total_debit'] - $partyIntegrity['assigned_debit']) < 0.005
    && abs($partyIntegrity['total_credit'] - $partyIntegrity['assigned_credit']) < 0.005,
    'Supplier subledger integrity reconciles assigned debit and credit with the collective account');
$partyPdf = (new TabularExportService(['business_name' => 'Societa test stampe fiscali S.r.l.',
    'vat_number' => '01234567890', 'tax_code' => '01234567890']))
    ->partyLedgerPdfContent($partyDetail, '2026-01-01', '2026-07-31', 'SUPPLIER');
$partyLedgerPath = $base . '/storage/private/fiscal-tests/' . $name . '/scheda-contabile-fornitore.pdf';
if (!is_dir(dirname($partyLedgerPath))) { mkdir(dirname($partyLedgerPath), 0750, true); }
file_put_contents($partyLedgerPath, $partyPdf);
$assert(str_starts_with($partyPdf, '%PDF-') && strlen($partyPdf) > 2000, 'Supplier accounting sheet PDF generated');
$periodOpening = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'OPEN-SUP-2026',
    'entry_date' => '2026-01-01', 'competence_date' => '2026-01-01', 'entry_type' => 'OPENING', 'status' => 'POSTED',
    'description' => 'Apertura fornitore esercizio 2026', 'total_debit' => 50, 'total_credit' => 50]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $periodOpening, 'line_number' => 1,
    'account_id' => $offset, 'debit' => 50, 'credit' => 0, 'description' => 'Contropartita apertura']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $periodOpening, 'line_number' => 2,
    'account_id' => $payableAccount, 'debit' => 0, 'credit' => 50, 'description' => 'Apertura fornitore',
    'supplier_id' => $partySupplier]);
$partyWithPeriodOpening = $partyLedger->detail('SUPPLIER', $partySupplier, '2026-01-01', '2026-07-31');
$assert((float) $partyWithPeriodOpening['totals']['opening_credit'] === 0.0
    && (float) $partyWithPeriodOpening['totals']['progressive_credit'] === 247.0,
    'Supplier opening entry replaces rather than duplicates prior progressives');
$supplierOverview = $partyLedger->overview('SUPPLIER', '2026-01-01', '2026-07-31', 'Magica Luce');
$assert(count($supplierOverview) === 1 && (float) $supplierOverview[0]['opening_balance'] === 0.0
    && (float) $supplierOverview[0]['closing_balance'] === 247.0,
    'Supplier overview applies the same opening replacement rule');
$technicalAliasEntry = $add('journal_entries', ['organization_id' => $org,
    'protocol_number' => 'GEN-2024-DATEV-ALIAS', 'entry_date' => '2024-06-30',
    'competence_date' => '2024-06-30', 'entry_type' => 'DATEV_IMPORT', 'status' => 'POSTED',
    'description' => 'Movimento storico rimasto sul duplicato tecnico', 'total_debit' => 33, 'total_credit' => 33]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $technicalAliasEntry,
    'line_number' => 1, 'account_id' => $offset, 'debit' => 33, 'credit' => 0, 'description' => 'Costo storico']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $technicalAliasEntry,
    'line_number' => 2, 'account_id' => $payableAccount, 'debit' => 0, 'credit' => 33,
    'description' => 'Debito storico', 'supplier_id' => $duplicateDatevSupplier]);
$aliasDetail = $partyLedger->detail('SUPPLIER', $partySupplier, '2024-01-01', '2026-12-31');
$assert(in_array('GEN-2024-DATEV-ALIAS', array_column($aliasDetail['rows'], 'protocol_number'), true),
    'The real party account sheet includes movements left on an equivalent DATEV technical alias');
$sourceOnlyEntry = $add('journal_entries', ['organization_id' => $org,
    'protocol_number' => 'GEN-2026-DATEV-SOURCE-ONLY', 'entry_date' => '2026-05-31',
    'competence_date' => '2026-05-31', 'entry_type' => 'DATEV_IMPORT', 'status' => 'POSTED',
    'description' => 'Movimento recuperabile soltanto dalla fonte DATEV', 'total_debit' => 44, 'total_credit' => 44]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $sourceOnlyEntry,
    'line_number' => 1, 'account_id' => $offset, 'debit' => 44, 'credit' => 0, 'description' => 'Costo fonte']);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $sourceOnlyEntry,
    'line_number' => 2, 'account_id' => $payableAccount, 'debit' => 0, 'credit' => 44,
    'description' => 'Debito fonte']);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch,
    'source_file_id' => $datevImportFile, 'source_row_number' => 8, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '4001', 'account_name' => 'Costo di prova']),
    'status' => 'IMPORTED', 'imported_entity_id' => $sourceOnlyEntry]);
$add('import_rows', ['organization_id' => $org, 'batch_id' => $datevImportBatch,
    'source_file_id' => $datevImportFile, 'source_row_number' => 9, 'raw_data_json' => '{}',
    'normalized_data_json' => json_encode(['account_code' => '3901010', 'account_name' => 'Magica Luce S.r.l.']),
    'status' => 'IMPORTED', 'imported_entity_id' => $sourceOnlyEntry]);
$sourceDetail = $partyLedger->detail('SUPPLIER', $partySupplier, '2024-01-01', '2026-12-31');
$assert(in_array('GEN-2026-DATEV-SOURCE-ONLY', array_column($sourceDetail['rows'], 'protocol_number'), true),
    'The party account sheet recovers unassigned movements directly from archived DATEV source rows');
$directionCustomer = $add('customers', ['organization_id' => $org, 'code' => 'CLI-DIRECTION-001',
    'business_name' => 'Cliente direzione test', 'active' => 1]);
$customerEntry = $add('journal_entries', ['organization_id' => $org, 'protocol_number' => 'GEN-2026-CUSTOMER-DIRECTION',
    'entry_date' => '2026-06-30', 'competence_date' => '2026-06-30', 'entry_type' => 'SALES_INVOICE',
    'status' => 'POSTED', 'description' => 'Vendita cliente test', 'total_debit' => 66, 'total_credit' => 66]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $customerEntry,
    'line_number' => 1, 'account_id' => $receivableAccount, 'debit' => 66, 'credit' => 0,
    'description' => 'Credito cliente', 'customer_id' => $directionCustomer]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $customerEntry,
    'line_number' => 2, 'account_id' => $offset, 'debit' => 0, 'credit' => 66, 'description' => 'Ricavo']);
$db->exec('UPDATE journal_entry_lines SET customer_id = ' . (int) $directionCustomer
    . ' WHERE id = ' . (int) $datevPayableLine);
$customerDirectionDetail = $partyLedger->detail('CUSTOMER', $directionCustomer, '2024-01-01', '2026-12-31');
$assert(array_column($customerDirectionDetail['rows'], 'protocol_number') === ['GEN-2026-CUSTOMER-DIRECTION'],
    'Customer account sheets exclude supplier/payable movements carrying an incorrect customer link');
$authoritativeDocument = $add('documents', ['organization_id' => $org, 'document_type' => 'PURCHASE_INVOICE',
    'number' => 'AUTH-1', 'fiscal_year' => 2026, 'document_date' => '2026-07-01',
    'counterparty_type' => 'SUPPLIER', 'counterparty_id' => $partySupplier,
    'counterparty_name' => 'Magica Luce S.r.l.', 'total' => 77, 'balance_due' => 77, 'status' => 'RECEIVED']);
$authoritativeEntry = $add('journal_entries', ['organization_id' => $org,
    'protocol_number' => 'GEN-2026-AUTHORITATIVE-PARTY', 'entry_date' => '2026-07-01',
    'competence_date' => '2026-07-01', 'entry_type' => 'PURCHASE_INVOICE', 'status' => 'POSTED',
    'source_type' => 'DOCUMENT', 'source_id' => $authoritativeDocument,
    'description' => 'Documento con precedente associazione errata', 'total_debit' => 77, 'total_credit' => 77]);
$authoritativeLine = $add('journal_entry_lines', ['organization_id' => $org,
    'journal_entry_id' => $authoritativeEntry, 'line_number' => 1, 'account_id' => $payableAccount,
    'debit' => 0, 'credit' => 77, 'description' => 'Debito documento',
    'supplier_id' => $correctDatevSupplier, 'customer_id' => $directionCustomer]);
$add('journal_entry_lines', ['organization_id' => $org, 'journal_entry_id' => $authoritativeEntry,
    'line_number' => 2, 'account_id' => $offset, 'debit' => 77, 'credit' => 0, 'description' => 'Costo documento']);
$add('accounting_open_items', ['organization_id' => $org, 'direction' => 'PAYABLE',
    'party_type' => 'SUPPLIER', 'party_id' => $partySupplier, 'party_name' => 'Magica Luce S.r.l.',
    'document_id' => $authoritativeDocument, 'journal_entry_id' => $authoritativeEntry,
    'account_id' => $payableAccount, 'reference' => 'AUTH-1', 'issue_date' => '2026-07-01',
    'due_date' => '2026-07-31', 'original_amount' => 77, 'settled_amount' => 0, 'status' => 'OPEN']);
$partyLedger->synchronize();
$authoritativeLink = $db->query('SELECT supplier_id, customer_id FROM journal_entry_lines WHERE id = '
    . (int) $authoritativeLine)->fetch();
$assert((int) $authoritativeLink['supplier_id'] === $partySupplier && $authoritativeLink['customer_id'] === null,
    'Document evidence overwrites stale or cross-direction party associations');
$ledgerPdf = (new TabularExportService(['business_name' => 'Societa test stampe fiscali S.r.l.',
    'vat_number' => '01234567890', 'tax_code' => '01234567890']))
    ->ledgerPdfContent($a, $lines, '2026-01-01', '2026-12-31');
$assert(str_starts_with($ledgerPdf, '%PDF-') && strlen($ledgerPdf) > 2000,
    'General ledger PDF uses professional accounting-sheet layout');
$asset = $add('fixed_assets', ['organization_id' => $org, 'asset_code' => 'CES-01', 'description' => 'Macchinario di collaudo',
    'purchase_date' => '2024-01-15', 'purchase_cost' => 10000, 'depreciation_rate' => 20, 'net_book_value' => 8000]);
$assetService = new AssetService($db, $org, $user);
$category = $add('fixed_asset_categories', ['organization_id' => $org, 'code' => 'TEST', 'name' => 'Categoria test',
    'civil_rate' => 20, 'tax_rate' => 20, 'first_year_percent' => 50]);
$savedAsset = $assetService->saveAsset(['asset_code' => 'CES-02', 'description' => 'Cespite inserito da procedura',
    'category_id' => $category, 'purchase_date' => '2026-01-15', 'purchase_cost' => '1200',
    'purchase_document_number' => 'FT-15', 'purchase_document_date' => '2026-01-10', 'acquisition_condition' => 'USED']);
$savedAssetRow = $db->query('SELECT purchase_document_number, acquisition_condition FROM fixed_assets WHERE id = ' . (int) $savedAsset)->fetch();
$assert($savedAssetRow['purchase_document_number'] === 'FT-15' && $savedAssetRow['acquisition_condition'] === 'USED',
    'Asset purchase evidence and used condition are persisted');
$db->exec('DELETE FROM fixed_assets WHERE id = ' . (int) $savedAsset);
$assetService->saveRegisterYear(['fixed_asset_id' => $asset, 'year' => 2026, 'original_cost' => '10000', 'revaluations' => '0',
    'writedowns' => '0', 'civil_opening_fund' => '2000', 'tax_opening_fund' => '2000', 'civil_rate' => '20', 'tax_rate' => '20',
    'civil_quota' => '2000', 'tax_quota' => '2000', 'evidence_reference' => 'PROSPETTO-2026-VERIFICATO']);
$out = $base . '/storage/private/fiscal-tests/' . $name;
if (!is_dir($out)) { mkdir($out, 0750, true); }
$prints = new OfficialPrintService($db, $org, $user, $out);
$ids = [];
foreach (array_keys(OfficialPrintService::types()) as $type) {
    $id = $prints->generate($type, '2026-01-01', '2026-12-31', 'Test automatico MariaDB');
    [$path, $run] = $prints->file($id);
    $assert(filesize($path) > 1000 && $run['page_count'] > 0, 'PDF generated ' . $type);
    $snapshot = json_decode($run['snapshot_json'], true, 512, JSON_THROW_ON_ERROR);
    foreach ($snapshot['rows'] as $row) { $assert(count($row) === count($snapshot['columns']), 'Column alignment ' . $type); }
    $assert($snapshot['issues'] === [], 'Unexpected issues ' . $type . ': ' . implode('; ', $snapshot['issues']));
    $prints->validate($id, 'REVIEW-TEST');
    $prints->lock($id);
    $dossier = $prints->dossier($id);
    $zip = new ZipArchive(); $zip->open($dossier);
    $manifest = json_decode($zip->getFromName('manifest.json'), true, 512, JSON_THROW_ON_ERROR);
    $assert(hash('sha256', $zip->getFromName('dati.json')) === $manifest['data_sha256'], 'ZIP data integrity');
    $assert(hash('sha256', $zip->getFromName(basename($path))) === $manifest['pdf_sha256'], 'ZIP PDF integrity');
    $zip->close(); unlink($dossier);
    $ids[$type] = [$id, $run, $path];
}
$next = $prints->generate('JOURNAL', '2026-01-01', '2026-12-31', null);
[, $nextRun] = $prints->file($next);
$assert($nextRun['first_page'] === $ids['JOURNAL'][1]['first_page'] + $ids['JOURNAL'][1]['page_count'], 'Continuous page allocation');
$db->prepare('UPDATE organizations SET tax_code = NULL WHERE id = ?')->execute([$org]);
$incomplete = $prints->generate('JOURNAL', '2026-01-01', '2026-12-31', null);
try { $prints->validate($incomplete, 'SHOULD-FAIL'); $assert(false, 'Incomplete metadata must block validation'); }
catch (InvalidArgumentException $e) { $assert(true, 'Incomplete validation blocked'); }
$db->prepare('UPDATE official_print_runs SET snapshot_json = ? WHERE id = ?')->execute(['{}', $next]);
try { $prints->validate($next, 'SHOULD-FAIL'); $assert(false, 'Tampered snapshot'); }
catch (RuntimeException $e) { $assert(true, 'Tampered snapshot rejected'); }
$artifacts = array_map(static fn ($row) => $row[2], $ids);
$artifacts['PARTY_LEDGER'] = $partyLedgerPath;
echo json_encode(['checks' => $checks, 'database' => $name, 'artifacts' => $artifacts], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
