<?php
declare(strict_types=1);

// Dedicated local database only. Never use the application's .env here.
require dirname(__DIR__) . '/vendor/autoload.php';
use Luna\Service\OfficialPrintService;
use Luna\Service\LedgerReportService;
use Luna\Service\AssetService;
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
$asset = $add('fixed_assets', ['organization_id' => $org, 'asset_code' => 'CES-01', 'description' => 'Macchinario di collaudo',
    'purchase_date' => '2024-01-15', 'purchase_cost' => 10000, 'depreciation_rate' => 20, 'net_book_value' => 8000]);
$assetService = new AssetService($db, $org, $user);
$assetService->saveRegisterYear(['fixed_asset_id' => $asset, 'year' => 2026, 'original_cost' => '10000', 'revaluations' => '0',
    'writedowns' => '0', 'civil_opening_fund' => '2000', 'tax_opening_fund' => '2000', 'civil_rate' => '20', 'tax_rate' => '20',
    'civil_quota' => '2000', 'tax_quota' => '2000', 'evidence_reference' => 'PROSPETTO-2026-VERIFICATO']);
$out = $base . '/storage/private/fiscal-tests/' . $name;
mkdir($out, 0750, true);
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
echo json_encode(['checks' => $checks, 'database' => $name, 'artifacts' => array_map(static fn ($row) => $row[2], $ids)], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
