<?php

declare(strict_types=1);

namespace Luna\Controller;

use InvalidArgumentException;
use Luna\Core\Auth;
use Luna\Service\BankingService;
use Luna\Service\ReceivablesService;
use Luna\Service\WithholdingService;
use RuntimeException;

final class TreasuryController extends BaseController
{
    private const ROLES = ['OWNER', 'ADMIN', 'ACCOUNTANT'];

    public function index(): never
    {
        $this->authorize();
        $organizationId = Auth::organizationId();
        $direction = strtoupper((string) ($_GET['direction'] ?? ''));
        $params = [$organizationId];
        $filter = '';
        if (in_array($direction, ['RECEIVABLE','PAYABLE'], true)) {
            $filter = ' AND i.direction = ?';
            $params[] = $direction;
        }
        $openItems = $this->query(
            "SELECT i.*, i.original_amount - i.settled_amount AS outstanding FROM accounting_open_items i
             WHERE i.organization_id = ? AND i.status IN ('OPEN','PARTIAL','OVERDUE','DISPUTED'){$filter}
             ORDER BY i.due_date, i.id LIMIT 1000", $params
        );
        $payments = $this->query(
            'SELECT p.*, COALESCE(c.business_name,s.business_name) AS party_name, b.name AS bank_name,
                    a.open_item_id FROM payments p
             LEFT JOIN customers c ON c.id = p.customer_id LEFT JOIN suppliers s ON s.id = p.supplier_id
             LEFT JOIN bank_accounts b ON b.id = p.bank_account_id
             LEFT JOIN payment_allocations a ON a.payment_id = p.id
             WHERE p.organization_id = ? ORDER BY p.payment_date DESC, p.id DESC LIMIT 300', [$organizationId]
        );
        $bankAccounts = $this->query('SELECT * FROM bank_accounts WHERE organization_id = ? ORDER BY active DESC, name', [$organizationId]);
        $bankTransactions = $this->query(
            'SELECT t.*, b.name AS account_name FROM bank_transactions t JOIN bank_accounts b ON b.id = t.bank_account_id
             WHERE t.organization_id = ? ORDER BY t.booking_date DESC, t.id DESC LIMIT 500', [$organizationId]
        );
        $links = $this->query(
            'SELECT l.*, t.booking_date, t.description AS transaction_description, p.payment_date, p.reference_number
             FROM reconciliation_links l JOIN bank_transactions t ON t.id = l.bank_transaction_id
             JOIN payments p ON p.id = l.payment_id WHERE l.organization_id = ? ORDER BY l.matched_at DESC LIMIT 300', [$organizationId]
        );
        $withholdings = $this->query(
            'SELECT w.*, s.business_name AS supplier_name FROM withholding_records w LEFT JOIN suppliers s ON s.id = w.supplier_id
             WHERE w.organization_id = ? ORDER BY w.record_date DESC, w.id DESC LIMIT 300', [$organizationId]
        );
        $accounts = $this->query(
            "SELECT id, code, name FROM chart_of_accounts WHERE organization_id = ? AND active = 1 AND is_postable = 1
             AND account_type = 'ASSET' ORDER BY code", [$organizationId]
        );
        $this->view->render('accounting/treasury', compact(
            'direction', 'openItems', 'payments', 'bankAccounts', 'bankTransactions', 'links', 'withholdings', 'accounts'
        ) + ['title' => 'Tesoreria e partite']);
    }

    public function sync(): never { $this->perform(fn (): int => $this->receivables()->syncDocuments(), 'Partite sincronizzate.', 'SYNC', 'accounting_open_items'); }
    public function payment(): never { $this->perform(fn (): int => $this->receivables()->recordPayment($_POST), 'Pagamento registrato e contabilizzato.', 'CREATE', 'payments'); }
    public function reversePayment(string $id): never { $this->perform(fn (): int => $this->receivables()->reversePayment((int) $id, (string) ($_POST['reversal_date'] ?? '')), 'Pagamento stornato.', 'REVERSE', 'payments'); }
    public function bankAccount(): never { $this->perform(fn (): int => $this->banking()->saveAccount($_POST, !empty($_POST['id']) ? (int) $_POST['id'] : null), 'Conto bancario salvato.', 'SAVE', 'bank_accounts'); }
    public function bankTransaction(): never { $this->perform(fn (): int => $this->banking()->saveTransaction($_POST), 'Movimento bancario acquisito.', 'CREATE', 'bank_transactions'); }
    public function reconcile(): never { $this->perform(function (): int { $this->banking()->reconcile((int) ($_POST['transaction_id'] ?? 0), (int) ($_POST['payment_id'] ?? 0), (float) str_replace(',', '.', (string) ($_POST['amount'] ?? 0))); return (int) ($_POST['transaction_id'] ?? 0); }, 'Riconciliazione salvata.', 'RECONCILE', 'bank_transactions'); }
    public function unreconcile(string $id): never { $this->perform(function () use ($id): int { $this->banking()->unreconcile((int) $id); return (int) $id; }, 'Riconciliazione annullata.', 'UNRECONCILE', 'reconciliation_links'); }
    public function withholding(): never { $this->perform(fn (): int => $this->withholdingService()->record($_POST), 'Ritenuta registrata e contabilizzata.', 'CREATE', 'withholding_records'); }
    public function payWithholding(string $id): never { $this->perform(fn (): int => $this->withholdingService()->markPaid((int) $id, (string) ($_POST['payment_date'] ?? ''), !empty($_POST['bank_account_id']) ? (int) $_POST['bank_account_id'] : null), 'Versamento ritenuta contabilizzato.', 'PAY', 'withholding_records'); }

    private function perform(callable $operation, string $success, string $auditAction, string $entity): never
    {
        $this->authorize();
        try {
            $id = (int) $operation();
            $this->audit($auditAction, $entity, $id);
            $this->redirect('/accounting/treasury', $success);
        } catch (InvalidArgumentException|RuntimeException $exception) {
            $this->redirect('/accounting/treasury', $exception->getMessage(), 'error');
        }
    }

    private function authorize(): void { $this->requireFeature('accounting'); $this->requireRoles(self::ROLES); }
    private function receivables(): ReceivablesService { return new ReceivablesService($this->db, Auth::organizationId(), Auth::id()); }
    private function banking(): BankingService { return new BankingService($this->db, Auth::organizationId(), Auth::id()); }
    private function withholdingService(): WithholdingService { return new WithholdingService($this->db, Auth::organizationId(), Auth::id()); }
    private function query(string $sql, array $params): array { $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetchAll(); }
}
