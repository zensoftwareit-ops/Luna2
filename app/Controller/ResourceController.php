<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;

final class ResourceController extends BaseController
{
    public function index(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleReadRole($module);
        $search = trim((string) ($_GET['q'] ?? ''));
        $columns = array_values(array_unique(array_merge(['id'], $module['columns'])));
        $sql = 'SELECT ' . implode(', ', array_map([$this, 'identifier'], $columns))
            . ' FROM ' . $this->identifier($module['table']) . ' WHERE organization_id = :organization_id';
        $params = ['organization_id' => Auth::organizationId()];

        if ($search !== '' && !empty($module['search'])) {
            $parts = [];
            foreach ($module['search'] as $index => $column) {
                $key = 'q' . $index;
                $parts[] = $this->identifier($column) . " LIKE :{$key}";
                $params[$key] = '%' . $search . '%';
            }
            $sql .= ' AND (' . implode(' OR ', $parts) . ')';
        }
        $sql .= ' ORDER BY id DESC LIMIT 250';
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $rows = $statement->fetchAll();

        $this->view->render('resource/index', compact('slug', 'module', 'rows', 'search') + ['title' => $module['title']]);
    }

    public function create(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleWriteRole($module);
        $row = [];
        foreach ($module['fields'] as $field => $settings) {
            $row[$field] = $settings['default'] ?? null;
        }
        $this->view->render('resource/form', compact('slug', 'module', 'row') + ['title' => 'Nuovo ' . $module['singular']]);
    }

    public function edit(string $slug, string $id): never
    {
        $module = $this->module($slug);
        $this->requireModuleWriteRole($module);
        $statement = $this->db->prepare('SELECT * FROM ' . $this->identifier($module['table']) . ' WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $row = $statement->fetch();
        if (!$row) {
            $this->redirect('/r/' . $slug, 'Record non trovato.', 'error');
        }
        $this->view->render('resource/form', compact('slug', 'module', 'row') + ['title' => 'Modifica ' . $module['singular']]);
    }

    public function save(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleWriteRole($module);
        $id = filter_var($_POST['id'] ?? null, FILTER_VALIDATE_INT) ?: null;
        $values = [];
        $errors = [];

        foreach ($module['fields'] as $field => $settings) {
            $raw = $settings['type'] === 'checkbox' ? (isset($_POST[$field]) ? '1' : '0') : ($_POST[$field] ?? null);
            $value = $this->normalize($raw, $settings['type']);
            if (($settings['required'] ?? false) && ($value === null || $value === '')) {
                $errors[] = $settings['label'] . ' Ã¨ obbligatorio.';
            }
            $values[$field] = $value;
        }

        $tenantForeignKeys = ['user_id' => 'users', 'customer_id' => 'customers', 'supplier_id' => 'suppliers', 'product_id' => 'products', 'warehouse_id' => 'warehouses', 'project_id' => 'projects'];
        foreach ($tenantForeignKeys as $field => $table) {
            if (empty($values[$field])) {
                continue;
            }
            $check = $this->db->prepare("SELECT 1 FROM `{$table}` WHERE id = ? AND organization_id = ?");
            $check->execute([(int) $values[$field], Auth::organizationId()]);
            if (!$check->fetchColumn()) {
                $errors[] = ($module['fields'][$field]['label'] ?? $field) . ' non appartiene allâ€™azienda attiva.';
            }
        }

        if ($errors !== []) {
            $_SESSION['form_errors'] = $errors;
            $_SESSION['form_old'] = $values + ['id' => $id];
            $this->redirect($id ? "/r/{$slug}/{$id}/edit" : "/r/{$slug}/create");
        }

        if ($id) {
            $sets = [];
            foreach (array_keys($values) as $field) {
                $sets[] = $this->identifier($field) . ' = :' . $field;
            }
            $sets[] = 'updated_by = :updated_by';
            $sets[] = 'updated_at = NOW()';
            $values['updated_by'] = Auth::id();
            $values['id'] = $id;
            $values['organization_id'] = Auth::organizationId();
            $sql = 'UPDATE ' . $this->identifier($module['table']) . ' SET ' . implode(', ', $sets) . ' WHERE id = :id AND organization_id = :organization_id';
            $this->db->prepare($sql)->execute($values);
            $action = 'UPDATE';
        } else {
            $values = ['organization_id' => Auth::organizationId()] + $values + ['created_by' => Auth::id(), 'updated_by' => Auth::id()];
            $columns = array_keys($values);
            $sql = 'INSERT INTO ' . $this->identifier($module['table'])
                . ' (' . implode(', ', array_map([$this, 'identifier'], $columns)) . ', created_at, updated_at) VALUES ('
                . implode(', ', array_map(static fn (string $column): string => ':' . $column, $columns)) . ', NOW(), NOW())';
            $this->db->prepare($sql)->execute($values);
            $id = (int) $this->db->lastInsertId();
            $action = 'CREATE';
        }

        $this->audit($action, $module['table'], $id, $values);
        $this->redirect('/r/' . $slug, $module['singular'] . ' salvato correttamente.');
    }

    public function delete(string $slug, string $id): never
    {
        $module = $this->module($slug);
        $this->requireModuleWriteRole($module);
        $statement = $this->db->prepare('DELETE FROM ' . $this->identifier($module['table']) . ' WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $this->audit('DELETE', $module['table'], (int) $id);
        $this->redirect('/r/' . $slug, $module['singular'] . ' eliminato.');
    }

    public function export(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleReadRole($module);
        $columns = array_values(array_unique(array_merge(['id'], $module['columns'])));
        $statement = $this->db->prepare(
            'SELECT ' . implode(', ', array_map([$this, 'identifier'], $columns))
            . ' FROM ' . $this->identifier($module['table']) . ' WHERE organization_id = ? ORDER BY id'
        );
        $statement->execute([Auth::organizationId()]);

        header('Content-Type: text/csv; charset=UTF-8');
        header('Content-Disposition: attachment; filename="' . $slug . '-' . date('Ymd-His') . '.csv"');
        $output = fopen('php://output', 'wb');
        fwrite($output, "\xEF\xBB\xBF");
        fputcsv($output, $columns, ';');
        while ($row = $statement->fetch()) {
            fputcsv($output, array_map(static fn (string $column): mixed => $row[$column] ?? null, $columns), ';');
        }
        fclose($output);
        exit;
    }

    private function module(string $slug): array
    {
        $module = $this->config['modules'][$slug] ?? null;
        if (!$module) {
            throw new InvalidArgumentException('Modulo non disponibile.');
        }
        $this->requireFeature((string) $module['feature']);
        return $module;
    }

    private function identifier(string $value): string
    {
        if (!preg_match('/^[a-zA-Z_][a-zA-Z0-9_]*$/', $value)) {
            throw new InvalidArgumentException('Identificatore SQL non valido.');
        }
        return '`' . $value . '`';
    }

    private function normalize(mixed $value, string $type): mixed
    {
        if (is_string($value)) {
            $value = trim($value);
        }
        if ($value === '' || $value === null) {
            return null;
        }
        return match ($type) {
            'decimal' => str_contains((string) $value, ',')
                ? str_replace(',', '.', str_replace('.', '', (string) $value))
                : (string) $value,
            'number' => (int) $value,
            'checkbox' => (int) (bool) $value,
            'datetime-local' => str_replace('T', ' ', (string) $value) . (strlen((string) $value) === 16 ? ':00' : ''),
            default => $value,
        };
    }

    private function requireModuleWriteRole(array $module): void
    {
        $roles = match ($module['group']) {
            'ContabilitÃ ' => ['OWNER', 'ADMIN', 'ACCOUNTANT'],
            'HR' => ['OWNER', 'ADMIN', 'HR'],
            'Vendite e magazzino' => ['OWNER', 'ADMIN', 'SALES', 'WAREHOUSE'],
            'CRM', 'Noleggio', 'OperativitÃ ' => ['OWNER', 'ADMIN', 'SALES'],
            'Integrazioni' => ['OWNER', 'ADMIN'],
            default => ['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES'],
        };
        $this->requireRoles($roles);
    }

    private function requireModuleReadRole(array $module): void
    {
        $roles = match ($module['group']) {
            'ContabilitÃ ' => ['OWNER', 'ADMIN', 'ACCOUNTANT'],
            'HR' => ['OWNER', 'ADMIN', 'HR'],
            'Integrazioni' => ['OWNER', 'ADMIN'],
            'Noleggio' => ['OWNER', 'ADMIN', 'SALES'],
            default => ['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'HR', 'VIEWER'],
        };
        $this->requireRoles($roles);
 :ç®m¢G§²ÚîÆ­yÙ¥…É¥„±„Á…¥¹„”É¥ÁÉ½Ù„¸t°€ÐÄä¤ì(€€€€€€€€€€€ô((€€€€€€€€€€€€‘Á…É…µ•Ñ•ÉÌ€ô…ÉÉ…å}™¥±Ñ•È ‘µ…Ñ¡•Ì°€¥Í}ÍÑÉ¥¹œœ°IIe}%1QI}UM}-d¤ì(€€€€€€€€€€€€‘¡…¹‘±•È€ô€‘É½ÕÑ•l¡…¹‘±•Ètì(€€€€€€€€€€€¥˜€ ‘¡…¹‘±•È¥¹ÍÑ…¹•½˜±½ÍÕÉ”¤ì(€€€€€€€€€€€€€€€€‘¡…¹‘±•È ¸¸¹…ÉÉ…å}Ù…±Õ•Ì ‘Á…É…µ•Ñ•ÉÌ¤¤ì(€€€€€€€€€€€€€€€É•ÑÕÉ¸ì(€€€€€€€€€€€ô((€€€€€€€€€€€l‘±…ÍÌ°€‘…Ñ¥½¹t€ô€‘¡…¹‘±•Èì(€€€€€€€€€€€€‘½¹ÑÉ½±±•È€ô¹•Ü€‘±…ÍÌ ‘Ñ¡¥Ì´ù‘ˆ°€‘Ñ¡¥Ì´ùÙ¥•Ü°€‘Ñ¡¥Ì´ù½¹™¥œ¤ì(€€€€€€€€€€€€‘½¹ÑÉ½±±•È´ùì‘…Ñ¥½¹ô ¸¸¹…ÉÉ…å}Ù…±Õ•Ì ‘Á…É…µ•Ñ•ÉÌ¤¤ì(€€€€€€€€€€€É•ÑÕÉ¸ì(€€€€€€€ô((€€€€€€€€‘Ñ¡¥Ì´ùÙ¥•Ü´ùÉ•¹‘•È •ÉÉ½Èœ°lÑ¥Ñ±”œ€ôø€A…¥¹„¹½¸ÑÉ½Ù…Ñ„œ°€µ•ÍÍ…”œ€ôø€1„É¥Í½ÉÍ„É¥¡¥•ÍÑ„¹½¸•Í¥ÍÑ”¸t°€ÐÀÐ¤ì(€€€ô)ô(