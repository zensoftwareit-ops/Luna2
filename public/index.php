<?php

declare(strict_types=1);

use LunaApi\Cache\FileCache;
use LunaApi\Config;
use LunaApi\Domain\VatNumber;
use LunaApi\Http\JsonResponse;
use LunaApi\Provider\OpenApiCompanyProvider;
use LunaApi\Provider\ProviderException;
use LunaApi\Security\BearerAuthenticator;
use LunaApi\Security\ClientRegistry;
use LunaApi\Security\RateLimiter;
use LunaApi\Service\VatLookupService;
use LunaApi\Sdi\EncryptedFileStore;
use LunaApi\Sdi\FatturaPaInspector;
use LunaApi\Sdi\SdiEnvironment;
use LunaApi\Sdi\SdiContractResponse;
use LunaApi\Sdi\SdiService;
use LunaApi\Sdi\SdiTransport;
use LunaApi\Sdi\SoapPayloadParser;
use LunaApi\Sdi\SoapResponse;
use LunaApi\Sdi\MutualTlsGuard;

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
$sdiEnvironment = SdiEnvironment::fromPath($path);
$applicationPath = $sdiEnvironment->withoutPrefix($path);

if ($method === 'GET' && ($applicationPath === '/health' || $applicationPath === '/')) {
    JsonResponse::send([
        'status' => 'ok',
        'service' => 'luna2-api',
        'version' => '1.1.0',
        'environment' => $sdiEnvironment->value,
        'time' => gmdate('Y-m-d\\TH:i:s\\Z'),
        'request_id' => $requestId,
    ]);
}

if ($method === 'GET' && in_array($applicationPath, ['/sdi/ricezione-fatture', '/sdi/trasmissione-fatture'], true) && (isset($_GET['wsdl']) || isset($_GET['xsd']))) {
    SdiContractResponse::serve(
        $root,
        $sdiEnvironment,
        $applicationPath === '/sdi/ricezione-fatture' ? 'ricezione' : 'trasmissione',
        isset($_GET['xsd']),
    );
}

$makeSdi = static function () use ($root): array {
    $store = new EncryptedFileStore($root . '/storage/sdi', Config::string('SDI_STORAGE_KEY'));
    $service = new SdiService($store, new FatturaPaInspector(), new SdiTransport(), ClientRegistry::recipientMap());
    return [$store, $service];
};

if (in_array($applicationPath, ['/sdi/ricezione-fatture', '/sdi/trasmissione-fatture'], true)) {
    if ($method !== 'POST') {
        http_response_code(405);
        header('Allow: POST');
        exit;
    }
    if (Config::boolean('SDI_REQUIRE_MTLS', true) && !MutualTlsGuard::verified($_SERVER)) {
        http_response_code(403);
        header('Content-Type: text/plain; charset=utf-8');
        echo 'Certificato client SdI non verificato.';
        exit;
    }
    try {
        $maximum = Config::integer('SDI_MAX_FILE_BYTES', 5242880);
        $raw = file_get_contents('php://input', false, null, 0, $maximum * 2 + 1048576);
        if ($raw === false || $raw === '') {
            throw new InvalidArgumentException('Richiesta SOAP vuota.');
        }
        $payload = (new SoapPayloadParser())->parse(
            $raw,
            (string) ($_SERVER['CONTENT_TYPE'] ?? 'text/xml'),
            isset($_SERVER['HTTP_SOAPACTION']) ? (string) $_SERVER['HTTP_SOAPACTION'] : null,
        );
        if (strlen($payload->content) > $maximum) {
            throw new InvalidArgumentException('File SdI oltre il limite configurato.');
        }
        [, $sdi] = $makeSdi();
        if ($applicationPath === '/sdi/ricezione-fatture' && in_array(strtolower($payload->operation), ['ricevifatture', 'ricevifatturesdi'], true)) {
            $sdi->receiveInvoice($sdiEnvironment, $payload);
            SoapResponse::invoiceAccepted();
        }
        $sdi->receiveNotification($sdiEnvironment, $payload);
        SoapResponse::oneWayAccepted();
    } catch (InvalidArgumentException $exception) {
        error_log('[luna2-api][' . $requestId . '] sdi client fault: ' . $exception->getMessage());
        SoapResponse::fault($exception->getMessage(), true);
    } catch (Throwable $exception) {
        error_log('[luna2-api][' . $requestId . '] sdi server fault: ' . $exception->getMessage());
        SoapResponse::fault('Servizio temporaneamente non disponibile.');
    }
}

try {
    $authorization = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? null;
    $clientId = (new BearerAuthenticator(ClientRegistry::credentials()))->authenticate(is_string($authorization) ? $authorization : null);
    if ($clientId === null) {
        JsonResponse::send(['error' => 'unauthorized', 'message' => 'Token Bearer mancante o non valido.', 'request_id' => $requestId], 401, ['WWW-Authenticate' => 'Bearer']);
    }

    $remoteAddress = (string) ($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    $limiter = new RateLimiter($root . '/storage/cache', Config::integer('RATE_LIMIT_PER_MINUTE', 60));
    if (!$limiter->consume($clientId . ':' . $remoteAddress)) {
        JsonResponse::send(['error' => 'rate_limit_exceeded', 'message' => 'Troppe richieste. Riprovare tra un minuto.', 'request_id' => $requestId], 429, ['Retry-After' => '60']);
    }

    if (str_starts_with($applicationPath, '/v1/einvoices')) {
        if (str_starts_with($clientId, 'legacy-')) {
            JsonResponse::send(['error' => 'named_client_required', 'message' => 'Configurare il client in LUNA_API_CLIENTS per usare i servizi SdI.', 'request_id' => $requestId], 403);
        }
        [$store, $sdi] = $makeSdi();
        $maximum = Config::integer('SDI_MAX_FILE_BYTES', 5242880);

        if ($method === 'POST' && $applicationPath === '/v1/einvoices/outbound') {
            $contentType = strtolower((string) ($_SERVER['CONTENT_TYPE'] ?? ''));
            $recipientType = isset($_POST['recipient_type']) ? (string) $_POST['recipient_type'] : null;
            if (str_contains($contentType, 'application/json')) {
                $input = json_decode((string) file_get_contents('php://input'), true);
                if (!is_array($input)) {
                    JsonResponse::send(['error' => 'invalid_json', 'message' => 'Corpo JSON non valido.', 'request_id' => $requestId], 422);
                }
                $filename = basename((string) ($input['filename'] ?? ''));
                $content = base64_decode((string) ($input['content_base64'] ?? ''), true);
                $recipientType = isset($input['recipient_type']) ? (string) $input['recipient_type'] : $recipientType;
            } else {
                $upload = $_FILES['invoice'] ?? null;
                $filename = is_array($upload) ? basename((string) ($upload['name'] ?? '')) : '';
                $content = is_array($upload) && is_uploaded_file((string) ($upload['tmp_name'] ?? ''))
                    ? file_get_contents((string) $upload['tmp_name'])
                    : false;
            }
            if (!is_string($content) || $content === '' || $filename === '') {
                JsonResponse::send(['error' => 'invalid_invoice', 'message' => 'Allegare invoice oppure inviare filename e content_base64.', 'request_id' => $requestId], 422);
            }
            if (strlen($content) > $maximum) {
                JsonResponse::send(['error' => 'invoice_too_large', 'message' => 'File oltre il limite configurato.', 'request_id' => $requestId], 413);
            }
            $record = $sdi->send($sdiEnvironment, $clientId, $filename, $content, $recipientType);
            JsonResponse::send(['transmission' => $record, 'request_id' => $requestId], 202);
        }

        if ($method === 'GET' && $applicationPath === '/v1/einvoices') {
            $direction = strtolower((string) ($_GET['direction'] ?? 'inbound'));
            if (!in_array($direction, ['inbound', 'outbound', 'notifications'], true)) {
                JsonResponse::send(['error' => 'invalid_direction', 'message' => 'Direzione non valida.', 'request_id' => $requestId], 422);
            }
            $pending = filter_var($_GET['pending_only'] ?? false, FILTER_VALIDATE_BOOL);
            $rows = $store->list($sdiEnvironment, $clientId, $direction, (int) ($_GET['limit'] ?? 100), $pending);
            if ($direction === 'inbound') {
                $invoices = [];
                foreach ($rows as $row) {
                    $content = $store->content($sdiEnvironment, $clientId, 'inbound', (string) $row['id']);
                    if ($content === null) {
                        continue;
                    }
                    $invoices[] = [
                        'external_id' => $row['id'],
                        'sdi_id' => $row['sdi_id'] ?? null,
                        'filename' => !empty($row['has_normalized_xml']) ? preg_replace('/(?:\.xml)?\.p7m$/i', '.xml', (string) $row['filename']) : $row['filename'],
                        'original_filename' => $row['filename'],
                        'content_base64' => base64_encode($content),
                        'received_at' => $row['created_at'],
                        'status' => $row['status'],
                    ];
                }
                JsonResponse::send(['environment' => $sdiEnvironment->value, 'invoices' => $invoices, 'request_id' => $requestId]);
            }
            JsonResponse::send(['environment' => $sdiEnvironment->value, $direction => $rows, 'request_id' => $requestId]);
        }

        if (preg_match('~^/v1/einvoices/(inbound|outbound|notifications)/([a-f0-9-]+)(?:/(content|ack))?$~', $applicationPath, $match)) {
            [, $direction, $id, $action] = array_pad($match, 4, '');
            $record = $store->get($sdiEnvironment, $clientId, $direction, $id);
            if ($record === null) {
                JsonResponse::send(['error' => 'not_found', 'message' => 'Documento SdI non trovato.', 'request_id' => $requestId], 404);
            }
            if ($method === 'GET' && $action === 'content') {
                $content = $store->content($sdiEnvironment, $clientId, $direction, $id);
                if ($content === null) {
                    JsonResponse::send(['error' => 'not_found', 'message' => 'Contenuto non disponibile.', 'request_id' => $requestId], 404);
                }
                JsonResponse::send(['id' => $id, 'filename' => $record['filename'], 'content_base64' => base64_encode($content), 'request_id' => $requestId]);
            }
            if ($method === 'POST' && $action === 'ack') {
                $record = $store->update($sdiEnvironment, $clientId, $direction, $id, ['acknowledged_at' => gmdate('Y-m-d\TH:i:s\Z')]);
                JsonResponse::send(['document' => $record, 'request_id' => $requestId]);
            }
            if ($method === 'GET' && $action === '') {
                JsonResponse::send(['document' => $record, 'request_id' => $requestId]);
            }
        }
        JsonResponse::send(['error' => 'not_found', 'message' => 'Endpoint fatturazione elettronica non disponibile.', 'request_id' => $requestId], 404);
    }

    if ($method !== 'GET' || $applicationPath !== '/v1/vat-lookup') {
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
