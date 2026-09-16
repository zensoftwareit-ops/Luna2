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
        if ($batch['import_type'] === 'datev_koinos') $this->datevPreview($batch);
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
        $check = $this->db->prepare('SELECT import_type FROM import_batches WHERE id=? AND organization_id=?');
        $check->execute([(int)$id, Auth::organizationId()]);
        if ($check->fetchColumn() === 'datev_koinos' && ($_POST['confirm_destination'] ?? '') !== '1') {
            $this->redirect('/imports/' . (int)$id, 'Conferma l’azienda destinataria e il backup prima di acquisire i dati.', 'error');
        }
        $result = $this->service()->commit((int) $id);
        $this->audit('COMMIT', 'import_batches', (int) $id, $result);
        if (array_key_exists('references', $result)) {
            $this->redirect('/imports/' . (int)$id, sprintf('Acquisizione: %d applicati, %d di riferimento, %d da completare o riconciliare, %d già presenti, %d errori. Non equivale al completamento della migrazione contabile.', $result['applied'], $result['references'], $result['pending'], $result['skipped'], $result['errors']), ($result['errors'] || $result['pending']) ? 'warning' : 'success');
        }
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

    private function datevPreview(array $batch): never
    {
        $org = Auth::organizationId();
        $kind = (string)($_GET['kind'] ?? '');
        $search = trim((string)($_GET['q'] ?? ''));
        $page = max(1, (int)($_GET['page'] ?? 1));
        $where = 'r.batch_id=? AND r.organization_id=?'; $params = [$batch['id'], $org];
        if (isset(\Luna\Service\DatevKoinosReader::LABELS[$kind])) {
            $where .= " AND JSON_UNQUOTE(JSON_EXTRACT(r.normalized_data_json,'$.kind'))=?"; $params[] = $kind;
        }
        if ($search !== '') { $where .= ' AND r.normalized_data_json LIKE ?'; $params[] = '%' . $search . '%'; }
        $q = $this->db->prepare('SELECT COUNT(*) FROM import_rows r WHERE ' . $where);
        $q->execute($params); $total = (int)$q->fetchColumn();
        $pages = max(1, (int)ceil($total / 50)); $page = min($page, $pages); $offset = ($page - 1) * 50;
        $q = $this->db->prepare('SELECT r.*,f.filename FROM import_rows r JOIN import_files f ON f.id=r.source_file_id AND f.organization_id=r.organization_id WHERE ' . $where . ' ORDER BY r.source_file_id,r.source_row_number,r.id LIMIT 50 OFFSET ' . $offset);
        $q->execute($params); $rows = $q->fetchAll();
        foreach ($rows as &$row) $row['data'] = json_decode($row['normalized_data_json'],true) ?: [];
        unset($row);
        $q=$this->db->prepare('SELECT id,filename,file_size,stored_relative_path FROM import_files WHERE batch_id=? AND organization_id=? ORDER BY id');
        $q->execute([$batch['id'],$org]); $files=$q->fetchAll();
        $q=$this->db->prepare('SELECT record_kind,application_status,COUNT(*) AS total FROM datev_reference_records WHERE batch_id=? AND organization_id=? GROUP BY record_kind,application_status ORDER BY record_kind,application_status');
        $q->execute([$batch['id'],$org]); $summary=$q->fetchAll();
        $q=$this->db->prepare('SELECT business_name,vat_number FROM organizations WHERE id=?'); $q->execute([$org]); $destination=$q->fetch();
        $this->view->render('imports/datev-preview',compact('batch','rows','files','summary','destination','kind','search','page','pages','total')+['title'=>'Acquisizione Koinos #'.$batch['id']]);
    }

    public function download(string $id): never
    {
        $this->guard(); $this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);
        $q=$this->db->prepare('SELECT * FROM import_files WHERE id=? AND organization_id=?');
        $q->execute([(int)$id,Auth::organizationId()]); $file=$q->fetch();
        $root=realpath($this->config['app']['storage_path'].'/imports');
        $path=$file && $root && !empty($file['stored_relative_path']) ? realpath($root.'/'.$file['stored_relative_path']) : false;
        if (!$path || !str_starts_with($path,$root.DIRECTORY_SEPARATOR) || !is_file($path) || !hash_equals($file['checksum_sha256'],hash_file('sha256',$path))) {
            $this->redirect('/imports','Originale non disponibile o integrità non verificata.','error');
        }
        header('Content-Type: application/octet-stream');
        header('Content-Disposition: attachment; filename="originale.'.preg_replace('/[^a-z0-9]/i','',pathinfo($file['filename'],PATHINFO_EXTENSION)).'"; filename*=UTF-8\'\''.rawurlencode($file['filename']));
        header('X-Content-Type-Options: nosniff'); header('Cache-Control: private, no-store');
        header('Content-Length: '.filesize($path)); readfile($path); exit;
    }

    private function guard(): void
    {
        $this->requireFeature('imports');
    }
}
