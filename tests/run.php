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
    $assert(str_contains($renderedAccounting, 'Prima nota') && str_contains($renderedAccounting, 'Registri IVA') && str_contains($renderedAccounting, 'Liquidazioni IVA'), 'Rendering viste contabili incompleto.');
} catch (Throwable $exception) {
    $assert(false, 'Errore rendering viste contabili: ' . $exception->getMessage());
}
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
$assert(str_contains($cli, 'random_bytes(18)') && str_contains($cli, 'CREDENZIALI SUPERUSER'), 'Bootstrap sicuro del superuser mancante.');
$assert(!str_contains($cli, "case 'setup:admin':"), 'Il setup azienda da CLI deve essere sostituito dal setup web riservato.');

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
