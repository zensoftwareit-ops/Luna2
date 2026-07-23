<?php

declare(strict_types=1);

$base = dirname(__DIR__);
$failures = [];
$passes = 0;

$assert = static function (bool $condition, string $message) use (&$failures, &$passes): void {
    if ($condition) {
        $passes++;
    } else {
        $failures[] = $message;
    }
};

$phpFiles = [];
$iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($base, FilesystemIterator::SKIP_DOTS));
foreach ($iterator as $file) {
    if ($file->isFile()
        && $file->getExtension() === 'php'
        && !str_contains($file->getPathname(), DIRECTORY_SEPARATOR . 'vendor' . DIRECTORY_SEPARATOR)) {
        $phpFiles[] = $file->getPathname();
    }
}
foreach ($phpFiles as $file) {
    $command = escapeshellarg(PHP_BINARY) . ' -l ' . escapeshellarg($file);
    exec($command, $output, $status);
    $assert($status === 0, 'Errore sintassi: ' . $file . ' (' . implode(' ', $output) . ')');
    $output = [];
}

$modules = require $base . '/config/modules.php';
$assert(count($modules) >= 15, 'Inventario moduli troppo ridotto.');
$schema = '';
foreach (glob($base . '/database/migrations/*.sql') ?: [] as $migration) {
    $schema .= "\n" . file_get_contents($migration);
}
$tableSchemas = [];
preg_match_all('/CREATE TABLE IF NOT EXISTS\s+([a-zA-Z0-9_]+)\s*\((.*?)\)\s*ENGINE/is', $schema, $tableMatches, PREG_SET_ORDER);
foreach ($tableMatches as $match) {
    $tableSchemas[$match[1]] = $match[2];
}
preg_match_all('/ALTER TABLE\s+([a-zA-Z0-9_]+)\s+(.*?);/is', $schema, $alterMatches, PREG_SET_ORDER);
foreach ($alterMatches as $match) {
    if (!isset($tableSchemas[$match[1]])) {
        continue;
    }
    preg_match_all('/(?:ADD|MODIFY)\s+COLUMN\s+`?([a-zA-Z0-9_]+)`?\s+/i', $match[2], $columnMatches);
    foreach ($columnMatches[1] ?? [] as $column) {
        $tableSchemas[$match[1]] .= "\n{$column} ALTERED";
    }
}
$features = require $base . '/config/features.php';
foreach ($modules as $slug => $module) {
    $table = $module['table'];
    $assert(isset($tableSchemas[$table]), "Tabella mancante per {$slug}: {$table}");
    $assert(isset($features[$module['feature'] ?? '']), "FunzionalitÃ  non configurata per {$slug}");
    $requiredColumns = array_unique(array_merge($module['columns'], $module['search'], array_keys($module['fields'])));
    foreach ($requiredColumns as $column) {
        $assert((bool) preg_match('/(?:^|\n)\s*`?' . preg_quote($column, '/') . '`?\s+/i', $tableSchemas[$table] ?? ''), "Colonna {$table}.{$column} non trovata nella tabella corretta");
    }
}
foreach ($features as $key => $feature) {
    $assert(isset($feature['label'], $feature['description'], $feature['icon'], $feature['required_tables']), "Configurazione incompleta per funzionalitÃ  {$key}");
    foreach ($feature['required_tables'] as $table) {
        $assert(isset($tableSchemas[$table]), "Tabella richiesta {$table} mancante per funzionalitÃ  {$key}");
    }
}

$composer = json_decode((string) file_get_contents($base . '/composer.json'), true);
$assert(is_array($composer) && isset($composer['require']['php']), 'composer.json non valido.');
$assert(is_file($base . '/public/index.php') && is_file($base . '/public/.htaccess'), 'Webroot incompleta.');
$assert(is_file($base . '/docs/DATEV_KOINOS_MIGRATION.md'), 'Piano migrazione Koinos mancante.');
$assert(is_file($base . '/docs/ACCOUNTING_PARITY.md'), 'Matrice paritÃ  contabile Koinos mancante.');
$assert(is_file($base . '/views/settings/modules.php') && is_file($base . '/views/settings/system.php') && is_file($base . '/views/settings/company.php'), 'Pannello impostazioni incompleto.');
$application = (string) file_get_contents($base . '/app/Core/Application.php');
$assert(str_contains($application, "'/settings/modules'"), 'Rotta gestione moduli mancante.');
$assert(str_contains($application, "'/settings/system'"), 'Rotta stato sistema mancante.');
$assert(str_contains($application, "'/settings/company'"), 'Rotta setup azienda mancante.');
$assert(str_contains($application, "'/settings/users'"), 'Rotta gestione utenti mancante.');
$assert(str_contains($application, "'/accounting/vat-registers'"), 'Rotta registri IVA mancante.');
$assert(str_contains($application, "'/accounting/vat-settlements'"), 'Rotta liquidazioni IVA mancante.');
$assert(str_contains($application, "'/accounting/setup'"), 'Rotta configurazione piano dei conti mancante.');
$assert(str_contains($application, "'/accounting/treasury'"), 'Rotta tesoreria e partite mancante.');
$assert(str_contains($application, "'/accounting/compliance'"), 'Rotta adempimenti e chiusure mancante.');
$assert(str_contains($application, "'/settings/endpoints'"), 'Rotta endpoint fatturazione elettronica mancante.');
$parityRoutes = ['/operations/logistics', '/operations/projects', '/operations/communications', '/operations/hr', '/operations/ecommerce', '/operations/rental', '/operations/calendar', '/reports/management'];
foreach ($parityRoutes as $route) {
    $assert(str_contains($application, "'{$route}"), "Rotta paritÃ  funzionale mancante: {$route}");
}
$parityServices = ['DocumentWorkflowService','InventoryService','ProjectService','MailService','LeaveService','EcommerceService','RentalService','CalendarService','PayrollService','ManagementReportService'];
foreach ($parityServices as $service) {
    $assert(is_file($base . '/app/Service/' . $service . '.php'), "Servizio paritÃ  mancante: {$service}");
}
$assert(str_contains($application, 'ErrorReporter::report'), 'Registrazione errori applicativi mancante.');
$platformController = (string) file_get_contents($base . '/app/Controller/PlatformController.php');
$settingsController = (string) file_get_contents($base . '/app/Controller/SettingsController.php');
$auth = (string) file_get_contents($base . '/app/Core/Auth.php');
$assert(str_contains($platformController, 'requireSuperuser()'), 'Setup piattaforma non protetto dal ruolo superuser.');
$assert(str_contains($settingsController, 'requireManagedOrganization()'), 'Gestione moduli non vincolata al superuser e a una azienda selezionata.');
$assert(!str_contains($settingsController, "requireRoles(['OWNER', 'ADMIN'])"), 'OWNER e ADMIN non devono gestire i moduli di piattaforma.');
$assert(str_contains($auth, "=== 'SUPERUSER'"), 'Ruolo SUPERUSER non gestito dallâ€™autenticazione.');
$assert(str_contains($schema, "ENUM('SUPERUSER','OWNER','ADMIN'"), 'Migrazione ruolo SUPERUSER mancante.');
$accountingController = (string) file_get_contents($base . '/app/Controller/AccountingController.php');
$accountingService = (string) file_get_contents($base . '/app/Service/AccountingService.php');
$vatService = (string) file_get_contents($base . '/app/Service/VatService.php');
$assert(str_contains($accountingController, 'saveVatMovement') && str_contains($accountingController, 'calculateVatSettlement'), 'Workflow IVA controller incompleto.');
$assert(str_contains($accountingService, 'saveManual') && str_contains($accountingService, 'postDraft'), 'Workflow prima nota con bozze incompleto.');
$assert(str_contains($accountingService, 'assertAccountsBelongToOrganization'), 'Validazione tenant dei conti mancante.');
$assert(str_contains($vatService, 'syncDocument') && str_contains($vatService, 'assertPeriodOpen'), 'Sincronizzazione IVA o blocco periodo mancante.');
$assert(isset($tableSchemas['vat_settlement_details']), 'Dettaglio liquidazioni IVA mancante.');
$assert(str_contains($schema, 'vat_settlement_details'), 'Storico di dettaglio delle liquidazioni IVA mancante.');
$advancedAccountingTables = [
    'accounting_settings', 'vat_registers', 'accounting_causes', 'accounting_account_mappings',
    'accounting_open_items', 'payment_allocations', 'vat_cash_events', 'vat_adjustments',
    'lipe_communications', 'vat_annual_summaries', 'accounting_period_locks', 'accounting_closing_runs',
    'accounting_adjustment_schedules', 'fixed_asset_categories', 'withholding_records', 'api_endpoint_configs',
];
foreach ($advancedAccountingTables as $table) {
    $assert(isset($tableSchemas[$table]), "Tabella contabile avanzata mancante: {$table}");
}
$assert(str_contains($schema, "normal_balance ENUM('DEBIT','CREDIT')") && str_contains($schema, 'statement_section'), 'Classificazione avanzata del piano dei conti mancante.');
$assert(str_contains($schema, 'secret_reference') && !str_contains($schema, 'secret_value'), 'Gli endpoint devono memorizzare riferimenti e non segreti.');
$assert(is_file($base . '/views/accounting/vat-registers.php') && is_file($base . '/views/accounting/vat-settlements.php') && is_file($base . '/views/accounting/vat-settlement.php'), 'Interfaccia IVA incompleta.');
$renderAccountingView = static function (string $file, array $variables) use ($base): string {
    extract($variables, EXTR_SKIP);
    ob_start();
    require $base . '/views/accounting/' . $file . '.php';
    return (string) ob_get_clean();
};
require_once $base . '/vendor/autoload.php';
$_SESSION = [];
try {
    $renderedAccounting = '';
    $renderedAccounting .= $renderAccountingView('journal', ['entries' => [], 'totals' => ['debit' => 0, 'credit' => 0, 'drafts' => 0], 'from' => '2026-01-01', 'to' => '2026-12-31', 'status' => '', 'type' => '', 'search' => '']);
    $renderedAccounting .= $renderAccountingView('form', ['accounts' => [], 'entry' => ['entry_date' => '2026-01-01', 'competence_date' => '2026-01-01', 'entry_type' => 'MANUAL'], 'lines' => []]);
    $entry = ['id' => 1, 'protocol_number' => 'GEN-2026-000001', 'description' => 'Test', 'entry_date' => '2026-01-01', 'entry_type' => 'MANUAL', 'counterparty' => null, 'status' => 'DRAFT', 'source_type' => 'MANUAL', 'total_debit' => 0, 'total_credit' => 0, 'document_number' => null, 'notes' => null];
    $renderedAccounting .= $renderAccountingView('entry', ['entry' => $entry, 'lines' => []]);
    $renderedAccounting .= $renderAccountingView('vat-registers', ['register' => 'SALES', 'year' => 2026, 'month' => 1, 'movements' => [], 'summary' => [], 'totals' => ['taxable' => 0, 'vat' => 0, 'deductible' => 0], 'vatCodes' => []]);
    $renderedAccounting .= $renderAccountingView('vat-settlements', ['settlements' => [], 'year' => 2026]);
    $settlement = ['id' => 1, 'period_type' => 'MONTHLY', 'period_year' => 2026, 'period_number' => 1, 'calculated_at' => '2026-02-01', 'updated_at' => '2026-02-01', 'status' => 'CALCULATED', 'vat_debit' => 0, 'vat_credit' => 0, 'previous_credit' => 0, 'interest_amount' => 0, 'balance' => 0, 'notes' => null];
    $renderedAccounting .= $renderAccountingView('vat-settlement', ['settlement' => $settlement, 'details' => []]);
    $renderedAccounting .= $renderAccountingView('setup', ['accounts' => [], 'settings' => [], 'registers' => [], 'causes' => [], 'mappings' => [], 'mappingLabels' => []]);
    $renderedAccounting .= $renderAccountingView('treasury', ['direction' => '', 'openItems' => [], 'payments' => [], 'bankAccounts' => [], 'bankTransactions' => [], 'links' => [], 'withholdings' => [], 'accounts' => []]);
    $periodStart = new DateTimeImmutable('2026-01-01');
    $periodEnd = new DateTimeImmutable('2026-12-31');
    $renderedAccounting .= $renderAccountingView('compliance', ['year' => 2026, 'adjustments' => [], 'lipe' => [], 'annual' => [], 'closingRuns' => [], 'schedules' => [], 'categories' => [], 'assets' => [], 'depreciations' => [], 'accounts' => [], 'statements' => [], 'statementTotals' => [], 'periodStart' => $periodStart, 'periodEnd' => $periodEnd]);
    $assert(str_contains($renderedAccounting, 'Prima nota') && str_contains($renderedAccounting, 'Registri IVA') && str_contains($renderedAccounting, 'Liquidazioni IVA'), 'Rendering viste contabili incompleto.');
    $assert(str_contains($renderedAccounting, 'Piano dei conti') && str_contains($renderedAccounting, 'Tesoreria e partite') && str_contains($renderedAccounting, 'Adempimenti, bilancio e cespiti'), 'Rendering contabilitÃ  avanzata incompleto.');
} catch (Throwable $exception) {
    $assert(false, 'Errore rendering viste contabili: ' . $exception->getMessage());
}
$renderView = static function (string $file, array $variables) use ($base): string {
    extract($variables, EXTR_SKIP);
    ob_start();
    require $base . '/views/' . $file . '.php';
    return (string) ob_get_clean();
};
try {
    $renderedOperations = '';
    $renderedOperations .= $renderView('operations/logistics', ['balances'=>[],'transfers'=>[],'picks'=>[],'warehouses'=>[],'products'=>[],'orders'=>[]]);
    $renderedOperations .= $renderView('operations/projects', ['projects'=>[],'project'=>null,'metrics'=>[],'time'=>[],'expenses'=>[],'milestones'=>[],'users'=>[]]);
    $renderedOperations .= $renderView('operations/communications', ['settings'=>[],'messages'=>[],'documents'=>[]]);
    $renderedOperations .= $renderView('operations/hr', ['leaves'=>[],'balances'=>[],'users'=>[],'runs'=>[],'configs'=>[],'admin'=>false]);
    $renderedOperations .= $renderView('operations/ecommerce', ['channels'=>[],'orders'=>[],'catalog'=>[],'queue'=>[]]);
    $renderedOperations .= $renderView('operations/rental', ['contracts'=>[],'tickets'=>[],'deadlines'=>[],'users'=>[]]);
    $renderedOperations .= $renderView('operations/calendar', ['accounts'=>[],'events'=>[],'logs'=>[],'users'=>[]]);
    $renderedOperations .= $renderView('operations/reports', ['exports'=>[]]);
    $assert(str_contains($renderedOperations, 'Logistica e magazzino') && str_contains($renderedOperations, 'Consuntivazione commesse'), 'Rendering pannelli operativi incompleto.');
    $assert(str_contains($renderedOperations, 'Hub e-commerce') && str_contains($renderedOperations, 'Report direzionali XLSX'), 'Rendering integrazioni o report incompleto.');
} catch (Throwable $exception) {
    $assert(false, 'Errore rendering pannelli operativi: ' . $exception->getMessage());
}
$endpointService = (string) file_get_contents($base . '/app/Service/EndpointConfigService.php');
$assert(!preg_match('/\bcurl_|file_get_contents\s*\(\s*\$baseUrl|new\s+(?:Client|HttpClient)\b/', $endpointService), 'La configurazione endpoint non deve effettuare chiamate di rete.');
$endpointView = (string) file_get_contents($base . '/views/settings/endpoints.php');
$assert(str_contains($endpointView, 'endpoint_id=') && str_contains($endpointView, 'name="id"'), 'Le configurazioni endpoint devono poter essere modificate e disattivate.');
$importService = (string) file_get_contents($base . '/app/Service/ImportService.php');
foreach (['open_items', 'vat_movements', 'fixed_assets', 'bank_transactions'] as $target) {
    $assert(str_contains($importService, "'{$target}'"), "Target import DATEV mancante: {$target}");
}
$assert(str_contains($importService, 'fondo_ammortamento') && str_contains($importService, 'tax_net_value'), 'L\'import cespiti deve preservare fondi e valori netti storici.');
$assert(str_contains($importService, 'SAVEPOINT import_fixed_asset_row'), 'L\'import cespiti deve isolare gli errori per riga.');
$assert(!str_contains($importService, 'Numero documento, data o importo non validi.'), 'I pagamenti storici senza docuß_w¶‰ËkºwµçOÏˆˆ™YH‹ØXØÛİ[[™ËÚ›İ\›˜[”š[XH›İOØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™Ëİ˜]\™YÚ\İ\œÉÊHÏˆˆ™YH‹ØXØÛİ[[™Ëİ˜]\™YÚ\İ\œÈ”™YÚ\İšHUOØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™Ëİ˜]\Ù][Y[ÉÊHÏˆˆ™YH‹ØXØÛİ[[™Ëİ˜]\Ù][Y[È“\]ZY^š[ÛšHUOØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™Ëİ™X\İ\IÊHÏˆˆ™YH‹ØXØÛİ[[™Ëİ™X\İ\H•\ÛÜ™\šXHH\]OØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™ËİšX[X˜[[˜ÙIÊHÏˆˆ™YH‹ØXØÛİ[[™ËİšX[X˜[[˜ÙHš[[˜Ú[ÈH™\šYšXØOØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™ËØÛÛ\X[˜ÙIÊHÏˆˆ™YH‹ØXØÛİ[[™ËØÛÛ\X[˜ÙHY[\[Y[HHÚ]\İ\™OØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËØXØÛİ[[™ËÜÙ]\	ÊHÏˆˆ™YH‹ØXØÛİ[[™ËÜÙ]\”X[›ÈZHÛÛHHØ]\Ø[OØO‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËÜ‹İ^YXY[™\ÉÊHÏˆˆ™YH‹Ü‹İ^YXY[™\È”ØØY[™Hš\ØØ[OØO‚ˆÜYˆ
[—Ø\œ˜^J
İš[™ÊH
]]\Ù\Š
VÉÜ›ÛI×HÏÈ	ÉÊKÉÓÕÓ‘T‰Ë	ĞQRS‰×KYJJNˆÏHÛ\ÜÏHÏH	Xİ]™J	ËÜÙ][™ÜËÙ[™Ú[ÉÊHÏˆˆ™YH‹ÜÙ][™ÜËÙ[™Ú[È‘[™Ú[KZ[›ÚXÙOØOÜ[™YÈÏ‚ˆÙ]‚ˆÙ]Z[Ï‚ˆÜ[™YÈÏ‚‚ˆÜYˆ
	[˜X›Y
	Ú[™[ÜIÊH	[˜X›Y
	Ü›Ú™XİÉÊH	[˜X›Y
	ØØ[[™\‰ÊJNˆÏ‚ˆ]Z[ÈÛ\ÜÏH›˜]‹YÜ›İ\ˆÏHİ—Üİ\×İÚ]
	İ\œ™[]	ËÛÜ\˜][ÛœËÛÙÚ\İXÜÉÊHİ—Üİ\×İÚ]
	İ\œ™[]	ËÛÜ\˜][ÛœËÜ›Ú™XİÉÊHİ—Üİ\×İÚ]
	İ\œ™[]	ËÛÜ\˜][ÛœËØØ[[™\‰ÊHÈ	ÛÜ[‰Èˆ	ÉÈÏ‚ˆİ[[X\OÏHšY]ÎšXÛÛŠ	ØœšYY˜Ø\ÙIÊHÏÜ[“Ü\˜]]š]0èÜÜ[ÏHšY]ÎšXÛÛŠ	ØÚ]œ›Û‰Ë	Û˜]‹XÚ]œ›Û‰ÊHÏÜİ[[X\O‚ˆ]ˆÛ\ÜÏH›˜]‹XÚ[™[ˆ‚ˆÜYˆ
	[˜X›Y
	Ú[™[ÜIÊJNˆÏHÛ\ÜÏHÏH	Xİ]™J	ËÛÜ\˜][ÛœËÛÙÚ\İXÜÉÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËÛÙÚ\İXÜÈ“ÙÚ\İXØHHXÚÚ[™ÏØOÜ[™YÈÏ‚ˆÜYˆ
	[˜X›Y
	Ü›Ú™XİÉÊJNˆÏHÛ\ÜÏHÏH	Xİ]™J	ËÛÜ\˜][ÛœËÜ›Ú™XİÉÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËÜ›Ú™XİÈÛÛœİ[]˜^š[Û™HÛÛ[Y\ÜÙOØOÜ[™YÈÏ‚ˆÜYˆ
	[˜X›Y
	ØØ[[™\‰ÊJNˆÏHÛ\ÜÏHÏH	Xİ]™J	ËÛÜ\˜][ÛœËØØ[[™\‰ÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËØØ[[™\ˆØ[[™\šHÚ[˜Ü›Ûš^˜]OØOÜ[™YÈÏ‚ˆÙ]‚ˆÙ]Z[Ï‚ˆÜ[™YÈÏ‚‚ˆÜYˆ
	[˜X›Y
	Ú‰ÊJNˆÏ‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÛÜ\˜][ÛœËÚ‰ÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËÚˆÏHšY]ÎšXÛÛŠ	ÚYXØ\™	ÊHÏÜ[‘™\šYHHYÚOÜÜ[ØO‚ˆÜ[™YÈÏ‚ˆÜYˆ
	[˜X›Y
	ÙXÛÛ[Y\˜ÙIÊJNˆÏ‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÛÜ\˜][ÛœËÙXÛÛ[Y\˜ÙIÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËÙXÛÛ[Y\˜ÙHÏHšY]ÎšXÛÛŠ	ÜİÜ™IÊHÏÜ[’XˆKXÛÛ[Y\˜ÙOÜÜ[ØO‚ˆÜ[™YÈÏ‚ˆÜYˆ
	[˜X›Y
	Ü™[[	ÊJNˆÏ‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÛÜ\˜][ÛœËÜ™[[	ÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËÜ™[[ÏHšY]ÎšXÛÛŠ	ØØ\‰ÊHÏÜ[“›ÛYÙÚ[ÈHXÚÙ]ÜÜ[ØO‚ˆÜ[™YÈÏ‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÛÜ\˜][ÛœËØÛÛ[][šXØ][ÛœÉÊHÏˆˆ™YH‹ÛÜ\˜][ÛœËØÛÛ[][šXØ][ÛœÈÏHšY]ÎšXÛÛŠ	Ü™XÙZ\	ÊHÏÜ[ÛÛ][šXØ^š[ÛšOÜÜ[ØO‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÜ™\ÜËÛX[˜YÙ[Y[	ÊHÏˆˆ™YH‹Ü™\ÜËÛX[˜YÙ[Y[ÏHšY]ÎšXÛÛŠ	ÙİÛ›ØY	ÊHÏÜ[”™\Ü\™^š[Û˜[OÜÜ[ØO‚‚ˆÜ›Ü™XXÚ
	Ü›İ\È\È	Ü›İ\Oˆ	[Ù[\ÊNˆÏ‚ˆÜˆ	š\œİH™\Ù]
	[Ù[\ÊNÂˆ	Ü›İ\XÛÛˆH	™X]\™\ÖÉš\œİÉÙ™X]\™I×WVÉÚXÛÛ‰×HÏÈ	Ø›Ş	ÎÂˆ	Ü›İ\Ü[ˆH˜[ÙNÂˆ›Ü™XXÚ
\œ˜^WÚÙ^\Ê	[Ù[\ÊH\È	ÛYÊHÂˆ	Ü›İ\Ü[ˆH	Ü›İ\Ü[ˆİ—Üİ\×İÚ]
	İ\œ™[]	ËÜ‹ÉÈˆ	ÛYÊNÂˆBˆÏ‚ˆ]Z[ÈÛ\ÜÏH›˜]‹YÜ›İ\ˆÏH	Ü›İ\Ü[ˆÈ	ÛÜ[‰Èˆ	ÉÈÏ‚ˆİ[[X\OÏHšY]ÎšXÛÛŠ	Ü›İ\XÛÛŠHÏÜ[ÏHšY]Î™J	Ü›İ\
HÏÜÜ[ÏHšY]ÎšXÛÛŠ	ØÚ]œ›Û‰Ë	Û˜]‹XÚ]œ›Û‰ÊHÏÜİ[[X\O‚ˆ]ˆÛ\ÜÏH›˜]‹XÚ[™[ˆ‚ˆÜ›Ü™XXÚ
	[Ù[\È\È	ÛYÈOˆ	[Ù[PÛÛ™šYÊNˆÏ‚ˆHÛ\ÜÏHÏH	Xİ]™J	ËÜ‹ÉÈˆ	ÛYÊHÏˆˆ™YH‹Ü‹ÏÏHšY]Î™J	ÛYÊHÏˆÏHšY]Î™J	[Ù[PÛÛ™šYÖÉİ]I×JHÏØO‚ˆÜ[™›Ü™XXÚÈÏ‚ˆÙ]‚ˆÙ]Z[Ï‚ˆÜ[™›Ü™XXÚÈÏ‚‚ˆÜYˆ
	[˜X›Y
	Ú[\ÜÉÊJNˆÏ‚ˆHÛ\ÜÏH›˜]‹[[šÏÏH	Xİ]™J	ËÚ[\ÜÉÊHÏˆˆ™YH‹Ú[\ÜÈÏHšY]ÎšXÛÛŠ	İ\ØY	ÊHÏÜ[’[\Ü^š[ÛšOÜÜ[ØO‚ˆÜ[™YÈÏ‚ˆÜ[™YÈÏ‚ˆÛ˜]‚‚ˆ]ˆÛ\ÜÏHœÚYX˜\‹]\Ù\ˆ‚ˆÜ[ˆÛ\ÜÏH˜]˜]\ˆÏHšY]Î™J	[š]X[ÈÎˆ	ÕIÊHÏÜÜ[‚ˆÜ[ˆÛ\ÜÏH\Ù\‹XÛÜHİ›Û™ÏÏHšY]Î™J]]\Ù\Š
VÉÛ˜[YI×HÏÈ	ÉÊHÏÜİ›Û™ÏÛX[ÏH	\Ôİ\\\Ù\ˆÈ	Ôİ\\\Ù\‰ÈˆšY]Î™J]]\Ù\Š
VÉÜ›ÛI×HÏÈ	ÉÊHÏÜÛX[ÜÜ[‚ˆ›Ü›HY]ÙHœÜİˆXİ[ÛH‹ÛÙÛİ]‚ˆ[œ]\OHšY[ˆˆ˜[YOH—İÚÙ[ˆˆ˜[YOHÏHšY]Î™JÜÜ™ÚÙ[Š
JHÏˆ‚ˆ]ÛˆÛ\ÜÏH›ÙÛİ]X]Ûˆˆ\OHœİX›Z]ˆ\šXK[X™[H‘\ØÚH¸¡¥ÏØ]Û‚ˆÙ›Ü›O‚ˆÙ]‚ˆØ\ÚYO‚ˆ]ÛˆÛ\ÜÏHœÚYX˜\‹X˜XÚÙ›Üˆ\OH˜]Ûˆˆ]K[Y[KXÛÜÙH\šXK[X™[HÚ]YHY[HØ]Û‚‚ˆXZ[ˆÛ\ÜÏH›XZ[‹XÛÛ[‚ˆXY\ˆÛ\ÜÏHÜ˜\ˆ‚ˆ]ÛˆÛ\ÜÏH›Y[K]ÙÙÛHˆ\OH˜]Ûˆˆ]K[Y[K]ÙÙÛH\šXK[X™[H\šHY[HÏHšY]ÎšXÛÛŠ	ÛY[IÊHÏØ]Û‚ˆ]ˆÛ\ÜÏHÜ˜\‹]]HÜ[ÏH	\Ôİ\\\Ù\ˆÈ	ÔX]Y›Ü›XIÈˆ	ÕÛÜšÜÜXÙIÈÏÏH]]›Ü™Ø[š^˜][Û“˜[YJ
HOOH	ÉÈÈ	È0­È	ÈˆšY]Î™J]]›Ü™Ø[š^˜][Û“˜[YJ
JHˆ	ÉÈÏÜÜ[İ›Û™ÏÏHšY]Î™J	]JHÏÜİ›Û™ÏÙ]‚ˆ]ˆÛ\ÜÏHÜ˜\‹XXİ[ÛœÈ‚ˆÜ[ˆÛ\ÜÏH™[š\›Û›Y[\[OÚOˆÛ›[™OÜÜ[‚ˆÜYˆ
I\Ôİ\\\Ù\ˆ	‰ˆ	[˜X›Y
	ÜØ[\ÉÊJNˆÏ‚ˆHÛ\ÜÏHœ]ZXÚËXÜ™X]Hˆ™YH‹ÙØİ[Y[ËÜ][İ\ËØÜ™X]HÏHšY]ÎšXÛÛŠ	Ü\ÉÊHÏÜ[“[İ›ÏÜÜ[ØO‚ˆÜ[ÙZYˆ
I\Ôİ\\\Ù\ˆ	‰ˆ	[˜X›Y
	Ø[˜YÜ˜\XÜÉÊJNˆÏ‚ˆHÛ\ÜÏHœ]ZXÚËXÜ™X]Hˆ™YH‹Ü‹Øİ\İÛY\œËØÜ™X]HÏHšY]ÎšXÛÛŠ	Ü\ÉÊHÏÜ[“[İ›ÏÜÜ[ØO‚ˆÜ[™YÈÏ‚ˆÙ]‚ˆÚXY\‚ˆ]ˆÛ\ÜÏHœYÙH‚ˆÜYˆ
	›\Ú
NˆÏ‚ˆ]ˆÛ\ÜÏH˜[\[\OÏHšY]Î™J	›\ÚÉİ\I×HÏÈ	ÜİXØÙ\ÜÉÊHÏˆˆ›ÛOHœİ]\È‚ˆÏHšY]ÎšXÛÛŠ
	›\ÚÉİ\I×HÏÈ	ÜİXØÙ\ÜÉÊHOOH	Ù\œ›Ü‰ÈÈ	Ø[\	Èˆ	ØÚXÚÉÊHÏ‚ˆÜ[ÏHšY]Î™J	›\ÚÉÛY\ÜØYÙI×HÏÈ	ÉÊHÏÜÜ[‚ˆÙ]‚ˆÜ[™YÈÏ‚ˆÏH	ÛÛ[Ï‚ˆÙ]‚ˆÛXZ[‚Ù]‚Ø›ÙO‚Ú[‚