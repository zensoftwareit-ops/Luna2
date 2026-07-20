<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\EndpointConfigService;

final class IntegrationController extends BaseController
{
    public function endpoints(): never
    {
        $this->authorize();
        $statement = $this->db->prepare('SELECT * FROM api_endpoint_configs WHERE organization_id = ? ORDER BY service_key, environment');
        $statement->execute([Auth::organizationId()]);
        $endpoints = $statement->fetchAll();
        $selectedEndpoint = [];
        $selectedId = (int) ($_GET['endpoint_id'] ?? 0);
        foreach ($endpoints as $endpoint) {
            if ((int) $endpoint['id'] === $selectedId) {
                $selectedEndpoint = $endpoint;
                break;
            }
        }
        $this->view->render('settings/endpoints', compact('endpoints', 'selectedEndpoint') + ['title' => 'Endpoint fatturazione elettronica']);
    }

    public function saveEndpoint(): never
    {
        $this->authorize();
        try {
            $id = (new EndpointConfigService($this->db, Auth::organizationId(), Auth::id()))
                ->save($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null);
            $this->audit('SAVE', 'api_endpoint_configs', $id, ['service_key' => $_POST['service_key'] ?? null]);
            $this->redirect('/settings/endpoints', 'Endpoint salvato. Nessuna chiamata esterna è stata eseguita.');
        } catch (InvalidArgumentException $exception) {
            $this->redirect('/settings/endpoints', $exception->getMessage(), 'error');
        }
    }

    private function authorize(): void
    {
        $this->requireFeature('accounting');
        $this->requireRoles(['OWNER', 'ADMIN']);
    }
}
