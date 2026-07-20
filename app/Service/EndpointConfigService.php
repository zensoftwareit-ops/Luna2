<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use JsonException;
use PDO;

final class EndpointConfigService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function save(array $data, ?int $endpointId = null): int
    {
        $serviceKey = strtoupper(trim((string) ($data['service_key'] ?? 'EINVOICE')));
        $name = trim((string) ($data['display_name'] ?? ''));
        $environment = strtoupper((string) ($data['environment'] ?? 'TEST'));
        $baseUrl = rtrim(trim((string) ($data['base_url'] ?? '')), '/');
        $authType = strtoupper((string) ($data['auth_type'] ?? 'NONE'));
        $secretReference = $this->nullable($data['secret_reference'] ?? null);
        if (!preg_match('/^[A-Z0-9_:-]{2,64}$/', $serviceKey) || $name === ''
            || !in_array($environment, ['TEST','PRODUCTION'], true)
            || !in_array($authType, ['NONE','BEARER','API_KEY','OAUTH2','MTLS','CUSTOM'], true)
            || !filter_var($baseUrl, FILTER_VALIDATE_URL)) {
            throw new InvalidArgumentException('Configurazione endpoint non valida.');
        }
        $scheme = mb_strtolower((string) parse_url($baseUrl, PHP_URL_SCHEME));
        if ($environment === 'PRODUCTION' && $scheme !== 'https') {
            throw new InvalidArgumentException('In produzione è obbligatorio un endpoint HTTPS.');
        }
        if ($authType !== 'NONE' && ($secretReference === null || !preg_match('/^(ENV:[A-Z0-9_]+|vault:\/\/.+)$/', $secretReference))) {
            throw new InvalidArgumentException('Indica solo un riferimento sicuro ENV:NOME_VARIABILE o vault://…, non la credenziale.');
        }
        $headers = $this->headers($data['header_json'] ?? null);
        $timeout = min(300, max(1, (int) ($data['timeout_seconds'] ?? 30)));
        $values = [
            $serviceKey, $name, $environment, $baseUrl,
            $this->path($data['send_path'] ?? null), $this->path($data['receive_path'] ?? null),
            $this->path($data['status_path'] ?? null), $this->path($data['webhook_path'] ?? null),
            $authType, $secretReference, $headers === [] ? null : json_encode($headers, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES),
            $timeout, isset($data['verify_tls']) ? 1 : 0, isset($data['enabled']) ? 1 : 0,
            $this->nullable($data['notes'] ?? null),
        ];
        if ($endpointId !== null) {
            $statement = $this->db->prepare(
                'UPDATE api_endpoint_configs SET service_key = ?, display_name = ?, environment = ?, base_url = ?,
                 send_path = ?, receive_path = ?, status_path = ?, webhook_path = ?, auth_type = ?, secret_reference = ?,
                 header_json = ?, timeout_seconds = ?, verify_tls = ?, enabled = ?, notes = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$this->userId, $endpointId, $this->organizationId]));
            if ($statement->rowCount() === 0) {
                $exists = $this->db->prepare('SELECT 1 FROM api_endpoint_configs WHERE id = ? AND organization_id = ?');
                $exists->execute([$endpointId, $this->organizationId]);
                if (!$exists->fetchColumn()) {
                    throw new InvalidArgumentException('Configurazione endpoint non trovata.');
                }
            }
            return $endpointId;
        }
        $statement = $this->db->prepare(
            'INSERT INTO api_endpoint_configs
             (organization_id, service_key, display_name, environment, base_url, send_path, receive_path, status_path,
              webhook_path, auth_type, secret_reference, header_json, timeout_seconds, verify_tls, enabled, notes,
              created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge([$this->organizationId], $values, [$this->userId, $this->userId]));
        return (int) $this->db->lastInsertId();
    }

    private function headers(mixed $value): array
    {
        $value = trim((string) $value);
        if ($value === '') {
            return [];
        }
        try {
            $headers = json_decode($value, true, 32, JSON_THROW_ON_ERROR);
        } catch (JsonException) {
            throw new InvalidArgumentException('Gli header devono essere un oggetto JSON valido.');
        }
        if (!is_array($headers) || array_is_list($headers)) {
            throw new InvalidArgumentException('Gli header devono essere espressi come oggetto JSON.');
        }
        foreach ($headers as $name => $headerValue) {
            if (!preg_match('/^[A-Za-z0-9-]{1,100}$/', (string) $name) || !is_scalar($headerValue)) {
                throw new InvalidArgumentException('Nome o valore header non valido.');
            }
            if (preg_match('/authorization|api[-_]?key|secret|token|password/i', (string) $name)) {
                throw new InvalidArgumentException('Le credenziali non possono essere salvate negli header: usa il riferimento segreto.');
            }
        }
        return $headers;
    }

    private function path(mixed $value): ?string
    {
        $value = trim((string) $value);
        if ($value === '') {
            return null;
        }
        if (strlen($value) > 500 || str_contains($value, "\r") || str_contains($value, "\n")) {
            throw new InvalidArgumentException('Percorso endpoint non valido.');
        }
        return $value;
    }

    private function nullable(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
    }
}
