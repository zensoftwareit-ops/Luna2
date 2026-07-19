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
foreach ($modules as $slug => $module) {
    $table = $module['table'];
    $assert((bool) preg_match('/CREATE TABLE IF NOT EXISTS\s+' . preg_quote($table, '/') . '\s*\(/i', $schema), "Tabella mancante per {$slug}: {$table}");
    foreach (array_keys($module['fields']) as $column) {
        $assert((bool) preg_match('/\b' . preg_quote($column, '/') . '\b/i', $schema), "Colonna {$table}.{$column} non trovata nelle migrazioni");
    }
}

$composer = json_decode((string) file_get_contents($base . '/composer.json'), true);
$assert(is_array($composer) && isset($composer['require']['php']), 'composer.json non valido.');
$assert(is_file($base . '/public/index.php') && is_file($base . '/public/.htaccess'), 'Webroot incompleta.');
$assert(is_file($base . '/docs/DATEV_KOINOS_MIGRATION.md'), 'Piano migrazione Koinos mancante.');

$cli = (string) file_get_contents($base . '/bin/luna');
$migrateStart = strpos($cli, "case 'migrate':");
$setupStart = strpos($cli, "case 'setup:admin':");
$migrateBlock = $migrateStart !== false && $setupStart !== false
    ? substr($cli, $migrateStart, $setupStart - $migrateStart)
    : '';
$assert($migrateBlock !== '', 'Comando migrate non trovato.');
$assert(!str_contains($migrateBlock, 'beginTransaction('), 'Le migrazioni DDL MySQL non devono usare una transazione PDO.');
$assert(!str_contains($migrateBlock, 'commit('), 'Le migrazioni DDL MySQL non devono invocare commit().');

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
