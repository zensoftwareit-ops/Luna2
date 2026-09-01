<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use Luna\Core\LicenseKeyCipher;
use PDO;
use RuntimeException;
use Throwable;

final class LicenseLifecycleService
{
    private const STATUSES = ['ACTIVE', 'PAST_DUE', 'SUSPENDED', 'EXPIRED', 'REVOKED'];

    public function __construct(private readonly PDO $db)
    {
    }

    public function activate(string $licenseKey, int $organizationId): array
    {
        $licenseKey = trim($licenseKey);
        if (strlen($licenseKey) < 16 || strlen($licenseKey) > 500) {
            throw new InvalidArgumentException('Formato chiave licenza non valido.');
        }
        $this->assertCustomerOrganization($organizationId);
        $recent = (int) $this->db->query("SELECT COUNT(*) FROM license_sync_logs WHERE operation = 'ACTIVATE' AND result IN ('FAILED','REJECTED') AND created_at >= DATE_SUB(NOW(), INTERVAL 15 MINUTE)")->fetchColumn();
        if ($recent >= 5) {
            throw new RuntimeException('Troppi tentativi di attivazione. Attendi 15 minuti.');
        }
        try {
            $identity = $this->identity();
            $result = (new LicenseApiClient())->activate($licenseKey, $identity);
            $payload = $this->validatePayload($result['payload'], $identity, $organizationId);
            $licenseId = $this->persistNew($licenseKey, $payload, $result['signature'], $result['payload']);
            $this->safeLog($licenseId, $result, 'ACTIVATE', 'SUCCESS');
            return $payload;
        } catch (Throwable $exception) {
            $this->recordActivationFailure($exception);
            throw $exception;
        }
    }

    public function sync(): array
    {
        $license = $this->activeLicense();
        if (!$license || empty($license['license_key_ciphertext'])) {
            throw new RuntimeException('Nessuna licenza attiva da sincronizzare.');
        }
        $identity = $this->identity();
        try {
            $licenseKey = (new LicenseKeyCipher())->decrypt((string) $license['license_key_ciphertext']);
            $result = (new LicenseApiClient())->validate($licenseKey, $identity);
            $payload = $this->validatePayload($result['payload'], $identity, (int) ($license['licensed_organization_id'] ?? 0));
            $this->updateExisting((int) $license['id'], $payload, $result['signature'], $result['payload']);
            $this->safeLog((int) $license['id'], $result, 'VALIDATE', 'SUCCESS');
            return $payload;
        } catch (Throwable $exception) {
            $this->recordFailure((int) $license['id'], $exception, 'VALIDATE');
            throw $exception;
        }
    }

    public function deactivate(): void
    {
        $license = $this->activeLicense();
        if (!$license || empty($license['license_key_ciphertext'])) {
            throw new RuntimeException('Nessuna licenza attiva da disattivare.');
        }
        $identity = $this->identity();
        try {
            $licenseKey = (new LicenseKeyCipher())->decrypt((string) $license['license_key_ciphertext']);
            $result = (new LicenseApiClient())->deactivate($licenseKey, $identity);
            $payload = $this->validatePayload($result['payload'], $identity, (int) ($license['licensed_organization_id'] ?? 0));
            if ($payload['status'] !== 'REVOKED') {
                throw new RuntimeException('Il servizio non ha confermato la revoca della licenza.');
            }
            $this->updateExisting((int) $license['id'], $payload, $result['signature'], $result['payload']);
            $this->db->prepare('UPDATE licenses SET license_key_ciphertext = NULL, deactivated_at = NOW(), updated_at = NOW() WHERE id = ?')
                ->execute([(int) $license['id']]);
            $this->safeLog((int) $license['id'], $result, 'DEACTIVATE', 'SUCCESS');
        } catch (Throwable $exception) {
            $this->recordFailure((int) $license['id'], $exception, 'DEACTIVATE');
            throw $exception;
        }
    }

    public function dueForSync(): bool
    {
        $license = $this->activeLicense();
        return $license !== null && !empty($license['license_key_ciphertext'])
            && (empty($license['next_sync_at']) || strtotime((string) $license['next_sync_at']) <= time());
    }

    private function persistNew(string $licenseKey, array $payload, string $signature, array $signedPayload): int
    {
        $cipher = new LicenseKeyCipher();
        $this->db->beginTransaction();
        try {
            $this->db->exec('UPDATE licenses SET active = 0, updated_at = NOW() WHERE active = 1');
            $statement = $this->db->prepare(
                'INSERT INTO licenses
                 (remote_license_id, license_key_ciphertext, license_key_hash, license_key_fingerprint, status, plan_code,
                  max_users, max_organizations, licensed_organization_id, modules_json, signed_payload_json, signature,
                  signature_version, bound_instance_uuid, bound_domain, issued_at, valid_from, valid_until,
                  last_sync_at, next_sync_at, last_validated_at, grace_until, consecutive_sync_failures, active)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 12 HOUR), NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 0, 1)'
            );
            $statement->execute($this->values($payload, $signature, $signedPayload, $cipher->encrypt($licenseKey), LicenseKeyCipher::hash($licenseKey), LicenseKeyCipher::fingerprint($licenseKey)));
            $licenseId = (int) $this->db->lastInsertId();
            $this->syncModules($licenseId, $payload['modules']);
            $this->db->commit();
            return $licenseId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function updateExisting(int $licenseId, array $payload, string $signature, array $signedPayload): void
    {
        $this->db->beginTransaction();
        try {
            $values = $this->values($payload, $signature, $signedPayload, null, null, null);
            $statement = $this->db->prepare(
                'UPDATE licenses SET remote_license_id = ?, status = ?, plan_code = ?, max_users = ?, max_organizations = ?,
                 licensed_organization_id = ?, modules_json = ?, signed_payload_json = ?, signature = ?, signature_version = ?,
                 bound_instance_uuid = ?, bound_domain = ?, issued_at = ?, valid_from = ?, valid_until = ?, last_sync_at = NOW(),
                 next_sync_at = DATE_ADD(NOW(), INTERVAL 12 HOUR), last_validated_at = NOW(), grace_until = DATE_ADD(NOW(), INTERVAL 7 DAY),
                 consecutive_sync_failures = 0, last_sync_error = NULL, updated_at = NOW() WHERE id = ?'
            );
            // Rimuove i tre valori riservati alla chiave usati soltanto dall'INSERT.
            array_splice($values, 1, 3);
            $statement->execute([...$values, $licenseId]);
            $this->syncModules($licenseId, $payload['modules']);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function values(array $payload, string $signature, array $signedPayload, ?string $ciphertext, ?string $hash, ?string $fingerprint): array
    {
        return [
            $payload['license_id'], $ciphertext, $hash, $fingerprint, $payload['status'], $payload['plan_code'],
            $payload['max_users'], $payload['max_organizations'], $payload['licensed_organization_id'],
            json_encode($payload['modules'], JSON_THROW_ON_ERROR), json_encode($signedPayload, JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES),
            $signature, 'ED25519-V1', $payload['installation_uuid'], $payload['domain'],
            $payload['issued_at'], $payload['valid_from'], $payload['valid_until'],
        ];
    }

    private function syncModules(int $licenseId, array $modules): void
    {
        $this->db->prepare('UPDATE license_modules SET enabled = 0, updated_at = NOW() WHERE license_id = ?')->execute([$licenseId]);
        $this->db->prepare('UPDATE license_module_history SET active_in_current_plan = 0, updated_at = NOW() WHERE license_id = ?')->execute([$licenseId]);
        $current = $this->db->prepare(
            'INSERT INTO license_modules (license_id, module_key, enabled) VALUES (?, ?, 1)
             ON DUPLICATE KEY UPDATE enabled = 1, updated_at = NOW()'
        );
        $history = $this->db->prepare(
            'INSERT INTO license_module_history (license_id, module_key, active_in_current_plan, first_entitled_at, last_entitled_at)
             VALUES (?, ?, 1, NOW(), NOW()) ON DUPLICATE KEY UPDATE active_in_current_plan = 1, last_entitled_at = NOW(), updated_at = NOW()'
        );
        foreach ($modules as $module) {
            $current->execute([$licenseId, $module]);
            $history->execute([$licenseId, $module]);
        }
    }

    private function validatePayload(array $payload, array $identity, int $organizationId): array
    {
        $status = strtoupper((string) ($payload['status'] ?? ''));
        $modules = $payload['modules'] ?? null;
        if (!in_array($status, self::STATUSES, true) || !is_array($modules) || !array_is_list($modules)) {
            throw new InvalidArgumentException('Payload licenza non valido.');
        }
        $installationUuid = (string) ($payload['installation_uuid'] ?? '');
        $domain = mb_strtolower((string) ($payload['domain'] ?? ''));
        if (!hash_equals((string) $identity['installation_uuid'], $installationUuid)
            || !hash_equals(mb_strtolower((string) ($identity['canonical_domain'] ?? '')), $domain)) {
            throw new InvalidArgumentException('La licenza non appartiene a questa installazione o dominio.');
        }
        $modules = array_values(array_unique(array_filter(array_map('strval', $modules), static fn (string $module): bool => (bool) preg_match('/^[a-z][a-z0-9_]{1,99}$/', $module))));
        if (count($modules) !== count($payload['modules'])) {
            throw new InvalidArgumentException('Il payload contiene moduli non validi.');
        }
        $maxUsers = (int) ($payload['max_users'] ?? 0);
        if ($maxUsers < 1 || trim((string) ($payload['license_id'] ?? '')) === '' || trim((string) ($payload['plan_code'] ?? '')) === '') {
            throw new InvalidArgumentException('Piano o limiti licenza non validi.');
        }
        foreach (['issued_at', 'valid_from', 'valid_until'] as $date) {
            if (!empty($payload[$date]) && strtotime((string) $payload[$date]) === false) {
                throw new InvalidArgumentException('Data licenza non valida: ' . $date);
            }
        }
        return [
            'license_id' => substr((string) $payload['license_id'], 0, 190), 'status' => $status,
            'plan_code' => substr((string) $payload['plan_code'], 0, 100), 'max_users' => $maxUsers,
            'max_organizations' => max(1, (int) ($payload['max_organizations'] ?? 1)),
            'licensed_organization_id' => $organizationId > 0 ? $organizationId : null,
            'modules' => $modules, 'installation_uuid' => $installationUuid, 'domain' => $domain,
            'issued_at' => $payload['issued_at'] ?? null, 'valid_from' => $payload['valid_from'] ?? null,
            'valid_until' => $payload['valid_until'] ?? null,
        ];
    }

    private function identity(): array
    {
        $identity = $this->db->query('SELECT * FROM instance_identity WHERE id = 1')->fetch();
        if (!$identity || empty($identity['installation_uuid']) || empty($identity['canonical_domain'])) {
            throw new RuntimeException('Identità installazione incompleta. Verifica APP_URL ed esegui migrate.');
        }
        return $identity;
    }

    private function assertCustomerOrganization(int $organizationId): void
    {
        $statement = $this->db->prepare(
            "SELECT 1 FROM organizations o WHERE o.id = ? AND o.active = 1
             AND NOT EXISTS (SELECT 1 FROM users u WHERE u.organization_id = o.id AND u.role = 'SUPERUSER')"
        );
        $statement->execute([$organizationId]);
        if (!$statement->fetchColumn()) {
            throw new InvalidArgumentException('Seleziona prima un’azienda cliente valida da associare alla licenza.');
        }
    }

    private function activeLicense(): ?array
    {
        $row = $this->db->query('SELECT * FROM licenses WHERE active = 1 ORDER BY id DESC LIMIT 1')->fetch();
        return $row ?: null;
    }

    private function log(?int $licenseId, array $result, string $operation, string $status): void
    {
        $this->db->prepare(
            'INSERT INTO license_sync_logs (license_id, request_id, operation, http_status, result, duration_ms, created_at)
             VALUES (?, ?, ?, ?, ?, ?, NOW())'
        )->execute([$licenseId, $result['request_id'], $operation, $result['http_status'], $status, $result['duration_ms']]);
    }

    private function safeLog(?int $licenseId, array $result, string $operation, string $status): void
    {
        try {
            $this->log($licenseId, $result, $operation, $status);
        } catch (Throwable) {
            // Una licenza già verificata e salvata non deve risultare fallita per un problema del solo log tecnico.
        }
    }

    private function recordFailure(int $licenseId, Throwable $exception, string $operation): void
    {
        $requestId = $exception instanceof LicenseTransportException ? $exception->requestId : bin2hex(random_bytes(16));
        $httpStatus = $exception instanceof LicenseTransportException ? $exception->httpStatus : null;
        $duration = $exception instanceof LicenseTransportException ? $exception->durationMs : null;
        $this->db->prepare('UPDATE licenses SET last_sync_at = NOW(), next_sync_at = DATE_ADD(NOW(), INTERVAL 1 HOUR), consecutive_sync_failures = consecutive_sync_failures + 1, last_sync_error = ?, updated_at = NOW() WHERE id = ?')
            ->execute([substr($exception->getMessage(), 0, 1000), $licenseId]);
        $this->db->prepare(
            "INSERT IGNORE INTO license_sync_logs (license_id, request_id, operation, http_status, result, duration_ms, error_code, error_message, created_at)
             VALUES (?, ?, ?, ?, 'FAILED', ?, ?, ?, NOW())"
        )->execute([$licenseId, $requestId, $operation, $httpStatus, $duration, $exception::class, substr($exception->getMessage(), 0, 1000)]);
    }

    private function recordActivationFailure(Throwable $exception): void
    {
        $requestId = $exception instanceof LicenseTransportException ? $exception->requestId : bin2hex(random_bytes(16));
        $httpStatus = $exception instanceof LicenseTransportException ? $exception->httpStatus : null;
        $duration = $exception instanceof LicenseTransportException ? $exception->durationMs : null;
        $this->db->prepare(
            "INSERT IGNORE INTO license_sync_logs (license_id, request_id, operation, http_status, result, duration_ms, error_code, error_message, created_at)
             VALUES (NULL, ?, 'ACTIVATE', ?, 'FAILED', ?, ?, ?, NOW())"
        )->execute([$requestId, $httpStatus, $duration, $exception::class, substr($exception->getMessage(), 0, 1000)]);
    }
}
