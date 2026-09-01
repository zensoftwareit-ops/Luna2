<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Core\Env;
use Luna\Service\ImportService;
use Luna\Service\InboundInvoiceService;
use Throwable;

final class ImportController extends BaseController
{
    public function index(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $statement = $this->db->prepare('SELECT * FROM import_batches WHERE organization_id = ? ORDER BY id DESC LIMIT 100');
        $statement->execute([Auth::organizationId()]);
        $batches = $statement->fetchAll();
        $this->view->render('imports/index', compact('batches') + ['title' => 'Importazioni e fatture passive']);
    }

    public function upload(): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $service = $this->service();
        $id = $service->upload($_FILES['import_file'] ?? [], (string) ($_POST['import_type'] ?? ''), Env::int('IMPORT_MAX_BYTES', 52428800));
        $this->audit('UPLOAD', 'import_batches', $id, ['type' => $_POST['import_type'] ?? null]);
        $this->redirect('/imports/' . $id, 'File caricato e analizzato. Verifica l’anteprima prima di confermare.');
    }

    public function pullInvoices(): never
    {
        $this->guard();$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);
        try {
            $files=(new InboundInvoiceService($this->db,Auth::organizationId()))->receive();
            $id=$this->service()->ingestRemoteInvoices($files,Env::int('IMPORT_MAX_BYTES',52428800));
            $this->audit('RECEIVE','import_batches',$id,['source'=>'EINVOICE','files'=>count($files)]);
            $this->redirect('/imports/'.$id,'Fatture ricevute dall’endpoint. Verifica l’anteprima e conferma l’importazione.');
        } catch (Throwable $exception) {
            $this->redirect('/imports', 'Ricezione non completata: ' . $exception->getMessage(), 'error');
        }
    }

    public function preview(string $id): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $statement = $this->db->prepare('SELECT * FROM import_batches WHERE id = ? AND organization_id = ?');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $batch = $statement->fetch();
        if (!$batch) {
            $this->redirect('/imports', 'Lotto non trovato.', 'error');
        }
        $statement = $this->db->prepare('SELECT source_row_number, normalized_data_json, status, error_message, imported_entity_id FROM import_rows WHERE batch_id = ? AND organization_id = ? ORDER BY source_file_id, source_row_number LIMIT 100');
        $statement->execute([(int) $id, Auth::organizationId()]);
        $rows = $statement->fetchAll();
        foreach ($rows as &$row) {
            $row['data'] = json_decode((string) $row['normalized_data_json'], true) ?: [];
        }
        unset($row);
        $this->view->render('imports/preview', compact('batch', 'rows') + ['title' => 'Anteprima import #' . $id]);
    }

    public function commit(string $id): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN', 'ACCOUNTANT']);
        $result = $this->service()->commit((int) $id);
        $this->audit('COMMIT', 'import_batches', (int) $id, $result);
        $this->redirect('/imports/' . $id, sprintf('Importazione conclusa: %d righe importate, %d errori.', $result['imported'], $result['errors']), $result['errors'] ? 'warning' : 'success');
    }

    public function rollback(string $id): never
    {
        $this->guard();
        $this->requireRoles(['OWNER', 'ADMIN']);
        $count = $this->service()->rollback((int) $id);
        $this->audit('ROLLBACK', 'import_batches', (int) $id, ['records' => $count]);
        $this->redirect('/imports/' . $id, sprintf('Rollback completato: %d record ripristinati o rimossi.', $count));
    }

    private function service(): ImportService
    {
        return new ImportService($this->db, Auth::organizationId(), Auth::id(), $this->config['app']['storage_path']);
    }

    private function guard(): void
    {
        $this->requireFeature('imports');
    }
}
