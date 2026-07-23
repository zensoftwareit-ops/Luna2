<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use Throwable;

final class PayrollService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId)
    {
    }

    public function createRun(string $start, string $end, string $mode = 'CONFIGURED_RATES'): int
    {
        if (strtotime($start) === false || strtotime($end) === false || $end < $start || !in_array($mode, ['CONFIGURED_RATES','IMPORTED_PAYSLIPS'], true)) {
            throw new InvalidArgumentException('Periodo o modalità paghe non valida.');
        }
        $label = date('m/Y', strtotime($start));
        $this->db->prepare("INSERT INTO payroll_runs (organization_id, period_label, period_start, period_end, calculation_mode, schema_version, status, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'CONTROLLED-2', 'DRAFT', ?, ?, NOW(), NOW())")
            ->execute([$this->organizationId, $label, $start, $end, $mode, $this->userId, $this->userId]);
        return (int) $this->db->lastInsertId();
    }

    public function calculate(int $runId): array
    {
        $this->db->beginTransaction();
        try {
            $run = $this->run($runId, true);
            if ($run['calculation_mode'] !== 'CONFIGURED_RATES' || !in_array($run['status'], ['DRAFT','CALCULATED'], true)) {
                throw new InvalidArgumentException('Questa elaborazione non può essere ricalcolata automaticamente.');
            }
            $this->db->prepare('DELETE FROM payroll_details WHERE payroll_run_id = ?')->execute([$runId]);
            $configs = $this->all("SELECT pc.*, u.name FROM payroll_employee_configs pc INNER JOIN users u ON u.id = pc.user_id AND u.active = 1 WHERE pc.organization_id = ? AND pc.active = 1 AND pc.valid_from <= ? AND (pc.valid_to IS NULL OR pc.valid_to >= ?)", [$this->organizationId, $run['period_end'], $run['period_start']]);
            $totals = ['employees' => 0, 'gross' => 0.0, 'net' => 0.0, 'contributions' => 0.0, 'employer_contributions' => 0.0, 'tax' => 0.0];
            foreach ($configs as $config) {
                $hours = $this->one("SELECT COALESCE(SUM(hours),0) regular, COALESCE(SUM(overtime_hours),0) overtime FROM time_records WHERE organization_id = ? AND user_id = ? AND work_date BETWEEN ? AND ? AND approved = 1 AND record_type = 'WORK'", [$this->organizationId, $config['user_id'], $run['period_start'], $run['period_end']]);
                $regular = (float) $hours['regular']; $overtime = (float) $hours['overtime'];
                $ordinary = (float) $config['fixed_monthly_amount'] > 0 ? (float) $config['fixed_monthly_amount'] : $regular * (float) $config['hourly_rate'];
                $overtimeAmount = $overtime * (float) $config['hourly_rate'] * (float) $config['overtime_multiplier'];
                $gross = round($ordinary + $overtimeAmount, 2);
                $contributions = round($gross * (float) $config['inps_rate'] / 100, 2);
                $taxable = max(0, $gross - $contributions);
                $tax = round($taxable * (float) $config['tax_rate'] / 100, 2);
                $employerContributions = round($gross * ((float) $config['employer_contribution_rate'] + (float) $config['inail_rate']) / 100, 2);
                $net = round($gross - $contributions - $tax, 2);
                $employerCost = round($gross + $employerContributions, 2);
                $calculation = ['schema' => 'CONTROLLED-2', 'warning' => 'Calcolo gestionale soggetto a validazione del consulente del lavoro', 'rates' => ['hourly' => (float) $config['hourly_rate'], 'overtime_multiplier' => (float) $config['overtime_multiplier'], 'employee_contribution_percent' => (float) $config['inps_rate'], 'tax_percent' => (float) $config['tax_rate'], 'employer_percent' => (float) $config['employer_contribution_rate'], 'inail_percent' => (float) $config['inail_rate']]];
                $this->db->prepare('INSERT INTO payroll_details (organization_id, payroll_run_id, user_id, employee_name, regular_hours, overtime_hours, gross_amount, contributions_amount, employer_contributions_amount, tax_amount, reimbursements_amount, deductions_amount, net_amount, employer_cost, source_reference, calculation_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?, NOW())')
                    ->execute([$this->organizationId, $runId, $config['user_id'], $config['name'], $regular, $overtime, $gross, $contributions, $employerContributions, $tax, $net, $employerCost, 'CONFIG:' . $config['id'], json_encode($calculation, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)]);
                $detailId = (int) $this->db->lastInsertId();
                $this->components($detailId, [['ORDINARY','Retribuzione ordinaria','EARNING',1,1,$ordinary],['OVERTIME','Straordinari','EARNING',$overtime,(float) $config['hourly_rate'] * (float) $config['overtime_multiplier'],$overtimeAmount],['INPS_EMP','Contributi dipendente','EMPLOYEE_CONTRIBUTION',1,(float) $config['inps_rate'],$contributions],['TAX','Ritenute fiscali','TAX',1,(float) $config['tax_rate'],$tax],['CONTR_EMP','Contributi datore','EMPLOYER_CONTRIBUTION',1,(float) $config['employer_contribution_rate'] + (float) $config['inail_rate'],$employerContributions]]);
                $totals['employees']++; $totals['gross'] += $gross; $totals['net'] += $net; $totals['contributions'] += $contributions; $totals['employer_contributions'] += $employerContributions; $totals['tax'] += $tax;
            }
            if ($totals['employees'] === 0) { throw new InvalidArgumentException('Nessuna configurazione dipendente valida per il periodo.'); }
            $this->updateRunTotals($runId, $totals, 'CALCULATED');
            $this->db->commit(); return $totals;
        } catch (Throwable $exception) { if ($this->db->inTransaction()) { $this->db->rollBack(); } throw $exception; }
    }

    public function importPayslipRows(int $runId, string $sourceFile, array $rows): array
    {
        $this->db->beginTransaction();
        try {
            $run = $this->run($runId, true);
            if ($run['calculation_mode'] !== 'IMPORTED_PAYSLIPS' || !in_array($run['status'], ['DRAFT','CALCULATED'], true)) { throw new InvalidArgumentException('Elaborazione non predisposta per cedolini importati.'); }
            $this->db->prepare('DELETE FROM payroll_components WHERE payroll_detail_id IN (SELECT id FROM payroll_details WHERE payroll_run_id = ?)')->execute([$runId]);
            $this->db->prepare('DELETE FROM payroll_details WHERE payroll_run_id = ?')->execute([$runId]);
            $this->db->prepare('DELETE FROM payroll_import_rows WHERE payroll_run_id = ?')->execute([$runId]);
            $totals = ['employees' => 0, 'gross' => 0.0, 'net' => 0.0, 'contributions' => 0.0, 'employer_contributions' => 0.0, 'tax' => 0.0];
            $seen=[];
            foreach ($rows as $index => $row) {
                $employeeCode = trim((string) ($row['employee_code'] ?? ''));
                try{$normalized=['employee_code'=>$employeeCode,'gross'=>$this->amount($row['gross']??null),'employee_contributions'=>$this->amount($row['employee_contributions']??null),'employer_contributions'=>$this->amount($row['employer_contributions']??null),'tax'=>$this->amount($row['tax']??null),'reimbursements'=>$this->amount($row['reimbursements']??0),'deductions'=>$this->amount($row['deductions']??0),'net'=>$this->amount($row['net']??null)];}catch(InvalidArgumentException $exception){$this->stage($runId,$sourceFile,$index+2,$employeeCode,['raw'=>$row],'ERROR',$exception->getMessage());continue;}
                $config = $this->one('SELECT pc.*, u.name FROM payroll_employee_configs pc INNER JOIN users u ON u.id = pc.user_id AND u.active=1 WHERE pc.organization_id = ? AND pc.employee_code = ? AND pc.active = 1 AND pc.valid_from<=? AND (pc.valid_to IS NULL OR pc.valid_to>=?) ORDER BY pc.valid_from DESC LIMIT 1', [$this->organizationId, $employeeCode,$run['period_end'],$run['period_start']]);
                $expectedNet=round($normalized['gross']-$normalized['employee_contributions']-$normalized['tax']+$normalized['reimbursements']-$normalized['deductions'],2);$negative=false;foreach(['gross','employee_contributions','employer_contributions','tax','reimbursements','deductions','net'] as $moneyKey){if($normalized[$moneyKey]<0){$negative=true;break;}}$error=null;if(!$config){$error='Codice dipendente non mappato o configurazione fuori validità';}elseif(isset($seen[$employeeCode])){$error='Dipendente duplicato nel file';}elseif($negative){$error='Importi negativi non ammessi';}elseif(abs($expectedNet-$normalized['net'])>.02){$error='Quadratura netto non valida: atteso '.number_format($expectedNet,2,'.','');}
                if($error!==null){$this->stage($runId,$sourceFile,$index+2,$employeeCode,$normalized,'ERROR',$error);continue;}$seen[$employeeCode]=true;
                $this->stage($runId, $sourceFile, $index + 2, $employeeCode, $normalized, 'IMPORTED', null);
                $employerCost = $normalized['gross'] + $normalized['employer_contributions'];
                $this->db->prepare('INSERT INTO payroll_details (organization_id, payroll_run_id, user_id, employee_name, gross_amount, contributions_amount, employer_contributions_amount, tax_amount, reimbursements_amount, deductions_amount, net_amount, employer_cost, source_reference, calculation_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())')
                    ->execute([$this->organizationId, $runId, $config['user_id'], $config['name'], $normalized['gross'], $normalized['employee_contributions'], $normalized['employer_contributions'], $normalized['tax'], $normalized['reimbursements'], $normalized['deductions'], $normalized['net'], $employerCost, basename($sourceFile) . ':' . ($index + 2), json_encode(['schema' => 'IMPORTED-1', 'source' => basename($sourceFile)], JSON_UNESCAPED_SLASHES)]);
                $detailId = (int) $this->db->lastInsertId();
                $this->components($detailId, [['GROSS','Lordo da cedolino','EARNING',1,1,$normalized['gross']],['INPS_EMP','Contributi dipendente','EMPLOYEE_CONTRIBUTION',1,1,$normalized['employee_contributions']],['CONTR_EMP','Contributi datore','EMPLOYER_CONTRIBUTION',1,1,$normalized['employer_contributions']],['TAX','Ritenute fiscali','TAX',1,1,$normalized['tax']],['REIMB','Rimborsi','REIMBURSEMENT',1,1,$normalized['reimbursements']],['DEDUCT','Altre trattenute','DEDUCTION',1,1,$normalized['deductions']]], 'IMPORTED');
                $totals['employees']++; foreach (['gross','net','tax'] as $key) { $totals[$key] += $normalized[$key]; } $totals['contributions'] += $normalized['employee_contributions']; $totals['employer_contributions'] += $normalized['employer_contributions'];
            }
            if ($totals['employees'] === 0) { throw new InvalidArgumentException('Nessuna riga cedolino valida importata.'); }
            $this->updateRunTotals($runId, $totals, 'CALCULATED');
            $this->db->commit(); return $totals;
        } catch (Throwable $exception) { if ($this->db->inTransaction()) { $this->db->rollBack(); } throw $exception; }
    }

    public function confirm(int $runId, string $professionalReference): void
    {
        $reference = trim($professionalReference);
        if (mb_strlen($reference) < 5) { throw new InvalidArgumentException('Inserisci il riferimento della validazione del consulente del lavoro.'); }
        $this->db->beginTransaction();
        try {
            $run = $this->run($runId, true);
            if ($run['status'] !== 'CALCULATED') { throw new InvalidArgumentException('Elaborazione non confermabile.'); }
            if($run['calculation_mode']!=='IMPORTED_PAYSLIPS'){throw new InvalidArgumentException('Il calcolo a tariffe è una simulazione gestionale e non può essere validato come payroll di produzione. Importa i cedolini certificati.');}
            $errors = $this->one("SELECT COUNT(*) count FROM payroll_import_rows WHERE payroll_run_id = ? AND status = 'ERROR'", [$runId]);
            if ($run['calculation_mode'] === 'IMPORTED_PAYSLIPS' && (int) $errors['count'] > 0) { throw new InvalidArgumentException('Correggi tutte le righe di importazione prima della conferma.'); }
            $this->db->prepare("UPDATE payroll_runs SET status = 'CONFIRMED', professional_validation_reference = ?, validated_at = NOW(), confirmed_by = ?, confirmed_at = NOW(), locked_at = NOW(), updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?")
                ->execute([$reference, $this->userId, $this->userId, $runId, $this->organizationId]);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $exception;
        }
    }

    public function markPaid(int $runId): void
    {
        $statement = $this->db->prepare("UPDATE payroll_runs SET status = 'PAID', paid_at = NOW(), updated_by = ?, updated_at = NOW() WHERE id = ? AND organization_id = ? AND status = 'CONFIRMED'");
        $statement->execute([$this->userId, $runId, $this->organizationId]);
        if ($statement->rowCount() !== 1) { throw new InvalidArgumentException('Elaborazione non pagabile.'); }
    }

    private function components(int $detailId, array $items, string $source = 'CALCULATED'): void
    {
        $insert = $this->db->prepare('INSERT INTO payroll_components (organization_id, payroll_detail_id, component_code, description, component_type, quantity, rate, amount, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
        foreach ($items as [$code,$description,$type,$quantity,$rate,$amount]) { if (abs((float) $amount) < .005) { continue; } $insert->execute([$this->organizationId,$detailId,$code,$description,$type,$quantity,$rate,round((float) $amount,2),$source]); }
    }
    private function stage(int $runId, string $file, int $row, string $code, array $normalized, string $status, ?string $error): void { $this->db->prepare('INSERT INTO payroll_import_rows (organization_id, payroll_run_id, source_file, source_row, employee_code, normalized_json, status, error_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())')->execute([$this->organizationId,$runId,basename($file),$row,$code ?: null,json_encode($normalized, JSON_UNESCAPED_UNICODE),$status,$error]); }
    private function updateRunTotals(int $id, array $t, string $status): void { $this->db->prepare('UPDATE payroll_runs SET employees_count = ?, gross_total = ?, net_total = ?, contributions_total = ?, tax_total = ?, status = ?, updated_by = ?, updated_at = NOW() WHERE id = ?')->execute([$t['employees'],round($t['gross'],2),round($t['net'],2),round($t['contributions'] + $t['employer_contributions'],2),round($t['tax'],2),$status,$this->userId,$id]); }
    private function amount(mixed $value): float { $raw=trim((string)$value);if($raw===''){return 0.0;}$raw=str_replace(["\xc2\xa0",' ','€'],'',$raw);if(str_contains($raw,',')&&str_contains($raw,'.')){if(strrpos($raw,',')>strrpos($raw,'.')){$raw=str_replace('.','',$raw);$raw=str_replace(',','.',$raw);}else{$raw=str_replace(',','',$raw);}}elseif(str_contains($raw,',')){$raw=str_replace(',','.',$raw);}if(!is_numeric($raw)){throw new InvalidArgumentException('Importo non numerico nel CSV paghe.');}return round((float)$raw,2); }
    private function run(int $id, bool $lock): array { $r = $this->one('SELECT * FROM payroll_runs WHERE id = ? AND organization_id = ?' . ($lock ? ' FOR UPDATE' : ''), [$id,$this->organizationId]); if (!$r) { throw new InvalidArgumentException('Elaborazione paghe non trovata.'); } return $r; }
    private function one(string $sql, array $params): array|false { $s=$this->db->prepare($sql);$s->execute($params);return $s->fetch(); }
    private function all(string $sql, array $params): array { $s=$this->db->prepare($sql);$s->execute($params);return $s->fetchAll(); }
}
