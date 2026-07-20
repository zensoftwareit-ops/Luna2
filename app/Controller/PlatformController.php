<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\OrganizationService;
use PDOException;

final class PlatformController extends BaseController
{
    private const USER_ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'HR', 'VIEWER'];

    public function index(): never
    {
        $this->requireSuperuser();
        $organizations = $this->organizations();
        if (Auth::managedOrganizationId() <= 0 && $organizations !== []) {
            Auth::manageOrganization((int) $organizations[0]['id'], (string) $organizations[0]['business_name']);
        }

        $organization = null;
        $users = [];
        if (Auth::managedOrganizationId() > 0) {
            $statement = $this->db->prepare(
                'SELECT * FROM organizations WHERE id = ? AND id <> ? AND active = 1'
            );
            $statement->execute([Auth::managedOrganizationId(), Auth::homeOrganizationId()]);
            $organization = $statement->fetch() ?: null;
            if ($organization) {
                $statement = $this->db->prepare(
                    "SELECT id, name, email, role, active, last_login_at, created_at
                     FROM users WHERE organization_id = ? AND role <> 'SUPERUSER' ORDER BY active DESC, name"
                );
                $statement->execute([(int) $organization['id']]);
                $users = $statement->fetchAll();
            }
        }

        $credentials = $_SESSION['generated_credentials'] ?? null;
        unset($_SESSION['generated_credentials']);
        $roles = self::USER_ROLES;
        $this->view->render('settings/company', compact('organizations', 'organization', 'users', 'credentials', 'roles') + [
            'title' => 'Setup piattaforma',
        ]);
    }

    public function createCompany(): never
    {
        $this->requireSuperuser();
        if (trim((string) ($_POST['business_name'] ?? '')) === '') {
            $this->redirect('/settings/company', 'La ragione sociale è obbligatoria.', 'error');
        }

        try {
            $organizationId = (new OrganizationService($this->db))->create($_POST);
        } catch (PDOException $exception) {
            $message = str_contains(strtolower($exception->getMessage()), 'duplicate')
                ? 'Partita IVA o dati aziendali già presenti.'
                : 'Non è stato possibile creare l’azienda.';
            $this->redirect('/settings/company', $message, 'error');
        }

        $businessName = trim((string) $_POST['business_name']);
        Auth::manageOrganization($organizationId, $businessName);
        $this->audit('CREATE_ORGANIZATION', 'organization', $organizationId, ['business_name' => $businessName]);
        $this->redirect('/settings/company', 'Azienda creata. Ora puoi configurare moduli e utenti.');
    }

    public function selectCompany(string $id): never
    {
        $this->requireSuperuser();
        $statement = $this->db->prepare(
            'SELECT id, business_name FROM organizations WHERE id = ? AND id <> ? AND active = 1'
        );
        $statement->execute([(int) $id, Auth::homeOrganizationId()]);
        $organization = $statement->fetch();
        if (!$organization) {
            $this->redirect('/settings/company', 'Azienda non disponibile.', 'error');
        }

        Auth::manageOrganization((int) $organization['id'], (string) $organization['business_name']);
        $this->audit('SELECT_ORGANIZATION', 'organization', (int) $organization['id']);
        $this->redirect('/settings/company');
    }

    public function createUser(): never
    {
        $organizationId = $this->requireManagedOrganization();
        $name = trim((string) ($_POST['name'] ?? ''));
        $email = mb_strtolower(trim((string) ($_POST['email'] ?? '')));
        $role = (string) ($_POST['role'] ?? 'VIEWER');
        if ($name === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || !in_array($role, self::USER_ROLES, true)) {
            $this->redirect('/settings/company', 'Controlla nome, email e ruolo del nuovo utente.', 'error');
        }

        $password = self::randomPassword();
        try {
            $statement = $this->db->prepare(
                'INSERT INTO users (organization_id, name, email, password_hash, role, active)
                 VALUES (?, ?, ?, ?, ?, 1)'
            );
            $statement->execute([$organizationId, $name, $email, password_hash($password, PASSWORD_ARGON2ID), $role]);
        } catch (PDOException $exception) {
            $message = str_contains(strtolower($exception->getMessage()), 'duplicate')
                ? 'Esiste già un utente con questo indirizzo email.'
                : 'Non è stato possibile creare l’utente.';
            $this->redirect('/settings/company', $message, 'error');
        }

        $userId = (int) $this->db->lastInsertId();
        $_SESSION['generated_credentials'] = ['name' => $name, 'email' => $email, 'password' => $password];
        $this->audit('CREATE_USER', 'user', $userId, ['email' => $email, 'role' => $role]);
        $this->redirect('/settings/company', 'Utente creato. Copia subito la password temporanea.');
    }

    public function toggleUser(string $id): never
    {
        $organizationId = $this->requireManagedOrganization();
        $statement = $this->db->prepare(
            "UPDATE users SET active = IF(active = 1, 0, 1), updated_at = NOW()
             WHERE id = ? AND organization_id = ? AND role <> 'SUPERUSER'"
        );
        $statement->execute([(int) $id, $organizationId]);
        $this->audit('TOGGLE_USER', 'user', (int) $id);
        $this->redirect('/settings/company', 'Stato dell’utente aggiornato.');
    }

    public function resetUserPassword(string $id): never
    {
        $organizationId = $this->requireManagedOrganization();
        $statement = $this->db->prepare(
            "SELECT id, name, email FROM users
             WHERE id = ? AND organization_id = ? AND role <> 'SUPERUSER'"
        );
        $statement->execute([(int) $id, $organizationId]);
        $user = $statement->fetch();
        if (!$user) {
            $this->redirect('/settings/company', 'Utente non trovato.', 'error');
        }

        $password = self::randomPassword();
        $this->db->prepare('UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?')
            ->execute([password_hash($password, PASSWORD_ARGON2ID), (int) $user['id']]);
        $_SESSION['generated_credentials'] = [
            'name' => (string) $user['name'],
            'email' => (string) $user['email'],
            'password' => $password,
        ];
        $this->audit('RESET_USER_PASSWORD', 'user', (int) $user['id']);
        $this->redirect('/settings/company', 'Nuova password generata. Copiala subito.');
    }

    public function changePassword(): never
    {
        $this->requireSuperuser();
        $current = (string) ($_POST['current_password'] ?? '');
        $password = (string) ($_POST['password'] ?? '');
        $confirmation = (string) ($_POST['password_confirmation'] ?? '');
        $statement = $this->db->prepare('SELECT password_hash FROM users WHERE id = ?');
        $statement->execute([Auth::id()]);
        $hash = (string) $statement->fetchColumn();
        if (!password_verify($current, $hash) || strlen($password) < 14 || $password !== $confirmation) {
            $this->redirect('/settings/company', 'Password attuale errata oppure nuova password non valida (minimo 14 caratteri).', 'error');
        }

        $this->db->prepare('UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?')
            ->execute([password_hash($password, PASSWORD_ARGON2ID), Auth::id()]);
        $this->audit('CHANGE_PASSWORD', 'user', Auth::id());
        $this->redirect('/settings/company', 'Password del superuser aggiornata.');
    }

    private function organizations(): array
    {
        $statement = $this->db->prepare(
            'SELECT id, business_name, vat_number, city, active
             FROM organizations WHERE id <> ? ORDER BY active DESC, business_name'
        );
        $statement->execute([Auth::homeOrganizationId()]);
        return $statement->fetchAll();
    }

    private static function randomPassword(): string
    {
        return rtrim(strtr(base64_encode(random_bytes(18)), '+/', '-_'), '=');
    }
}
