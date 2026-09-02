<?php

declare(strict_types=1);

namespace LunaApi\Provider;

use JsonException;

final class OpenApiCompanyProvider implements CompanyProvider
{
    public function __construct(
        private readonly string $baseUrl,
        private readonly string $token,
        private readonly int $timeoutSeconds = 12,
    ) {
    }

    public function findItalianCompany(string $vatNumber): ?array
    {
        $url = rtrim($this->baseUrl, '/') . '/IT-start/' . rawurlencode($vatNumber);
        $curl = curl_init($url);
        if ($curl === false) {
            throw new ProviderException('Impossibile inizializzare il collegamento al provider.');
        }

        curl_setopt_array($curl, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_CONNECTTIMEOUT => 5,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json',
                'Authorization: Bearer ' . $this->token,
                'User-Agent: Luna2-API/1.0',
            ],
        ]);

        $body = curl_exec($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
        $curlError = curl_error($curl);
        curl_close($curl);

        if ($body === false) {
            throw new ProviderException('Provider temporaneamente non raggiungibile: ' . $curlError, 503);
        }
        if ($status === 204 || $status === 404) {
            return null;
        }
        if ($status === 401 || $status === 403) {
            throw new ProviderException('Credenziali del provider non valide.', 503);
        }
        if ($status === 402) {
            throw new ProviderException('Credito del provider insufficiente.', 503);
        }
        if ($status < 200 || $status >= 300) {
            throw new ProviderException('Il provider ha restituito un errore HTTP ' . $status . '.', 502);
        }

        try {
            $payload = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
        } catch (JsonException) {
            throw new ProviderException('Risposta del provider non valida.');
        }
        if (!is_array($payload) || ($payload['success'] ?? false) !== true) {
            throw new ProviderException('Il provider non ha completato la ricerca.');
        }

        return self::normalizePayload($payload, $vatNumber);
    }

    /**
     * Traduce il contratto del provider nel contratto pubblico stabile di Luna2.
     *
     * @param array<string, mixed> $payload
     * @return array<string, mixed>|null
     */
    public static function normalizePayload(array $payload, string $vatNumber): ?array
    {
        $rows = $payload['data'] ?? null;
        if (!is_array($rows) || !isset($rows[0]) || !is_array($rows[0])) {
            return null;
        }
        $company = $rows[0];
        $office = $company['address']['registeredOffice'] ?? [];
        if (!is_array($office)) {
            $office = [];
        }

        return [
            'business_name' => self::text($company['companyName'] ?? null),
            'tax_code' => self::text($company['taxCode'] ?? null),
            'vat_number' => self::text($company['vatCode'] ?? null) ?: $vatNumber,
            'address' => self::text($office['streetName'] ?? null),
            'postal_code' => self::text($office['zipCode'] ?? null),
            'city' => self::text($office['town'] ?? null),
            'province' => self::text($office['province'] ?? null),
            'country_code' => 'IT',
            'status' => self::normalizeStatus(self::text($company['activityStatus'] ?? null)),
            'sdi_code' => self::text($company['sdiCode'] ?? null),
            'provider_updated_at' => isset($company['lastUpdateTimestamp']) && is_numeric($company['lastUpdateTimestamp'])
                ? gmdate('Y-m-d\\TH:i:s\\Z', (int) $company['lastUpdateTimestamp'])
                : null,
        ];
    }

    private static function text(mixed $value): ?string
    {
        return is_scalar($value) && trim((string) $value) !== '' ? trim((string) $value) : null;
    }

    private static function normalizeStatus(?string $status): string
    {
        return match (strtoupper($status ?? '')) {
            'ATTIVA', 'ACTIVE', 'A' => 'ACTIVE',
            'CESSATA', 'CLOSED' => 'CLOSED',
            'SOSPESA', 'SUSPENDED' => 'SUSPENDED',
            'INATTIVA', 'INACTIVE' => 'INACTIVE',
            default => 'UNKNOWN',
        };
    }
}
