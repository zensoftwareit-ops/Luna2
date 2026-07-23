<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use Throwable;

final class ProjectService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId)
    {
    }

    public function addTime(int $projectId, array $data): int
    {
        $project = $this->project($projectId);
        $hours = round((float) ($data['hours'] ?? 0), 2);
        if ($hours <= 0 || $hours > 24 || trim((string) ($data['description'] ?? '')) === '') {
            throw new InvalidArgumentException('Ore o descrizione non valide.');
        }
        $this->db->prepare("INSERT INTO project_time_entries (organization_id, project_id, user_id, work_date, description, hours, hourly_cost, hourly_rate, billable, billing_status, approved_by, approved_at, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, NOW())")
            ->execute([$this->organizationId, $project['id'], (int) ($data['user_id'] ?? $this->userId) ?: null, $data['work_date'] ?? date('Y-m-d'), trim((string) $data['description']), $hours, round((float) ($data['hourly_cost'] ?? 0), 4), round((float) ($data['hourly_rate'] ?? 0), 4), !empty($data['billable']) ? 1 : 0, !empty($data['billable']) ? 'OPEN' : 'NON_BILLABLE', $this->userId]);
        return (int) $this->db->lastInsertId();
    }

    public function approveTime(int $entryId): void
    {
        $statement = $this->db->prepare('UPDATE project_time_entries SET approved_by = ?, approved_at = NOW() WHERE id = ? AND organization_id = ? AND billing_status IN (\'OPEN\', \'NON_BILLABLE\')');
        $statement->execute([$this->userId, $entryId, $this->organizationId]);
        if ($statement->rowCount() !== 1) {
            throw new InvalidArgumentException('Consuntivo non approvabile.');
        }
    }

    public function addExpense(int $projectId, array $data): int
    {
        $project = $this->project($projectId);
        $cost = round((float) ($data['cost_amount'] ?? 0), 2);
        $description = trim((string) ($data['description'] ?? ''));
        if ($cost < 0 || $description === '') {
            throw new InvalidArgumentException('Spesa non valida.');
        }
        $billable = !empty($data['billable']);
        $this->db->prepare('INSERT INTO project_expenses (organization_id, project_id, expense_date, supplier_id, description, cost_amount, billable_amount, billable, billing_status, source_document_id, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())')
            ->execute([$this->organizationId, $project['id'], $data['expense_date'] ?? date('Y-m-d'), (int) ($data['supplier_id'] ?? 0) ?: null, $description, $cost, round((float) ($data['billable_amount'] ?? $cost), 2), $billable ? 1 : 0, $billable ? 'OPEN' : 'NON_BILLABLE', (int) ($data['source_document_id'] ?? 0) ?: null, $this->userId]);
        return (int) $this->db->lastInsertId();
    }

    public function addMilestone(int $projectId, array $data): int
    {
        $project = $this->project($projectId);
        $name = trim((string) ($data['name'] ?? ''));
        if ($name === '') {
            throw new InvalidArgumentException('Nome milestone obbligatorio.');
        }
        $this->db->prepare("INSERT INTO project_milestones (organization_id, project_id, name, due_date, billing_amount, progress_percent, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'OPEN', NOW(), NOW())")
            ->execute([$this->organizationId, $project['id'], $name, ($data['due_date'] ?? '') ?: null, round((float) ($data['billing_amount'] ?? 0), 2), max(0, min(100, (float) ($data['progress_percent'] ?? 0)))]);
        return (int) $this->db->lastInsertId();
    }

    public function markMilestoneReady(int $milestoneId): void
    {
        $statement = $this->db->prepare("UPDATE project_milestones SET status = 'READY', progress_percent = 100, updated_at = NOW() WHERE id = ? AND organization_id = ? AND status = 'OPEN'");
        $statement->execute([$milestoneId, $this->organizationId]);
        if ($statement->rowCount() !== 1) {
            throw new InvalidArgumentException('Milestone non aggiornabile.');
        }
    }

    public function metrics(int $projectId): array
    {
        $project = $this->project($projectId);
        $sql = "SELECT
            COALESCE((SELECT SUM(hours * hourly_cost) FROM project_time_entries WHERE project_id = p.id AND billing_status <> 'CANCELLED'),0) labor_cost,
            COALESCE((SELECT SUM(cost_amount) FROM project_expenses WHERE project_id = p.id AND billing_status <> 'CANCELLED'),0) expense_cost,
            COALESCE((SELECT SUM(hours * hourly_rate) FROM project_time_entries WHERE project_id = p.id AND billable = 1 AND billing_status = 'OPEN' AND approved_at IS NOT NULL),0) open_time_revenue,
            COALESCE((SELECT SUM(billable_amount) FROM project_expenses WHERE project_id = p.id AND billable = 1 AND billing_status = 'OPEN'),0) open_expense_revenue,
            COALESCE((SELECT SUM(billing_amount) FROM project_milestones WHERE project_id = p.id AND status = 'READY'),0) ready_milestones,
            COALESCE((SELECT SUM(dl.taxable_amount) FROM document_lines dl INNER JOIN documents d ON d.id = dl.document_id WHERE dl.project_id = p.id AND d.document_type = 'SALES_INVOICE' AND d.status <> 'CANCELLED'),0) invoiced
            FROM projects p WHERE p.id = ? AND p.organization_id = ?";
        return $this->one($sql, [$project['id'], $this->organizationId]) ?: [];
    }

    public function createInvoice(int $projectId, array $selection = [], ?string $date = null): int
    {
        $date ??= date('Y-m-d');
        $this->db->beginTransaction();
        try {
            $project = $this->one('SELECT * FROM projects WHERE id = ? AND organization_id = ? FOR UPDATE', [$projectId, $this->organizationId]);
            if (!$project || empty($project['customer_id'])) {
                throw new InvalidArgumentException('La commessa deve avere un cliente associato.');
            }
            $customer = $this->one('SELECT * FROM customers WHERE id = ? AND organization_id = ? AND active = 1', [$project['customer_id'], $this->organizationId]);
            if (!$customer) {
                throw new InvalidArgumentException('Cliente della commessa non valido.');
            }
            $time = $this->selectedRows('project_time_entries', $projectId, "billable = 1 AND billing_status = 'OPEN' AND approved_at IS NOT NULL", $selection['time'] ?? []);
            $expenses = $this->selectedRows('project_expenses', $projectId, "billable = 1 AND billing_status = 'OPEN'", $selection['expenses'] ?? []);
            $milestones = $this->selectedRows('project_milestones', $projectId, "status = 'READY'", $selection['milestones'] ?? []);
            $rows = [];
            foreach ($time as $item) {
                $rows[] = ['source' => 'time', 'id' => $item['id'], 'description' => 'Consuntivo ' . $item['work_date'] . ' - ' . $item['description'], 'quantity' => $item['hours'], 'unit' => 'ORE', 'unit_price' => $item['hourly_rate']];
            }
            foreach ($expenses as $item) {
                $rows[] = ['source' => 'expense', 'id' => $item['id'], 'description' => 'Riaddebito ' . $item['expense_date'] . ' - ' . $item['description'], 'quantity' => 1, 'unit' => 'NR', 'unit_price' => $item['billable_amount']];
            }
            foreach ($milestones as $item) {
                $rows[] = ['source' => 'milestone', 'id' => $item['id'], 'description' => 'Milestone - ' . $item['name'], 'quantity' => 1, 'unit' => 'NR', 'unit_price' => $item['billing_amount']];
            }
            if ($rows === []) {
                throw new InvalidArgumentException('Nessun consuntivo approvato o milestone pronta da fatturare.');
            }
            $vatRate = 22.0;
            $taxable = round(array_sum(array_map(fn (array $row): float => (float) $row['quantity'] * (float) $row['unit_price'], $rows)), 2);
            $vat = round($taxable * $vatRate / 100, 2);
            $total = round($taxable + $vat, 2);
            $number = (new DocumentNumberService($this->db, $this->organizationId))->next('SALES_INVOICE', $date);
            $this->db->prepare("INSERT INTO documents (organization_id, document_type, number, fiscal_year, document_date, due_date, counterparty_type, counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, total, balance_due, status, fulfillment_status, fatturapa_type, vat_collectability, payment_method_code, notes, project_id, created_by, updated_by, created_at, updated_at) VALUES (?, 'SALES_INVOICE', ?, ?, ?, ?, 'CUSTOMER', ?, ?, ?, 'EUR', ?, ?, ?, ?, 'DRAFT', 'NOT_REQUIRED', 'TD01', 'I', 'MP05', ?, ?, ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $number, (int) substr($date, 0, 4), $date, date('Y-m-d', strtotime($date . ' +30 days')), $customer['id'], $customer['business_name'], 'Fatturazione commessa ' . $project['code'], $taxable, $vat, $total, $total, 'Consuntivazione commessa ' . $project['name'], $projectId, $this->userId, $this->userId]);
            $invoiceId = (int) $this->db->lastInsertId();
            $insert = $this->db->prepare('INSERT INTO document_lines (organization_id, document_id, line_number, project_id, description, quantity, unit, unit_price, taxable_amount, vat_code, vat_rate, vat_amount, total_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())');
            foreach ($rows as $index => $row) {
                $rowTaxable = round((float) $row['quantity'] * (float) $row['unit_price'], 2);
                $rowVat = round($rowTaxable * $vatRate / 100, 2);
                $insert->execute([$this->organizationId, $invoiceId, $index + 1, $projectId, $row['description'], $row['quantity'], $row['unit'], $row['unit_price'], $rowTaxable, '22', $vatRate, $rowVat, $rowTaxable + $rowVat]);
                $lineId = (int) $this->db->lastInsertId();
                $table = match ($row['source']) { 'time' => 'project_time_entries', 'expense' => 'project_expenses', default => 'project_milestones' };
                $status = $row['source'] === 'milestone' ? "status = 'INVOICED'" : "billing_status = 'INVOICED'";
                $this->db->prepare("UPDATE {$table} SET {$status}, document_line_id = ? WHERE id = ? AND organization_id = ?")->execute([$lineId, $row['id'], $this->organizationId]);
            }
            if (!empty($project['source_document_id'])) {
                $this->db->prepare("INSERT INTO document_links (organization_id, source_document_id, target_document_id, link_type, created_by, created_at) VALUES (?, ?, ?, 'PROJECT_BILLING', ?, NOW())")
                    ->execute([$this->organizationId, $project['source_document_id'], $invoiceId, $this->userId]);
            }
            $this->db->commit();
            return $invoiceId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function selectedRows(string $table, int $projectId, string $condition, array $ids): array
    {
        $sql = "SELECT * FROM {$table} WHERE organization_id = ? AND project_id = ? AND {$condition}";
        $params = [$this->organizationId, $projectId];
        $ids = array_values(array_filter(array_map('intval', $ids), static fn (int $id): bool => $id > 0));
        if ($ids !== []) {
            $sql .= ' AND id IN (' . implode(',', array_fill(0, count($ids), '?')) . ')';
            $params = array_merge($params, $ids);
        }
        $sql .= ' FOR UPDATE';
        return $this->all($sql, $params);
    }

    private function project(int $id): array
    {
        $project = $this->one('SELECT * FROM projects WHERE id = ? AND organization_id = ?', [$id, $this->organizationId]);
        if (!$project) {
            throw new InvalidArgumentException('Commessa non trovata.');
        }
        return $project;
    }

    private function one(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetch();
    }

    private function all(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetchAll();
    }
}
