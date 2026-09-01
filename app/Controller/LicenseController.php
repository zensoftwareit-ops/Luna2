<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\LicenseService;
use Luna\Service\LicenseLifecycleService;
use Throwable;

final class LicenseController extends BaseController
{
    public function index(): never
    {
        $this->requireSuperuser();
        $service = new LicenseService($this->db);
        $license = $service->snapshot();
        $enforcement = $service->enforcement();
        $organizationName = Auth::organizationName();
        $identity = null;
        try {
            $identity = $this->db->query('SELECT * FROM instance_identity WHERE id = 1')->fetch() ?: null;
            $logs = $this->db->query('SELECT * FROM license_sync_logs ORDER BY id DESC LIMIT 30')->fetchAll();
        } catch (Throwable) {
            $logs = [];
        }
        $this->view->render('settings/license', compact('license', 'enforcement', 'identity', 'logs', 'organizationName') + ['title' => 'Licenza installazione']);
    }

    public function activate(): never
    {
        $this->requireSuperuser();
        try {
            $payload = (new LicenseLifecycleService($this->db))->activate((string) ($_POST['license_key'] ?? ''), Auth::managedOrganizationId());
            $this->audit('LICENSE_ACTIVATE', 'license', substr((string) ($payload['license_id'] ?? ''), 0, 64), ['plan' => $payload['plan_code'] ?? null]);
            $this->redirect('/settings/license', 'Licenza attivata e verificata correttamente.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/license', $exception->getMessage(), 'error');
        }
    }

    public function sync(): never
    {
        $this->requireSuperuser();
        try {
            $payload = (new LicenseLifecycleService($this->db))->sync();
            $this->audit('LICENSE_SYNC', 'license', substr((string) ($payload['license_id'] ?? ''), 0, 64), ['status' => $payload['status'] ?? null]);
            $this->redirect('/settings/license', 'Licenza sincronizzata e firma verificata.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/license', 'Sincronizzazione non riuscita: ' . $exception->getMessage(), 'error');
        }
    }

    public function deactivate(): never
    {
        $this->requireSuperuser();
        if ((string) ($_POST['confirmation'] ?? '') !== 'DISATTIVA') {
            $this->redirect('/settings/license', 'Scrivi DISATTIVA per confermare.', 'error');
        }
        try {
            (new LicenseLifecycleService($this->db))->deactivate();
            $this->audit('LICENSE_DEACTIVATE', 'license', null);
            $this->redirect('/settings/license', 'Licenza disattivata su questa installazione.');
        } catch (Throwable $exception) {
            $this->redirect('/settings/license', 'Disattivazione non riuscita: ' . $exception->getMessage(), 'error');
        }
    }
}
