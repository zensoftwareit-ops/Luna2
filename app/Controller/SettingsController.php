<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\ModuleManager;
use Luna\Core\SystemHealth;

final class SettingsController extends BaseController
{
    public function modules(): never
    {
        $organizationId = $this->requireManagedOrganization();
        $manager = new ModuleManager($this->db, $this->config['features'], $organizationId);
        $features = $manager->all();
        $tableStatus = SystemHealth::featureTables($this->db, $features);
        $organizationName = Auth::organizationName();
        $this->view->render('settings/modules', compact('features', 'tableStatus', 'organizationName') + ['title' => 'Gestione moduli']);
    }

    public function saveModules(): never
    {
        $organizationId = $this->requireManagedOrganization();
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
        $this->view->render('settings/system', compact('migrations', 'runtime', 'tables') + ['title' => 'Stato del sistema']);
    }
}
