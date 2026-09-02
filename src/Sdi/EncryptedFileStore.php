<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use RuntimeException;

final class EncryptedFileStore
{
    private readonly string $key;

    public function __construct(private readonly string $root, string $secret)
    {
        if (strlen($secret) < 32) {
            throw new RuntimeException('SDI_STORAGE_KEY deve contenere almeno 32 caratteri.');
        }
        $this->key = hash('sha256', $secret, true);
        if (!is_dir($root) && !mkdir($root, 0770, true) && !is_dir($root)) {
            throw new RuntimeException('Impossibile inizializzare lo storage SdI.');
        }
    }

    /**
     * @param array<string, mixed> $attributes
     * @return array<string, mixed>
     */
    public function create(SdiEnvironment $environment, string $clientId, string $direction, string $filename, string $content, array $attributes = [], ?string $normalizedXml = null): array
    {
        $id = self::uuid();
        $directory = $this->directory($environment, $clientId, $direction);
        $now = gmdate('Y-m-d\TH:i:s\Z');
        $metadata = array_replace([
            'id' => $id,
            'environment' => $environment->value,
            'client_id' => $clientId,
            'direction' => $direction,
            'filename' => basename($filename),
            'content_sha256' => hash('sha256', $content),
            'content_bytes' => strlen($content),
            'has_normalized_xml' => $normalizedXml !== null,
            'status' => 'RECEIVED',
            'created_at' => $now,
            'updated_at' => $now,
            'acknowledged_at' => null,
        ], $attributes, [
            'id' => $id,
            'environment' => $environment->value,
            'client_id' => $clientId,
            'direction' => $direction,
            'filename' => basename($filename),
        ]);

        $this->writeBinary($directory . '/' . $id . '.payload', $this->encrypt($content));
        if ($normalizedXml !== null) {
            $this->writeBinary($directory . '/' . $id . '.xml', $this->encrypt($normalizedXml));
        }
        $this->writeJson($directory . '/' . $id . '.json', $metadata);
        return $metadata;
    }

    /** @return array<string, mixed>|null */
    public function get(SdiEnvironment $environment, string $clientId, string $direction, string $id): ?array
    {
        if (!self::validId($id)) {
            return null;
        }
        $file = $this->directory($environment, $clientId, $direction, false) . '/' . $id . '.json';
        if (!is_file($file)) {
            return null;
        }
        return $this->readJson($file);
    }

    /** @return list<array<string, mixed>> */
    public function list(SdiEnvironment $environment, string $clientId, string $direction, int $limit = 100, bool $onlyPending = false): array
    {
        $directory = $this->directory($environment, $clientId, $direction, false);
        if (!is_dir($directory)) {
            return [];
        }
        $rows = [];
        foreach (glob($directory . '/*.json') ?: [] as $file) {
            $row = $this->readJson($file);
            if (!is_array($row) || ($onlyPending && !empty($row['acknowledged_at']))) {
                continue;
            }
            $rows[] = $row;
        }
        usort($rows, static fn (array $a, array $b): int => strcmp((string) ($b['created_at'] ?? ''), (string) ($a['created_at'] ?? '')));
        return array_slice($rows, 0, max(1, min($limit, 500)));
    }

    public function content(SdiEnvironment $environment, string $clientId, string $direction, string $id, bool $preferNormalized = true): ?string
    {
        if (!self::validId($id)) {
            return null;
        }
        $directory = $this->directory($environment, $clientId, $direction, false);
        $file = $preferNormalized && is_file($directory . '/' . $id . '.xml') ? $directory . '/' . $id . '.xml' : $directory . '/' . $id . '.payload';
        if (!is_file($file)) {
            return null;
        }
        return $this->decrypt((string) file_get_contents($file));
    }

    /** @param array<string, mixed> $changes @return array<string, mixed> */
    public function update(SdiEnvironment $environment, string $clientId, string $direction, string $id, array $changes): array
    {
        $metadata = $this->get($environment, $clientId, $direction, $id);
        if ($metadata === null) {
            throw new RuntimeException('Documento SdI non trovato.');
        }
        $metadata = array_replace($metadata, $changes, ['updated_at' => gmdate('Y-m-d\TH:i:s\Z')]);
        $this->writeJson($this->directory($environment, $clientId, $direction, false) . '/' . $id . '.json', $metadata);
        return $metadata;
    }

    /** @return array{client_id:string, metadata:array<string,mixed>}|null */
    public function findOutboundBySdiId(SdiEnvironment $environment, string $sdiId): ?array
    {
        $pattern = $this->root . '/' . $environment->value . '/*/outbound/*.json';
        foreach (glob($pattern) ?: [] as $file) {
            $row = $this->readJson($file);
            if (is_array($row) && (string) ($row['sdi_id'] ?? '') === $sdiId) {
                return ['client_id' => (string) $row['client_id'], 'metadata' => $row];
            }
        }
        return null;
    }

    /** @return array{client_id:string, metadata:array<string,mixed>}|null */
    public function findBySdiId(SdiEnvironment $environment, string $direction, string $sdiId): ?array
    {
        if (!in_array($direction, ['inbound', 'outbound', 'notifications'], true)) {
            return null;
        }
        $pattern = $this->root . '/' . $environment->value . '/*/' . $direction . '/*.json';
        foreach (glob($pattern) ?: [] as $file) {
            $row = $this->readJson($file);
            if (is_array($row) && (string) ($row['sdi_id'] ?? '') === $sdiId) {
                return ['client_id' => (string) $row['client_id'], 'metadata' => $row];
            }
        }
        return null;
    }

    private function directory(SdiEnvironment $environment, string $clientId, string $direction, bool $create = true): string
    {
        if (!preg_match('/^[A-Za-z0-9._-]{2,80}$/', $clientId) || !in_array($direction, ['inbound', 'outbound', 'notifications'], true)) {
            throw new RuntimeException('Percorso storage SdI non valido.');
        }
        $directory = $this->root . '/' . $environment->value . '/' . $clientId . '/' . $direction;
        if ($create && !is_dir($directory) && !mkdir($directory, 0770, true) && !is_dir($directory)) {
            throw new RuntimeException('Impossibile creare lo storage SdI.');
        }
        return $directory;
    }

    private function encrypt(string $plaintext): string
    {
        $iv = random_bytes(12);
        $tag = '';
        $ciphertext = openssl_encrypt($plaintext, 'aes-256-gcm', $this->key, OPENSSL_RAW_DATA, $iv, $tag, '', 16);
        if ($ciphertext === false) {
            throw new RuntimeException('Cifratura del documento non riuscita.');
        }
        return "L2E1" . $iv . $tag . $ciphertext;
    }

    private function decrypt(string $payload): string
    {
        if (!str_starts_with($payload, 'L2E1') || strlen($payload) < 32) {
            throw new RuntimeException('Documento cifrato non valido.');
        }
        $plaintext = openssl_decrypt(substr($payload, 32), 'aes-256-gcm', $this->key, OPENSSL_RAW_DATA, substr($payload, 4, 12), substr($payload, 16, 16));
        if ($plaintext === false) {
            throw new RuntimeException('Impossibile decifrare il documento SdI.');
        }
        return $plaintext;
    }

    /** @param array<string, mixed> $payload */
    private function writeJson(string $file, array $payload): void
    {
        $json = json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
        $this->writeBinary($file, $this->encrypt($json));
    }

    /** @return array<string,mixed>|null */
    private function readJson(string $file): ?array
    {
        try {
            $payload = json_decode($this->decrypt((string) file_get_contents($file)), true, 512, JSON_THROW_ON_ERROR);
            return is_array($payload) ? $payload : null;
        } catch (\Throwable) {
            return null;
        }
    }

    private function writeBinary(string $file, string $content): void
    {
        $temporary = $file . '.' . bin2hex(random_bytes(4)) . '.tmp';
        if (file_put_contents($temporary, $content, LOCK_EX) === false || !rename($temporary, $file)) {
            @unlink($temporary);
            throw new RuntimeException('Scrittura dello storage SdI non riuscita.');
        }
    }

    private static function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);
        return substr($hex, 0, 8) . '-' . substr($hex, 8, 4) . '-' . substr($hex, 12, 4) . '-' . substr($hex, 16, 4) . '-' . substr($hex, 20);
    }

    private static function validId(string $id): bool
    {
        return preg_match('/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/', $id) === 1;
    }
}
