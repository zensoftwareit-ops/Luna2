<?php

declare(strict_types=1);

use LunaApi\Cache\FileCache;
use LunaApi\Domain\VatNumber;
use LunaApi\Provider\CompanyProvider;
use LunaApi\Provider\OpenApiCompanyProvider;
use LunaApi\Security\BearerAuthenticator;
use LunaApi\Service\VatLookupService;

require dirname(__DIR__) . '/bootstrap/autoload.php';

$passed = 0;
$failed = 0;

$test = static function (string $name, callable $callback) use (&$passed, &$failed): void {
    try {
        $callback();
        echo "[OK] {$name}\n";
        $passed++;
    } catch (Throwable $exception) {
        echo "[ERRORE] {$name}: {$exception->getMessage()}\n";
        $failed++;
    }
};

$assert = static function (bool $condition, string $message = 'Asserzione fallita.'): void {
    if (!$condition) {
        throw new RuntimeException($message);
    }
};

$test('normalizza partita IVA italiana', static function () use ($assert): void {
    $assert(VatNumber::italian('IT 12485671007') === '12485671007');
});

$test('rifiuta checksum errato', static function () use ($assert): void {
    try {
        VatNumber::italian('12485671008');
    } catch (InvalidArgumentException) {
        $assert(true);
        return;
    }
    throw new RuntimeException('La partita IVA non valida è stata accettata.');
});

$test('autentica token bearer senza confronto debole', static function () use ($assert): void {
    $auth = new BearerAuthenticator(['token-corretto']);
    $assert($auth->authenticate('Bearer token-corretto') !== null);
    $assert($auth->authenticate('Bearer token-errato') === null);
    $assert($auth->authenticate(null) === null);
});

$test('normalizza la risposta ufficiale OpenAPI Company', static function () use ($assert): void {
    $result = OpenApiCompanyProvider::normalizePayload([
        'success' => true,
        'data' => [[
            'taxCode' => '12485671007',
            'companyName' => 'OPENAPI S.P.A.',
            'vatCode' => '12485671007',
            'address' => ['registeredOffice' => [
                'streetName' => 'VIALE F TOMMASO MARINETTI 221',
                'town' => 'ROMA',
                'province' => 'RM',
                'zipCode' => '00143',
            ]],
            'activityStatus' => 'ATTIVA',
            'sdiCode' => 'USAL8PV',
            'lastUpdateTimestamp' => 1708705000,
        ]],
    ], '12485671007');
    $assert(is_array($result));
    $assert($result['business_name'] === 'OPENAPI S.P.A.');
    $assert($result['city'] === 'ROMA');
    $assert($result['status'] === 'ACTIVE');
    $assert($result['sdi_code'] === 'USAL8PV');
});

$test('usa cache dopo la prima ricerca', static function () use ($assert): void {
    $provider = new class implements CompanyProvider {
        public int $calls = 0;
        public function findItalianCompany(string $vatNumber): ?array
        {
            $this->calls++;
            return ['business_name' => 'OPENAPI S.P.A.', 'vat_number' => $vatNumber, 'country_code' => 'IT', 'status' => 'ACTIVE'];
        }
    };
    $directory = sys_get_temp_dir() . '/luna2-api-test-' . bin2hex(random_bytes(5));
    $service = new VatLookupService($provider, new FileCache($directory), 60);
    $first = $service->lookup('12485671007');
    $second = $service->lookup('12485671007');
    $assert($first['cache'] === 'MISS');
    $assert($second['cache'] === 'HIT');
    $assert($provider->calls === 1);
    foreach (glob($directory . '/*') ?: [] as $file) {
        unlink($file);
    }
    rmdir($directory);
});

echo "\nRisultato: {$passed} superati, {$failed} falliti.\n";
exit($failed === 0 ? 0 : 1);
