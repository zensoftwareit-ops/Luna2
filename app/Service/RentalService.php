<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use Throwable;

final class RentalService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId = 0)
    {
    }

    public function recordMeter(int $contractId, string $date, int $kilometers, string $source = 'MANUAL', ?string $notes = null): int
    {
        $contract = $this->contract($contractId);
        if ($kilometers < (int) $contract['current_km']) { throw new InvalidArgumentException('Il chilometraggio non può diminuire.'); }
        $this->db->prepare('INSERT INTO rental_meter_readings (organization_id, rental_contract_id, reading_date, kilometers, source, notes, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE kilometers = VALUES(kilometers), source = VALUES(source), notes = VALUES(notes), created_by = VALUES(created_by), id = LAST_INSERT_ID(id)')
            ->execute([$this->organizationId, $contractId, $date, $kilometers, in_array($source, ['MANUAL','CUSTOMER','SERVICE','IMPORT'], true) ? $source : 'MANUAL', $notes, $this->userId ?: null]);
        $this->db->prepare('UPDATE rental_contracts SET current_km = GREATEST(current_km, ?), updated_by = ?, updated_at = NOW() WHERE id = ?')->execute([$kilometers, $this->userId ?: null, $contractId]);
        return (int) $this->db->lastInsertId();
    }

    public function changeTicketStatus(int $ticketId, string $status, ?string $notes = null, ?int $assignedTo = null): void
    {
        $status = strtoupper($status);
        if (!in_array($status, ['OPEN','IN_PROGRESS','WAITING','RESOLVED','CLOSED'], true)) { throw new InvalidArgumentException('Stato ticket non valido.'); }
        $ticket = $this->one('SELECT * FROM rental_tickets WHERE id = ? AND organization_id = ?', [$ticketId, $this->organizationId]);
        if (!$ticket) { throw new InvalidArgumentException('Ticket non trovato.'); }
        $resolved = in_array($status, ['RESOLVED','CLOSED'], true) ? date('Y-m-d H:i:s') : null;
        $this->db->prepare('UPDATE rental_tickets SET status = ?, assigned_to = COALESCE(?, assigned_to), resolved_at = COALESCE(?, resolved_at), updated_by = ?, updated_at = NOW() WHERE id = ?')
            ->execute([$status, $assignedTo, $resolved, $this->userId ?: null, $ticketId]);
        $this->event($ticketId, $status === 'RESOLVED' ? 'RESOLVED' : ($status === 'CLOSED' ? 'CLOSED' : 'STATUS'), $ticket['status'], $status, $notes);
    }

    public function runAutomations(?string $today = null): array
    {
        $today ??= date('Y-m-d'); $deadlines = 0; $breaches = 0; $invoices = 0;
        $contracts = $this->all("SELECT * FROM rental_contracts WHERE organization_id = ? AND status IN ('ACTIVE','EXPIRING')", [$this->organizationId]);
        foreach ($contracts as $contract) {
            $noticeDate = (new DateTimeImmutable((string) $contract['end_date']))->modify('-' . (int) $contract['renewal_notice_days'] . ' days')->format('Y-m-d');
            if ($noticeDate <= $today) {
                $this->db->prepare("INSERT INTO rental_deadlines (organization_id, rental_contract_id, deadline_type, due_date, description, status, created_at, updated_at) SELECT ?, ?, 'RENEWAL', ?, ?, 'OPEN', NOW(), NOW() WHERE NOT EXISTS (SELECT 1 FROM rental_deadlines WHERE rental_contract_id = ? AND deadline_type = 'RENEWAL' AND status IN ('OPEN','OVERDUE'))")
                    ->execute([$this->organizationId, $contract['id'], $contract['end_date'], 'Rinnovo contratto ' . $contract['contract_number'], $contract['id']]);
                $deadlines++;
            }
            if (!$contract['next_invoice_date'] && (float) $contract['monthly_fee'] > 0 && (string)$contract['start_date'] <= $today) {
                $invoiceDay=max(1,min(28,(int)($contract['invoice_day']?:date('d',strtotime((string)$contract['start_date'])))));
                $candidate=date('Y-m-',strtotime($today)).str_pad((string)$invoiceDay,2,'0',STR_PAD_LEFT);
                if($candidate<(string)$contract['start_date']){$candidate=(string)$contract['start_date'];}
                $contract['next_invoice_date']=$candidate;
                $this->db->prepare('UPDATE rental_contracts SET invoice_day=?,next_invoice_date=?,updated_at=NOW() WHERE id=? AND organization_id=?')->execute([$invoiceDay,$candidate,$contract['id'],$this->organizationId]);
            }
            if ($contract['next_invoice_date'] && $contract['next_invoice_date'] <= $today && (float) $contract['monthly_fee'] > 0) {
                $this->createRecurringInvoice($contract, $today); $invoices++;
            }
        }
        $tickets = $this->all("SELECT id, status FROM rental_tickets WHERE organization_id = ? AND status NOT IN ('RESOLVED','CLOSED') AND sla_due_at IS NOT NULL AND sla_due_at < NOW() AND sla_breached = 0", [$this->organizationId]);
        foreach ($tickets as $ticket) {
            $this->db->prepare('UPDATE rental_tickets SET sla_breached = 1, updated_at = NOW() WHERE id = ?')->execute([$ticket['id']]);
            $this->event((int) $ticket['id'], 'SLA_BREACH', '0', '1', 'Scadenza SLA superata automaticamente.'); $breaches++;
        }
        $this->db->prepare("UPDATE rental_deadlines SET status = 'OVERDUE', updated_at = NOW() WHERE organization_id = ? AND status = 'OPEN' AND due_date < ?")->execute([$this->organizationId, $today]);
        return compact('deadlines', 'breaches', 'invoices');
    }

    private function createRecurringInvoice(array $contract, string $today): int
    {
        if (!$contract['customer_id']) { throw new InvalidArgumentException('Contratto senza cliente associato.'); }
        $periodStart = (string) $contract['next_invoice_date'];
        $periodEnd = (new DateTimeImmutable($periodStart))->modify('+1 month -1 day')->format('Y-m-d');
        $existing = $this->one('SELECT document_id FROM rental_invoice_links WHERE rental_contract_id = ? AND period_start = ? AND period_end = ?', [$contract['id'], $periodStart, $periodEnd]);
        if ($existing) { return (int) $existing['document_id']; }
        $this->db->beginTransaction();
        try {
            $number = (new DocumentNumberService($this->db, $this->organizationId))->next('SALES_INVOICE', $today);
            $taxable = round((float) $contract['monthly_fee'], 2); $vat = round($taxable * .22, 2); $total = $taxable + $vat;
            $this->db->prepare("INSERT INTO documents (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type, counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, total, balance_due, status, fulfillment_status, fatturapa_type, vat_collectability, payment_method_code, notes, created_by, updated_by, created_at, updated_at) VALUES (?, 'SALES_INVOICE', ?, ?, ?, ?, 'CUSTOMER', ?, ?, ?, 'EUR', ?, ?, ?, ?, 'DRAFT', 'NOT_REQUIRED', 'TD01', 'I', 'MP05', ?, ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $number, (int) substr($today, 0, 4), $today, date('Y-m-d', strtotime($today . ' +30 days')), $contract['customer_id'], $contract['customer_name'], 'Canone noleggio ' . $contract['contract_number'], $taxable, $vat, $total, $total, 'Periodo ' . $periodStart . ' / ' . $periodEnd, $this->userId ?: null, $this->userId ?: null]);
            $documentId = (int) $this->db->lastInsertId();
            $this->db->prepare("INSERT INTO document_lines (organization_id, document_id, line_number, description, quantity, unit, unit_price, taxable_amount, vat_code, vat_rate, vat_amount, total_amount, created_at, updated_at) VALUES (?, ?, 1, ?, 1, 'MESE', ?, ?, '22', 22, ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $documentId, 'Canone noleggio ' . $contract['vehicle_plate'], $taxable, $taxable, $vat, $total]);
            $this->db->prepare('INSERT INTO rental_invoice_links (organization_id, rental_contract_id, document_id, period_start, period_end, amount, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())')
                ->execute([$this->organizationId, $contract['id'], $documentId, $periodStart, $periodEnd, $taxable]);
            $next = (new DateTimeImmutable($periodStart))->modify('+1 month')->format('Y-m-d');
            $this->db->prepare('UPDATE rental_contracts SET next_invoice_date = ?, updated_at = NOW() WHERE id = ?')->execute([$next, $contract['id']]);
            $this->db->commit(); return $documentId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $exception;
        }
    }

    private function event(int $ticketId, string $type, ?string $old, ?string $new, ?string $notes): void
    {
        $this->db->prepare('INSERT INTO rental_ticket_events (organization_id, rental_ticket_id, event_type, old_value, new_value, notes, actor_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())')
            ->execute([$this->organizationId, $ticketId, $type, $old, $new, $notes, $this->userId ?: null]);
    }
    private function contract(int $id): array { $r = $this->one('SELECT * FROM rental_contracts WHERE id = ? AND organization_id = ?', [$id, $this->organizationId]); if (!$r) { throw new InvalidArgumentException('Contratto non trovato.'); } return $r; }
    private function one(string $sql, array $params): array|false { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetch(); }
    private function all(string $sql, array $params): array { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetchAll(); }
}
