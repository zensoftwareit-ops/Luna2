<?php

declare(strict_types=1);

namespace Luna\Core;

use Luna\Controller\AccountingController;
use Luna\Controller\AuthController;
use Luna\Controller\DashboardController;
use Luna\Controller\DocumentController;
use Luna\Controller\ImportController;
use Luna\Controller\ResourceController;
use Throwable;

final class Application
{
    private function __construct(private readonly string $basePath, private readonly Router $router, private readonly View $view)
    {
    }

    public static function boot(string $basePath): self
    {
        Env::load($basePath . '/.env');
        date_default_timezone_set((string) Env::get('APP_TIMEZONE', 'Europe/Rome'));

        $secure = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') || ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https';
        session_name((string) Env::get('SESSION_NAME', 'luna2_session'));
        if (is_dir($basePath . '/storage/sessions')) {
            session_save_path($basePath . '/storage/sessions');
        }
        session_set_cookie_params([
            'lifetime' => 0,
            'path' => '/',
            'secure' => $secure,
            'httponly' => true,
            'samesite' => 'Lax',
        ]);
        session_start();

        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: SAMEORIGIN');
        header('Referrer-Policy: strict-origin-when-cross-origin');
        header("Permissions-Policy: camera=(), microphone=(), geolocation=()");
        header("Content-Security-Policy: default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; font-src 'self' data:; frame-ancestors 'self'; base-uri 'self'; form-action 'self'");

        $config = require $basePath . '/bootstrap/app.php';
        $db = Database::connect();
        $view = new View($basePath, $config);
        $router = new Router($db, $view, $config);
        self::routes($router);

        return new self($basePath, $router, $view);
    }

    private static function routes(Router $router): void
    {
        $router->add('GET', '/health', static function (): never {
            header('Content-Type: application/json; charset=utf-8');
            echo json_encode(['status' => 'ok', 'application' => 'Luna2 PHP'], JSON_THROW_ON_ERROR);
            exit;
        }, false);
        $router->add('GET', '/', static function (): never {
            header('Location: ' . (Auth::check() ? '/dashboard' : '/login'));
            exit;
        }, false);
        $router->add('GET', '/login', [AuthController::class, 'form'], false);
        $router->add('POST', '/login', [AuthController::class, 'login'], false);
        $router->add('POST', '/logout', [AuthController::class, 'logout']);
        $router->add('GET', '/dashboard', [DashboardController::class, 'index']);

        $router->add('GET', '/r/{module}', [ResourceController::class, 'index']);
        $router->add('GET', '/r/{module}/create', [ResourceController::class, 'create']);
        $router->add('GET', '/r/{module}/export', [ResourceController::class, 'export']);
        $router->add('GET', '/r/{module}/{id}/edit', [ResourceController::class, 'edit']);
        $router->add('POST', '/r/{module}/save', [ResourceController::class, 'save']);
        $router->add('POST', '/r/{module}/{id}/delete', [ResourceController::class, 'delete']);

        $router->add('GET', '/documents/{type}', [DocumentController::class, 'index']);
        $router->add('GET', '/documents/{type}/create', [DocumentController::class, 'create']);
        $router->add('POST', '/documents/{type}/save', [DocumentController::class, 'save']);
        $router->add('GET', '/documents/{type}/{id}', [DocumentController::class, 'view']);
        $router->add('GET', '/documents/{type}/{id}/pdf', [DocumentController::class, 'pdf']);
        $router->add('GET', '/documents/{type}/{id}/xml', [DocumentController::class, 'xml']);
        $router->add('POST', '/documents/{type}/{id}/status', [DocumentController::class, 'status']);

        $router->add('GET', '/accounting/journal', [AccountingController::class, 'journal']);
        $router->add('GET', '/accounting/journal/create', [AccountingController::class, 'create']);
        $router->add('POST', '/accounting/journal/post', [AccountingController::class, 'post']);
        $router->add('GET', '/accounting/trial-balance', [AccountingController::class, 'trialBalance']);
        $router->add('GET', '/accounting/ledger/{id}', [AccountingController::class, 'ledger']);

        $router->add('GET', '/imports', [ImportController::class, 'index']);
        $router->add('POST', '/imports/upload', [ImportController::class, 'upload']);
        $router->add('GET', '/imports/{id}', [ImportController::class, 'preview']);
        $router->add('POST', '/imports/{id}/commit', [ImportController::class, 'commit']);
        $router->add('POST', '/imports/{id}/rollback', [ImportController::class, 'rollback']);
    }

    public function run(): void
    {
        try {
            $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
            $this->router->dispatch(strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET'), rtrim($path, '/') ?: '/');
        } catch (Throwable $exception) {
            error_log($exception->__toString());
            $message = Env::bool('APP_DEBUG') ? $exception->getMessage() : 'Si è verificato un errore. Il dettaglio è stato registrato.';
            $this->view->render('error', ['title' => 'Errore applicativo', 'message' => $message], 500);
        }
    }
}
