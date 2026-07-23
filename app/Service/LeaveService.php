<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use Throwable;

final class LeaveService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $actorId)
    {
    }

    public function request(int $userId, array $data): int
    {
        if(trim((string)($data['starts_at']??''))===''||trim((string)($data['ends_at']??''))===''||strtotime((string)$data['starts_at'])===false||strtotime((string)$data['ends_at'])===false){throw new InvalidArgumentException('Periodo ferie non valido.');}
        $start = new DateTimeImmutable((string) ($data['starts_at'] ?? ''));
        $end = new DateTimeImmutable((string) ($data['ends_at'] ?? ''));
        $type = strtoupper((string) ($data['leave_type'] ?? 'HOLIDAY'));
        $hours = round((float) ($data['requested_hours'] ?? 0), 2);
        if ($end <= $start || $hours <= 0 || !in_array($type, ['HOLIDAY', 'PERMIT', 'ROL', 'SICK', 'OTHER'], true)) {
            throw new InvalidArgumentException('Periodo, causale o monte ore non valido.');
        }
        if($start->format('Y')!==$end->format('Y')){throw new InvalidArgumentException('Le richieste a cavallo d’anno vanno suddivise per esercizio.');}
        if($this->workDates($start,$end)===[]){throw new InvalidArgumentException('La richiesta non contiene giorni lavorativi.');}
        if (!$this->one('SELECT id FROM users WHERE id = ? AND organization_id = ? AND active = 1', [$userId, $this->organizationId])) {
            throw new InvalidArgumentException('Utente non valido.');
        }
        $overlap = $this->one("SELECT id FROM leave_requests WHERE organization_id = ? AND user_id = ? AND status IN ('SUBMITTED','APPROVED') AND starts_at < ? AND ends_at > ? LIMIT 1", [$this->organizationId, $userId, $end->format('Y-m-d H:i:s'), $start->format('Y-m-d H:i:s')]);
        if ($overlap) {
            throw new InvalidArgumentException('Esiste già una richiesta sovrapposta.');
        }
        $this->db->beginTransaction();
        try {
            $this->db->prepare("INSERT INTO leave_requests (organization_id, user_id, leave_type, starts_at, ends_at, requested_hours, reason, status, submitted_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', NOW(), NOW(), NOW())")
                ->execute([$this->organizationId, $userId, $type, $start->format('Y-m-d H:i:s'), $end->format('Y-m-d H:i:s'), $hours, trim((string) ($data['reason'] ?? '')) ?: null]);
            $leaveId = (int) $this->db->lastInsertId();
            $this->db->prepare("INSERT INTO approval_requests (organization_id, request_type, requester_id, entity_type, entity_id, status, requested_at, reason, created_at, updated_at) VALUES (?, 'LEAVE', ?, 'leave_requests', ?, 'SUBMITTED', NOW(), ?, NOW(), NOW())")
                ->execute([$this->organizationId, $userId, $leaveId, trim((string) ($data['reason'] ?? '')) ?: null]);
            $approvalId = (int) $this->db->lastInsertId();
            $this->db->prepare('UPDATE leave_requests SET approval_request_id = ? WHERE id = ?')->execute([$approvalId, $leaveId]);
            $this->history($approvalId, 'CREATE', null);
            $this->history($approvalId, 'SUBMIT', $data['reason'] ?? null);
            $this->db->commit();
            return $leaveId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $exception;
        }
    }

    public function decide(int $leaveId, bool $approve, ?string $notes = null): void
    {
        $this->db->beginTransaction();
        try {
            $leave = $this->one('SELECT * FROM leave_requests WHERE id = ? AND organization_id = ? FOR UPDATE', [$leaveId, $this->organizationId]);
            if (!$leave || $leave['status'] !== 'SUBMITTED') {
                throw new InvalidArgumentException('Richiesta non decidibile.');
            }
            if ((int) $leave['user_id'] === $this->actorId) {
                throw new InvalidArgumentException('Non puoi approvare la tua richiesta.');
            }
            $status = $approve ? 'APPROVED' : 'REJECTED';
            if ($approve && in_array($leave['leave_type'], ['HOLIDAY', 'PERMIT', 'ROL', 'OTHER'], true)) {
                $year = (int) substr((string) $leave['starts_at'], 0, 4);
                $balance = $this->one('SELECT * FROM leave_balances WHERE organization_id = ? AND user_id = ? AND balance_year = ? AND leave_type = ? FOR UPDATE', [$this->organizationId, $leave['user_id'], $year, $leave['leave_type']]);
                if (!$balance) {
                    throw new InvalidArgumentException('Configura prima il saldo ferie/permessi del dipendente.');
                }
                if (((float) $balance['opening_hours'] + (float) $balance['accrued_hours'] + (float) $balance['adjusted_hours'] - (float) $balance['used_hours']) + 0.005 < (float) $leave['requested_hours']) {
                    throw new InvalidArgumentException('Saldo ferie/permessi insufficiente.');
                }
                $this->db->prepare('UPDATE leave_balances SET used_hours = used_hours + ? WHERE id = ?')->execute([$leave['requested_hours'], $balance['id']]);
            }
            if($approve){$user=$this->one('SELECT name FROM users WHERE id=? AND organization_id=?',[$leave['user_id'],$this->organizationId]);$recordType=match($leave['leave_type']){'HOLIDAY'=>'HOLIDAY','SICK'=>'SICK','OTHER'=>'OTHER',default=>'LEAVE'};$dates=$this->workDates(new DateTimeImmutable((string)$leave['starts_at']),new DateTimeImmutable((string)$leave['ends_at']));$remaining=(float)$leave['requested_hours'];foreach($dates as $index=>$date){$daysLeft=count($dates)-$index;$dayHours=round($remaining/$daysLeft,2);$remaining=round($remaining-$dayHours,2);$this->db->prepare('INSERT INTO time_records (organization_id,user_id,work_date,employee_name,record_type,hours,approved,notes,created_by,updated_by,created_at,updated_at) VALUES (?,?,?,?,?,?,1,?,?,?,NOW(),NOW()) ON DUPLICATE KEY UPDATE hours=VALUES(hours),approved=1,notes=VALUES(notes),updated_by=VALUES(updated_by),updated_at=NOW()')->execute([$this->organizationId,$leave['user_id'],$date,$user['name']??'Dipendente',$recordType,$dayHours,'Richiesta #'.$leaveId,$this->actorId,$this->actorId]);}}
            $this->db->prepare('UPDATE leave_requests SET status = ?, decided_at = NOW(), decided_by = ?, decision_notes = ?, updated_at = NOW() WHERE id = ?')->execute([$status, $this->actorId, $notes, $leaveId]);
            $this->db->prepare('UPDATE approval_requests SET status = ?, approver_id = ?, decided_at = NOW(), decision_notes = ?, updated_at = NOW() WHERE id = ?')->execute([$status, $this->actorId, $notes, $leave['approval_request_id']]);
            $this->history((int) $leave['approval_request_id'], $approve ? 'APPROVE' : 'REJECT', $notes);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $exception;
        }
    }

    public function cancel(int $leaveId,bool $canManage=false): void
    {
        $this->db->beginTransaction();try{$leave=$this->one('SELECT * FROM leave_requests WHERE id=? AND organization_id=? FOR UPDATE',[$leaveId,$this->organizationId]);if(!$leave||!in_array($leave['status'],['SUBMITTED','APPROVED'],true)){throw new InvalidArgumentException('Richiesta non annullabile.');}if((int)$leave['user_id']!==$this->actorId&&!$canManage){throw new InvalidArgumentException('Non puoi annullare la richiesta di un altro utente.');}if($leave['status']==='APPROVED'){if(in_array($leave['leave_type'],['HOLIDAY','PERMIT','ROL','OTHER'],true)){$year=(int)substr((string)$leave['starts_at'],0,4);$this->db->prepare('UPDATE leave_balances SET used_hours=GREATEST(0,used_hours-?) WHERE organization_id=? AND user_id=? AND balance_year=? AND leave_type=?')->execute([$leave['requested_hours'],$this->organizationId,$leave['user_id'],$year,$leave['leave_type']]);}$this->db->prepare('DELETE FROM time_records WHERE organization_id=? AND user_id=? AND notes=?')->execute([$this->organizationId,$leave['user_id'],'Richiesta #'.$leaveId]);}$this->db->prepare("UPDATE leave_requests SET status='CANCELLED',decided_at=NOW(),decided_by=?,decision_notes='Annullata',updated_at=NOW() WHERE id=?")->execute([$this->actorId,$leaveId]);$this->db->prepare("UPDATE approval_requests SET status='CANCELLED',approver_id=?,decided_at=NOW(),decision_notes='Annullata',updated_at=NOW() WHERE id=?")->execute([$this->actorId,$leave['approval_request_id']]);$this->history((int)$leave['approval_request_id'],'CANCEL','Richiesta annullata');$this->db->commit();}catch(Throwable $exception){if($this->db->inTransaction()){$this->db->rollBack();}throw $exception;}
    }

    private function workDates(DateTimeImmutable $start,DateTimeImmutable $end): array{$dates=[];$cursor=$start->setTime(0,0);$last=$end->setTime(0,0);while($cursor<=$last){if((int)$cursor->format('N')<=5){$dates[]=$cursor->format('Y-m-d');}$cursor=$cursor->modify('+1 day');}return $dates;}

    private function history(int $approvalId, string $action, ?string $notes): void
    {
        $this->db->prepare('INSERT INTO approval_history (organization_id, approval_request_id, action, actor_id, notes, created_at) VALUES (?, ?, ?, ?, ?, NOW())')
            ->execute([$this->organizationId, $approvalId, $action, $this->actorId ?: null, $notes]);
    }

    private function one(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetch();
    }
}
