<?php
declare(strict_types=1);

// Render real templates with isolated fixtures; no database or live account required.
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
$base = dirname(__DIR__);
spl_autoload_register(static function (string $class) use ($base): void {
    if (str_starts_with($class, 'Luna\\')) {
        require_once $base . '/app/' . str_replace('\\', '/', substr($class, 5)) . '.php';
    }
});
set_error_handler(static function (int $level, string $message, string $file, int $line): never {
    throw new ErrorException($message, 0, $level, $file, $line);
});
$_SESSION = ['user' => ['id' => 1, 'organization_id' => 1, 'name' => 'Utente verifica', 'role' => 'ADMIN', 'organization_name' => 'Azienda di verifica']];
$config = ['modules' => require $base . '/config/modules.php'];
$features = require $base . '/config/features.php';
$view = new class($features) {
    public function __construct(private array $features) {}
    public function features(): array { return array_map(static fn (array $f): array => $f + ['enabled' => true], $this->features); }
    public function workspaceSummary(): array { return ['unread' => 0, 'notifications' => [], 'readiness' => 80]; }
};
$fixtures = [
    'datev' => ['imports/datev-preview','/imports/2',[
        'batch'=>['id'=>2,'status'=>'READY','original_filename'=>'Esempio Koinos.csv','total_rows'=>100,'imported_rows'=>0,'error_rows'=>0,'error_message'=>''],
        'destination'=>['business_name'=>'Azienda di collaudo','vat_number'=>'01234567890'],
        'files'=>[['id'=>1,'filename'=>'Originale.csv','file_size'=>2048,'stored_relative_path'=>'example/original.csv']],
        'summary'=>[],'kind'=>'','search'=>'','page'=>1,'pages'=>2,'total'=>100,
        'rows'=>[['filename'=>'Originale.csv','source_row_number'=>7,'status'=>'STAGED','error_message'=>'','data'=>['kind'=>'journal_headers','key'=>'2025:1','data'=>['Descrizione'=>'Esempio <script>alert(1)</script>','IDPrimanota'=>'001'],'issue'=>'Mancano righe Dare e Avere.']]],
    ]],
    'quotes' => ['documents/form', '/documents/quotes/create', [
        'type' => 'quotes', 'definition' => ['counterparty' => 'customer', 'code' => 'QUOTE'],
        'counterparties' => [['id' => 1, 'code' => 'C001', 'business_name' => 'Cliente di verifica']],
        'products' => [], 'document' => ['document_date' => '2026-09-15', 'due_date' => '2026-10-15', 'currency' => 'EUR'],
    ]],
    'hr' => ['operations/hr', '/operations/hr', ['leaves' => [], 'balances' => [], 'users' => [['id' => 1, 'name' => 'Utente verifica']], 'runs' => [], 'configs' => [], 'admin' => true, 'selectedRun' => null]],
    'logistics' => ['operations/logistics', '/operations/logistics', ['balances' => [], 'transfers' => [], 'picks' => [], 'warehouses' => [], 'products' => [], 'orders' => []]],
    'communications' => ['operations/communications', '/operations/communications', ['settings' => [], 'messages' => [], 'documents' => []]],
    'ecommerce' => ['operations/ecommerce', '/operations/ecommerce', ['channels' => [], 'orders' => [], 'catalog' => [], 'queue' => []]],
    'calendar' => ['operations/calendar', '/operations/calendar', ['accounts' => [], 'events' => [], 'logs' => [], 'users' => []]],
    'endpoints' => ['settings/endpoints', '/settings/endpoints', ['endpoints' => []]],
    'imports' => ['imports/preview', '/imports/1', [
        'batch' => ['id' => 1, 'status' => 'READY', 'import_type' => 'customers', 'total_rows' => 1, 'imported_rows' => 0, 'error_rows' => 0, 'error_message' => ''],
        'rows' => [['source_row_number' => 2, 'status' => 'VALID', 'error_message' => '', 'data' => ['business_name' => 'Esempio & Associati <script>alert(1)</script>', 'vat_number' => '01234567890', 'city' => 'Milano', 'email' => 'verifica@example.test', 'details' => ['note' => 'Dato di verifica']]]],
    ]],
    'professional' => ['professional/index', '/professional', [
        'prints' => [], 'filings' => [], 'imports' => [], 'suggestions' => [], 'quality' => [], 'bankAccounts' => [], 'endpoints' => [],
        'printTypes' => ['JOURNAL' => 'Libro giornale'], 'filingTypes' => ['F24' => 'Deleghe F24'],
    ]],
];
$out = $base . '/storage/private/ui-review';
if (!is_dir($out)) { mkdir($out, 0770, true); }
$render = static function (string $template, array $data) use ($base, $config, $view): string {
    extract($data, EXTR_SKIP);
    $title = 'Verifica interfaccia';
    ob_start();
    require $base . '/views/' . $template . '.php';
    $content = (string) ob_get_clean();
    ob_start();
    require $base . '/views/layout.php';
    return (string) ob_get_clean();
};
foreach ($fixtures as $name => [$template, $route, $data]) {
    $_SERVER['REQUEST_URI'] = $route;
    $html = $render($template, $data);
    if ($name === 'imports' && (str_contains($html, '<script>alert(1)</script>') || !str_contains($html, 'Ragione sociale'))) {
        throw new RuntimeException('Import preview escaping or labels failed.');
    }
    if ($name === 'hr' && (!str_contains($html, '<option value="HOLIDAY">Ferie</option>') || !str_contains($html, 'Simulazione gestionale'))) {
        throw new RuntimeException('Translated form values must preserve their original codes.');
    }
    file_put_contents($out . '/' . $name . '.html', $html);
}
foreach (['DRAFT' => 'Bozza', 'IMPORTED_PAYSLIPS' => 'Cedolini importati', 'UNKNOWN_CODE' => 'UNKNOWN_CODE', '' => ''] as $code => $label) {
    if (Luna\Core\View::label($code) !== $label) { throw new RuntimeException('Unexpected UI label: ' . $code); }
}
echo count($fixtures) . " viste renderizzate senza avvisi PHP; etichette, valori dei form ed escaping verificati.\n";
