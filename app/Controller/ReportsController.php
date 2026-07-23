<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\ManagementReportService;

final class ReportsController extends BaseController
{
    public function index(): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);$s=$this->db->prepare('SELECT * FROM report_exports WHERE organization_id=? ORDER BY id DESC LIMIT 100');$s->execute([Auth::organizationId()]);$exports=$s->fetchAll();$this->view->render('operations/reports',compact('exports')+['title'=>'Report direzionali']);}
    public function generate(): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);$directory=dirname(__DIR__,2).'/storage/exports/'.Auth::organizationId();$result=(new ManagementReportService($this->db,Auth::organizationId(),Auth::id()))->generate((string)$_POST['period_from'],(string)$_POST['period_to'],$directory);header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');header('Content-Disposition: attachment; filename="'.$result['filename'].'"');header('Content-Length: '.filesize($result['path']));readfile($result['path']);exit;}
    public function download(string $id): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);$s=$this->db->prepare('SELECT * FROM report_exports WHERE id=? AND organization_id=?');$s->execute([(int)$id,Auth::organizationId()]);$export=$s->fetch();$path=dirname(__DIR__,2).'/storage/exports/'.Auth::organizationId().'/'.basename((string)($export['filename']??''));if(!$export||!is_file($path)||!hash_equals((string)$export['checksum_sha256'],hash_file('sha256',$path))){throw new \InvalidArgumentException('Esportazione non trovata o non integra.');}header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');header('Content-Disposition: attachment; filename="'.basename($path).'"');header('Content-Length: '.filesize($path));readfile($path);exit;}
}
