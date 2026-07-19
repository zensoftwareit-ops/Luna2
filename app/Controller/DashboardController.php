<?php

declare(strict_types=1);

namespace Luna\Controller;

use Luna\Core\Auth;

final class DashboardController extends BaseController
{
    public function index(): never
    {
        $organizationId = Auth::organizationId();
        $metrics = [];

        $queries = [
            'customers' => 'SELECT COUNT(*) FROM customers WHERE organization_id = ? AND active = 1',
            'open_quotes' => "SELECT COUNT(*) FROM documents WHERE organization_id = ? AND document_type = 'QUOTE' AND status IN ('DRAFT','SENT')",
            'receivables' => "SELECT COALESCE(SUM(balance_due),0) FROM documents WHERE organization_id = ? AND document_type IN ('SALES_INVOICE','CREDIT_NOTE') AND status NOT IN ('DRAFT','CANCELLED','PAID')",
            'payables' => "SELECT COALESCE(SUM(balance_due),0) FROM documents WHERE organization_id = ? AND document_type = 'PURCHASE_INVOICE' AND status NOT IN ('DRAFT','CANCELLED','PAID')",
            'overdue_tax' => "SELECT COUNT(*) FROM tax_deadlines WHERE organization_id = ? AND status <> 'COMPLETED' AND due_date < CURRENT_DATE()",
            'low_stock' => 'SELECT COUNT(*) FROM inventory_balances WHERE organization_id = ? AND quantity <= minimum_stock',
        ];
        foreach ($queries as $key => $sql) {
            $statement = $this->db->prepare($sql);
            $statement->execute([$organizationId]);
            $metrics[$key] = $statement->fetchColumn();
        }

        $statement = $this->db->prepare(
            "SELECT id, number, document_date, counterparty_name, total, balance_due, status, document_type
             FROM documents WHERE organization_id = ? ORDER BY document_date DESC, id DESC LIMIT 8"
        );
        $statement->execute([$organizationId]);
        $recentDocuments = $statement->fetchAll();

        $statement = $this->db->prepare(
            "SELECT due_date, deadline_type, description, amount, status
             FROM tax_deadlines WHERE organization_id = ? AND status <> 'COMPLETED'
             ORDER BY due_date ASC LIMIT 8"
        );
        $statement->execute([$organizationId]);
        $deadlines = $statement->fetchAll();

        $this->view->render('dashboard', compact('metrics', 'recentDocuments', 'deadlines') + ['title' => 'Dashboard']);
    }
}
