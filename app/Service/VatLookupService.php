<?php

declare(strict_types=1);

namespace Luna\Service;

use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;

final class VatLookupService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId)
    {
    }

    public function lookup(string $vat, string $country = 'IT'): array
    {
        $country = strtoupper(trim($country)) ?: 'IT';
        $vat = PartyAutomationService::normalizeVat($vat, $country);
        if ($vat === null) { throw new InvalidArgumentException('Inserisci una Partita IVA.'); }
        if ($country === 'IT' && !PartyAutomationService::validateItalianVat($vat)) {
            throw new InvalidArgumentException('La Partita IVA italiana non supera il controllo formale.');
        }
        $statement = $this->db->prepare(
            "SELECT * FROM api_endpoint_configs WHERE organization_id = ? AND service_key = 'VAT_LOOKUP'
             AND enabled = 1 ORDER BY environment = 'PRODUCTION' DESC, id DESC LIMIT 1"
        );
        $statement->execute([$this->organizationId]);
        $endpoint = $statement->fetch();
        if (!$endpoint) {
            throw new InvalidArgumentException('Connettore verifica Partita IVA non configurato. Configura il servizio VAT_LOOKUP negli endpoint; il controllo duplicati locale è già attivo.');
        }
        $headers = json_decode((string) ($endpoint['header_json'] ?? '{}'), true) ?: [];
        $secret = SecretResolver::resolve($endpoint['secret_reference'] ?? null);
        if ($endpoint['auth_type'] === 'BEARER') { $headers['Authorization'] = 'Bearer ' . $secret; }
        if ($endpoint['auth_type'] === 'API_KEY') { $headers['X-API-Key'] = $secret; }
        $path = trim((string) ($endpoint['receive_path'] ?: '/vat-lookup'));
        $client = new Client(['timeout' => (int) $endpoint['timeout_seconds'], 'verify' => (bool) $endpoint['verify_tls']]);
        $response = $client->get(rtrim((string) $endpoint['base_url'], '/') . '/' . ltrim($path, '/'), [
            'headers' => ['Accept' => 'application/json'] + $headers,
            'query' => ['country' => $country, 'vat_number' => $vat],
        ]);
        $data = json_decode((string) $response->getBody(), true);
        if (!is_array($data) || empty($data['business_name'])) {
            throw new InvalidArgumentException('Il servizio non ha restituito dati anagrafici utilizzabili.');
        }
        return [
            'vat_number' => $vat,
            'business_name' => trim((string) $data['business_name']),
            'tax_code' => trim((string) ($data['tax_code'] ?? '')),
            'address' => trim((string) ($data['address'] ?? '')),
            'postal_code' => trim((string) ($data['postal_code'] ?? '')),
            'city' => trim((string) ($data['city'] ?? '')),
            'province' => strtoupper(trim((string) ($data['province'] ?? ''))),
            'country_code' => strtoupper(trim((string) ($data['country_code'] ?? $country))),
            'status' => strtoupper(trim((string) ($data['status'] ?? 'VERIFIED'))),
        ];
    }
}
