<?php

declare(strict_types=1);

namespace Luna\Core;

use PDO;
use Throwable;

final class LicenseService
{
    public const BASE_MODULES = ['anagraphics', 'sales', 'purchases', 'inventory', 'imports'];

    private ?array $snapshot = null;

    public function __construct(private readonly PDO $db, private readonly ?int $organizationId = null)
    {
    }

    public function enforcement(): string
    {
        $mode = strtolower((string) Env::get('LUNA_LICENSE_ENFORCEMENT', 'shadow'));
        return in_array($mode, ['shadow', 'warn', 'enforce'], true) ? $mode : 'shadow';
    }

    public function snapshot(): array
    {
        if ($this->snapshot !== null) {
            return $this->snapshot;
        }
        try {
            $row = $this->db->query('SELECT * FROM licenses WHERE active = 1 ORDER BY id DESC LIMIT 1')->fetch();
            if (!$row) {
                return $this->snapshot = $this->unconfigured();
            }
            $row = $this->hydrateSignedPayload($row);
            if (($row['signature_valid'] ?? true) === false) {
                $row['modules'] = [];
                $row['effective_status'] = 'INVALID_SIGNATURE';
                return $this->snapshot = $row;
            }
            $row['effective_status'] = $this->effectiveStatus($row);
            $row['modules'] = $this->effectiveModules($row);
            return $this->snapshot = $row;
        } catch (Throwable) {
            return $this->snapshot = $this->unconfigured();
        }
    }

    public function moduleAllowed(string $module, string $operation = 'READ'): bool
    {
        if ($this->enforcement() !== 'enforce') {
            return true;
        }
        $license = $this->snapshot();
        if (!$this->organizationAllowed($license)) {
            return false;
        }
        if (in_array($module, self::BASE_MODULES, true)) {
            return true;
        }
        if (in_array($module, $license['modules'], true)) {
            return true;
        }
        if (!in_array(strtoupper($operation), ['READ', 'EXPORT'], true) || empty($license['id'])) {
            return false;
        }
        try {
            $statement = $this->db->prepare('SELECT 1 FROM license_module_history WHERE license_id = ? AND module_key = ? LIMIT 1');
            $statement->execute([(int) $license['id'], $module]);
            return (bool) $statement->fetchColumn();
        } catch (Throwable) {
            return false;
        }
    }

    public function currentModuleAllowed(string $module): bool
    {
        if ($this->enforcement() !== 'enforce' || in_array($module, self::BASE_MODULES, true)) {
            return true;
        }
        $license = $this->snapshot();
        return $this->organizationAllowed($license) && in_array($module, $license['modules'], true);
    }

    public function operationAllowed(string $operation): bool
    {
        if ($this->enforcement() !== 'enforce' || in_array($operation, ['PUBLIC', 'READ', 'EXPORT', 'TECHNICAL'], true)) {
            return true;
        }
        $license = $this->snapshot();
        return $this->organizationAllowed($license) && in_array($license['effective_status'], ['ACTIVE', 'GRACE'], true);
    }

    private function effectiveStatus(array $license): string
    {
        $status = strtoupper((string) ($license['status'] ?? 'UNCONFIGURED'));
        if ($status === 'REVOKED') {
            return 'REVOKED';
        }
        $now = time();
        $validUntil = strtotime((string) ($license['valid_until'] ?? '')) ?: 0;
        $graceUntil = strtotime((string) ($license['grace_until'] ?? '')) ?: 0;
        if ($status === 'ACTIVE' && ($validUntil === 0 || $validUntil >= $now)) {
            if (!empty($license['last_validated_at']) && $graceUntil > 0 && $graceUntil < $now) {
                return 'OFFLINE_RESTRICTED';
            }
            return 'ACTIVE';
        }
        if ($graceUntil >= $now) {
            return 'GRACE';
        }
        return in_array($status, ['PAST_DUE', 'SUSPENDED', 'EXPIRED'], true) ? $status : 'EXPIRED';
    }

    private function hydrateSignedPayload(array $row): array
    {
        $signedPayload = json_decode((string) ($row['signed_payload_json'] ?? ''), true);
        if (!is_array($signedPayload) || empty($row['signature'])) {
            $modules = json_decode((string) ($row['modules_json'] ?? '[]'), true);
            $row['modules'] = is_array($modules) ? array_values(array_map('strval', $modules)) : [];
            $row['signature_valid'] = $this->enforcement() !== 'enforce';
            return $row;
        }
        try {
            (new LicenseSignatureVerifier())->verify($signedPayload, (string) $row['signature']);
            $identity = $this->db->query('SELECT installation_uuid, canonical_domain FROM instance_identity WHERE id = 1')->fetch();
            if (!$identity
                || !hash_equals((string) $identity['installation_uuid'], (string) ($signedPayload['installation_uuid'] ?? ''))
                || !hash_equals(mb_strtolower((string) ($identity['canonical_domain'] ?? '')), mb_strtolower((string) ($signedPayload['domain'] ?? '')))) {
                throw new \RuntimeException('Binding della licenza non valido.');
            }
            foreach (['status', 'plan_code', 'max_users', 'max_organizations', 'valid_from', 'valid_until', 'issued_at'] as $field) {
                if (array_key_exists($field, $signedPayload)) {
                    $row[$field] = $signedPayload[$field];
                }
            }
            $modules = $signedPayload['modules'] ?? [];
            $row['modules'] = is_array($modules) ? array_values(array_map('strval', $modules)) : [];
            $row['signature_valid'] = true;
        } catch (Throwable $exception) {
            $row['signature_valid'] = false;
            $row['signature_error'] = $exception->getMessage();
        }
        return $row;
    }

    private function effectiveModules(array $license): array
    {
        $modules = array_fill_keys((array) ($license['modules'] ?? []), true);
        try {
            $statement = $this->db->prepare(
                'SELECT module_key, effect FROM license_overrides
                 WHERE (license_id = ? OR license_id IS NULL) AND valid_from <= NOW() AND valid_until >= NOW() AND revoked_at IS NULL
                 ORDER BY id'
            );
            $statement->execute([(int) ($license['id'] ?? 0)]);
            foreach ($statement->fetchAll() as $row) {
                if ((string) ($row['module_key'] ?? '') !== '') {
                    $modules[(string) $row['module_key']] = (string) $row['effect'] === 'ALLOW';
                }
            }
        } catch (Throwable) {
            // Il payload firmato resta la fonte valida durante migrazioni o manutenzione.
        }
        return array_keys(array_filter($modules));
    }

    private function organizationAllowed(array $license): bool
    {
        $licensedOrganizationId = (int) ($license['licensed_organization_id'] ?? 0);
        return $this->organizationId === null || $this->organizationId <= 0 || $licensedOrganizationId <= 0
            || $licensedOrganizationId === $this->organizationId;
    }

    private function unconfigured(): array
    {
        return [
            'status' => 'UNCONFIGURED',
            'effective_status' => 'UNCONFIGURED',
            'plan_code' => null,
            'max_users' => null,
            'modules' => [],
            'last_validated_at' => null,
            'grace_until' => null,
        ];
    }
}
