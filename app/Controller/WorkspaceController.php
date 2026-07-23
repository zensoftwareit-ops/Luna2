<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\WorkspaceService;
use Throwable;

final class WorkspaceController extends BaseController
{
    public function search(): never
    {
        $input = $_GET['q'] ?? '';
        $query = is_scalar($input) ? trim((string) $input) : '';
        $results = $this->service()->search($query);
        $this->view->render('workspace/search', compact('query', 'results') + ['title' => 'Ricerca']);
    }

    public function notifications(): never
    {
        $notifications = $this->service()->notifications();
        $this->view->render('workspace/notifications', compact('notifications') + ['title' => 'Notifiche']);
    }

    public function readNotification(string $id): never
    {
        $this->service()->markRead((int) $id);
        $redirect = $this->internalPath($_POST['redirect'] ?? '/workspace/notifications');
        $this->redirect($redirect);
    }

    public function readAllNotifications(): never
    {
        $this->service()->markRead();
        $this->redirect('/workspace/notifications', 'Tutte le notifiche sono state contrassegnate come lette.');
    }

    public function onboarding(): never
    {
        $onboarding = $this->service()->onboarding();
        $quality = $this->service()->dataQuality();
        $this->view->render('workspace/onboarding', compact('onboarding', 'quality') + ['title' => 'Configurazione guidata']);
    }

    public function onboardingStep(string $key): never
    {
        $this->requireRoles(['OWNER', 'ADMIN']);
        $completed = isset($_POST['completed']);
        try {
            $this->service()->setOnboardingStep($key, $completed, $_POST['notes'] ?? null);
            $this->audit($completed ? 'COMPLETE' : 'REOPEN', 'onboarding_progress', $key, ['notes' => $_POST['notes'] ?? null]);
            $this->redirect('/workspace/onboarding', 'Passaggio di configurazione aggiornato.');
        } catch (Throwable $exception) {
            $this->redirect('/workspace/onboarding', $exception->getMessage(), 'error');
        }
    }

    public function saveView(): never
    {
        $module = trim((string) ($_POST['module_key'] ?? ''));
        $redirect = $this->internalPath($_POST['redirect'] ?? '/dashboard');
        try {
            $query = json_decode((string) ($_POST['query_json'] ?? '{}'), true, 32, JSON_THROW_ON_ERROR);
            $this->service()->saveView($module, (string) ($_POST['name'] ?? ''), is_array($query) ? $query : [], isset($_POST['is_default']));
            $this->redirect($redirect, 'Vista salvata.');
        } catch (Throwable $exception) {
            $this->redirect($redirect, $exception->getMessage(), 'error');
        }
    }

    public function deleteView(string $id): never
    {
        $redirect = $this->internalPath($_POST['redirect'] ?? '/dashboard');
        $this->service()->deleteView((int) $id);
        $this->redirect($redirect, 'Vista eliminata.');
    }

    private function service(): WorkspaceService
    {
        return new WorkspaceService($this->db, Auth::organizationId(), Auth::id());
    }

    private function internalPath(mixed $path): string
    {
        $path = (string) $path;
        return str_starts_with($path, '/') && !str_starts_with($path, '//') ? $path : '/dashboard';
    }
}
