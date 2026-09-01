<?php

declare(strict_types=1);

namespace Luna\Core;

use Luna\Controller\AccountingController;
use Luna\Controller\AccountingAdminController;
use Luna\Controller\AuthController;
use Luna\Controller\DashboardController;
use Luna\Controller\ComplianceController;
use Luna\Controller\DocumentController;
use Luna\Controller\ImportController;
use Luna\Controller\IntegrationController;
use Luna\Controller\LicenseController;
use Luna\Controller\LogisticsController;
use Luna\Controller\ProjectOpsController;
use Luna\Controller\CommunicationsController;
use Luna\Controller\HrController;
use Luna\Controller\OperationsIntegrationController;
use Luna\Controller\ReportsController;
use Luna\Controller\PlatformController;
use Luna\Controller\ResourceController;
use Luna\Controller\SettingsController;
use Luna\Controller\TreasuryController;
use Luna\Controller\WorkspaceController;
use Luna\Controller\ProfessionalController;
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
        $view = new View($basePath, $config, $db);
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
            header('Location: ' . (Auth::check() ? Auth::landingPath() : '/login'));
            exit;
        }, false);
        $router->add('GET', '/login', [AuthController::class, 'form'], false);
        $router->add('POST', '/login', [AuthController::class, 'login'], false);
        $router->add('POST', '/logout', [AuthController::class, 'logout']);
        $router->add('GET', '/dashboard', [DashboardController::class, 'index']);

        $router->add('GET', '/r/{module}', [ResourceController::class, 'index']);
        $router->add('GET', '/r/{module}/create', [ResourceController::class, 'create']);
        $router->add('GET', '/r/{module}/export', [ResourceController::class, 'export']);
        $router->add('GET', '/r/{module}/export/{format}', [ResourceController::class, 'export']);
        $router->add('GET', '/r/{module}/{id}/edit', [ResourceController::class, 'edit']);
        $router->add('POST', '/r/{module}/save', [ResourceController::class, 'save']);
        $router->add('POST', '/r/{module}/vat-lookup', [ResourceController::class, 'vatLookup']);
        $router->add('POST', '/r/{module}/bulk', [ResourceController::class, 'bulk']);
        $router->add('POST', '/r/{module}/{id}/delete', [ResourceController::class, 'delete']);

        $router->add('GET', '/workspace/search', [WorkspaceController::class, 'search']);
        $router->add('GET', '/workspace/notifications', [WorkspaceController::class, 'notifications']);
        $router->add('POST', '/workspace/notifications/read-all', [WorkspaceController::class, 'readAllNotifications']);
        $router->add('POST', '/workspace/notifications/{id}/read', [WorkspaceController::class, 'readNotification']);
        $router->add('GET', '/workspace/onboarding', [WorkspaceController::class, 'onboarding']);
        $router->add('POST', '/workspace/onboarding/{key}', [WorkspaceController::class, 'onboardingStep']);
        $router->add('POST', '/workspace/views', [WorkspaceController::class, 'saveView']);
        $router->add('POST', '/workspace/views/{id}/delete', [WorkspaceController::class, 'deleteView']);

        $router->add('GET', '/professional', [ProfessionalController::class, 'index']);
        $router->add('POST', '/professional/prints', [ProfessionalController::class, 'generatePrint']);
        $router->add('POST', '/professional/prints/{id}/validate', [ProfessionalController::class, 'validatePrint']);
        $router->add('POST', '/professional/prints/{id}/lock', [ProfessionalController::class, 'lockPrint']);
        $router->add('GET', '/professional/prints/{id}/download', [ProfessionalController::class, 'downloadPrint']);
        $router->add('POST', '/professional/filings', [ProfessionalController::class, 'createFiling']);
        $router->add('POST', '/professional/filings/{id}/status', [ProfessionalController::class, 'transitionFiling']);
        $router->add('GET', '/professional/filings/{id}/download', [ProfessionalController::class, 'downloadFiling']);
        $router->add('POST', '/professional/bank-statements', [ProfessionalController::class, 'importBankStatement']);
        $router->add('POST', '/professional/reconciliation/suggest', [ProfessionalController::class, 'suggestReconciliation']);
        $router->add('POST', '/professional/reconciliation/{id}/review', [ProfessionalController::class, 'reviewSuggestion']);

        $router->add('GET', '/documents/{type}', [DocumentController::class, 'index']);
        $router->add('GET', '/documents/{type}/create', [DocumentController::class, 'create']);
        $router->add('GET', '/documents/{type}/export/{format}', [DocumentController::class, 'export']);
        $router->add('POST', '/documents/{type}/save', [DocumentController::class, 'save']);
        $router->add('GET', '/documents/{type}/{id}', [DocumentController::class, 'view']);
        $router->add('GET', '/documents/{type}/{id}/pdf', [DocumentController::class, 'pdf']);
        $router->add('GET', '/documents/{type}/{id}/xml', [DocumentController::class, 'xml']);
        $router->add('POST', '/documents/{type}/{id}/status', [DocumentController::class, 'status']);
        $router->add('POST', '/documents/{type}/{id}/convert', [DocumentController::class, 'convert']);

        $router->add('GET', '/operations/logistics', [LogisticsController::class, 'index']);
        $router->add('POST', '/operations/logistics/movement', [LogisticsController::class, 'movement']);
        $router->add('POST', '/operations/logistics/transfers', [LogisticsController::class, 'transfer']);
        $router->add('POST', '/operations/logistics/transfers/{id}/confirm', [LogisticsController::class, 'confirm']);
        $router->add('POST', '/operations/logistics/transfers/{id}/receive', [LogisticsController::class, 'receive']);
        $router->add('POST', '/operations/logistics/transfers/{id}/cancel', [LogisticsController::class, 'cancelTransfer']);
        $router->add('POST', '/operations/logistics/picks', [LogisticsController::class, 'pick']);
        $router->add('POST', '/operations/logistics/picks/{id}/scan', [LogisticsController::class, 'scan']);
        $router->add('POST', '/operations/logistics/picks/{id}/complete', [LogisticsController::class, 'completePick']);
        $router->add('POST', '/operations/logistics/picks/{id}/cancel', [LogisticsController::class, 'cancelPick']);

        $router->add('GET', '/operations/projects', [ProjectOpsController::class, 'index']);
        $router->add('POST', '/operations/projects/time', [ProjectOpsController::class, 'time']);
        $router->add('POST', '/operations/projects/time/{id}/approve', [ProjectOpsController::class, 'approveTime']);
        $router->add('POST', '/operations/projects/expenses', [ProjectOpsController::class, 'expense']);
        $router->add('POST', '/operations/projects/milestones', [ProjectOpsController::class, 'milestone']);
        $router->add('POST', '/operations/projects/milestones/{id}/ready', [ProjectOpsController::class, 'ready']);
        $router->add('POST', '/operations/projects/{id}/invoice', [ProjectOpsController::class, 'invoice']);

        $router->add('GET', '/operations/communications', [CommunicationsController::class, 'index']);
        $router->add('POST', '/operations/communications/settings', [CommunicationsController::class, 'settings']);
        $router->add('POST', '/operations/communications/queue', [CommunicationsController::class, 'queue']);
        $router->add('POST', '/operations/communications/process', [CommunicationsController::class, 'process']);
        $router->add('GET', '/mail/o/{token}.gif', [CommunicationsController::class, 'open'], false);
        $router->add('GET', '/mail/c/{token}', [CommunicationsController::class, 'click'], false);

        $router->add('GET', '/operations/hr', [HrController::class, 'index']);
        $router->add('POST', '/operations/hr/leave', [HrController::class, 'leave']);
        $router->add('POST', '/operations/hr/leave/{id}/decide', [HrController::class, 'decide']);
        $router->add('POST', '/operations/hr/leave/{id}/cancel', [HrController::class, 'cancelLeave']);
        $router->add('POST', '/operations/hr/balances', [HrController::class, 'saveBalance']);
        $router->add('POST', '/operations/hr/payroll-configs', [HrController::class, 'savePayrollConfig']);
        $router->add('POST', '/operations/hr/payroll', [HrController::class, 'createRun']);
        $router->add('POST', '/operations/hr/payroll/{id}/calculate', [HrController::class, 'calculate']);
        $router->add('POST', '/operations/hr/payroll/{id}/import', [HrController::class, 'importPayroll']);
        $router->add('POST', '/operations/hr/payroll/{id}/confirm', [HrController::class, 'confirm']);
        $router->add('POST', '/operations/hr/payroll/{id}/paid', [HrController::class, 'paid']);

        $router->add('GET', '/operations/ecommerce', [OperationsIntegrationController::class, 'ecommerce']);
        $router->add('POST', '/operations/ecommerce/channels/{id}/enqueue', [OperationsIntegrationController::class, 'enqueue']);
        $router->add('POST', '/operations/ecommerce/process', [OperationsIntegrationController::class, 'processCommerce']);
        $router->add('POST', '/operations/ecommerce/orders/{id}/convert', [OperationsIntegrationController::class, 'convertOrder']);
        $router->add('POST', '/webhooks/ecommerce/{id}', [OperationsIntegrationController::class, 'webhook'], false, false, [
            'module' => 'ecommerce', 'permission' => 'public', 'operation' => 'WRITE',
        ]);
        $router->add('GET', '/operations/rental', [OperationsIntegrationController::class, 'rental']);
        $router->add('POST', '/operations/rental/contracts/{id}/meter', [OperationsIntegrationController::class, 'meter']);
        $router->add('POST', '/operations/rental/tickets/{id}/status', [OperationsIntegrationController::class, 'ticket']);
        $router->add('POST', '/operations/rental/automate', [OperationsIntegrationController::class, 'automateRental']);
        $router->add('GET', '/operations/calendar', [OperationsIntegrationController::class, 'calendar']);
        $router->add('POST', '/operations/calendar/settings', [OperationsIntegrationController::class, 'calendarSettings']);
        $router->add('POST', '/operations/calendar/{id}/sync', [OperationsIntegrationController::class, 'syncCalendar']);
        $router->add('GET', '/reports/management', [ReportsController::class, 'index']);
        $router->add('POST', '/reports/management/generate', [ReportsController::class, 'generate']);
        $router->add('GET', '/reports/management/{id}/download', [ReportsController::class, 'download']);

        $router->add('GET', '/accounting/journal', [AccountingController::class, 'journal']);
        $router->add('GET', '/accounting/journal/export/{format}', [AccountingController::class, 'exportJournal']);
        $router->add('GET', '/accounting/journal/create', [AccountingController::class, 'create']);
        $router->add('POST', '/accounting/journal/save', [AccountingController::class, 'save']);
        $router->add('POST', '/accounting/journal/post', [AccountingController::class, 'post']);
        $router->add('GET', '/accounting/journal/{id}/edit', [AccountingController::class, 'edit']);
        $router->add('POST', '/accounting/journal/{id}/post', [AccountingController::class, 'postDraft']);
        $router->add('POST', '/accounting/journal/{id}/delete', [AccountingController::class, 'deleteDraft']);
        $router->add('GET', '/accounting/journal/{id}', [AccountingController::class, 'show']);
        $router->add('GET', '/accounting/trial-balance', [AccountingController::class, 'trialBalance']);
        $router->add('GET', '/accounting/trial-balance/export/{format}', [AccountingController::class, 'exportTrialBalance']);
        $router->add('GET', '/accounting/ledger/{id}', [AccountingController::class, 'ledger']);
        $router->add('GET', '/accounting/ledger/{id}/export/{format}', [AccountingController::class, 'exportLedger']);
        $router->add('GET', '/accounting/vat-registers', [AccountingController::class, 'vatRegisters']);
        $router->add('GET', '/accounting/vat-registers/export/{format}', [AccountingController::class, 'exportVatRegisters']);
        $router->add('POST', '/accounting/vat-registers/manual', [AccountingController::class, 'saveVatMovement']);
        $router->add('POST', '/accounting/vat-registers/sync', [AccountingController::class, 'syncVatDocuments']);
        $router->add('POST', '/accounting/vat-registers/{id}/delete', [AccountingController::class, 'deleteVatMovement']);
        $router->add('GET', '/accounting/vat-settlements', [AccountingController::class, 'vatSettlements']);
        $router->add('GET', '/accounting/vat-settlements/export/{format}', [AccountingController::class, 'exportVatSettlements']);
        $router->add('POST', '/accounting/vat-settlements/calculate', [AccountingController::class, 'calculateVatSettlement']);
        $router->add('POST', '/accounting/vat-settlements/{id}/status', [AccountingController::class, 'updateVatSettlementStatus']);
        $router->add('GET', '/accounting/vat-settlements/{id}', [AccountingController::class, 'showVatSettlement']);

        $router->add('GET', '/accounting/setup', [AccountingAdminController::class, 'index']);
        $router->add('POST', '/accounting/setup/accounts', [AccountingAdminController::class, 'saveAccount']);
        $router->add('POST', '/accounting/setup/accounts/{id}/toggle', [AccountingAdminController::class, 'toggleAccount']);
        $router->add('POST', '/accounting/setup/settings', [AccountingAdminController::class, 'saveSettings']);
        $router->add('POST', '/accounting/setup/registers', [AccountingAdminController::class, 'saveRegister']);
        $router->add('POST', '/accounting/setup/causes', [AccountingAdminController::class, 'saveCause']);
        $router->add('POST', '/accounting/setup/mappings', [AccountingAdminController::class, 'saveMapping']);

        $router->add('GET', '/accounting/treasury', [TreasuryController::class, 'index']);
        $router->add('POST', '/accounting/treasury/sync', [TreasuryController::class, 'sync']);
        $router->add('POST', '/accounting/treasury/payments', [TreasuryController::class, 'payment']);
        $router->add('POST', '/accounting/treasury/payments/{id}/reverse', [TreasuryController::class, 'reversePayment']);
        $router->add('POST', '/accounting/treasury/bank-accounts', [TreasuryController::class, 'bankAccount']);
        $router->add('POST', '/accounting/treasury/bank-transactions', [TreasuryController::class, 'bankTransaction']);
        $router->add('POST', '/accounting/treasury/reconcile', [TreasuryController::class, 'reconcile']);
        $router->add('POST', '/accounting/treasury/reconciliations/{id}/delete', [TreasuryController::class, 'unreconcile']);
        $router->add('POST', '/accounting/treasury/withholdings', [TreasuryController::class, 'withholding']);
        $router->add('POST', '/accounting/treasury/withholdings/{id}/pay', [TreasuryController::class, 'payWithholding']);

        $router->add('GET', '/accounting/compliance', [ComplianceController::class, 'index']);
        $router->add('POST', '/accounting/compliance/vat-adjustments', [ComplianceController::class, 'vatAdjustment']);
        $router->add('POST', '/accounting/compliance/lipe', [ComplianceController::class, 'generateLipe']);
        $router->add('POST', '/accounting/compliance/annual-vat', [ComplianceController::class, 'generateAnnual']);
        $router->add('POST', '/accounting/compliance/{kind}/{id}/status', [ComplianceController::class, 'vatStatus']);
        $router->add('POST', '/accounting/compliance/closings', [ComplianceController::class, 'prepareClosing']);
        $router->add('POST', '/accounting/compliance/closings/{id}/post', [ComplianceController::class, 'postClosing']);
        $router->add('POST', '/accounting/compliance/adjustments', [ComplianceController::class, 'adjustment']);
        $router->add('POST', '/accounting/compliance/adjustments/{id}/post', [ComplianceController::class, 'postAdjustment']);
        $router->add('POST', '/accounting/compliance/asset-categories', [ComplianceController::class, 'assetCategory']);
        $router->add('POST', '/accounting/compliance/assets', [ComplianceController::class, 'asset']);
        $router->add('POST', '/accounting/compliance/assets/{id}/calculate', [ComplianceController::class, 'calculateDepreciation']);
        $router->add('POST', '/accounting/compliance/depreciations/{id}/post', [ComplianceController::class, 'postDepreciation']);

        $router->add('GET', '/imports', [ImportController::class, 'index']);
        $router->add('POST', '/imports/upload', [ImportController::class, 'upload']);
        $router->add('POST', '/imports/einvoice/pull', [ImportController::class, 'pullInvoices']);
        $router->add('GET', '/imports/{id}', [ImportController::class, 'preview']);
        $router->add('POST', '/imports/{id}/commit', [ImportController::class, 'commit']);
        $router->add('POST', '/imports/{id}/rollback', [ImportController::class, 'rollback']);

        $router->add('GET', '/settings/modules', [SettingsController::class, 'modules']);
        $router->add('POST', '/settings/modules', [SettingsController::class, 'saveModules']);
        $router->add('GET', '/settings/system', [SettingsController::class, 'system']);
        $router->add('GET', '/settings/license', [LicenseController::class, 'index'], true, true, ['module' => null, 'permission' => 'settings.license', 'operation' => 'TECHNICAL']);
        $router->add('POST', '/settings/license/activate', [LicenseController::class, 'activate'], true, true, ['module' => null, 'permission' => 'settings.license', 'operation' => 'TECHNICAL']);
        $router->add('POST', '/settings/license/sync', [LicenseController::class, 'sync'], true, true, ['module' => null, 'permission' => 'settings.license', 'operation' => 'TECHNICAL']);
        $router->add('POST', '/settings/license/deactivate', [LicenseController::class, 'deactivate'], true, true, ['module' => null, 'permission' => 'settings.license', 'operation' => 'TECHNICAL']);
        $router->add('GET', '/settings/endpoints', [IntegrationController::class, 'endpoints']);
        $router->add('POST', '/settings/endpoints', [IntegrationController::class, 'saveEndpoint']);
        $router->add('GET', '/settings/company', [PlatformController::class, 'index']);
        $router->add('POST', '/settings/company', [PlatformController::class, 'createCompany']);
        $router->add('POST', '/settings/company/update', [PlatformController::class, 'updateCompany']);
        $router->add('POST', '/settings/company/{id}/select', [PlatformController::class, 'selectCompany']);
        $router->add('POST', '/settings/users', [PlatformController::class, 'createUser']);
        $router->add('POST', '/settings/users/{id}/toggle', [PlatformController::class, 'toggleUser']);
        $router->add('POST', '/settings/users/{id}/reset-password', [PlatformController::class, 'resetUserPassword']);
        $router->add('POST', '/settings/security/password', [PlatformController::class, 'changePassword']);
    }

    public function run(): void
    {
        try {
            $path = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH) ?: '/';
            $this->router->dispatch(strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET'), rtrim($path, '/') ?: '/');
        } catch (Throwable $exception) {
            $reference = ErrorReporter::report($exception, $this->basePath);
            $schemaIssue = ErrorReporter::isSchemaError($exception);
            $message = $schemaIssue
                ? 'Il database non è completamente aggiornato. Controlla le migrazioni dalla pagina Stato del sistema.'
                : 'Non è stato possibile completare l’operazione. Il dettaglio tecnico è stato registrato.';
            if (Env::bool('APP_DEBUG')) {
                $message .= ' ' . $exception->getMessage();
            }
            $this->view->render('error', [
                'title' => $schemaIssue ? 'Database da aggiornare' : 'Errore applicativo',
                'message' => $message,
                'reference' => $reference,
                'schemaIssue' => $schemaIssue,
                'actionUrl' => Auth::isSuperuser() ? '/settings/system' : '/dashboard',
                'actionLabel' => Auth::isSuperuser() ? 'Controlla il sistema' : 'Torna alla dashboard',
            ], $schemaIssue ? 503 : 500);
        }
    }
}
