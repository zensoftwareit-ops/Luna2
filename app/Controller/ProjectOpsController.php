<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\ProjectService;

final class ProjectOpsController extends BaseController
{
    public function index(): never
    {
        $this->requireFeature('projects');$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT','SALES','VIEWER']);$org=Auth::organizationId();$id=(int)($_GET['project']??0);
        $projects=$this->rows('SELECT * FROM projects WHERE organization_id=? ORDER BY status,name',[$org]);$project=null;$metrics=[];$time=[];$expenses=[];$milestones=[];
        if($id>0){foreach($projects as $p){if((int)$p['id']===$id){$project=$p;break;}}if($project){$svc=new ProjectService($this->db,$org,Auth::id());$metrics=$svc->metrics($id);$time=$this->rows('SELECT t.*,u.name user_name FROM project_time_entries t LEFT JOIN users u ON u.id=t.user_id WHERE t.organization_id=? AND t.project_id=? ORDER BY t.work_date DESC,t.id DESC',[$org,$id]);$expenses=$this->rows('SELECT * FROM project_expenses WHERE organization_id=? AND project_id=? ORDER BY expense_date DESC,id DESC',[$org,$id]);$milestones=$this->rows('SELECT * FROM project_milestones WHERE organization_id=? AND project_id=? ORDER BY due_date,id',[$org,$id]);}}
        $users=$this->rows('SELECT id,name FROM users WHERE organization_id=? AND active=1 ORDER BY name',[$org]);
        $this->view->render('operations/projects',compact('projects','project','metrics','time','expenses','milestones','users')+['title'=>'Consuntivazione commesse']);
    }
    public function time(): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT','SALES']);$id=(int)$_POST['project_id'];(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->addTime($id,$_POST);$this->redirect('/operations/projects?project='.$id,'Consuntivo aggiunto.');}
    public function approveTime(string $id): never{$this->requireRoles(['OWNER','ADMIN']);$project=(int)$_POST['project_id'];(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->approveTime((int)$id);$this->redirect('/operations/projects?project='.$project,'Consuntivo approvato.');}
    public function expense(): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT']);$id=(int)$_POST['project_id'];(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->addExpense($id,$_POST);$this->redirect('/operations/projects?project='.$id,'Spesa aggiunta.');}
    public function milestone(): never{$this->requireRoles(['OWNER','ADMIN','SALES']);$id=(int)$_POST['project_id'];(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->addMilestone($id,$_POST);$this->redirect('/operations/projects?project='.$id,'Milestone aggiunta.');}
    public function ready(string $id): never{$this->requireRoles(['OWNER','ADMIN','SALES']);$project=(int)$_POST['project_id'];(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->markMilestoneReady((int)$id);$this->redirect('/operations/projects?project='.$project,'Milestone pronta per la fatturazione.');}
    public function invoice(string $id): never{$this->requireRoles(['OWNER','ADMIN','ACCOUNTANT','SALES']);$invoice=(new ProjectService($this->db,Auth::organizationId(),Auth::id()))->createInvoice((int)$id,['time'=>$_POST['time']??[],'expenses'=>$_POST['expenses']??[],'milestones'=>$_POST['milestones']??[]]);$this->redirect('/documents/invoices/'.$invoice,'Fattura di commessa creata in bozza.');}
    private function rows(string $sql,array $p):array{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetchAll();}
}
