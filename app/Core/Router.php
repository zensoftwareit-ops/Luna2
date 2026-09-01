<?php

declare(strict_types=1);

namespace Luna\Core;

use Closure;
use PDO;

final class Router
{
    private array $routes = [];

    public function __construct(
        private readonly PDO $db,
        private readonly View $view,
        private readonly array $config,
    ) {
    }

    public function add(string $method, string $pattern, array|Closure $handler, bool $auth = true, bool $csrf = true, ?array $policy = null): void
    {
        $regex = preg_replace_callback('/\{([a-zA-Z_][a-zA-Z0-9_]*)\}/', static fn (array $m): string => '(?P<' . $m[1] . '>[^/]+)', $pattern);
        $policy ??= RoutePolicyCatalog::classify($method, $pattern, $auth);
        $this->routes[] = compact('method', 'pattern', 'handler', 'auth', 'csrf', 'regex', 'policy');
    }

    public function dispatch(string $method, string $path): void
    {
        foreach ($this->routes as $route) {
            if ($route['method'] !== $method || !preg_match('#^' . $route['regex'] . '$#', $path, $matches)) {
                continue;
            }

            if ($route['auth'] && !Auth::check()) {
                header('Location: /login');
                exit;
            }

            if ($route['auth'] || ($route['policy']['operation'] ?? 'PUBLIC') !== 'PUBLIC') {
                $this->authorize($route['policy'], $matches);
            }

            if ($method === 'POST' && $route['csrf'] && !Csrf::validate($_POST['_token'] ?? ($_SERVER['HTTP_X_CSRF_TOKEN'] ?? null))) {
                $this->view->render('error', ['title' => 'Sessione scaduta', 'message' => 'Token di sicurezza non valido. Ricarica la pagina e riprova.'], 419);
            }

            $parameters = array_filter($matches, 'is_string', ARRAY_FILTER_USE_KEY);
            $handler = $route['handler'];
            if ($handler instanceof Closure) {
                $handler(...array_values($parameters));
                return;
            }

            [$class, $action] = $handler;
            $controller = new $class($this->db, $this->view, $this->config);
            $controller->{$action}(...array_values($parameters));
            return;
        }

        $this->view->render('error', ['title' => 'Pagina non trovata', 'message' => 'La risorsa richiesta non esiste.'], 404);
    }

    public function policies(): array
    {
        return array_map(static fn (array $route): array => [
            'method' => $route['method'],
            'pattern' => $route['pattern'],
            'policy' => $route['policy'],
        ], $this->routes);
    }

    private function authorize(array $policy, array $matches): void
    {
        $policy = $this->resolveDynamicPolicy($policy, $matches);
        $permission = (string) ($policy['permission'] ?? '');
        PermissionGate::setCurrentPermission($permission);
        $gate = new PermissionGate($this->config['permissions'] ?? []);
        $roleAllowed = $gate->allows((string) (Auth::user()['role'] ?? ''), $permission);
        $license = new LicenseService($this->db, Auth::organizationId());
        $operationAllowed = $license->operationAllowed((string) ($policy['operation'] ?? 'READ'));
        $module = $policy['module'] ?? null;
        $moduleAllowed = !is_string($module) || $module === '' || $license->moduleAllowed($module, (string) ($policy['operation'] ?? 'READ'));

        if ($roleAllowed && $operationAllowed && $moduleAllowed) {
            return;
        }

        $roleDenied = !$roleAllowed && $gate->enforcement() === 'enforce';
        $licenseDenied = (!$operationAllowed || !$moduleAllowed) && $license->enforcement() === 'enforce';
        if (!$roleDenied && !$licenseDenied) {
            error_log(sprintf(
                'Luna2 policy shadow: user=%d permission=%s role=%s module=%s operation=%s',
                Auth::id(), $permission, $roleAllowed ? 'allow' : 'deny', (string) $module,
                ($operationAllowed && $moduleAllowed) ? 'allow' : 'deny',
            ));
            return;
        }

        if ($licenseDenied && !$operationAllowed) {
            $this->view->render('error', [
                'title' => 'Modalità limitata',
                'message' => 'La licenza consente consultazione ed esportazione, ma non nuove modifiche. Verifica lo stato della licenza.',
            ], 403);
        }
        if ($licenseDenied && !$moduleAllowed) {
            $this->view->render('error', [
                'title' => 'Modulo non incluso',
                'message' => 'Questa funzione non è inclusa nel piano attivo.',
            ], 403);
        }
        $this->view->render('error', [
            'title' => 'Accesso negato',
            'message' => 'Il tuo profilo non dispone del permesso richiesto.',
        ], 403);
    }

    private function resolveDynamicPolicy(array $policy, array $matches): array
    {
        $operation = strtolower((string) ($policy['operation'] ?? 'READ'));
        if (($policy['dynamic'] ?? null) === 'document_type') {
            $feature = str_starts_with((string) ($matches['type'] ?? ''), 'purchase-') ? 'purchases' : 'sales';
            $policy['module'] = $feature;
            $policy['permission'] = $feature . '.' . $operation;
        } elseif (($policy['dynamic'] ?? null) === 'resource_module') {
            $slug = (string) ($matches['module'] ?? '');
            $feature = (string) ($this->config['modules'][$slug]['feature'] ?? 'resources');
            $policy['module'] = $feature === 'resources' ? null : $feature;
            $policy['permission'] = $feature . '.' . $operation;
        }
        return $policy;
    }
}
