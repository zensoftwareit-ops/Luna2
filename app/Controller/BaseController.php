<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\ModuleManager;
use Luna\Core\PermissionGate;
use Luna\Core\SystemHealth;
use Luna\Core\View;
use PDO;

abstract class BaseController
{
    public function __construct(
        protected readonly PDO $db,
        protected readonly View $view,
        protected readonly array $config,
    ) {
    }

    protected function redirect(string $path, ?string $message = null, string $type = 'success'): never
    {
        if ($message !== null) {
            $_SESSION['flash'] = ['message' => $message, 'type' => $type];
        }
        header('Location: ' . $path);
        exit;
    }

    protected function audit(string $action, string $entityType, int|string|null $entityId, array $payload = []): void
    {
        $statement = $this->db->prepare(
            'INSERT INTO audit_logs (organization_id, user_id, action, entity_type, entity_id, ip_address, user_agent, payload_json, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())'
        );
        $statement->execute([
            Auth::organizationId(), Auth::id(), $action, $entityType, $entityId,
            substr((string) ($_SERVER['REMOTE_ADDR'] ?? ''), 0, 45),
            substr((string) ($_SERVER['HTTP_USER_AGENT'] ?? ''), 0, 500),
            json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
        ]);
    }

    protected function requireRoles(array $roles): void
    {
        $role = (string) (Auth::user()['role'] ?? '');
        if (in_array($role, $roles, true)) {
            return;
        }

        // I profili introdotti dopo i ruoli legacy usano il permesso della route,
        // evitando di doverli aggiungere manualmente a ogni controller esistente.
        if (in_array($role, ['MANAGER', 'OPERATOR'], true)) {
            $gate = new PermissionGate($this->config['permissions'] ?? []);
            if ($gate->allows($role, PermissionGate::currentPermission())) {
                return;
            }
        }
        $this->view->render('error', ['title' => 'Accesso negato', 'message' => 'Il tuo ruolo non consente questa operazione.'], 403);
    }

    protected function requirePermission(string $permission): void
    {
        $gate = new PermissionGate($this->config['permissions'] ?? []);
        if (!$gate->allows((string) (Auth::user()['role'] ?? ''), $permission)) {
            $this->view->render('error', ['title' => 'Accesso negato', 'message' => 'Il tuo profilo non dispone del permesso richiesto.'], 403);
        }
    }

    protected function requireSuperuser(): void
    {
        if (!Auth::isSuperuser()) {
            $this->view->render('error', [
                'title' => 'Accesso riservato',
                'message' => 'Questa funzione è disponibile esclusivamente al superuser di piattaforma.',
            ], 403);
        }
    }

    protected function requireManagedOrganization(): int
    {
        $this->requireSuperuser();
        $organizationId = Auth::managedOrganizationId();
        if ($organizationId <= 0) {
            $this->redirect('/settings/company', 'Crea o seleziona prima un’azienda.', 'error');
        }
        return $organizationId;
    }

    protected function requireOrganizationAdministrator(): int
    {
        if (Auth::isSuperuser()) {
            return $this->requireManagedOrganization();
        }
        $this->requireRoles(['OWNER', 'ADMIN']);
        $organizationId = Auth::homeOrganizationId();
        if ($organizationId <= 0) {
            $this->view->render('error', ['title' => 'Azienda non disponibile', 'message' => 'Non è stato possibile determinare l’azienda associata al tuo profilo.'], 403);
        }
        return $organizationId;
    }

    protected function requireFeature(string $featureKey): void
    {
        $feature = $this->config['features'][$featureKey] ?? null;
        if (!$feature) {
            $this->view->render('error', ['title' => 'Modulo non disponibile', 'message' => 'Configurazione del modulo non trovata.'], 404);
        }

        $manager = new ModuleManager($this->db, $this->config['features'], Auth::organizationId());
        if (!$manager->enabled($featureKey)) {
            $canManageModules = Auth::isSuperuser() || Auth::isAdmin();
            $this->view->render('error', [
                'title' => 'Modulo disattivato',
                'message' => 'Il modulo “' . $feature['label'] . '” è disattivato per questa azienda.',
                'actionUrl' => $canManageModules ? '/settings/modules' : '/dashboard',
                'actionLabel' => $canManageModules ? 'Gestisci moduli' : 'Torna alla dashboard',
            ], 403);
        }

        $missing = array_values(array_filter(
            $feature['required_tables'] ?? [],
            fn (string $table): bool => !SystemHealth::tableExists($this->db, $table),
        ));
        if ($missing !== []) {
            $this->view->render('error', [
                'title' => 'Modulo non inizializzato',
                'message' => 'Il database non è ancora aggiornato per questo modulo.',
                'schemaIssue' => true,
                'missingTables' => $missing,
                'actionUrl' => Auth::isSuperuser() ? '/settings/system' : '/dashboard',
                'actionLabel' => Auth::isSuperuser() ? 'Controlla il sistema' : 'Torna alla dashboard',
            ], 503);
        }
    }
}
