<?php

declare(strict_types=1);

namespace Luna\Core;

final class PermissionGate
{
    private static string $currentPermission = '';

    public function __construct(private readonly array $config)
    {
    }

    public function allows(string $role, string $permission): bool
    {
        if ($permission === '' || $permission === 'public') {
            return true;
        }

        foreach ((array) ($this->config['roles'][$role] ?? []) as $grant) {
            if ($grant === '*' || $grant === $permission) {
                return true;
            }
            if (str_ends_with($grant, '.*') && str_starts_with($permission, substr($grant, 0, -1))) {
                return true;
            }
            if (str_starts_with($grant, '*.') && str_ends_with($permission, substr($grant, 1))) {
                return true;
            }
        }

        return false;
    }

    public function family(string $role): string
    {
        return (string) ($this->config['role_families'][$role] ?? 'ROLE_OPERATOR');
    }

    public function enforcement(): string
    {
        $mode = strtolower((string) ($this->config['enforcement'] ?? 'shadow'));
        return in_array($mode, ['shadow', 'warn', 'enforce'], true) ? $mode : 'shadow';
    }

    public static function setCurrentPermission(string $permission): void
    {
        self::$currentPermission = $permission;
    }

    public static function currentPermission(): string
    {
        return self::$currentPermission;
    }
}
