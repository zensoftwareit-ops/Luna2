<?php

declare(strict_types=1);

namespace Luna\Core;

use Luna\Service\WorkspaceService;
use PDO;
use RuntimeException;

final class View
{
    private ?ModuleManager $moduleManager = null;
    private ?array $workspaceSummary = null;

    public function __construct(
        private readonly string $basePath,
        private readonly array $config,
        private readonly PDO $db,
    )
    {
    }

    public function render(string $template, array $data = [], int $status = 200): never
    {
        $file = $this->basePath . '/views/' . $template . '.php';
        if (!is_file($file)) {
            throw new RuntimeException("Vista non trovata: {$template}");
        }

        http_response_code($status);
        extract($data, EXTR_SKIP);
        $view = $this;
        $config = $this->config;

        ob_start();
        require $file;
        $content = (string) ob_get_clean();

        if (($data['layout'] ?? true) === false) {
            echo $content;
            exit;
        }

        require $this->basePath . '/views/layout.php';
        exit;
    }

    public static function e(mixed $value): string
    {
        return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    }

    public static function money(mixed $value): string
    {
        return number_format((float) $value, 2, ',', '.') . ' €';
    }

    /** Presentation only: stored codes and submitted form values remain unchanged. */
    public static function label(mixed $value): string
    {
        $code = (string) $value;
        return [
            'DRAFT' => 'Bozza', 'NEW' => 'Nuovo', 'OPEN' => 'Aperto',
            'ACTIVE' => 'Attivo', 'INACTIVE' => 'Inattivo', 'DISABLED' => 'Disattivato',
            'ENABLED' => 'Attivato', 'PENDING' => 'In attesa', 'QUEUED' => 'In coda',
            'PROCESSING' => 'In elaborazione', 'RUNNING' => 'In corso',
            'COMPLETED' => 'Completato', 'COMPLETED_WITH_ERRORS' => 'Completato con errori',
            'DONE' => 'Completato', 'ERROR' => 'Errore', 'FAILED' => 'Non riuscito',
            'RETRY' => 'Nuovo tentativo previsto', 'SUCCESS' => 'Completato',
            'READY' => 'Pronto', 'REVIEW' => 'Da verificare', 'VALID' => 'Valido',
            'INVALID' => 'Da correggere', 'VALIDATED' => 'Validato', 'LOCKED' => 'Bloccato',
            'GENERATED' => 'Generato', 'IMPORTED' => 'Importato', 'ROLLED_BACK' => 'Annullato',
            'SUBMITTED' => 'Presentato', 'APPROVED' => 'Approvato', 'REJECTED' => 'Rifiutato',
            'ACCEPTED' => 'Accettato', 'CANCELLED' => 'Annullato', 'CANCELED' => 'Annullato',
            'CONFIRMED' => 'Confermato', 'CLOSED' => 'Chiuso', 'SENT' => 'Inviato',
            'DELIVERED' => 'Consegnato', 'RECEIVED' => 'Ricevuto', 'ISSUED' => 'Emesso', 'HISTORICAL' => 'Storico importato',
            'PAID' => 'Pagato', 'PARTIAL' => 'Parziale', 'OVERDUE' => 'Scaduto',
            'POSTED' => 'Contabilizzato', 'REVERSED' => 'Stornato', 'CALCULATED' => 'Calcolato',
            'CONNECTED' => 'Connesso', 'DISCONNECTED' => 'Disconnesso', 'SYNCED' => 'Sincronizzato',
            'PICKING' => 'In preparazione', 'PICKED' => 'Preparato', 'SHIPPED' => 'Spedito',
            'IN_TRANSIT' => 'In trasferimento', 'RESERVED' => 'Impegnato',
            'HOLIDAY' => 'Ferie', 'PERMIT' => 'Permesso', 'SICK' => 'Malattia', 'OTHER' => 'Altro',
            'IMPORTED_PAYSLIPS' => 'Cedolini importati', 'CONFIGURED_RATES' => 'Simulazione gestionale',
            'PULL_ORDERS' => 'Importa ordini', 'PULL_PRODUCTS' => 'Importa prodotti',
            'PUSH_STOCK' => 'Aggiorna disponibilità', 'PUSH_PRICES' => 'Aggiorna prezzi',
            'ACK_ORDER' => 'Conferma ricezione ordine', 'TEST' => 'Test', 'PRODUCTION' => 'Produzione',
            'customers' => 'Clienti', 'suppliers' => 'Fornitori', 'chart_of_accounts' => 'Piano dei conti',
            'journal_entries' => 'Prima nota', 'payments' => 'Pagamenti', 'open_items' => 'Partite aperte',
            'vat_movements' => 'Movimenti IVA', 'fixed_assets' => 'Cespiti', 'bank_transactions' => 'Movimenti bancari',
            'documents' => 'Documenti', 'products' => 'Prodotti', 'fatturapa' => 'Fatture elettroniche',
            'QUOTE' => 'Preventivo', 'SALES_ORDER' => 'Ordine cliente', 'DELIVERY_NOTE' => 'DDT',
            'SALES_INVOICE' => 'Fattura attiva', 'PURCHASE_INVOICE' => 'Fattura passiva',
            'CREDIT_NOTE' => 'Nota di credito', 'PROFORMA' => 'Proforma',
        ][$code] ?? $code;
    }

    public static function date(mixed $value): string
    {
        if (!$value) {
            return '—';
        }
        $time = strtotime((string) $value);
        return $time ? date('d/m/Y', $time) : self::e($value);
    }

    public function features(): array
    {
        return $this->modules()->all();
    }

    public function featureEnabled(string $key): bool
    {
        return $this->modules()->enabled($key);
    }

    public function workspaceSummary(): array
    {
        if ($this->workspaceSummary !== null) {
            return $this->workspaceSummary;
        }
        if (!Auth::check() || Auth::isSuperuser()) {
            return $this->workspaceSummary = ['unread' => 0, 'notifications' => [], 'readiness' => 100];
        }
        $service = new WorkspaceService($this->db, Auth::organizationId(), Auth::id());
        $onboarding = $service->onboarding();
        return $this->workspaceSummary = [
            'unread' => $service->unreadCount(),
            'notifications' => array_slice($service->notifications(6), 0, 6),
            'readiness' => (int) ($onboarding['percentage'] ?? 0),
        ];
    }

    public static function icon(string $name, string $class = ''): string
    {
        $paths = [
            'home' => '<path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10.5V20h13v-9.5"/>',
            'users' => '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
            'receipt' => '<path d="M6 2h12v20l-3-2-3 2-3-2-3 2Z"/><path d="M9 7h6M9 11h6M9 15h4"/>',
            'bag' => '<path d="M6 8h12l1 13H5L6 8Z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
            'box' => '<path d="m21 8-9 5-9-5 9-5 9 5Z"/><path d="m3 8 9 5 9-5v8l-9 5-9-5V8Z"/><path d="M12 13v8"/>',
            'target' => '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
            'briefcase' => '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 12h18"/>',
            'calculator' => '<rect x="4" y="2" width="16" height="20" rx="2"/><path d="M8 6h8v3H8zM8 13h.01M12 13h.01M16 13h.01M8 17h.01M12 17h.01M16 17h.01"/>',
            'calendar' => '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/>',
            'id-card' => '<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="8" cy="11" r="2"/><path d="M5.5 16a3 3 0 0 1 5 0M13 10h5M13 14h4"/>',
            'store' => '<path d="M4 10v10h16V10M3 4h18l-1 6a3 3 0 0 1-4 1.5A3 3 0 0 1 12 10a3 3 0 0 1-4 1.5A3 3 0 0 1 4 10L3 4Z"/><path d="M9 20v-6h6v6"/>',
            'car' => '<path d="m5 17-1 2v2M19 17l1 2v2M3 13l2-6h14l2 6v5H3v-5Z"/><path d="M6 14h.01M18 14h.01"/>',
            'upload' => '<path d="M12 16V3M7 8l5-5 5 5M4 14v6h16v-6"/>',
            'settings' => '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1v.1h-4v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1-.4h-.1v-4H3a1.7 1.7 0 0 0 1.6-1.1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1v-.1h4V3a1.7 1.7 0 0 0 1.1 1.6 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9c.36.3.57.73.6 1.2v.2h.1v4H20a1.7 1.7 0 0 0-.6.6Z"/>',
            'key' => '<circle cx="8" cy="15" r="4"/><path d="m11 12 9-9M16 7l3 3M14 9l3 3"/>',
            'plus' => '<path d="M12 5v14M5 12h14"/>',
            'search' => '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
            'download' => '<path d="M12 3v13M7 11l5 5 5-5M4 21h16"/>',
            'menu' => '<path d="M4 7h16M4 12h16M4 17h16"/>',
            'chevron' => '<path d="m9 18 6-6-6-6"/>',
            'check' => '<path d="m5 12 4 4L19 6"/>',
            'alert' => '<path d="M10.3 3.7 2.2 18a2 2 0 0 0 1.7 3h16.2a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4M12 17h.01"/>',
            'bell' => '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/>',
            'help' => '<circle cx="12" cy="12" r="9"/><path d="M9.7 9a2.5 2.5 0 1 1 3.8 2.1c-.9.5-1.5 1.1-1.5 2.4M12 17h.01"/>',
            'lock' => '<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/>',
            'bank' => '<path d="M3 10h18M5 10v8M9 10v8M15 10v8M19 10v8M3 18h18M2 22h20M12 2 2 7h20L12 2Z"/>',
            'filter' => '<path d="M4 5h16M7 12h10M10 19h4"/>',
            'globe' => '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18"/>',
            'link' => '<path d="M10 13a5 5 0 0 0 7.1.1l2-2a5 5 0 0 0-7.1-7.1l-1.1 1.1M14 11a5 5 0 0 0-7.1-.1l-2 2A5 5 0 0 0 12 20l1.1-1.1"/>',
        ];
        $path = $paths[$name] ?? $paths['chevron'];
        return '<svg class="icon ' . self::e($class) . '" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' . $path . '</svg>';
    }

    private function modules(): ModuleManager
    {
        return $this->moduleManager ??= new ModuleManager($this->db, $this->config['features'] ?? [], Auth::organizationId());
    }
}
