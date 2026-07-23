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

    public function add(string $method, string $pattern, array|Closure $handler, bool $auth = true, bool $csrf = true): void
    {
        $regex = preg_replace_callback('/\{([a-zA-Z_][a-zA-Z0-9_]*)\}/', static fn (array $m): string => '(?P<' . $m[1] . '>[^/]+)', $pattern);
        $this->routes[] = compact('method', 'pattern', 'handler', 'auth', 'csrf', 'regex');
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
}
