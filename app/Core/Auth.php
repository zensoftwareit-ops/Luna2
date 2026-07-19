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
            'SELECT id, organization_id, name, email, password_hash, role, locale
             FROM users WHERE email = :email AND active = 1 LIMIT 1'
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
        return (int) ($_SESSION['user']['organization_id'] ?? 0);
    }

    public static function isAdmin(): bool
    {
        return in_array($_SESSION['user']['role'] ?? '', ['OWNER', 'ADMIN'], true);
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
