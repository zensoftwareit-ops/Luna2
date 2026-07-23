<?php

declare(strict_types=1);

namespace Luna\Service;

use PDO;
use Throwable;

final class WorkspaceService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function search(string $query, int $perArea = 6): array
    {
        $query = mb_substr(trim($query), 0, 120);
        $perArea = max(1, min($perArea, 20));
        if (mb_strlen($query) < 2) {
            return [];
        }

        $like = '%' . $query . '%';
        $areas = [
            'customers' => [
                'label' => 'Clienti', 'icon' => 'users',
                'sql' => 'SELECT id, business_name AS title, CONCAT_WS(\' · \', vat_number, email, city) AS subtitle FROM customers WHERE organization_id = ? AND (business_name LIKE ? OR vat_number LIKE ? OR tax_code LIKE ? OR email LIKE ?) ORDER BY business_name LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like, $like],
                'url' => static fn (array $row): string => '/r/customers/' . $row['id'] . '/edit',
            ],
            'suppliers' => [
                'label' => 'Fornitori', 'icon' => 'bag',
                'sql' => 'SELECT id, business_name AS title, CONCAT_WS(\' · \', vat_number, email, city) AS subtitle FROM suppliers WHERE organization_id = ? AND (business_name LIKE ? OR vat_number LIKE ? OR tax_code LIKE ? OR email LIKE ?) ORDER BY business_name LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like, $like],
                'url' => static fn (array $row): string => '/r/suppliers/' . $row['id'] . '/edit',
            ],
            'products' => [
                'label' => 'Prodotti e servizi', 'icon' => 'box',
                'sql' => 'SELECT id, name AS title, CONCAT_WS(\' · \', code, sku, ean) AS subtitle FROM products WHERE organization_id = ? AND (name LIKE ? OR code LIKE ? OR sku LIKE ? OR ean LIKE ?) ORDER BY name LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like, $like],
                'url' => static fn (array $row): string => '/r/products/' . $row['id'] . '/edit',
            ],
            'documents' => [
                'label' => 'Documenti', 'icon' => 'receipt',
                'sql' => 'SELECT id, document_type, number AS title, CONCAT_WS(\' · \', counterparty_name, document_date, total) AS subtitle FROM documents WHERE organization_id = ? AND (number LIKE ? OR counterparty_name LIKE ? OR subject LIKE ?) ORDER BY document_date DESC, id DESC LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like],
                'url' => static fn (array $row): string => '/documents/' . self::documentSlug((string) $row['document_type']) . '/' . $row['id'],
            ],
            'journal' => [
                'label' => 'Prima nota', 'icon' => 'calculator',
                'sql' => 'SELECT id, protocol_number AS title, CONCAT_WS(\' · \', entry_date, description, counterparty) AS subtitle FROM journal_entries WHERE organization_id = ? AND (protocol_number LIKE ? OR document_number LIKE ? OR description LIKE ? OR counterparty LIKE ?) ORDER BY entry_date DESC, id DESC LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like, $like],
                'url' => static fn (array $row): string => '/accounting/journal/' . $row['id'],
            ],
            'projects' => [
                'label' => 'Commesse', 'icon' => 'briefcase',
                'sql' => 'SELECT id, name AS title, CONCAT_WS(\' · \', code, customer_name, status) AS subtitle FROM projects WHERE organization_id = ? AND (name LIKE ? OR code LIKE ? OR customer_name LIKE ?) ORDER BY id DESC LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like],
                'url' => static fn (array $row): string => '/operations/projects?project_id=' . $row['id'],
            ],
            'rental' => [
                'label' => 'Ticket noleggio', 'icon' => 'car',
                'sql' => 'SELECT id, subject AS title, CONCAT_WS(\' · \', ticket_number, customer_name, status) AS subtitle FROM rental_tickets WHERE organization_id = ? AND (ticket_number LIKE ? OR subject LIKE ? OR customer_name LIKE ?) ORDER BY opened_at DESC LIMIT ' . $perArea,
                'params' => [$this->organizationId, $like, $like, $like],
                'url' => static fn (array $row): string => '/operations/rental#ticket-' . $row['id'],
            ],
        ];

        $results = [];
        foreach ($areas as $key => $area) {
            try {
                $statement = $this->db->prepare($area['sql']);
                $statement->execute($area['params']);
                $rows = [];
                foreach ($statement->fetchAll() as $row) {
                    $rows[] = [
                        'title' => (string) $row['title'],
                        'subtitle' => trim((string) ($row['subtitle'] ?? ''), " \t\n\r\0\x0B·"),
                        'url' => $area['url']($row),
                    ];
                }
                if ($rows !== []) {
                    $results[$key] = ['label' => $area['label'], 'icon' => $area['icon'], 'rows' => $rows];
                }
            } catch (Throwable) {
                // La ricerca resta disponibile anche durante una migrazione parziale.
            }
        }
        return $results;
    }

    public function notifications(int $limit = 50): array
    {
        try {
            $statement = $this->db->prepare(
                'SELECT * FROM workspace_notifications
                 WHERE organization_id = ? AND user_id = ?
                   AND (expires_at IS NULL OR expires_at > NOW())
                 ORDER BY read_at IS NULL DESC, created_at DESC LIMIT ' . max(1, min($limit, 100))
            );
            $statement->execute([$this->organizationId, $this->userId]);
            return $statement->fetchAll();
        } catch (Throwable) {
            return [];
        }
    }

    public function unreadCount(): int
    {
        try {
            $statement = $this->db->prepare(
                'SELECT COUNT(*) FROM workspace_notifications
                 WHERE organization_id = ? AND user_id = ? AND read_at IS NULL
                   AND (expires_at IS NULL OR expires_at > NOW())'
            );
            $statement->execute([$this->organizationId, $this->userId]);
            return (int) $statement->fetchColumn();
        } catch (Throwable) {
            return 0;
        }
    }

    public function markRead(?int $notificationId = null): void
    {
        if ($notificationId === null) {
            $statement = $this->db->prepare(
                'UPDATE workspace_notifications SET read_at = COALESCE(read_at, NOW())
                 WHERE organization_id = ? AND user_id = ?'
            );
            $statement->execute([$this->organizationId, $this->userId]);
            return;
        }
        $statement = $this->db->prepare(
            'UPDATE workspace_notifications SET read_at = COALESCE(read_at, NOW())
             WHERE id = ? AND organization_id = ? AND user_id = ?'
        );
        $statement->execute([$notificationId, $this->organizationId, $this->userId]);
    }

    public function refreshOperationalNotifications(): int
    {
        $users = $this->db->prepare('SELECT id FROM users WHERE organization_id = ? AND active = 1');
        $users->execute([$this->organizationId]);
        $userIds = array_map('intval', $users->fetchAll(PDO::FETCH_COLUMN));
        if ($userIds === []) {
            return 0;
        }

        $signals = [
            [
                'key' => 'tax-overdue',
                'count' => $this->count("SELECT COUNT(*) FROM tax_deadlines WHERE organization_id = ? AND status = 'OVERDUE'"),
                'category' => 'SCADENZE', 'severity' => 'DANGER', 'title' => 'Scadenze fiscali da gestire',
                'message' => '%d scadenze fiscali risultano oltre la data prevista.', 'url' => '/r/tax-deadlines',
            ],
            [
                'key' => 'documents-overdue',
                'count' => $this->count("SELECT COUNT(DISTINCT document_id) FROM payment_schedules WHERE organization_id = ? AND status = 'OVERDUE'"),
                'category' => 'TESORERIA', 'severity' => 'WARNING', 'title' => 'Documenti scaduti',
                'message' => '%d documenti richiedono un sollecito o una registrazione di pagamento.', 'url' => '/accounting/treasury',
            ],
            [
                'key' => 'low-stock',
                'count' => $this->count('SELECT COUNT(*) FROM inventory_balances WHERE organization_id = ? AND quantity <= minimum_stock'),
                'category' => 'MAGAZZINO', 'severity' => 'WARNING', 'title' => 'Prodotti sotto scorta',
                'message' => '%d saldi di magazzino sono sotto la soglia minima.', 'url' => '/operations/logistics',
            ],
            [
                'key' => 'mail-failed',
                'count' => $this->count("SELECT COUNT(*) FROM outbound_emails WHERE organization_id = ? AND status = 'FAILED'"),
                'category' => 'COMUNICAZIONI', 'severity' => 'DANGER', 'title' => 'Email non consegnate',
                'message' => '%d email risultano definitivamente fallite.', 'url' => '/operations/communications',
            ],
            [
                'key' => 'commerce-failed',
                'count' => $this->count("SELECT COUNT(*) FROM ecommerce_sync_queue WHERE organization_id = ? AND status = 'FAILED'"),
                'category' => 'ECOMMERCE', 'severity' => 'DANGER', 'title' => 'Sincronizzazioni e-commerce fallite',
                'message' => '%d operazioni richiedono una verifica.', 'url' => '/operations/ecommerce',
            ],
            [
                'key' => 'calendar-errors',
                'count' => $this->count("SELECT COUNT(*) FROM calendar_accounts WHERE organization_id = ? AND active = 1 AND last_error IS NOT NULL"),
                'category' => 'CALENDARIO', 'severity' => 'WARNING', 'title' => 'Calendari da riconnettere',
                'message' => '%d account calendario riportano un errore.', 'url' => '/operations/calendar',
            ],
            [
                'key' => 'sla-breached',
                'count' => $this->count('SELECT COUNT(*) FROM rental_tickets WHERE organization_id = ? AND sla_breached = 1 AND status NOT IN (\'RESOLVED\',\'CLOSED\')'),
                'category' => 'NOLEGGIO', 'severity' => 'DANGER', 'title' => 'SLA superati',
                'message' => '%d ticket hanno superato il tempo di risposta.', 'url' => '/operations/rental',
            ],
        ];

        $upsert = $this->db->prepare(
            'INSERT INTO workspace_notifications
             (organization_id, user_id, category, severity, title, message, action_url, dedupe_key, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE read_at = IF(expires_at IS NOT NULL OR message <> VALUES(message), NULL, read_at),
                 severity = VALUES(severity), title = VALUES(title), message = VALUES(message),
                 action_url = VALUES(action_url), expires_at = NULL, updated_at = NOW()'
        );
        $expire = $this->db->prepare(
            'UPDATE workspace_notifications SET expires_at = NOW()
             WHERE organization_id = ? AND dedupe_key = ? AND expires_at IS NULL'
        );
        $written = 0;
        foreach ($signals as $signal) {
            foreach ($userIds as $userId) {
                $key = $signal['key'] . ':user:' . $userId;
                if ($signal['count'] <= 0) {
                    $expire->execute([$this->organizationId, $key]);
                    continue;
                }
                $upsert->execute([
                    $this->organizationId, $userId, $signal['category'], $signal['severity'],
                    $signal['title'], sprintf($signal['message'], $signal['count']), $signal['url'], $key,
                ]);
                $written++;
            }
        }
        return $written;
    }

    public function savedViews(string $moduleKey): array
    {
        try {
            $statement = $this->db->prepare(
                'SELECT * FROM saved_views WHERE organization_id = ? AND user_id = ? AND module_key = ? ORDER BY is_default DESC, name'
            );
            $statement->execute([$this->organizationId, $this->userId, $moduleKey]);
            return array_map(static function (array $row): array {
                $row['query'] = json_decode((string) $row['query_json'], true) ?: [];
                return $row;
            }, $statement->fetchAll());
        } catch (Throwable) {
            return [];
        }
    }

    public function saveView(string $moduleKey, string $name, array $query, bool $isDefault): void
    {
        $name = mb_substr(trim($name), 0, 100);
        if ($name === '' || !preg_match('/^[a-zA-Z0-9_-]+$/', $moduleKey)) {
            throw new \InvalidArgumentException('Nome o modulo della vista non valido.');
        }
        $allowed = ['q', 'filters', 'per_page', 'sort', 'direction'];
        $query = array_intersect_key($query, array_flip($allowed));
        $this->db->beginTransaction();
        try {
            if ($isDefault) {
                $this->db->prepare(
                    'UPDATE saved_views SET is_default = 0 WHERE organization_id = ? AND user_id = ? AND module_key = ?'
                )->execute([$this->organizationId, $this->userId, $moduleKey]);
            }
            $statement = $this->db->prepare(
                'INSERT INTO saved_views (organization_id, user_id, module_key, name, query_json, is_default)
                 VALUES (?, ?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE query_json = VALUES(query_json), is_default = VALUES(is_default), updated_at = NOW()'
            );
            $statement->execute([
                $this->organizationId, $this->userId, $moduleKey, $name,
                json_encode($query, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE), $isDefault ? 1 : 0,
            ]);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function deleteView(int $id): void
    {
        $statement = $this->db->prepare('DELETE FROM saved_views WHERE id = ? AND organization_id = ? AND user_id = ?');
        $statement->execute([$id, $this->organizationId, $this->userId]);
    }

    public function onboarding(): array
    {
        $manual = [];
        try {
            $statement = $this->db->prepare('SELECT step_key, completed_at, notes FROM onboarding_progress WHERE organization_id = ?');
            $statement->execute([$this->organizationId]);
            foreach ($statement->fetchAll() as $row) {
                $manual[(string) $row['step_key']] = $row;
            }
        } catch (Throwable) {
        }

        $organizationComplete = $this->count(
            "SELECT COUNT(*) FROM organizations WHERE id = ? AND business_name <> '' AND COALESCE(NULLIF(vat_number,''), NULLIF(tax_code,'')) IS NOT NULL AND address IS NOT NULL AND city IS NOT NULL",
            [$this->organizationId],
        ) > 0;
        $steps = [
            'company' => ['title' => 'Profilo aziendale', 'description' => 'Dati fiscali, sede, contatti e coordinate.', 'url' => '/settings/company', 'automatic' => $organizationComplete],
            'modules' => ['title' => 'Moduli e ruoli', 'description' => 'Perimetro funzionale e utenti verificati.', 'url' => '/settings/company', 'automatic' => $this->count('SELECT COUNT(*) FROM module_settings WHERE organization_id = ?') > 0 && $this->count('SELECT COUNT(*) FROM users WHERE organization_id = ? AND active = 1') > 0],
            'accounting' => ['title' => 'Impianto contabile', 'description' => 'Piano dei conti, causali e automatismi.', 'url' => '/accounting/setup', 'automatic' => $this->count('SELECT COUNT(*) FROM chart_of_accounts WHERE organization_id = ?') >= 5],
            'vat' => ['title' => 'Configurazione IVA', 'description' => 'Registri, periodicità e codici IVA.', 'url' => '/accounting/setup', 'automatic' => $this->count('SELECT COUNT(*) FROM vat_registers WHERE organization_id = ? AND active = 1') > 0],
            'banking' => ['title' => 'Tesoreria', 'description' => 'Conti bancari e conto contabile associato.', 'url' => '/accounting/treasury', 'automatic' => $this->count('SELECT COUNT(*) FROM bank_accounts WHERE organization_id = ? AND active = 1') > 0],
            'numbering' => ['title' => 'Numerazioni documentali', 'description' => 'Sequenze di vendita e registri controllate.', 'url' => '/documents/invoices', 'automatic' => $this->count('SELECT COUNT(*) FROM document_sequences WHERE organization_id = ?') > 0],
            'imports' => ['title' => 'Migrazione e quadrature', 'description' => 'Dati storici importati e confrontati con Koinos.', 'url' => '/imports', 'automatic' => $this->count("SELECT COUNT(*) FROM import_batches WHERE organization_id = ? AND status IN ('COMPLETED','COMPLETED_WITH_ERRORS')") > 0],
            'communications' => ['title' => 'Comunicazioni', 'description' => 'Mittente e trasporto email collaudati.', 'url' => '/operations/communications', 'automatic' => $this->count("SELECT COUNT(*) FROM communication_settings WHERE organization_id = ? AND transport <> 'DISABLED'") > 0],
            'integrations' => ['title' => 'Endpoint e integrazioni', 'description' => 'Credenziali esterne configurate e testate.', 'url' => '/settings/endpoints', 'automatic' => $this->count('SELECT COUNT(*) FROM api_endpoint_configs WHERE organization_id = ? AND enabled = 1') > 0],
            'backup' => ['title' => 'Backup e ripristino', 'description' => 'Backup Plesk e prova di restore documentata.', 'url' => '/workspace/onboarding', 'automatic' => false],
            'acceptance' => ['title' => 'Collaudo amministrativo', 'description' => 'Verbale di quadratura e approvazione del consulente.', 'url' => '/professional', 'automatic' => false],
        ];

        $completed = 0;
        foreach ($steps as $key => &$step) {
            $step['key'] = $key;
            $step['manual'] = isset($manual[$key]) && $manual[$key]['completed_at'] !== null;
            $step['completed'] = $step['automatic'] || $step['manual'];
            $step['notes'] = $manual[$key]['notes'] ?? null;
            $completed += $step['completed'] ? 1 : 0;
        }
        unset($step);
        return [
            'steps' => array_values($steps),
            'completed' => $completed,
            'total' => count($steps),
            'percentage' => count($steps) ? (int) round($completed / count($steps) * 100) : 0,
        ];
    }

    public function setOnboardingStep(string $stepKey, bool $completed, ?string $notes): void
    {
        if (!preg_match('/^[a-z0-9_-]{2,100}$/', $stepKey)) {
            throw new \InvalidArgumentException('Passaggio non valido.');
        }
        $statement = $this->db->prepare(
            'INSERT INTO onboarding_progress (organization_id, step_key, completed_at, completed_by, notes)
             VALUES (?, ?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE completed_at = VALUES(completed_at), completed_by = VALUES(completed_by),
                 notes = VALUES(notes), updated_at = NOW()'
        );
        $statement->execute([
            $this->organizationId, $stepKey, $completed ? date('Y-m-d H:i:s') : null,
            $completed ? $this->userId : null, $this->nullable($notes),
        ]);
    }

    public function dataQuality(): array
    {
        $checks = [
            ['label' => 'Scritture contabili quadrate', 'count' => $this->count("SELECT COUNT(*) FROM journal_entries WHERE organization_id = ? AND status = 'POSTED' AND ABS(total_debit - total_credit) > 0.005"), 'url' => '/accounting/journal', 'severity' => 'danger'],
            ['label' => 'Documenti senza controparte', 'count' => $this->count("SELECT COUNT(*) FROM documents WHERE organization_id = ? AND COALESCE(counterparty_name,'') = ''"), 'url' => '/documents/invoices', 'severity' => 'warning'],
            ['label' => 'Partite scadute', 'count' => $this->count("SELECT COUNT(*) FROM accounting_open_items WHERE organization_id = ? AND status = 'OVERDUE'"), 'url' => '/accounting/treasury', 'severity' => 'warning'],
            ['label' => 'Movimenti bancari non riconciliati', 'count' => $this->count("SELECT COUNT(*) FROM bank_transactions WHERE organization_id = ? AND reconciliation_status <> 'MATCHED'"), 'url' => '/accounting/treasury#reconciliation', 'severity' => 'info'],
            ['label' => 'Errori code operative', 'count' => $this->count("SELECT (SELECT COUNT(*) FROM outbound_emails WHERE organization_id = ? AND status = 'FAILED') + (SELECT COUNT(*) FROM ecommerce_sync_queue WHERE organization_id = ? AND status = 'FAILED')", [$this->organizationId, $this->organizationId]), 'url' => '/workspace/notifications', 'severity' => 'danger'],
            ['label' => 'Importazioni con errori', 'count' => $this->count("SELECT COUNT(*) FROM import_batches WHERE organization_id = ? AND status IN ('ERROR','COMPLETED_WITH_ERRORS')"), 'url' => '/imports', 'severity' => 'warning'],
        ];
        foreach ($checks as &$check) {
            $check['ok'] = $check['count'] === 0;
        }
        unset($check);
        return $checks;
    }

    private function count(string $sql, ?array $params = null): int
    {
        try {
            $statement = $this->db->prepare($sql);
            $statement->execute($params ?? [$this->organizationId]);
            return (int) $statement->fetchColumn();
        } catch (Throwable) {
            return 0;
        }
    }

    private function nullable(?string $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : mb_substr($value, 0, 500);
    }

    private static function documentSlug(string $type): string
    {
        return match ($type) {
            'QUOTE' => 'quotes',
            'SALES_ORDER' => 'orders',
            'DDT' => 'ddt',
            'SALES_INVOICE' => 'invoices',
            'CREDIT_NOTE' => 'credit-notes',
            'PROFORMA' => 'proformas',
            'PURCHASE_ORDER' => 'purchase-orders',
            'PURCHASE_INVOICE' => 'purchase-invoices',
            default => 'invoices',
        };
    }
}
