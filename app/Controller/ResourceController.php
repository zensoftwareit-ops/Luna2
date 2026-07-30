<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\TabularExportService;
use Luna\Service\WorkspaceService;

final class ResourceController extends BaseController
{
    public function index(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleReadRole($module);
        $searchInput = $_GET['q'] ?? '';
        $search = is_scalar($searchInput) ? trim((string) $searchInput) : '';
        $filters = is_array($_GET['filters'] ?? null) ? $_GET['filters'] : [];
        $dateFrom = is_array($_GET['date_from'] ?? null) ? $_GET['date_from'] : [];
        $dateTo = is_array($_GET['date_to'] ?? null) ? $_GET['date_to'] : [];
        $filterFields = array_filter(
            $module['fields'],
            static fn (array $field): bool => in_array($field['type'] ?? '', ['select', 'date', 'checkbox'], true),
        );
        foreach (['customer_id' => 'customers', 'supplier_id' => 'suppliers'] as $field => $table) {
            if (!isset($module['fields'][$field])) {
                continue;
            }
            $options = $this->db->prepare("SELECT id, code, business_name FROM {$table} WHERE organization_id = ? ORDER BY business_name");
            $options->execute([Auth::organizationId()]);
            $filterFields[$field] = [
                'label' => $table === 'customers' ? 'Cliente' : 'Fornitore',
                'type' => 'relation',
                'options' => array_column(array_map(
                    static fn (array $row): array => ['id' => (string) $row['id'], 'label' => trim((string) $row['code'] . ' · ' . (string) $row['business_name'], ' ·')],
                    $options->fetchAll(),
                ), 'label', 'id'),
            ];
        }
        $sortInput = $_GET['sort'] ?? 'id';
        $sort = is_scalar($sortInput) ? (string) $sortInput : 'id';
        if (!in_array($sort, array_merge(['id'], $module['columns']), true)) {
            $sort = 'id';
        }
        $directionInput = $_GET['direction'] ?? 'DESC';
        $direction = is_scalar($directionInput) && strtoupper((string) $directionInput) === 'ASC' ? 'ASC' : 'DESC';
        $perPageInput = $_GET['per_page'] ?? 50;
        $perPage = is_scalar($perPageInput) ? (int) $perPageInput : 50;
        $perPage = in_array($perPage, [25, 50, 100, 250], true) ? $perPage : 50;
        $pageInput = $_GET['page'] ?? 1;
        $page = max(1, is_scalar($pageInput) ? (int) $pageInput : 1);
        $columns = array_values(array_unique(array_merge(['id'], $module['columns'])));
        $where = ' FROM ' . $this->identifier($module['table']) . ' WHERE organization_id = :organization_id';
        $params = ['organization_id' => Auth::organizationId()];

        if ($search !== '' && !empty($module['search'])) {
            $parts = [];
            foreach ($module['search'] as $index => $column) {
                $key = 'q' . $index;
                $parts[] = $this->identifier($column) . " LIKE :{$key}";
                $params[$key] = '%' . $search . '%';
            }
            $where .= ' AND (' . implode(' OR ', $parts) . ')';
        }
        foreach ($filterFields as $field => $settings) {
            $value = $filters[$field] ?? null;
            if (($settings['type'] ?? '') === 'date') {
                $fromValue = $dateFrom[$field] ?? null;
                $toValue = $dateTo[$field] ?? null;
                if (is_scalar($fromValue) && $fromValue !== '') {
                    $key = 'date_from_' . $field;
                    $where .= ' AND ' . $this->identifier($field) . " >= :{$key}";
                    $params[$key] = (string) $fromValue;
                }
                if (is_scalar($toValue) && $toValue !== '') {
                    $key = 'date_to_' . $field;
                    $where .= ' AND ' . $this->identifier($field) . " <= :{$key}";
                    $params[$key] = (string) $toValue;
                }
                continue;
            }
            if ($value === null || $value === '' || !is_scalar($value)) {
                continue;
            }
            $key = 'filter_' . $field;
            $where .= ' AND ' . $this->identifier($field) . " = :{$key}";
            $params[$key] = ($settings['type'] ?? '') === 'checkbox' ? (int) (bool) $value : $value;
        }
        $count = $this->db->prepare('SELECT COUNT(*)' . $where);
        $count->execute($params);
        $total = (int) $count->fetchColumn();
        $pages = max(1, (int) ceil($total / $perPage));
        $page = min($page, $pages);
        $offset = ($page - 1) * $perPage;
        $sql = 'SELECT ' . implode(', ', array_map([$this, 'identifier'], $columns)) . $where
            . ' ORDER BY ' . $this->identifier($sort) . ' ' . $direction
            . ' LIMIT ' . $perPage . ' OFFSET ' . $offset;
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        $rows = $statement->fetchAll();
        $savedViews = (new WorkspaceService($this->db, Auth::organizationId(), Auth::id()))->savedViews($slug);

        $this->view->render('resource/index', compact(
            'slug', 'module', 'rows', 'search', 'filters', 'filterFields', 'sort', 'direction',
            'dateFrom', 'dateTo', 'perPage', 'page', 'pages', 'total', 'savedViews'
        ) + ['title' => $module['title']]);
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
                $errors[] = $settings['label'] . ' è obbligatorio.';
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
                $errors[] = ($module['fields'][$field]['label'] ?? $field) . ' non appartiene all’azienda attiva.';
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
            if ($module['author_columns'] ?? true) {
                $sets[] = 'updated_by = :updated_by';
                $values['updated_by'] = Auth::id();
            }
            $sets[] = 'updated_at = NOW()';
            $values['id'] = $id;
            $values['organization_id'] = Auth::organizationId();
            $sql = 'UPDATE ' . $this->identifier($module['table']) . ' SET ' . implode(', ', $sets) . ' WHERE id = :id AND organization_id = :organization_id';
            $this->db->prepare($sql)->execute($values);
            $action = 'UPDATE';
        } else {
            // Lascia che il database applichi i DEFAULT ai campi opzionali
            // anziché forzarli a NULL (per esempio customers.country_code).
            $insertValues = array_filter(
                $values,
                static fn (mixed $value): bool => $value !== null,
            );
            $insertValues = ['organization_id' => Auth::organizationId()]
                + $insertValues;
            if ($module['author_columns'] ?? true) {
                $insertValues += ['created_by' => Auth::id(), 'updated_by' => Auth::id()];
            }
            $columns = array_keys($insertValues);
            $sql = 'INSERT INTO ' . $this->identifier($module['table'])
                . ' (' . implode(', ', array_map([$this, 'identifier'], $columns)) . ', created_at, updated_at) VALUES ('
                . implode(', ', array_map(static fn (string $column): string => ':' . $column, $columns)) . ', NOW(), NOW())';
            $this->db->prepare($sql)->execute($insertValues);
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

    public function export(string $slug, string $format = 'csv'): never
    {
        $module = $this->module($slug);
        $this->requireModuleReadRole($module);
        $searchInput = $_GET['q'] ?? '';
        $search = is_scalar($searchInput) ? trim((string) $searchInput) : '';
        $filters = is_array($_GET['filters'] ?? null) ? $_GET['filters'] : [];
        $dateFrom = is_array($_GET['date_from'] ?? null) ? $_GET['date_from'] : [];
        $dateTo = is_array($_GET['date_to'] ?? null) ? $_GET['date_to'] : [];
        $columns = array_values(array_unique(array_merge(['id'], $module['columns'])));
        $where = ' FROM ' . $this->identifier($module['table']) . ' WHERE organization_id = :organization_id';
        $params = ['organization_id' => Auth::organizationId()];
        if ($search !== '' && !empty($module['search'])) {
            $parts = [];
            foreach ($module['search'] as $index => $column) {
                $key = 'q' . $index;
                $parts[] = $this->identifier($column) . " LIKE :{$key}";
                $params[$key] = '%' . $search . '%';
            }
            $where .= ' AND (' . implode(' OR ', $parts) . ')';
        }
        foreach ($module['fields'] as $field => $settings) {
            $type = $settings['type'] ?? '';
            if ($type === 'date') {
                if (isset($dateFrom[$field]) && is_scalar($dateFrom[$field]) && $dateFrom[$field] !== '') {
                    $where .= ' AND ' . $this->identifier($field) . ' >= :from_' . $field;
                    $params['from_' . $field] = (string) $dateFrom[$field];
                }
                if (isset($dateTo[$field]) && is_scalar($dateTo[$field]) && $dateTo[$field] !== '') {
                    $where .= ' AND ' . $this->identifier($field) . ' <= :to_' . $field;
                    $params['to_' . $field] = (string) $dateTo[$field];
                }
                continue;
            }
            $value = $filters[$field] ?? null;
            if ($value === null || $value === '' || !is_scalar($value)) {
                continue;
            }
            $where .= ' AND ' . $this->identifier($field) . ' = :filter_' . $field;
            $params['filter_' . $field] = $type === 'checkbox' ? (int) (bool) $value : $value;
        }
        foreach (['customer_id', 'supplier_id'] as $field) {
            $value = $filters[$field] ?? null;
            if (!isset($module['fields'][$field]) || $value === null || $value === '' || !is_scalar($value)) {
                continue;
            }
            $where .= ' AND ' . $this->identifier($field) . ' = :relation_' . $field;
            $params['relation_' . $field] = (int) $value;
        }
        $statement = $this->db->prepare(
            'SELECT ' . implode(', ', array_map([$this, 'identifier'], $columns)) . $where . ' ORDER BY id'
        );
        $statement->execute($params);
        $definitions = [];
        foreach ($columns as $column) {
            $field = $module['fields'][$column] ?? [];
            $type = match ($field['type'] ?? '') {
                'decimal' => 'decimal',
                'number' => 'number',
                'date' => 'date',
                'datetime-local' => 'datetime',
                'checkbox' => 'boolean',
                default => $column === 'id' ? 'number' : 'text',
            };
            $definitions[] = ['key' => $column, 'label' => $field['label'] ?? ucfirst(str_replace('_', ' ', $column)), 'type' => $type];
        }
        $filterLabels = [];
        if ($search !== '') {
            $filterLabels['Ricerca'] = $search;
        }
        foreach ($filters as $field => $value) {
            if (is_scalar($value) && $value !== '') {
                $filterLabels[$module['fields'][$field]['label'] ?? ucfirst(str_replace('_', ' ', $field))] = (string) $value;
            }
        }
        foreach ($dateFrom as $field => $value) {
            if (is_scalar($value) && $value !== '') {
                $filterLabels[($module['fields'][$field]['label'] ?? $field) . ' dal'] = (string) $value;
            }
        }
        foreach ($dateTo as $field => $value) {
            if (is_scalar($value) && $value !== '') {
                $filterLabels[($module['fields'][$field]['label'] ?? $field) . ' al'] = (string) $value;
            }
        }
        (new TabularExportService())->stream(
            $format,
            $module['title'],
            $definitions,
            $statement->fetchAll(),
            $filterLabels,
            Auth::organizationName(),
            $slug,
        );
    }

    public function bulk(string $slug): never
    {
        $module = $this->module($slug);
        $this->requireModuleWriteRole($module);
        $ids = array_values(array_unique(array_filter(
            array_map('intval', is_array($_POST['ids'] ?? null) ? $_POST['ids'] : []),
            static fn (int $id): bool => $id > 0,
        )));
        $ids = array_slice($ids, 0, 250);
        $operation = (string) ($_POST['operation'] ?? '');
        if ($ids === []) {
            $this->redirect('/r/' . $slug, 'Selezionare almeno un record.', 'error');
        }
        if (!in_array($operation, ['export', 'activate', 'deactivate'], true)) {
            $this->redirect('/r/' . $slug, 'Operazione massiva non disponibile.', 'error');
        }

        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        $tenantParams = array_merge($ids, [Auth::organizationId()]);
        if ($operation === 'export') {
            $columns = array_values(array_unique(array_merge(['id'], $module['columns'])));
            $statement = $this->db->prepare(
                'SELECT ' . implode(', ', array_map([$this, 'identifier'], $columns))
                . ' FROM ' . $this->identifier($module['table'])
                . " WHERE id IN ({$placeholders}) AND organization_id = ? ORDER BY id"
            );
            $statement->execute($tenantParams);
            $this->recordBulk($slug, $operation, $ids, count($ids), 0);
            header('Content-Type: text/csv; charset=UTF-8');
            header('Content-Disposition: attachment; filename="' . $slug . '-selezione-' . date('Ymd-His') . '.csv"');
            $output = fopen('php://output', 'wb');
            fwrite($output, "\xEF\xBB\xBF");
            fputcsv($output, $columns, ';');
            while ($row = $statement->fetch()) {
                fputcsv($output, array_map(static fn (string $column): mixed => $row[$column] ?? null, $columns), ';');
            }
            fclose($output);
            exit;
        }

        if (!isset($module['fields']['active'])) {
            $this->redirect('/r/' . $slug, 'Questo archivio non supporta attivazione e disattivazione massive.', 'error');
        }
        $statement = $this->db->prepare(
            'UPDATE ' . $this->identifier($module['table']) . ' SET active = ?, updated_at = NOW()'
            . " WHERE id IN ({$placeholders}) AND organization_id = ?"
        );
        $statement->execute(array_merge([$operation === 'activate' ? 1 : 0], $tenantParams));
        $processed = $statement->rowCount();
        $this->recordBulk($slug, $operation, $ids, $processed, count($ids) - $processed);
        $this->audit(strtoupper($operation), $module['table'], null, ['ids' => $ids]);
        $this->redirect('/r/' . $slug, "{$processed} record aggiornati.");
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
            'Contabilità' => ['OWNER', 'ADMIN', 'ACCOUNTANT'],
            'HR' => ['OWNER', 'ADMIN', 'HR'],
            'Vendite e magazzino' => ['OWNER', 'ADMIN', 'SALES', 'WAREHOUSE'],
            'CRM', 'Noleggio', 'Operatività' => ['OWNER', 'ADMIN', 'SALES'],
            'Integrazioni' => ['OWNER', 'ADMIN'],
            default => ['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES'],
        };
        $this->requireRoles($roles);
    }

    private function requireModuleReadRole(array $module): void
    {
        $roles = match ($module['group']) {
            'Contabilità' => ['OWNER', 'ADMIN', 'ACCOUNTANT'],
            'HR' => ['OWNER', 'ADMIN', 'HR'],
            'Integrazioni' => ['OWNER', 'ADMIN'],
            'Noleggio' => ['OWNER', 'ADMIN', 'SALES'],
            default => ['OWNER', 'ADMIN', 'ACCOUNTANT', 'SALES', 'WAREHOUSE', 'HR', 'VIEWER'],
        };
        $this->requireRoles($roles);
    }

    private function recordBulk(string $slug, string $operation, array $ids, int $processed, int $failed): void
    {
        try {
            $statement = $this->db->prepare(
                "INSERT INTO bulk_operations
                 (organization_id, user_id, module_key, operation, selection_json, status,
                  processed_count, failed_count, result_json, started_at, completed_at, created_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW())"
            );
            $statement->execute([
                Auth::organizationId(), Auth::id(), $slug, strtoupper($operation),
                json_encode($ids, JSON_THROW_ON_ERROR),
                $failed > 0 ? 'PARTIAL' : 'COMPLETED', $processed, $failed,
                json_encode(['processed' => $processed, 'failed' => $failed], JSON_THROW_ON_ERROR),
            ]);
        } catch (\Throwable) {
            // La funzione principale non deve fallire se il registro massivo non è ancora migrato.
        }
    }
}
