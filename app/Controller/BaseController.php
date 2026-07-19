<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\ModuleManager;
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
        if (!in_array($role, $roles, true)) {
            $this->view->render('error', ['title' => 'Accesso negato', 'message' => 'Il tuo ruolo non consente questa operazione.'], 403);
        }
    }

    protected function requireFeature(string $featureKey): void
    {
        $feature = $this->config['features'][$featureKey] ?? null;
        if (!$feature) {
            $this->view->render('error', ['title' => 'Modulo non disponibile', 'message' => 'Configurazione del modulo non trovata.'], 404);
        }

        $manager = new ModuleManager($this->db, $this->config['features'], Auth::organizationId());
        if (!$manager->enabled($featureKey)) {
            $this->view->render('error', [
                'title' => 'Modulo disattivato',
                'message' => 'Il modulo “' . $feature['label'] . '” è disattivato per questa azienda.',
                'actionUrl' => Auth::isAdmin() ? '/settings/modules' : '/dashboard',
                'actionLabel' => Auth::isAdmin() ? 'Gestisci moduli' : 'Torna alla dashboard',
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
                'actionUrl' => Auth::isAdmin() ? '/settings/system' : '/dashboard',
                'actionLabel' => Auth::isAdmin() ? 'Controlla il sistema' : 'Torna alla dashboard',
            ], 503);
        }
    }
}
