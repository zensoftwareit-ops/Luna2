<?php

declare(strict_types=1);

$text = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'text', 'required' => $required];
$date = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'date', 'required' => $required];
$decimal = static fn (string $label, bool $required = false): array => ['label' => $label, 'type' => 'decimal', 'required' => $required];
$select = static fn (string $label, array $options, bool $required = false): array => ['label' => $label, 'type' => 'select', 'options' => $options, 'required' => $required];

return [
    'customers' => [
        'feature' => 'anagraphics', 'group' => 'Anagrafiche', 'title' => 'Clienti', 'singular' => 'Cliente', 'table' => 'customers',
        'title_column' => 'business_name', 'search' => ['code', 'business_name', 'vat_number', 'tax_code', 'email'],
        'columns' => ['code', 'business_name', 'vat_number', 'tax_code', 'city', 'email', 'active'],
        'fields' => [
            'code' => $text('Codice'), 'business_name' => $text('Ragione sociale', true),
            'vat_number' => $text('Partita IVA'), 'tax_code' => $text('Codice fiscale'),
            'sdi_code' => $text('Codice destinatario'), 'pec' => ['label' => 'PEC', 'type' => 'email'],
            'email' => ['label' => 'Email', 'type' => 'email'], 'phone' => $text('Telefono'),
            'address' => $text('Indirizzo'), 'postal_code' => $text('CAP'), 'city' => $text('Città'),
            'province' => $text('Provincia'), 'country_code' => $text('Paese'), 'iban' => $text('IBAN'),
            'payment_terms' => $text('Condizioni di pagamento'), 'credit_limit' => $decimal('Fido'),
            'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'suppliers' => [
        'feature' => 'anagraphics', 'group' => 'Anagrafiche', 'title' => 'Fornitori', 'singular' => 'Fornitore', 'table' => 'suppliers',
        'title_column' => 'business_name', 'search' => ['code', 'business_name', 'vat_number', 'tax_code', 'email'],
        'columns' => ['code', 'business_name', 'vat_number', 'city', 'email', 'active'],
        'fields' => [
            'code' => $text('Codice'), 'business_name' => $text('Ragione sociale', true),
            'vat_number' => $text('Partita IVA'), 'tax_code' => $text('Codice fiscale'),
            'email' => ['label' => 'Email', 'type' => 'email'], 'pec' => ['label' => 'PEC', 'type' => 'email'],
            'phone' => $text('Telefono'), 'address' => $text('Indirizzo'), 'postal_code' => $text('CAP'),
            'city' => $text('Città'), 'province' => $text('Provincia'), 'country_code' => $text('Paese'),
            'iban' => $text('IBAN'), 'payment_terms' => $text('Condizioni di pagamento'),
            'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'products' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Prodotti e servizi', 'singular' => 'Prodotto', 'table' => 'products',
        'title_column' => 'name', 'search' => ['code', 'sku', 'ean', 'name', 'category'],
        'columns' => ['code', 'name', 'category', 'unit', 'sale_price', 'purchase_cost', 'active'],
        'fields' => [
            'code' => $text('Codice', true), 'sku' => $text('SKU'), 'ean' => $text('EAN/barcode'),
            'name' => $text('Nome', true), 'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
            'category' => $text('Categoria'), 'product_type' => $select('Tipo', ['PRODUCT' => 'Prodotto', 'SERVICE' => 'Servizio'], true),
            'unit' => $text('Unità di misura', true), 'sale_price' => $decimal('Prezzo vendita'),
            'purchase_cost' => $decimal('Costo acquisto'), 'vat_rate' => $decimal('IVA %'),
            'track_inventory' => ['label' => 'Gestione giacenza', 'type' => 'checkbox'],
            'minimum_stock' => $decimal('Scorta minima'), 'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'leads' => [
        'feature' => 'crm', 'group' => 'CRM', 'title' => 'Lead', 'singular' => 'Lead', 'table' => 'leads',
        'title_column' => 'company_name', 'search' => ['company_name', 'contact_name', 'email', 'phone', 'source'],
        'columns' => ['company_name', 'contact_name', 'status', 'estimated_value', 'next_action_at', 'owner_name'],
        'fields' => [
            'company_name' => $text('Azienda', true), 'contact_name' => $text('Referente'),
            'email' => ['label' => 'Email', 'type' => 'email'], 'phone' => $text('Telefono'), 'source' => $text('Fonte'),
            'status' => $select('Stato', ['NEW' => 'Nuovo', 'CONTACTED' => 'Contattato', 'QUALIFIED' => 'Qualificato', 'PROPOSAL' => 'Proposta', 'NEGOTIATION' => 'Negoziazione', 'WON' => 'Vinto', 'LOST' => 'Perso'], true),
            'estimated_value' => $decimal('Valore stimato'), 'probability' => $decimal('Probabilità %'),
            'next_action_at' => ['label' => 'Prossima azione', 'type' => 'datetime-local'], 'owner_name' => $text('Responsabile'),
            'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'activities' => [
        'feature' => 'crm', 'group' => 'CRM', 'title' => 'Attività CRM', 'singular' => 'Attività', 'table' => 'activities',
        'title_column' => 'subject', 'search' => ['subject', 'activity_type', 'status', 'assigned_to'],
        'columns' => ['subject', 'activity_type', 'status', 'starts_at', 'due_at', 'assigned_to'],
        'fields' => [
            'subject' => $text('Oggetto', true), 'activity_type' => $select('Tipo', ['CALL' => 'Chiamata', 'EMAIL' => 'Email', 'MEETING' => 'Riunione', 'TASK' => 'Attività'], true),
            'status' => $select('Stato', ['OPEN' => 'Aperta', 'IN_PROGRESS' => 'In corso', 'DONE' => 'Completata', 'CANCELLED' => 'Annullata'], true),
            'starts_at' => ['label' => 'Inizio', 'type' => 'datetime-local'], 'due_at' => ['label' => 'Scadenza', 'type' => 'datetime-local'],
            'assigned_to' => $text('Assegnata a'), 'related_type' => $text('Tipo collegamento'), 'related_id' => ['label' => 'ID collegato', 'type' => 'number'],
            'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
    'warehouses' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Magazzini', 'singular' => 'Magazzino', 'table' => 'warehouses',
        'title_column' => 'name', 'search' => ['code', 'name', 'address'], 'columns' => ['code', 'name', 'address', 'active'],
        'fields' => ['code' => $text('Codice', true), 'name' => $text('Nome', true), 'address' => $text('Indirizzo'), 'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1]],
    ],
    'inventory-movements' => [
        'feature' => 'inventory', 'group' => 'Vendite e magazzino', 'title' => 'Movimenti magazzino', 'singular' => 'Movimento', 'table' => 'inventory_movements',
        'title_column' => 'reason', 'search' => ['product_code', 'movement_type', 'reason', 'document_number'],
        'columns' => ['movement_date', 'product_code', 'movement_type', 'quantity', 'unit_cost', 'warehouse_code', 'document_number'],
        'fields' => [
            'movement_date' => $date('Data', true), 'product_id' => ['label' => 'ID prodotto', 'type' => 'number', 'required' => true],
            'product_code' => $text('Codice prodotto'), 'warehouse_id' => ['label' => 'ID magazzino', 'type' => 'number', 'required' => true],
            'warehouse_code' => $text('Codice magazzino'), 'movement_type' => $select('Tipo', ['IN' => 'Carico', 'OUT' => 'Scarico', 'TRANSFER_IN' => 'Trasferimento in', 'TRANSFER_OUT' => 'Trasferimento out', 'ADJUSTMENT' => 'Rettifica'], true),
            'quantity' => $decimal('Quantità', true), 'unit_cost' => $decimal('Costo unitario'), 'reason' => $text('Causale', true),
            'document_type' => $text('Tipo documento'), 'document_number' => $text('Numero documento'),
        ],
    ],
    'projects' => [
        'feature' => 'projects', 'group' => 'Operatività', 'title' => 'Commesse', 'singular' => 'Commessa', 'table' => 'projects',
        'title_column' => 'name', 'search' => ['code', 'name', 'customer_name', 'status'],
        'columns' => ['code', 'name', 'customer_name', 'status', 'start_date', 'end_date', 'budget'],
        'fields' => [
            'code' => $text('Codice', true), 'name' => $text('Nome', true), 'customer_id' => ['label' => 'ID cliente', 'type' => 'number'],
            'customer_name' => $text('Cliente'), 'status' => $select('Stato', ['DRAFT' => 'Bozza', 'ACTIVE' => 'Attiva', 'SUSPENDED' => 'Sospesa', 'COMPLETED' => 'Completata', 'CANCELLED' => 'Annullata'], true),
            'start_date' => $date('Data inizio'), 'end_date' => $date('Data fine'), 'budget' => $decimal('Budget'),
            'progress_percent' => $decimal('Avanzamento %'), 'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
    'tax-deadlines' => [
        'feature' => 'accounting', 'group' => 'Contabilità', 'title' => 'Scadenzario fiscale', 'singular' => 'Scadenza', 'table' => 'tax_deadlines',
        'title_column' => 'description', 'search' => ['deadline_type', 'description', 'status', 'reference_period'],
        'columns' => ['due_date', 'deadline_type', 'description', 'reference_period', 'amount', 'status'],
        'fields' => [
            'due_date' => $date('Scadenza', true), 'deadline_type' => $select('Tipo', ['VAT' => 'IVA', 'F24' => 'F24', 'INPS' => 'INPS', 'WITHHOLDING' => 'Ritenute', 'LIPE' => 'LIPE', 'CU' => 'CU', 'MODEL_770' => '770', 'INCOME_TAX' => 'Redditi', 'OTHER' => 'Altro'], true),
            'description' => $text('Descrizione', true), 'reference_period' => $text('Periodo'), 'amount' => $decimal('Importo'),
            'status' => $select('Stato', ['OPEN' => 'Aperta', 'IN_PROGRESS' => 'In corso', 'COMPLETED' => 'Completata', 'OVERDUE' => 'Scaduta'], true),
            'completed_at' => ['label' => 'Completata il', 'type' => 'datetime-local'], 'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'fixed-assets' => [
        'feature' => 'accounting', 'group' => 'Contabilità', 'title' => 'Cespiti', 'singular' => 'Cespite', 'table' => 'fixed_assets',
        'title_column' => 'description', 'search' => ['asset_code', 'description', 'category'],
        'columns' => ['asset_code', 'description', 'purchase_date', 'purchase_cost', 'depreciation_rate', 'net_book_value', 'status'],
        'fields' => [
            'asset_code' => $text('Codice', true), 'description' => $text('Descrizione', true), 'category' => $text('Categoria'),
            'purchase_date' => $date('Data acquisto', true), 'purchase_cost' => $decimal('Costo storico', true),
            'depreciation_rate' => $decimal('Aliquota ammortamento %', true), 'accumulated_depreciation' => $decimal('Fondo ammortamento'),
            'net_book_value' => $decimal('Valore netto'), 'status' => $select('Stato', ['ACTIVE' => 'Attivo', 'DISPOSED' => 'Dismesso', 'SOLD' => 'Venduto'], true),
        ],
    ],
    'calendar-events' => [
        'feature' => 'calendar', 'group' => 'Operatività', 'title' => 'Calendario', 'singular' => 'Evento', 'table' => 'calendar_events',
        'title_column' => 'title', 'search' => ['title', 'location', 'provider'],
        'columns' => ['title', 'starts_at', 'ends_at', 'location', 'provider', 'sync_status'],
        'fields' => [
            'title' => $text('Titolo', true), 'starts_at' => ['label' => 'Inizio', 'type' => 'datetime-local', 'required' => true],
            'ends_at' => ['label' => 'Fine', 'type' => 'datetime-local'], 'all_day' => ['label' => 'Tutto il giorno', 'type' => 'checkbox'],
            'location' => $text('Luogo'), 'provider' => $select('Calendario', ['LOCAL' => 'Luna2', 'GOOGLE' => 'Google', 'ICLOUD' => 'iCloud', 'CALDAV' => 'CalDAV'], true),
            'sync_status' => $select('Sincronizzazione', ['LOCAL' => 'Locale', 'PENDING' => 'Da sincronizzare', 'SYNCED' => 'Sincronizzato', 'ERROR' => 'Errore'], true),
            'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
    'calendar-accounts' => [
        'feature' => 'calendar', 'group' => 'Operatività', 'title' => 'Account calendario', 'singular' => 'Account calendario', 'table' => 'calendar_accounts',
        'title_column' => 'account_email', 'search' => ['provider', 'account_email', 'endpoint_url'], 'columns' => ['user_id', 'provider', 'account_email', 'endpoint_url', 'auth_type', 'active', 'last_sync_at'],
        'fields' => [
            'user_id' => ['label' => 'ID utente', 'type' => 'number', 'required' => true], 'provider' => $select('Provider', ['GOOGLE'=>'Google Calendar','ICLOUD'=>'iCloud','CALDAV'=>'CalDAV'], true),
            'account_email' => ['label'=>'Account email','type'=>'email'], 'endpoint_url' => ['label'=>'Endpoint HTTPS (CalDAV/iCloud)','type'=>'url'],
            'auth_type' => $select('Autenticazione', ['OAUTH2'=>'OAuth 2.0','BASIC'=>'Basic','APP_PASSWORD'=>'Password specifica app','BEARER'=>'Bearer'], true),
            'secret_reference' => $text('Riferimento segreto ENV', true), 'external_calendar_id' => $text('ID calendario esterno'), 'active' => ['label'=>'Attivo','type'=>'checkbox','default'=>1],
        ],
    ],
    'time-records' => [
        'feature' => 'hr', 'group' => 'HR', 'title' => 'Presenze', 'singular' => 'Presenza', 'table' => 'time_records',
        'title_column' => 'employee_name', 'search' => ['employee_name', 'record_type', 'notes'],
        'columns' => ['work_date', 'employee_name', 'record_type', 'check_in', 'check_out', 'hours', 'approved'],
        'fields' => [
            'work_date' => $date('Data', true), 'employee_name' => $text('Dipendente', true),
            'record_type' => $select('Tipo', ['WORK' => 'Lavoro', 'HOLIDAY' => 'Ferie', 'SICK' => 'Malattia', 'LEAVE' => 'Permesso', 'OTHER' => 'Altro'], true),
            'check_in' => ['label' => 'Entrata', 'type' => 'time'], 'check_out' => ['label' => 'Uscita', 'type' => 'time'],
            'hours' => $decimal('Ore'), 'overtime_hours' => $decimal('Straordinario'), 'approved' => ['label' => 'Approvata', 'type' => 'checkbox'],
            'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'payroll-runs' => [
        'feature' => 'hr', 'group' => 'HR', 'title' => 'Elaborazioni paghe', 'singular' => 'Elaborazione', 'table' => 'payroll_runs',
        'title_column' => 'period_label', 'search' => ['period_label', 'status'],
        'columns' => ['period_label', 'period_start', 'period_end', 'employees_count', 'gross_total', 'net_total', 'status'],
        'fields' => [
            'period_label' => $text('Periodo', true), 'period_start' => $date('Dal', true), 'period_end' => $date('Al', true),
            'employees_count' => ['label' => 'Dipendenti', 'type' => 'number'], 'gross_total' => $decimal('Lordo totale'), 'net_total' => $decimal('Netto totale'),
            'contributions_total' => $decimal('Contributi'), 'tax_total' => $decimal('Imposte'),
            'status' => $select('Stato', ['DRAFT' => 'Bozza', 'CALCULATED' => 'Calcolata', 'CONFIRMED' => 'Confermata', 'PAID' => 'Pagata'], true),
        ],
    ],
    'payroll-configs' => [
        'feature' => 'hr', 'group' => 'HR', 'title' => 'Configurazioni dipendenti', 'singular' => 'Configurazione dipendente', 'table' => 'payroll_employee_configs',
        'title_column' => 'employee_code', 'search' => ['employee_code'], 'columns' => ['user_id','employee_code','hourly_rate','fixed_monthly_amount','inps_rate','tax_rate','valid_from','valid_to','active'],
        'fields' => [
            'user_id'=>['label'=>'ID utente','type'=>'number','required'=>true], 'employee_code'=>$text('Codice dipendente',true), 'standard_weekly_hours'=>$decimal('Ore settimanali'),
            'hourly_rate'=>$decimal('Tariffa oraria'), 'fixed_monthly_amount'=>$decimal('Lordo mensile fisso'), 'overtime_multiplier'=>$decimal('Moltiplicatore straordinari'),
            'inps_rate'=>$decimal('Contributi dipendente %'), 'inail_rate'=>$decimal('INAIL %'), 'employer_contribution_rate'=>$decimal('Contributi datore %'), 'tax_rate'=>$decimal('Ritenuta fiscale gestionale %'),
            'valid_from'=>$date('Valida dal',true), 'valid_to'=>$date('Valida al'), 'active'=>['label'=>'Attiva','type'=>'checkbox','default'=>1],
        ],
    ],
    'leave-balances' => [
        'feature'=>'hr','group'=>'HR','title'=>'Saldi ferie e permessi','singular'=>'Saldo','table'=>'leave_balances','title_column'=>'leave_type','search'=>['leave_type'],'columns'=>['user_id','balance_year','leave_type','opening_hours','accrued_hours','used_hours','adjusted_hours'],
        'fields'=>['user_id'=>['label'=>'ID utente','type'=>'number','required'=>true],'balance_year'=>['label'=>'Anno','type'=>'number','required'=>true],'leave_type'=>$select('Tipo',['HOLIDAY'=>'Ferie','PERMIT'=>'Permessi','ROL'=>'ROL','OTHER'=>'Altro'],true),'opening_hours'=>$decimal('Saldo iniziale'),'accrued_hours'=>$decimal('Maturate'),'used_hours'=>$decimal('Usate'),'adjusted_hours'=>$decimal('Rettifiche')],
    ],
    'ecommerce-channels' => [
        'feature' => 'ecommerce', 'group' => 'Integrazioni', 'title' => 'Canali e-commerce', 'singular' => 'Canale', 'table' => 'ecommerce_channels',
        'title_column' => 'name', 'search' => ['name', 'platform', 'status'], 'columns' => ['name', 'platform', 'status', 'last_sync_at', 'active'],
        'fields' => [
            'name' => $text('Nome', true), 'platform' => $select('Piattaforma', ['WOOCOMMERCE' => 'WooCommerce', 'SHOPIFY' => 'Shopify', 'AMAZON' => 'Amazon', 'EBAY' => 'eBay'], true),
            'base_url' => ['label' => 'URL', 'type' => 'url'], 'secret_reference' => $text('Riferimento credenziali ENV', true), 'webhook_secret_reference' => $text('Riferimento firma webhook ENV'),
            'settings_json' => ['label'=>'Impostazioni adapter JSON','type'=>'textarea'], 'status' => $select('Stato', ['NEW' => 'Nuovo', 'CONNECTED' => 'Connesso', 'ERROR' => 'Errore', 'DISABLED' => 'Disabilitato'], true),
            'active' => ['label' => 'Attivo', 'type' => 'checkbox', 'default' => 1],
        ],
    ],
    'ecommerce-orders' => [
        'feature' => 'ecommerce', 'group' => 'Integrazioni', 'title' => 'Ordini e-commerce', 'singular' => 'Ordine e-commerce', 'table' => 'ecommerce_orders',
        'title_column' => 'external_order_id', 'search' => ['platform', 'external_order_id', 'customer_email', 'status'],
        'columns' => ['order_date', 'platform', 'external_order_id', 'customer_email', 'total', 'currency', 'status', 'import_status'],
        'fields' => [
            'order_date' => $date('Data', true), 'platform' => $text('Piattaforma', true), 'external_order_id' => $text('ID esterno', true),
            'customer_email' => ['label' => 'Email cliente', 'type' => 'email'], 'total' => $decimal('Totale'), 'currency' => $text('Valuta'),
            'status' => $text('Stato esterno'), 'import_status' => $select('Importazione', ['PENDING' => 'Da importare', 'IMPORTED' => 'Importato', 'IGNORED' => 'Ignorato', 'ERROR' => 'Errore'], true),
        ],
    ],
    'rental-contracts' => [
        'feature' => 'rental', 'group' => 'Noleggio', 'title' => 'Contratti noleggio', 'singular' => 'Contratto', 'table' => 'rental_contracts',
        'title_column' => 'contract_number', 'search' => ['contract_number', 'customer_name', 'vehicle_plate', 'status'],
        'columns' => ['contract_number', 'customer_name', 'vehicle_plate', 'start_date', 'end_date', 'monthly_fee', 'status'],
        'fields' => [
            'contract_number' => $text('Numero contratto', true), 'customer_name' => $text('Cliente', true), 'vehicle_plate' => $text('Targa'),
            'start_date' => $date('Inizio', true), 'end_date' => $date('Fine', true), 'monthly_fee' => $decimal('Canone mensile'), 'invoice_day'=>['label'=>'Giorno fatturazione','type'=>'number'], 'next_invoice_date'=>$date('Prossima fattura'),
            'annual_km' => ['label' => 'Km annui', 'type' => 'number'], 'included_km'=>['label'=>'Km inclusi','type'=>'number'], 'current_km'=>['label'=>'Km attuali','type'=>'number'], 'renewal_notice_days'=>['label'=>'Preavviso rinnovo giorni','type'=>'number'], 'status' => $select('Stato', ['DRAFT' => 'Bozza', 'ACTIVE' => 'Attivo', 'EXPIRING' => 'In scadenza', 'CLOSED' => 'Chiuso', 'CANCELLED' => 'Annullato'], true),
            'notes' => ['label' => 'Note', 'type' => 'textarea'],
        ],
    ],
    'rental-tickets' => [
        'feature' => 'rental', 'group' => 'Noleggio', 'title' => 'Ticket noleggio', 'singular' => 'Ticket', 'table' => 'rental_tickets',
        'title_column' => 'subject', 'search' => ['ticket_number', 'subject', 'customer_name', 'status', 'priority'],
        'columns' => ['ticket_number', 'opened_at', 'customer_name', 'subject', 'priority', 'status', 'sla_due_at'],
        'fields' => [
            'ticket_number' => $text('Numero', true), 'opened_at' => ['label' => 'Aperto il', 'type' => 'datetime-local', 'required' => true],
            'customer_name' => $text('Cliente', true), 'subject' => $text('Oggetto', true),
            'priority' => $select('Priorità', ['LOW' => 'Bassa', 'MEDIUM' => 'Media', 'HIGH' => 'Alta', 'CRITICAL' => 'Critica'], true),
            'status' => $select('Stato', ['OPEN' => 'Aperto', 'IN_PROGRESS' => 'In lavorazione', 'WAITING' => 'In attesa', 'RESOLVED' => 'Risolto', 'CLOSED' => 'Chiuso'], true),
            'sla_due_at' => ['label' => 'SLA entro', 'type' => 'datetime-local'], 'description' => ['label' => 'Descrizione', 'type' => 'textarea'],
        ],
    ],
];
