<?php

declare(strict_types=1);

namespace Luna\Controller;

use Dompdf\Dompdf;
use Luna\Core\Auth;
use Luna\Service\MailService;

final class CommunicationsController extends BaseController
{
    public function index(): never
    {
        $this->requireRoles(['OWNER','ADMIN','ACCOUNTANT','SALES']);$org=Auth::organizationId();
        $settings=$this->one('SELECT * FROM communication_settings WHERE organization_id=?',[$org])?:[];
        $messages=$this->rows('SELECT e.*,d.number document_number,(SELECT COUNT(*) FROM email_events ev WHERE ev.outbound_email_id=e.id AND ev.event_type=\'OPEN\') opens,(SELECT COUNT(*) FROM email_events ev WHERE ev.outbound_email_id=e.id AND ev.event_type=\'CLICK\') clicks FROM outbound_emails e LEFT JOIN documents d ON d.id=e.document_id WHERE e.organization_id=? ORDER BY e.id DESC LIMIT 200',[$org]);
        $documents=$this->rows("SELECT id,number,document_type,counterparty_name FROM documents WHERE organization_id=? AND document_type IN ('QUOTE','SALES_ORDER','DDT','SALES_INVOICE','CREDIT_NOTE') ORDER BY id DESC LIMIT 200",[$org]);
        $this->view->render('operations/communications',compact('settings','messages','documents')+['title'=>'Comunicazioni e tracking']);
    }
    public function settings(): never{$this->requireRoles(['OWNER','ADMIN']);(new MailService($this->db,Auth::organizationId(),Auth::id()))->saveSettings($_POST);$this->redirect('/operations/communications','Configurazione e-mail aggiornata.');}
    public function queue(): never
    {
        $this->requireRoles(['OWNER','ADMIN','ACCOUNTANT','SALES']);$documentId=(int)($_POST['document_id']??0)?:null;$attachments=[];
        if($documentId){$attachments[]=$this->documentPdf($documentId);}
        $id=(new MailService($this->db,Auth::organizationId(),Auth::id()))->queue((string)$_POST['recipient'],(string)$_POST['subject'],(string)$_POST['html_body'],$documentId,$attachments);$this->audit('QUEUE','outbound_emails',$id);$this->redirect('/operations/communications','E-mail aggiunta alla coda con allegato PDF.');
    }
    public function process(): never{$this->requireRoles(['OWNER','ADMIN']);$r=(new MailService($this->db,Auth::organizationId(),Auth::id()))->processQueue();$this->redirect('/operations/communications','Coda elaborata: '.$r['sent'].' inviate, '.$r['failed'].' non riuscite.',$r['failed']?'error':'success');}
    public function open(string $token): never{(new MailService($this->db,0))->registerEvent($token,'OPEN',null,(string)($_SERVER['REMOTE_ADDR']??''),(string)($_SERVER['HTTP_USER_AGENT']??''));header('Content-Type: image/gif');header('Cache-Control: no-store');echo base64_decode('R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==');exit;}
    public function click(string $token): never{$url=(string)($_GET['url']??'');if(!str_starts_with($url,'https://')){$url='/';}(new MailService($this->db,0))->registerEvent($token,'CLICK',$url,(string)($_SERVER['REMOTE_ADDR']??''),(string)($_SERVER['HTTP_USER_AGENT']??''));header('Location: '.$url);exit;}
    private function documentPdf(int $documentId):array
    {
        $document=$this->one('SELECT * FROM documents WHERE id=? AND organization_id=?',[$documentId,Auth::organizationId()]);if(!$document){throw new \InvalidArgumentException('Documento non trovato.');}
        $lines=$this->rows('SELECT * FROM document_lines WHERE document_id=? AND organization_id=? ORDER BY line_number',[$documentId,Auth::organizationId()]);$organization=$this->one('SELECT * FROM organizations WHERE id=?',[Auth::organizationId()]);
        $labels=['QUOTE'=>['singular'=>'Preventivo'],'SALES_ORDER'=>['singular'=>'Ordine'],'DDT'=>['singular'=>'DDT'],'PROFORMA'=>['singular'=>'Proforma'],'SALES_INVOICE'=>['singular'=>'Fattura'],'CREDIT_NOTE'=>['singular'=>'Nota di credito'],'PURCHASE_ORDER'=>['singular'=>'Ordine fornitore'],'PURCHASE_INVOICE'=>['singular'=>'Fattura passiva']];$definition=$labels[$document['document_type']]??['singular'=>'Documento'];
        ob_start();require dirname(__DIR__,2).'/views/documents/pdf.php';$html=(string)ob_get_clean();$pdf=new Dompdf(['isRemoteEnabled'=>false]);$pdf->loadHtml($html,'UTF-8');$pdf->setPaper('A4');$pdf->render();
        $dir=dirname(__DIR__,2).'/storage/private/mail/'.Auth::organizationId();if(!is_dir($dir)){mkdir($dir,0770,true);}$name=preg_replace('/[^A-Za-z0-9._-]/','-',(string)$document['number']).'.pdf';$path=$dir.'/'.date('YmdHis').'-'.$documentId.'-'.$name;file_put_contents($path,$pdf->output(),LOCK_EX);return['path'=>$path,'name'=>$name];
    }
    private function one(string $sql,array $p):array|false{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetch();}private function rows(string $sql,array $p):array{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetchAll();}
}
