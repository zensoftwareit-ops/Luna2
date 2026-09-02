<?php

declare(strict_types=1);

use LunaApi\Cache\FileCache;
use LunaApi\Config;
use LunaApi\Domain\VatNumber;
use LunaApi\Http\JsonResponse;
use LunaApi\Provider\OpenApiCompanyProvider;
use LunaApi\Provider\ProviderException;
use LunaApi\Security\BearerAuthenticator;
use LunaApi\Security\RateLimiter;
use LunaApi\Service\VatLookupService;

$root = dirname(__DIR__);
require $root . '/bootstrap/autoload.php';
Config::load($root);

$requestId = preg_match('/^[A-Za-z0-9._-]{8,80}$/', (string) ($_SERVER['HTTP_X_REQUEST_ID'] ?? ''))
    ? (string) $_SERVER['HTTP_X_REQUEST_ID']
    : bin2hex(random_bytes(12));
header('X-Request-Id: ' . $requestId);

$path = parse_url((string) ($_SERVER['REQUEST_URI'] ?? '/'), PHP_URL_PATH) ?: '/';
$path = '/' . trim($path, '/');
$method = strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET'));

if ($method === 'GET' && ($path === '/health' || $path === '/')) {
    JsonResponse::send([
        'status' => 'ok',
        'service' => 'luna2-api',
        'version' => '1.0.0',
        'time' => gmdate('Y-m-d\\TH:i:s\\Z'),
        'request_id' => $requestId,
    ]);
}

try {
    $tokens = array_values(array_filter(array_map('trim', explode(',', Config::string('LUNA_API_TOKENS')))));
    $authorization = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null;
    $clientId = (new BearerAuthenticator($tokens))->authenticate(is_string($authorization) ? $authorization : null);
    if ($clientId === null) {
        JsonResponse::send(['error' => 'unauthorized', 'message' => 'Token Bearer mancante o non valido.', 'request_id' => $requestId], 401, ['WWW-Authenticate' => 'Bearer']);
    }

    $remoteAddress = (string) ($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $limiter = new RateLimiter($root . '/storage/cache', Config::integer('RATE_LIMIT_PER_MINUTE', 60));
    if (!$limiter->consume($clientId . ':' . $remoteAddress)) {
        JsonResponse::send(['error' => 'rate_limit_exceeded', 'message' => 'Troppe richieste. Riprovare tra un minuto.', 'request_id' => $requestId], 429, ['Retry-After' => '60']);
    }

    if ($method !== 'GET' || $path !== '/v1/vat-lookup') {
        JsonResponse::send(['error' => 'not_found', 'message' => 'Endpoint non disponibile.', 'request_id' => $requestId], 404);
    }

    $country = strtoupper(trim((string) ($_GET['country'] ?? 'IT')));
    if ($country !== 'IT') {
        JsonResponse::send(['error' => 'unsupported_country', 'message' => 'In questa versione è supportata solo l’Italia.', 'request_id' => $requestId], 422);
    }
    try {
        $vatNumber = VatNumber::italian((string) ($_GET['vat_number'] ?? ''));
    } catch (InvalidArgumentException $exception) {
        JsonResponse::send(['error' => 'invalid_vat_number', 'message' => $exception->getMessage(), 'request_id' => $requestId], 422);
    }

    $environment = strtolower(Config::string('OPENAPI_ENV', 'sandbox'));
    $baseUrl = match ($environment) {
        'production' => 'https://company.openapi.com',
        'sandbox' => 'https://test.company.openapi.com',
        default => throw new RuntimeException('OPENAPI_ENV deve essere sandbox o production.'),
    };
    $provider = new OpenApiCompanyProvider($baseUrl, Config::string('OPENAPI_TOKEN'), Config::integer('OPENAPI_TIMEOUT_SECONDS', 12));
    $service = new VatLookupService($provider, new FileCache($root . '/storage/cache'), Config::integer('CACHE_TTL_SECONDS', 604800));
    $result = $service->lookup($vatNumber);

    if ($result['data'] === null) {
        JsonResponse::send(['error' => 'company_not_found', 'message' => 'Nessuna impresa trovata per la partita IVA indicata.', 'request_id' => $requestId], 404, ['X-Cache' => $result['cache']]);
    }
    JsonResponse::send($result['data'] + ['request_id' => $requestId], 200, ['X-Cache' => $result['cache']]);
} catch (ProviderException $exception) {
    error_log('[luna2-api][' . $requestId . '] provider: ' . $exception->getMessage());
    JsonResponse::send(['error' => 'provider_error', 'message' => $exception->getMessage(), 'request_id' => $requestId], $exception->httpStatus);
} catch (Throwable $exception) {
    error_log('[luna2-api][' . $requestId . '] ' . $exception::class . ': ' . $exception->getMessage());
    $message = Config::boolean('APP_DEBUG') ? $exception->getMessage() : 'Errore interno del servizio.';
    JsonResponse::send(['error' => 'internal_error', 'message' => $message, 'request_id' => $requestId], 500);
}
