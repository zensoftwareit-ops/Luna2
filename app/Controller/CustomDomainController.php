<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\CustomDomainService;
use Throwable;

final class CustomDomainController extends BaseController
{
    public function index(): never
    {
        $this->requireRoles(['OWNER', 'ADMIN']);
        try {
            $service = new CustomDomainService($this->db);
            $domains = $service->all();
            $events = $service->events();
            $settings = $service->settings();
            $this->view->render('settings/domains', compact('domains', 'events', 'settings') + ['title' => 'Dominio personalizzato']);
        } catch (Throwable $exception) {
            $this->view->render('error', [
                'title' => 'Dominio personalizzato non disponibile',
                'message' => $exception->getMessage(),
                'schemaIssue' => str_contains($exception->getMessage(), 'custom_domains'),
                'actionUrl' => '/settings/system',
                'actionLabel' => 'Controlla le migrazioni',
            ], 503);
        }
    }

    public function store(): never
    {
        $this->requireRoles(['OWNER', 'ADMIN']);
        try {
            $service = new CustomDomainService($this->db);
            $id = $service->add((string) ($_POST['hostname'] ?? ''), Auth::id());
            $this->audit('CUSTOM_DOMAIN_CREATE', 'custom_domain', $id, ['hostname' => (string) ($_POST['hostname'] ?? '')]);
            $this->redirect('/settings/domains', 'Dominio registrato e predisposto. Ora configura il record CNAME indicato nella pagina; Luna2 completerà automaticamente DNS e HTTPS.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/domains', $exception->getMessage(), 'error');
        }
    }

    public function sync(string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN']);
        try {
            $domain = (new CustomDomainService($this->db))->reconcile((int) $id, Auth::id());
            $this->audit('CUSTOM_DOMAIN_SYNC', 'custom_domain', (int) $id, ['status' => $domain['status']]);
            $message = $domain['status'] === 'ACTIVE'
                ? 'Dominio attivo: DNS, alias Plesk e certificato HTTPS sono validi.'
                : 'Verifica completata. Stato corrente: ' . $domain['status'] . '.';
            $this->redirect('/settings/domains', $message, $domain['status'] === 'ERROR' ? 'error' : 'success');
        } catch (Throwable $exception) {
            $this->redirect('/settings/domains', $exception->getMessage(), 'error');
        }
    }

    public function remove(string $id): never
    {
        $this->requireRoles(['OWNER', 'ADMIN']);
        if ((string) ($_POST['confirmation'] ?? '') !== 'RIMUOVI') {
            $this->redirect('/settings/domains', 'Scrivi RIMUOVI per confermare.', 'error');
        }
        try {
            (new CustomDomainService($this->db))->remove((int) $id);
            $this->audit('CUSTOM_DOMAIN_DELETE', 'custom_domain', (int) $id);
            $this->redirect('/settings/domains', 'Alias rimosso da Plesk e da Luna2.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/domains', $exception->getMessage(), 'error');
        }
    }
}
