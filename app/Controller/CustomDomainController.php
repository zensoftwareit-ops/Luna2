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
        $this->requireSuperuser();
        try {
            $service = new CustomDomainService($this->db);
            $domains = $service->all();
            $events = $service->events();
            $settings = $service->settings();
            $this->view->render('settings/domains', compact('domains', 'events', 'settings') + ['title' => 'Domini personalizzati']);
        } catch (Throwable $exception) {
            $this->view->render('error', [
                'title' => 'Domini personalizzati non disponibili',
                'message' => $exception->getMessage(),
                'schemaIssue' => str_contains($exception->getMessage(), 'custom_domains'),
                'actionUrl' => '/settings/system',
                'actionLabel' => 'Controlla le migrazioni',
            ], 503);
        }
    }

    public function store(): never
    {
        $this->requireSuperuser();
        try {
            $id = (new CustomDomainService($this->db))->add((string) ($_POST['hostname'] ?? ''), Auth::id());
            $this->audit('CUSTOM_DOMAIN_CREATE', 'custom_domain', $id, ['hostname' => (string) ($_POST['hostname'] ?? '')]);
            $this->redirect('/settings/domains', 'Dominio registrato. La verifica DNS e l’attivazione HTTPS sono state avviate.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/domains', $exception->getMessage(), 'error');
        }
    }

    public function sync(string $id): never
    {
        $this->requireSuperuser();
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
        $this->requireSuperuser();
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
