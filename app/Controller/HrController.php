<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;
use Luna\Service\LeaveService;
use Luna\Service\PayrollService;

final class HrController extends BaseController
{
    public function index(): never
    {
        $this->requireFeature('hr');$org=Auth::organizationId();$role=(string)(Auth::user()['role']??'');$admin=in_array($role,['OWNER','ADMIN'],true);
        $where=$admin?'lr.organization_id=?':'lr.organization_id=? AND lr.user_id=?';$params=$admin?[$org]:[$org,Auth::id()];
        $leaves=$this->rows("SELECT lr.*,u.name user_name,a.id approval_id FROM leave_requests lr INNER JOIN users u ON u.id=lr.user_id LEFT JOIN approval_requests a ON a.id=lr.approval_request_id WHERE {$where} ORDER BY lr.id DESC",$params);
        $balances=$this->rows('SELECT b.*,u.name user_name,(b.opening_hours+b.accrued_hours+b.adjusted_hours-b.used_hours) available_hours FROM leave_balances b INNER JOIN users u ON u.id=b.user_id WHERE b.organization_id=? ORDER BY b.balance_year DESC,u.name,b.leave_type',[$org]);
        $users=$this->rows('SELECT id,name FROM users WHERE organization_id=? AND active=1 ORDER BY name',[$org]);
        $runs=$admin?$this->rows('SELECT * FROM payroll_runs WHERE organization_id=? ORDER BY period_start DESC',[$org]):[];
        $configs=$admin?$this->rows('SELECT pc.*,u.name FROM payroll_employee_configs pc INNER JOIN users u ON u.id=pc.user_id WHERE pc.organization_id=? ORDER BY u.name,pc.valid_from DESC',[$org]):[];
        $selectedRun=null;$payrollDetails=[];$payrollComponents=[];$runId=(int)($_GET['run']??0);
        if($admin&&$runId>0){foreach($runs as $r){if((int)$r['id']===$runId){$selectedRun=$r;break;}}if($selectedRun){$payrollDetails=$this->rows('SELECT * FROM payroll_details WHERE organization_id=? AND payroll_run_id=? ORDER BY employee_name',[$org,$runId]);$payrollComponents=$this->rows('SELECT pc.* FROM payroll_components pc INNER JOIN payroll_details pd ON pd.id=pc.payroll_detail_id WHERE pc.organization_id=? AND pd.payroll_run_id=? ORDER BY pd.employee_name,pc.id',[$org,$runId]);}}
        $this->view->render('operations/hr',compact('leaves','balances','users','runs','configs','admin','selectedRun','payrollDetails','payrollComponents')+['title'=>'Ferie, approvazioni e paghe']);
    }
    public function leave(): never{$user=in_array((string)(Auth::user()['role']??''),['OWNER','ADMIN'],true)?(int)($_POST['user_id']??Auth::id()):Auth::id();(new LeaveService($this->db,Auth::organizationId(),Auth::id()))->request($user,$_POST);$this->redirect('/operations/hr','Richiesta inviata per approvazione.');}
    public function decide(string $id): never{$this->requireRoles(['OWNER','ADMIN']);(new LeaveService($this->db,Auth::organizationId(),Auth::id()))->decide((int)$id,($_POST['decision']??'')==='approve',$_POST['notes']??null);$this->redirect('/operations/hr','Richiesta aggiornata.');}
    public function cancelLeave(string $id): never{$admin=in_array((string)(Auth::user()['role']??''),['OWNER','ADMIN'],true);(new LeaveService($this->db,Auth::organizationId(),Auth::id()))->cancel((int)$id,$admin);$this->redirect('/operations/hr','Richiesta annullata e saldi ripristinati.');}
    public function createRun(): never{$this->requireRoles(['OWNER','ADMIN']);$id=(new PayrollService($this->db,Auth::organizationId(),Auth::id()))->createRun((string)$_POST['period_start'],(string)$_POST['period_end'],(string)$_POST['calculation_mode']);$this->redirect('/operations/hr?run='.$id,'Elaborazione paghe creata.');}
    public function saveBalance(): never
    {
        $this->requireRoles(['OWNER','ADMIN']);$org=Auth::organizationId();$user=(int)($_POST['user_id']??0);if(!$this->belongs('users',$user,$org)){throw new \InvalidArgumentException('Dipendente non valido.');}$type=(string)($_POST['leave_type']??'HOLIDAY');if(!in_array($type,['HOLIDAY','PERMIT','ROL','OTHER'],true)){throw new \InvalidArgumentException('Tipo saldo non valido.');}
        $this->db->prepare('INSERT INTO leave_balances (organization_id,user_id,balance_year,leave_type,opening_hours,accrued_hours,used_hours,adjusted_hours,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),NOW()) ON DUPLICATE KEY UPDATE opening_hours=VALUES(opening_hours),accrued_hours=VALUES(accrued_hours),used_hours=VALUES(used_hours),adjusted_hours=VALUES(adjusted_hours),updated_by=VALUES(updated_by),updated_at=NOW()')->execute([$org,$user,(int)$_POST['balance_year'],$type,$this->decimal($_POST['opening_hours']??0),$this->decimal($_POST['accrued_hours']??0),$this->decimal($_POST['used_hours']??0),$this->decimal($_POST['adjusted_hours']??0),Auth::id(),Auth::id()]);$this->redirect('/operations/hr','Saldo ferie/permessi aggiornato.');
    }
    public function savePayrollConfig(): never
    {
        $this->requireRoles(['OWNER','ADMIN']);$org=Auth::organizationId();$user=(int)($_POST['user_id']??0);if(!$this->belongs('users',$user,$org)){throw new \InvalidArgumentException('Dipendente non valido.');}$from=(string)($_POST['valid_from']??'');if(strtotime($from)===false){throw new \InvalidArgumentException('Data validità non valida.');}
        $values=[trim((string)$_POST['employee_code']),$this->decimal($_POST['standard_weekly_hours']??40),$this->decimal($_POST['hourly_rate']??0),$this->decimal($_POST['overtime_multiplier']??1.3),$this->decimal($_POST['inps_rate']??0),$this->decimal($_POST['inail_rate']??0),$this->decimal($_POST['employer_contribution_rate']??0),$this->decimal($_POST['tax_rate']??0),$this->decimal($_POST['fixed_monthly_amount']??0),($_POST['valid_to']??'')?:null];
        $find=$this->db->prepare('SELECT id FROM payroll_employee_configs WHERE organization_id=? AND user_id=? AND valid_from=? ORDER BY id LIMIT 1');$find->execute([$org,$user,$from]);$existing=$find->fetchColumn();
        if($existing){$this->db->prepare('UPDATE payroll_employee_configs SET employee_code=?,standard_weekly_hours=?,hourly_rate=?,overtime_multiplier=?,inps_rate=?,inail_rate=?,employer_contribution_rate=?,tax_rate=?,fixed_monthly_amount=?,valid_to=?,active=1,updated_by=?,updated_at=NOW() WHERE id=? AND organization_id=?')->execute(array_merge($values,[Auth::id(),$existing,$org]));}else{$this->db->prepare('INSERT INTO payroll_employee_configs (organization_id,user_id,employee_code,standard_weekly_hours,hourly_rate,overtime_multiplier,inps_rate,inail_rate,employer_contribution_rate,tax_rate,fixed_monthly_amount,valid_from,valid_to,active,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,1,?,?,NOW(),NOW())')->execute([$org,$user,$values[0],$values[1],$values[2],$values[3],$values[4],$values[5],$values[6],$values[7],$values[8],$from,$values[9],Auth::id(),Auth::id()]);}$this->redirect('/operations/hr','Configurazione retributiva aggiornata.');
    }
    public function calculate(string $id): never{$this->requireRoles(['OWNER','ADMIN']);(new PayrollService($this->db,Auth::organizationId(),Auth::id()))->calculate((int)$id);$this->redirect('/operations/hr?run='.$id,'Calcolo gestionale completato: serve la validazione del consulente.');}
    public function importPayroll(string $id): never
    {
        $this->requireRoles(['OWNER','ADMIN']);
        $file=$_FILES['payslips']??null;
        if(!$file||($file['error']??UPLOAD_ERR_NO_FILE)!==UPLOAD_ERR_OK||($file['size']??0)>5*1024*1024){$this->redirect('/operations/hr?run='.$id,'File CSV mancante o superiore a 5 MB.','error');}
        $handle=fopen((string)$file['tmp_name'],'rb');if(!$handle){$this->redirect('/operations/hr?run='.$id,'Impossibile leggere il CSV.','error');}
        $first=(string)fgets($handle);rewind($handle);$delimiter=substr_count($first,';')>=substr_count($first,',')?';':',';
        $headers=fgetcsv($handle,0,$delimiter)?:[];$aliases=['codice_dipendente'=>'employee_code','matricola'=>'employee_code','lordo'=>'gross','contributi_dipendente'=>'employee_contributions','contributi_azienda'=>'employer_contributions','contributi_datore'=>'employer_contributions','imposte'=>'tax','ritenute'=>'tax','rimborsi'=>'reimbursements','trattenute'=>'deductions','netto'=>'net'];
        $headers=array_map(static function($h)use($aliases){$key=strtolower(trim((string)$h));$key=preg_replace('/[^a-z0-9_]+/','_',iconv('UTF-8','ASCII//TRANSLIT',$key)?:$key);return $aliases[$key]??$key;},$headers);
        $required=['employee_code','gross','employee_contributions','employer_contributions','tax','net'];if(array_diff($required,$headers)!==[]){fclose($handle);$this->redirect('/operations/hr?run='.$id,'Colonne richieste: '.implode(', ',$required).'.','error');}
        $rows=[];while(($values=fgetcsv($handle,0,$delimiter))!==false){if(count($values)!==count($headers)){continue;}$rows[]=array_combine($headers,$values);if(count($rows)>5000){break;}}fclose($handle);
        (new PayrollService($this->db,Auth::organizationId(),Auth::id()))->importPayslipRows((int)$id,(string)$file['name'],$rows);$this->redirect('/operations/hr?run='.$id,'Cedolini importati e controllati.');
    }
    public function confirm(string $id): never{$this->requireRoles(['OWNER','ADMIN']);(new PayrollService($this->db,Auth::organizationId(),Auth::id()))->confirm((int)$id,(string)($_POST['professional_validation_reference']??''));$this->redirect('/operations/hr?run='.$id,'Elaborazione validata e bloccata.');}
    public function paid(string $id): never{$this->requireRoles(['OWNER','ADMIN']);(new PayrollService($this->db,Auth::organizationId(),Auth::id()))->markPaid((int)$id);$this->redirect('/operations/hr?run='.$id,'Elaborazione marcata come pagata.');}
    private function rows(string $sql,array $p):array{$s=$this->db->prepare($sql);$s->execute($p);return $s->fetchAll();}
    private function decimal(mixed $value):float{$v=trim((string)$value);if(str_contains($v,',')){$v=str_replace('.','',$v);$v=str_replace(',','.',$v);}return(float)$v;}
    private function belongs(string $table,int $id,int $org):bool{$s=$this->db->prepare("SELECT 1 FROM {$table} WHERE id=? AND organization_id=?");$s->execute([$id,$org]);return(bool)$s->fetchColumn();}
}
