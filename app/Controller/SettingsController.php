<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\Csrf;
use Luna\Core\LicenseService;
use Luna\Core\ModuleManager;
use Luna\Core\SystemHealth;
use Luna\Service\SystemResetService;
use Throwable;

final class SettingsController extends BaseController
{
    public function modules(): never
    {
        $organizationId = $this->requireOrganizationAdministrator();
        $manager = new ModuleManager($this->db, $this->config['features'], $organizationId);
        $features = $manager->all();
        $tableStatus = SystemHealth::featureTables($this->db, $features);
        $organizationName = Auth::organizationName();
        $isSuperuser = Auth::isSuperuser();
        $this->view->render('settings/modules', compact('features', 'tableStatus', 'organizationName', 'isSuperuser') + ['title' => 'Gestione moduli']);
    }

    public function saveModules(): never
    {
        $organizationId = $this->requireOrganizationAdministrator();
        $selected = array_values(array_intersect(
            array_keys($this->config['features']),
            array_map('strval', (array) ($_POST['modules'] ?? [])),
        ));
        $manager = new ModuleManager($this->db, $this->config['features'], $organizationId);
        $manager->update($selected);
        $this->audit('UPDATE_MODULES', 'module_settings', null, ['enabled' => $selected]);
        $this->redirect('/settings/modules', 'Configurazione dei moduli aggiornata.');
    }

    public function system(): never
    {
        $this->requireSuperuser();
        $basePath = dirname(__DIR__, 2);
        $migrations = SystemHealth::migrationStatus($this->db, $basePath);
        $runtime = SystemHealth::runtime($basePath);
        $tables = SystemHealth::featureTables($this->db, $this->config['features']);
        $licenseService = new LicenseService($this->db);
        $license = $licenseService->snapshot();
        $licenseEnforcement = $licenseService->enforcement();
        $identity = null;
        $resetCounts = ['organizations' => 0, 'users' => 0];
        try {
            $identity = $this->db->query('SELECT * FROM instance_identity WHERE id = 1')->fetch() ?: null;
        } catch (Throwable) {
            // La diagnostica deve restare accessibile anche prima della migrazione 010.
        }
        try {
            $statement = $this->db->prepare('SELECT COUNT(*) FROM organizations WHERE id <> ?');
            $statement->execute([Auth::homeOrganizationId()]);
            $resetCounts['organizations'] = (int) $statement->fetchColumn();
            $resetCounts['users'] = (int) $this->db->query("SELECT COUNT(*) FROM users WHERE role <> 'SUPERUSER'")->fetchColumn();
        } catch (Throwable) {
            // La pagina di diagnostica deve funzionare anche con schema incompleto.
        }
        $this->view->render('settings/system', compact('migrations', 'runtime', 'tables', 'license', 'licenseEnforcement', 'identity', 'resetCounts') + ['title' => 'Stato del sistema']);
    }

    public function resetSystem(): never
    {
        $this->requireSuperuser();
        if (SystemHealth::migrationStatus($this->db, dirname(__DIR__, 2))['pending'] !== []) {
            $this->redirect('/settings/system', 'Aggiorna prima il database: il ripristino è stato annullato.', 'error');
        }
        $confirmation = trim((string) ($_POST['confirmation'] ?? ''));
        $password = (string) ($_POST['current_password'] ?? '');
        $policy = (string) ($_POST['user_policy'] ?? '');
        if (empty($_POST['backup_confirmed']) || $confirmation !== 'RESET LUNA2' || !in_array($policy, ['KEEP', 'DELETE'], true)) {
            $this->redirect('/settings/system', 'Conferme di sicurezza incomplete: ripristino annullato.', 'error');
        }

        $statement = $this->db->prepare("SELECT password_hash FROM users WHERE id = ? AND role = 'SUPERUSER' AND active = 1");
        $statement->execute([Auth::id()]);
        $hash = (string) $statement->fetchColumn();
        if ($hash === '' || !password_verify($password, $hash)) {
            $this->redirect('/settings/system', 'Password del superuser non corretta: ripristino annullato.', 'error');
        }

        $result = (new SystemResetService($this->db, dirname(__DIR__, 2)))->reset(
            Auth::id(),
            Auth::homeOrganizationId(),
            $policy === 'KEEP',
        );

        Auth::clearManagedOrganization();
        Csrf::rotate();
        $message = sprintf(
            'Ripristino completato: %d tabelle svuotate, %d righe e %d file eliminati.',
            $result['tables_cleared'],
            $result['rows_deleted'],
            $result['files_deleted'],
        );
        if ($policy === 'DELETE') {
            $message .= sprintf(' Eliminate %d aziende e %d utenti; il superuser è stato conservato.', $result['organizations_deleted'], $result['users_deleted']);
        } else {
            $message .= ' Aziende e utenti sono stati conservati.';
        }
        if ($result['file_errors'] !== []) {
            $message .= ' Alcuni file non sono stati eliminati: controllare i permessi di storage.';
        }
        $this->redirect('/settings/system', $message, $result['file_errors'] === [] ? 'success' : 'warning');
    }
}
