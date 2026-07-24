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
    $assert(isset($features[$module['feature'] ?? '']), "Funzionalità non configurata per {$slug}");
    $requiredColumns = array_unique(array_merge($module['columns'], $module['search'], array_keys($module['fields'])));
    foreach ($requiredColumns as $column) {
        $assert((bool) preg_match('/(?:^|\n)\s*`?' . preg_quote($column, '/') . '`?\s+/i', $tableSchemas[$table] ?? ''), "Colonna {$table}.{$column} non trovata nella tabella corretta");
    }
}
foreach ($features as $key => $feature) {
    $assert(isset($feature['label'], $feature['description'], $feature['icon'], $feature['required_tables']), "Configurazione incompleta per funzionalità {$key}");
    foreach ($feature['required_tables'] as $table) {
        $assert(isset($tableSchemas[$table]), "Tabella richiesta {$table} mancante per funzionalità {$key}");
    }
}

$composer = json_decode((string) file_get_contents($base . '/composer.json'), true);
$assert(is_array($composer) && isset($composer['require']['php']), 'composer.json non valido.');
$assert(is_file($base . '/public/index.php') && is_file($base . '/public/.htaccess'), 'Webroot incompleta.');
$assert(is_file($base . '/docs/DATEV_KOINOS_MIGRATION.md'), 'Piano migrazione Koinos mancante.');
$assert(is_file($base . '/docs/ACCOUNTING_PARITY.md'), 'Matrice parità contabile Koinos mancante.');
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
$assert(str_contains($application, "'/r/{module}/export/{format}'"), 'Esportazioni PDF/XLSX/CSV degli archivi mancanti.');
$assert(str_contains($application, "'/documents/{type}/export/{format}'"), 'Esportazioni documenti filtrati mancanti.');
$assert(str_contains($application, "'/accounting/ledger/{id}/export/{format}'"), 'Esportazioni mastrino mancanti.');
$assert(is_file($base . '/app/Service/TabularExportService.php'), 'Servizio esportazioni tabellari mancante.');
$tabularExport = (string) file_get_contents($base . '/app/Service/TabularExportService.php');
$assert(str_contains($tabularExport, "['pdf', 'xlsx', 'csv']") && str_contains($tabularExport, "setPaper('A4', 'landscape')"), 'Formati tabellari o PDF tecnico orizzontale incompleti.');
$appJs = (string) file_get_contents($base . '/public/assets/app.js');
$assert(str_contains($appJs, 'Filtra tabella') && str_contains($appJs, 'application/vnd.ms-excel'), 'Ricerca ed esportazione delle tabelle operative mancanti.');
$tableJs = (string) file_get_contents($base . '/public/assets/tables.js');
$tableCss = (string) file_get_contents($base . '/public/assets/tables.css');
$layoutView = (string) file_get_contents($base . '/views/layout.php');
$assert(
    str_contains($layoutView, '/assets/tables.css?v=1.0.0')
    && str_contains($layoutView, '/assets/tables.js?v=1.0.1'),
    'Asset tabellari isolati non caricati dal layout.'
);
$assert(
    str_contains($tableJs, "document.querySelectorAll('.table-wrap > table').forEach((table)")
    && str_contains($tableJs, "table.matches('.line-table,.selectable-table,[data-no-table-controls]')")
    && str_contains($tableJs, "table.closest('form')")
    && str_contains($tableJs, "aria-sort")
    && str_contains($tableJs, "data-luna-size"),
    'Ordinamento e paginazione sicuri delle tabelle dati incompleti.'
);
$assert(
    !str_contains($tableJs, "if (rows.length === 0) return;"),
    'Le intestazioni devono restare ordinabili anche quando la tabella dati è vuota.'
);
$assert(
    substr_count($tableCss, '{') === substr_count($tableCss, '}')
    && str_contains($tableCss, '.page .list-toolbar.exportable-toolbar')
    && str_contains($tableCss, '.luna-table-pagination')
    && !str_contains($tableCss, '.topbar')
    && !str_contains($tableCss, '.sidebar'),
    'CSS tabellare non isolato o incompleto.'
);
$parityRoutes = ['/operations/logistics', '/operations/projects', '/operations/communications', '/operations/hr', '/operations/ecommerce', '/operations/rental', '/operations/calendar', '/reports/management'];
foreach ($parityRoutes as $route) {
    $assert(str_contains($application, "'{$route}"), "Rotta parità funzionale mancante: {$route}");
}
$workspaceRoutes = [
    '/workspace/search', '/workspace/notifications', '/workspace/onboarding', '/workspace/views',
    '/professional', '/professional/prints', '/professional/filings', '/professional/bank-statements',
    '/professional/reconciliation/suggest',
];
foreach ($workspaceRoutes as $route) {
    $assert(str_contains($application, "'{$route}"), "Rotta workspace professionale mancante: {$route}");
}
$parityServices = ['DocumentWorkflowService','InventoryService','ProjectService','MailService','LeaveService','EcommerceService','RentalService','CalendarService','PayrollService','ManagementReportService'];
foreach ($parityServices as $service) {
    $assert(is_file($base . '/app/Service/' . $service . '.php'), "Servizio parità mancante: {$service}");
}
$assert(str_contains($application, 'ErrorReporter::report'), 'Registrazione errori applicativi mancante.');
$platformController = (string) file_get_contents($base . '/app/Controller/PlatformController.php');
$settingsController = (string) file_get_contents($base . '/app/Controller/SettingsController.php');
$auth = (string) file_get_contents($base . '/app/Core/Auth.php');
$assert(str_contains($platformController, 'requireSuperuser()'), 'Setup piattaforma non protetto dal ruolo superuser.');
$assert(str_contains($settingsController, 'requireManagedOrganization()'), 'Gestione moduli non vincolata al superuser e a una azienda selezionata.');
$assert(!str_contains($settingsController, "requireRoles(['OWNER', 'ADMIN'])"), 'OWNER e ADMIN non devono gestire i moduli di piattaforma.');
$assert(str_contains($auth, "=== 'SUPERUSER'"), 'Ruolo SUPERUSER non gestito dall’autenticazione.');
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
$professionalTables = [
    'user_preferences', 'saved_views', 'workspace_notifications', 'onboarding_progress', 'bulk_operations',
    'official_print_runs', 'compliance_filing_runs', 'bank_statement_imports', 'bank_reconciliation_suggestions',
];
foreach ($professionalTables as $table) {
    $assert(isset($tableSchemas[$table]), "Tabella workspace professionale mancante: {$table}");
}
$assert(str_contains($tableSchemas['bank_transactions'] ?? '', 'bank_statement_import_id ALTERED'), 'Collegamento tra movimenti ed estratti conto mancante.');
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
    $renderedAccounting .= $renderAccountingView('journal', ['entries' => [], 'totals' => ['debit' => 0, 'credit' => 0, 'drafts' => 0], 'from' => '2026-01-01', 'to' => '2026-12-31', 'status' => '', 'type' => '', 'search' => '', 'accountId' => 0, 'accounts' => []]);
    $renderedAccounting .= $renderAccountingView('form', ['accounts' => [], 'entry' => ['entry_date' => '2026-01-01', 'competence_date' => '2026-01-01', 'entry_type' => 'MANUAL'], 'lines' => []]);
    $entry = ['id' => 1, 'protocol_number' => 'GEN-2026-000001', 'description' => 'Test', 'entry_date' => '2026-01-01', 'entry_type' => 'MANUAL', 'counterparty' => null, 'status' => 'DRAFT', 'source_type' => 'MANUAL', 'total_debit' => 0, 'total_credit' => 0, 'document_number' => null, 'notes' => null];
    $renderedAccounting .= $renderAccountingView('entry', ['entry' => $entry, 'lines' => []]);
    $renderedAccounting .= $renderAccountingView('vat-registers', ['register' => 'SALES', 'year' => 2026, 'month' => 1, 'search' => '', 'movements' => [], 'summary' => [], 'totals' => ['taxable' => 0, 'vat' => 0, 'deductible' => 0], 'vatCodes' => []]);
    $renderedAccounting .= $renderAccountingView('vat-settlements', ['settlements' => [], 'year' => 2026]);
    $settlement = ['id' => 1, 'period_type' => 'MONTHLY', 'period_year' => 2026, 'period_number' => 1, 'calculated_at' => '2026-02-01', 'updated_at' => '2026-02-01', 'status' => 'CALCULATED', 'vat_debit' => 0, 'vat_credit' => 0, 'previous_credit' => 0, 'interest_amount' => 0, 'balance' => 0, 'notes' => null];
    $renderedAccounting .= $renderAccountingView('vat-settlement', ['settlement' => $settlement, 'details' => []]);
    $renderedAccounting .= $renderAccountingView('setup', ['accounts' => [], 'settings' => [], 'registers' => [], 'causes' => [], 'mappings' => [], 'mappingLabels' => []]);
    $renderedAccounting .= $renderAccountingView('treasury', ['direction' => '', 'openItems' => [], 'payments' => [], 'bankAccounts' => [], 'bankTransactions' => [], 'links' => [], 'withholdings' => [], 'accounts' => []]);
    $periodStart = new DateTimeImmutable('2026-01-01');
    $periodEnd = new DateTimeImmutable('2026-12-31');
    $renderedAccounting .= $renderAccountingView('compliance', ['year' => 2026, 'adjustments' => [], 'lipe' => [], 'annual' => [], 'closingRuns' => [], 'schedules' => [], 'categories' => [], 'assets' => [], 'depreciations' => [], 'accounts' => [], 'statements' => [], 'statementTotals' => [], 'periodStart' => $periodStart, 'periodEnd' => $periodEnd]);
    $assert(str_contains($renderedAccounting, 'Prima nota') && str_contains($renderedAccounting, 'Registri IVA') && str_contains($renderedAccounting, 'Liquidazioni IVA'), 'Rendering viste contabili incompleto.');
    $assert(str_contains($renderedAccounting, 'Piano dei conti') && str_contains($renderedAccounting, 'Tesoreria e partite') && str_contains($renderedAccounting, 'Adempimenti, bilancio e cespiti'), 'Rendering contabilità avanzata incompleto.');
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
try {
    $_SERVER['REQUEST_URI'] = '/r/customers';
    $renderedWorkspace = [];
    $renderedWorkspace[] = $renderView('workspace/search', ['query' => 'rossi', 'results' => []]);
    $renderedWorkspace[] = $renderView('workspace/notifications', ['notifications' => []]);
    $renderedWorkspace[] = $renderView('workspace/onboarding', [
        'onboarding' => ['percentage' => 0, 'completed' => 0, 'total' => 1, 'steps' => [[
            'key' => 'company', 'title' => 'Profilo aziendale', 'description' => 'Dati fiscali',
            'url' => '/settings/company', 'automatic' => true, 'manual' => false, 'completed' => false, 'notes' => null,
        ]]],
        'quality' => [['label' => 'Scritture contabili quadrate', 'count' => 0, 'ok' => true, 'url' => '/accounting/journal', 'severity' => 'danger']],
    ]);
    $renderedWorkspace[] = $renderView('professional/index', [
        'prints' => [], 'filings' => [], 'imports' => [], 'suggestions' => [],
        'quality' => [['label' => 'Scritture contabili quadrate', 'count' => 0, 'ok' => true, 'url' => '/accounting/journal', 'severity' => 'danger']],
        'bankAccounts' => [], 'endpoints' => [],
        'printTypes' => ['JOURNAL' => 'Libro giornale'], 'filingTypes' => ['F24' => 'Deleghe F24'],
    ]);
    $resourceModule = [
        'group' => 'Anagrafiche', 'title' => 'Clienti', 'singular' => 'Cliente',
        'columns' => ['business_name', 'active'],
        'fields' => [
            'business_name' => ['label' => 'Ragione sociale', 'type' => 'text'],
            'active' => ['label' => 'Attivo', 'type' => 'checkbox'],
        ],
    ];
    $renderedWorkspace[] = $renderView('resource/index', [
        'slug' => 'customers', 'module' => $resourceModule, 'rows' => [], 'search' => '',
        'filters' => ['active' => ''], 'filterFields' => ['active' => $resourceModule['fields']['active']],
        'dateFrom' => [], 'dateTo' => [],
        'sort' => 'id', 'direction' => 'DESC', 'perPage' => 50, 'page' => 1, 'pages' => 1,
        'total' => 0, 'savedViews' => [],
    ]);
    $joinedWorkspace = implode("\n", $renderedWorkspace);
    $assert(str_contains($joinedWorkspace, 'Centro professionale') && str_contains($joinedWorkspace, 'Prontezza operativa'), 'Rendering workspace professionale incompleto.');
    $assert(str_contains($joinedWorkspace, 'Operazione massiva') && str_contains($joinedWorkspace, 'Ricerca globale'), 'Rendering UX archivi o ricerca incompleto.');
    if (class_exists(DOMDocument::class)) {
        foreach ($renderedWorkspace as $index => $html) {
            $dom = new DOMDocument();
            $previous = libxml_use_internal_errors(true);
            $loaded = $dom->loadHTML('<!doctype html><html><body><main>' . $html . '</main></body></html>', LIBXML_NONET);
            libxml_clear_errors();
            libxml_use_internal_errors($previous);
            $assert($loaded, "HTML non analizzabile nella vista workspace {$index}.");
            $ids = [];
            foreach ($dom->getElementsByTagName('*') as $element) {
                if ($element->hasAttribute('id')) {
                    $id = $element->getAttribute('id');
                    $assert(!isset($ids[$id]), "ID HTML duplicato {$id} nella vista workspace {$index}.");
                    $ids[$id] = true;
                }
            }
        }
    }
} catch (Throwable $exception) {
    $assert(false, 'Errore rendering workspace professionale: ' . $exception->getMessage());
}
$workspaceService = (string) file_get_contents($base . '/app/Service/WorkspaceService.php');
$complianceWorkspaceService = (string) file_get_contents($base . '/app/Service/ComplianceWorkspaceService.php');
$officialPrintService = (string) file_get_contents($base . '/app/Service/OfficialPrintService.php');
$assert(!str_contains($workspaceService, "status = 'COMMITTED'") && !str_contains($workspaceService, "'INVALID','FAILED','PARTIAL'"), 'Stati import non compatibili con lo schema.');
$assert(!str_contains($complianceWorkspaceService, 'api_endpoint_configs WHERE id = ? AND organization_id = ? AND active = 1'), 'Gli endpoint professionali devono usare il campo enabled.');
$assert(str_contains($complianceWorkspaceService, 'e.display_name AS endpoint_name'), 'Il Centro professionale deve usare la colonna display_name degli endpoint.');
$assert(!str_contains($complianceWorkspaceService, 'lipe_communications WHERE organization_id = ? AND period_year'), 'Il fascicolo LIPE usa colonne non presenti.');
$assert(!str_contains($complianceWorkspaceService, 'reverse_charge = 1') && !str_contains($officialPrintService, 'civil_accumulated_depreciation'), 'Query professionali non allineate allo schema contabile.');
$professionalServices = ['WorkspaceService', 'BankStatementService', 'OfficialPrintService', 'ComplianceWorkspaceService'];
foreach ($professionalServices as $service) {
    $assert(is_file($base . '/app/Service/' . $service . '.php'), "Servizio workspace professionale mancante: {$service}");
}
$css = (string) file_get_contents($base . '/public/assets/app.css');
$assert(substr_count($css, '{') === substr_count($css, '}'), 'Parentesi CSS non bilanciate.');
$assert(str_contains($css, '.topbar-search') && str_contains($css, '.professional-section') && str_contains($css, '@media(prefers-reduced-motion:reduce)'), 'Design system professionale o accessibilitÃ  CSS incompleti.');

$assert(
    str_contains($css, '.page :where(input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]),select,textarea)')
    && str_contains($css, 'input[type="file"]::file-selector-button')
    && str_contains($css, '.inline-form>input:not([type="hidden"])'),
    'Il design system deve coprire tutti i controlli visibili, i file e i form compatti.'
);
$documentFormView = (string) file_get_contents($base . '/views/documents/form.php');
$assert(
    substr_count($documentFormView, 'class="field') >= 8
    && !preg_match('/<label(?:\s+class="full-width")?>\s*<span>/', $documentFormView),
    'Tutti i campi della testata documenti devono usare lo stile field.'
);
$importIndexView = (string) file_get_contents($base . '/views/imports/index.php');
$assert(
    str_contains($importIndexView, '<label class="field"><span>Tipo dati *')
    && str_contains($importIndexView, '<label class="field"><span>File *'),
    'Il form import deve applicare lo stile field anche a select e file.'
);

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
$assert(!str_contains($importService, 'Numero documento, data o importo non validi.'), 'I pagamenti storici senza documento devono poter essere importati.');
$assert(str_contains($schema, 'code_conflict') && str_contains($schema, 'L2S.'), 'Il seed contabile non deve sovrascrivere codici del piano dei conti esistente.');
$allViews = '';
foreach (glob($base . '/views/*.php') ?: [] as $viewFile) {
    $allViews .= file_get_contents($viewFile);
}
$assert(!str_contains($allViews, 'javascript:'), 'URL javascript non consentiti nelle viste.');

$cli = (string) file_get_contents($base . '/bin/luna');
$migrateStart = strpos($cli, "case 'migrate':");
$bootstrapStart = strpos($cli, 'bootstrapSuperuser($db)', $migrateStart ?: 0);
$migrateBlock = $migrateStart !== false && $bootstrapStart !== false
    ? substr($cli, $migrateStart, $bootstrapStart - $migrateStart)
    : '';
$assert($migrateBlock !== '', 'Comando migrate non trovato.');
$assert(!str_contains($migrateBlock, 'beginTransaction('), 'Le migrazioni DDL MySQL non devono usare una transazione PDO.');
$assert(!str_contains($migrateBlock, 'commit('), 'Le migrazioni DDL MySQL non devono invocare commit().');
$assert(str_contains($cli, 'migration_steps') && str_contains($cli, 'columnExists'), 'Le migrazioni devono poter riprendere dopo un DDL parzialmente applicato.');
$assert(!str_contains($schema, 'chk_cause_line_account'), 'Il CHECK su account_id non Ã¨ compatibile con le versioni MariaDB usate da Plesk.');
$assert(str_contains($schema, 'professional_validation_reference') && str_contains($schema, 'IMPORTED_PAYSLIPS'), 'Il payroll deve richiedere validazione professionale e supportare cedolini importati.');
$payrollService=(string)file_get_contents($base.'/app/Service/PayrollService.php');
$ecommerceService=(string)file_get_contents($base.'/app/Service/EcommerceService.php');
$leaveService=(string)file_get_contents($base.'/app/Service/LeaveService.php');
$inventoryService=(string)file_get_contents($base.'/app/Service/InventoryService.php');
$assert(str_contains($payrollService,"calculation_mode']!=='IMPORTED_PAYSLIPS'")&&str_contains($payrollService,'Quadratura netto non valida'),'Il payroll di produzione deve accettare solo cedolini importati e quadrati.');
$assert(str_contains($ecommerceService,'/orders/2026-01-01/orders')&&!str_contains($ecommerceService,'AWS4-HMAC-SHA256'),'Adapter Amazon non aggiornato a Orders 2026 e autenticazione LWA.');
$assert(str_contains($ecommerceService,"'PULL_PRODUCTS' =>")&&str_contains($ecommerceService,"'PUSH_PRICES' =>")&&str_contains($ecommerceService,"'ACK_ORDER' =>"),'Operazioni adapter e-commerce incomplete.');
$assert(str_contains($leaveService,'function cancel(')&&str_contains($inventoryService,'function cancelPick(')&&str_contains($inventoryService,'function cancelTransfer('),'Workflow di annullamento operativo incompleto.');
$assert(str_contains((string) file_get_contents($base . '/app/Service/SecretResolver.php'), 'ENV:'), 'I segreti delle integrazioni devono essere risolti da ambiente.');
$assert(str_contains($cli, 'random_bytes(18)') && str_contains($cli, 'CREDENZIALI SUPERUSER'), 'Bootstrap sicuro del superuser mancante.');
$assert(!str_contains($cli, "case 'setup:admin':"), 'Il setup azienda da CLI deve essere sostituito dal setup web riservato.');

$demoSeederPath = $base . '/app/Service/DemoDataSeeder.php';
$assert(is_file($demoSeederPath), 'Servizio di generazione dati demo mancante.');
$demoSeeder = is_file($demoSeederPath) ? (string) file_get_contents($demoSeederPath) : '';
$assert(
    str_contains($cli, "case 'demo:seed':")
    && str_contains($cli, '--months=')
    && str_contains($cli, '--reset')
    && str_contains($cli, "Password: {\$result['password']}"),
    'Il comando demo:seed deve supportare storico configurabile, rigenerazione e credenziali in output.'
);
$assert(
    str_contains($demoSeeder, 'beginTransaction()')
    && str_contains($demoSeeder, 'rollBack()')
    && str_contains($demoSeeder, "DELETE FROM organizations WHERE id = ?")
    && str_contains($demoSeeder, "random_bytes(4)"),
    'Il seed demo deve essere isolato, atomico, rigenerabile e usare una password casuale.'
);
$assert(
    str_contains($demoSeeder, 'array_values(')
    && str_contains($demoSeeder, "array_intersect_key(\$users"),
    'Gli utenti demo devono essere reindicizzati prima dei calcoli numerici payroll.'
);
$demoCoverage = [
    'customers', 'suppliers', 'products', 'documents', 'payment_schedules',
    'journal_entries', 'vat_movements', 'vat_settlements', 'vat_cash_events',
    'bank_transactions', 'accounting_open_items', 'payment_allocations',
    'inventory_movements', 'inventory_transfers', 'inventory_pick_lists',
    'leads', 'activities', 'projects', 'project_time_entries',
    'time_records', 'leave_requests', 'payroll_runs', 'payroll_details',
    'ecommerce_orders', 'rental_contracts', 'rental_tickets',
    'calendar_events', 'outbound_emails', 'sdi_transmissions',
    'fixed_assets', 'tax_deadlines', 'report_exports', 'import_batches',
];
foreach ($demoCoverage as $table) {
    $assert(str_contains($demoSeeder, "add('{$table}'"), "Copertura dati demo mancante per {$table}.");
}
$assert(
    !str_contains($demoSeeder, "'status' => 'COMMITTED'")
    && !str_contains($demoSeeder, "'status' => 'FINAL'")
    && !str_contains($demoSeeder, "'document_id' => \$documents['ddt'], 'status' => 'IN_PROGRESS'"),
    'Il seed demo usa stati non compatibili con lo schema MariaDB.'
);

$secretPatterns = [
    '/GOCSPX-[A-Za-z0-9_-]+/',
    '/DefaultCalendarSecretKey/i',
    '/calendar\.google\.clientSecret\s*=\s*[^\s]/i',
    '/DB_PASSWORD\s*=\s*(?!change-me\b)[^\s]+/i',
];
foreach ($iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($base, FilesystemIterator::SKIP_DOTS)) as $file) {
    if (!$file->isFile()
        || realpath($file->getPathname()) === realpath(__FILE__)
        || str_contains($file->getPathname(), DIRECTORY_SEPARATOR . 'vendor' . DIRECTORY_SEPARATOR)) {
        continue;
    }
    $content = file_get_contents($file->getPathname());
    if ($content === false) {
        continue;
    }
    foreach ($secretPatterns as $pattern) {
        $assert(!preg_match($pattern, $content), 'Possibile segreto in ' . $file->getPathname());
    }
}

echo sprintf("Controlli superati: %d\n", $passes);
if ($failures !== []) {
    fwrite(STDERR, "Controlli falliti:\n- " . implode("\n- ", $failures) . "\n");
    exit(1);
}
echo "Tutti i controlli sono passati.\n";
