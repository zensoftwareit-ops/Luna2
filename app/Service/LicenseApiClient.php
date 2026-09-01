<?php

declare(strict_types=1);

namespace Luna\Service;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\GuzzleException;
use Luna\Core\Env;
use Luna\Core\LicenseSignatureVerifier;
use RuntimeException;

final class LicenseApiClient
{
    private Client $http;

    public function __construct(?Client $http = null)
    {
        $baseUrl = rtrim(trim((string) Env::get('LUNA_LICENSE_API_URL', '')), '/');
        if ($baseUrl === '' || !filter_var($baseUrl, FILTER_VALIDATE_URL) || parse_url($baseUrl, PHP_URL_SCHEME) !== 'https') {
            throw new RuntimeException('LUNA_LICENSE_API_URL deve essere un endpoint HTTPS valido.');
        }
        $this->http = $http ?? new Client([
            'base_uri' => $baseUrl . '/', 'timeout' => 15, 'connect_timeout' => 5,
            'http_errors' => false, 'verify' => true,
        ]);
    }

    public function activate(string $licenseKey, array $identity): array
    {
        return $this->request('POST', 'wp-json/luna/v1/licenses/activate', $licenseKey, $identity);
    }

    public function validate(string $licenseKey, array $identity): array
    {
        return $this->request('POST', 'wp-json/luna/v1/licenses/validate', $licenseKey, $identity);
    }

    public function deactivate(string $licenseKey, array $identity): array
    {
        return $this->request('POST', 'wp-json/luna/v1/licenses/deactivate', $licenseKey, $identity);
    }

    private function request(string $method, string $path, string $licenseKey, array $identity): array
    {
        $requestId = self::uuid();
        $timestamp = (string) time();
        $nonce = bin2hex(random_bytes(24));
        $body = [
            'request_id' => $requestId,
            'installation_uuid' => (string) ($identity['installation_uuid'] ?? ''),
            'instance_id' => (string) ($identity['instance_id'] ?? ''),
            'domain' => (string) ($identity['canonical_domain'] ?? ''),
            'environment' => (string) ($identity['environment'] ?? 'production'),
            'software_version' => (string) ($identity['software_version'] ?? ''),
        ];
        $json = json_encode($body, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES);
        $signingInput = implode("\n", [$timestamp, $nonce, $method, '/' . ltrim($path, '/'), hash('sha256', $json)]);
        $started = microtime(true);
        try {
            $response = $this->http->request($method, $path, [
                'headers' => [
                    'Accept' => 'application/json', 'Content-Type' => 'application/json',
                    'X-Luna-License' => $licenseKey, 'X-Luna-Request-Id' => $requestId,
                    'X-Luna-Timestamp' => $timestamp, 'X-Luna-Nonce' => $nonce,
                    'X-Luna-Signature' => base64_encode(hash_hmac('sha256', $signingInput, $licenseKey, true)),
                ],
                'body' => $json,
            ]);
        } catch (GuzzleException $exception) {
            throw new LicenseTransportException('Servizio licenze non raggiungibile.', $requestId, 0, (int) round((microtime(true) - $started) * 1000), $exception);
        }
        $status = $response->getStatusCode();
        $duration = (int) round((microtime(true) - $started) * 1000);
        $decoded = json_decode((string) $response->getBody(), true);
        if ($status < 200 || $status >= 300 || !is_array($decoded)) {
            $message = is_array($decoded) ? (string) ($decoded['message'] ?? $decoded['error'] ?? 'Richiesta rifiutata.') : 'Risposta non valida dal servizio licenze.';
            throw new LicenseTransportException($message, $requestId, $status, $duration);
        }
        if (!hash_equals($requestId, (string) ($decoded['request_id'] ?? ''))) {
            throw new LicenseTransportException('Request ID della risposta non corrispondente.', $requestId, $status, $duration);
        }
        $responseTimestamp = (int) ($decoded['timestamp'] ?? 0);
        if ($responseTimestamp <= 0 || abs(time() - $responseTimestamp) > 300) {
            throw new LicenseTransportException('Risposta licenza scaduta o fuori finestra temporale.', $requestId, $status, $duration);
        }
        $payload = $decoded['payload'] ?? null;
        if (!is_array($payload) || !is_string($decoded['signature'] ?? null)) {
            throw new LicenseTransportException('Payload licenza incompleto.', $requestId, $status, $duration);
        }
        (new LicenseSignatureVerifier())->verify($payload, $decoded['signature']);
        if (!hash_equals($requestId, (string) ($payload['request_id'] ?? ''))
            || (int) ($payload['response_timestamp'] ?? 0) !== $responseTimestamp) {
            throw new LicenseTransportException('La firma non lega correttamente risposta e richiesta.', $requestId, $status, $duration);
        }
        return ['payload' => $payload, 'signature' => $decoded['signature'], 'request_id' => $requestId, 'http_status' => $status, 'duration_ms' => $duration];
    }

    private static function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);
        return sprintf('%s-%s-%s-%s-%s', substr($hex, 0, 8), substr($hex, 8, 4), substr($hex, 12, 4), substr($hex, 16, 4), substr($hex, 20));
    }
}
