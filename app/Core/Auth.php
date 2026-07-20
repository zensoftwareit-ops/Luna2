<?php

declare(strict_types=1);

namespace Luna\Core;

use PDO;

final class Auth
{
    public function __construct(private readonly PDO $db)
    {
    }

    public function attempt(string $email, string $password): bool
    {
        $statement = $this->db->prepare(
            'SELECT u.id, u.organization_id, u.name, u.email, u.password_hash, u.role, u.locale,
                    o.business_name AS organization_name
             FROM users u
             INNER JOIN organizations o ON o.id = u.organization_id
             WHERE u.email = :email AND u.active = 1 AND o.active = 1 LIMIT 1'
        );
        $statement->execute(['email' => mb_strtolower(trim($email))]);
        $user = $statement->fetch();

        if (!$user || !password_verify($password, (string) $user['password_hash'])) {
            password_verify($password, '$argon2id$v=19$m=65536,t=4,p=1$YUtRbFc3enhwbEIuSjQybQ$b21FATTTi+m3ukbNDDnF0dwsS+k8/4yiMR5v39bNuv0');
            return false;
        }

        session_regenerate_id(true);
        unset($user['password_hash']);
        $_SESSION['user'] = $user;
        unset($_SESSION['managed_organization_id'], $_SESSION['managed_organization_name']);
        if (($user['role'] ?? '') === 'SUPERUSER') {
            $organization = $this->db->prepare(
                'SELECT id, business_name FROM organizations
                 WHERE active = 1 AND id <> ? ORDER BY id LIMIT 1'
            );
            $organization->execute([(int) $user['organization_id']]);
            if ($selected = $organization->fetch()) {
                self::manageOrganization((int) $selected['id'], (string) $selected['business_name']);
            }
        }
        Csrf::rotate();

        $this->db->prepare('UPDATE users SET last_login_at = NOW() WHERE id = ?')->execute([$user['id']]);
        return true;
    }

    public static function check(): bool
    {
        return isset($_SESSION['user']['id'], $_SESSION['user']['organization_id']);
    }

    public static function user(): array
    {
        return $_SESSION['user'] ?? [];
    }

    public static function id(): int
    {
        return (int) ($_SESSION['user']['id'] ?? 0);
    }

    public static function organizationId(): int
    {
        if (self::isSuperuser() && isset($_SESSION['managed_organization_id'])) {
            return (int) $_SESSION['managed_organization_id'];
        }
        return (int) ($_SESSION['user']['organization_id'] ?? 0);
    }

    public static function homeOrganizationId(): int
    {
        return (int) ($_SESSION['user']['organization_id'] ?? 0);
    }

    public static function organizationName(): string
    {
        if (self::isSuperuser() && isset($_SESSION['managed_organization_name'])) {
            return (string) $_SESSION['managed_organization_name'];
        }
        return (string) ($_SESSION['user']['organization_name'] ?? '');
    }

    public static function managedOrganizationId(): int
    {
        return self::isSuperuser() ? (int) ($_SESSION['managed_organization_id'] ?? 0) : 0;
    }

    public static function manageOrganization(int $organizationId, string $businessName): void
    {
        if (!self::isSuperuser() || $organizationId <= 0 || $organizationId === self::homeOrganizationId()) {
            return;
        }
        $_SESSION['managed_organization_id'] = $organizationId;
        $_SESSION['managed_organization_name'] = $businessName;
    }

    public static function isSuperuser(): bool
    {
        return ($_SESSION['user']['role'] ?? '') === 'SUPERUSER';
    }

    public static function isAdmin(): bool
    {
        return in_array($_SESSION['user']['role'] ?? '', ['OWNER', 'ADMIN'], true);
    }

    public static function landingPath(): string
    {
        return self::isSuperuser() ? '/settings/company' : '/dashboard';
    }

    public static function logout(): void
    {
        $_SESSION = [];
        if (ini_get('session.use_cookies')) {
            $params = session_get_cookie_params();
            setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'], $params['secure'], $params['httponly']);
        }
        session_destroy();
    }
}
