<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\Env;
use Luna\Service\BankStatementService;
use Luna\Service\ComplianceWorkspaceService;
use Luna\Service\OfficialPrintService;
use Luna\Service\WorkspaceService;
use Throwable;

final class ProfessionalController extends BaseController
{
    public function index(): never
    {
        $this->authorize();
        $prints = $this->prints()->all();
        $filings = $this->compliance()->all();
        $imports = $this->bankStatements()->imports();
        $suggestions = $this->bankStatements()->suggestions();
        $quality = (new WorkspaceService($this->db, Auth::organizationId(), Auth::id()))->dataQuality();
        $bankAccounts = $this->query('SELECT * FROM bank_accounts WHERE organization_id = ? AND active = 1 ORDER BY name');
        $endpoints = $this->query('SELECT id, display_name AS name, service_key AS endpoint_type FROM api_endpoint_configs WHERE organization_id = ? AND enabled = 1 ORDER BY display_name');
        $printTypes = OfficialPrintService::types();
        $filingTypes = ComplianceWorkspaceService::types();
        $this->view->render('professional/index', compact(
            'prints', 'filings', 'imports', 'suggestions', 'quality', 'bankAccounts', 'endpoints', 'printTypes', 'filingTypes'
        ) + ['title' => 'Centro professionale']);
    }

    public function generatePrint(): never
    {
        $this->authorize();
        $this->perform(function (): int {
            return $this->prints()->generate(
                (string) ($_POST['print_type'] ?? ''),
                (string) ($_POST['period_start'] ?? ''),
                (string) ($_POST['period_end'] ?? ''),
                $_POST['notes'] ?? null,
            );
        }, 'Stampa numerata generata e verificata.', 'GENERATE', 'official_print_runs');
    }

    public function validatePrint(string $id): never
    {
        $this->authorize();
        $this->perform(function () use ($id): int {
            $this->prints()->validate((int) $id, (string) ($_POST['professional_validation_reference'] ?? ''));
            return (int) $id;
        }, 'Stampa validata.', 'VALIDATE', 'official_print_runs');
    }

    public function lockPrint(string $id): never
    {
        $this->authorize();
        $this->perform(function () use ($id): int {
            $this->prints()->lock((int) $id);
            return (int) $id;
        }, 'Stampa bloccata: il contenuto non è più modificabile.', 'LOCK', 'official_print_runs');
    }

    public function downloadPrint(string $id): never
    {
        $this->authorize();
        [$path, $row] = $this->prints()->file((int) $id);
        header('Content-Type: application/pdf');
        header('Content-Length: ' . filesize($path));
        header('Content-Disposition: attachment; filename="' . basename((string) $row['file_path']) . '"');
        header('X-Content-Type-Options: nosniff');
        readfile($path);
        exit;
    }

    public function createFiling(): never
    {
        $this->authorize();
        $this->perform(function (): int {
            return $this->compliance()->create(
                (string) ($_POST['filing_type'] ?? ''),
                (int) ($_POST['period_year'] ?? 0),
                $_POST['period_code'] ?? null,
                $_POST['schema_version'] ?? null,
                $_POST['notes'] ?? null,
            );
        }, 'Fascicolo di controllo creato.', 'CREATE', 'compliance_filing_runs');
    }

    public function transitionFiling(string $id): never
    {
        $this->authorize();
        $this->perform(function () use ($id): int {
            $this->compliance()->transition((int) $id, (string) ($_POST['target_status'] ?? ''), $_POST);
            return (int) $id;
        }, 'Stato del fascicolo aggiornato.', 'STATUS', 'compliance_filing_runs');
    }

    public function downloadFiling(string $id): never
    {
        $this->authorize();
        [$path, $row] = $this->compliance()->dossier((int) $id);
        header('Content-Type: application/json; charset=UTF-8');
        header('Content-Length: ' . filesize($path));
        header('Content-Disposition: attachment; filename="dossier-' . strtolower((string) $row['filing_type']) . '-' . (int) $row['period_year'] . '-' . (int) $row['id'] . '.json"');
        header('X-Content-Type-Options: nosniff');
        readfile($path);
        exit;
    }

    public function importBankStatement(): never
    {
        $this->authorize();
        $file = $_FILES['statement'] ?? null;
        if (!is_array($file) || ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file((string) $file['tmp_name'])) {
            $this->redirect('/professional#banking', 'Selezionare un file bancario valido.', 'error');
        }
        $maximumBytes = max(1, (int) Env::get('IMPORT_MAX_BYTES', '52428800'));
        if ((int) ($file['size'] ?? 0) <= 0 || (int) ($file['size'] ?? 0) > $maximumBytes) {
            $this->redirect('/professional#banking', 'Il file bancario supera la dimensione consentita o è vuoto.', 'error');
        }
        try {
            $result = $this->bankStatements()->import(
                (string) $file['tmp_name'], (string) $file['name'],
                (int) ($_POST['bank_account_id'] ?? 0), (string) ($_POST['source_format'] ?? ''),
            );
            $this->audit('IMPORT', 'bank_statement_imports', $result['batchId'], $result);
            $this->redirect(
                '/professional#banking',
                "Estratto acquisito: {$result['imported']} movimenti, {$result['duplicates']} duplicati, " . count($result['errors']) . ' errori.',
                $result['errors'] === [] ? 'success' : 'warning',
            );
        } catch (Throwable $exception) {
            $this->redirect('/professional#banking', $exception->getMessage(), 'error');
        }
    }

    public function suggestReconciliation(): never
    {
        $this->authorize();
        try {
            $count = $this->bankStatements()->generateSuggestions();
            $this->redirect('/professional#banking', "{$count} suggerimenti di riconciliazione aggiornati.");
        } catch (Throwable $exception) {
            $this->redirect('/professional#banking', $exception->getMessage(), 'error');
        }
    }

    public function reviewSuggestion(string $id): never
    {
        $this->authorize();
        try {
            $accept = ($_POST['decision'] ?? '') === 'accept';
            $this->bankStatements()->reviewSuggestion((int) $id, $accept);
            $this->audit($accept ? 'ACCEPT' : 'REJECT', 'bank_reconciliation_suggestions', (int) $id);
            $this->redirect('/professional#banking', $accept ? 'Riconciliazione applicata.' : 'Suggerimento scartato.');
        } catch (Throwable $exception) {
            $this->redirect('/professional#banking', $exception->getMessage(), 'error');
        }
    }

    private function authorize(): void
    {
        $this->requireFeature('accounting');
        $this->requireFeature('professional');
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
    }

    private function prints(): OfficialPrintService
    {
        return new OfficialPrintService($this->db, Auth::organizationId(), Auth::id(), dirname(__DIR__, 2));
    }

    private function compliance(): ComplianceWorkspaceService
    {
        return new ComplianceWorkspaceService($this->db, Auth::organizationId(), Auth::id(), dirname(__DIR__, 2));
    }

    private function bankStatements(): BankStatementService
    {
        return new BankStatementService($this->db, Auth::organizationId(), Auth::id(), dirname(__DIR__, 2));
    }

    private function perform(callable $operation, string $message, string $action, string $entity): never
    {
        try {
            $id = (int) $operation();
            $this->audit($action, $entity, $id, $_POST);
            $this->redirect('/professional', $message);
        } catch (Throwable $exception) {
            $this->redirect('/professional', $exception->getMessage(), 'error');
        }
    }

    private function query(string $sql): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute([Auth::organizationId()]);
        return $statement->fetchAll();
    }
}
