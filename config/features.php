<?php

declare(strict_types=1);

return [
    'anagraphics' => [
        'label' => 'Anagrafiche',
        'description' => 'Clienti, fornitori e contatti aziendali.',
        'icon' => 'users',
        'required_tables' => ['customers', 'suppliers'],
        'default' => true,
    ],
    'sales' => [
        'label' => 'Vendite',
        'description' => 'Preventivi, ordini, DDT, proforma e fatturazione attiva.',
        'icon' => 'receipt',
        'required_tables' => ['documents', 'document_lines', 'document_sequences'],
        'default' => true,
    ],
    'purchases' => [
        'label' => 'Acquisti',
        'description' => 'Ordini fornitori e fatture passive.',
        'icon' => 'bag',
        'required_tables' => ['suppliers', 'documents', 'document_lines'],
        'default' => true,
    ],
    'inventory' => [
        'label' => 'Prodotti e magazzino',
        'description' => 'Catalogo, giacenze, magazzini e movimenti.',
        'icon' => 'box',
        'required_tables' => ['products', 'warehouses', 'inventory_movements', 'inventory_balances'],
        'default' => true,
    ],
    'crm' => [
        'label' => 'CRM',
        'description' => 'Lead, opportunità e attività commerciali.',
        'icon' => 'target',
        'required_tables' => ['leads', 'activities', 'pipeline_stages'],
        'default' => true,
    ],
    'projects' => [
        'label' => 'Commesse',
        'description' => 'Progetti, avanzamento, budget e consuntivi.',
        'icon' => 'briefcase',
        'required_tables' => ['projects', 'project_lines'],
        'default' => true,
    ],
    'accounting' => [
        'label' => 'Contabilità',
        'description' => 'Prima nota, mastri, registri e liquidazioni IVA, bilanci, scadenze e cespiti.',
        'icon' => 'calculator',
        'required_tables' => [
            'chart_of_accounts', 'journal_entries', 'journal_entry_lines', 'accounting_settings', 'accounting_causes',
            'accounting_account_mappings', 'vat_registers', 'vat_movements', 'vat_settlements', 'vat_settlement_details',
            'accounting_open_items', 'payment_allocations', 'vat_cash_events', 'bank_accounts', 'bank_transactions',
            'reconciliation_links', 'vat_adjustments', 'lipe_communications', 'vat_annual_summaries',
            'accounting_period_locks', 'accounting_closing_runs', 'accounting_adjustment_schedules',
            'tax_deadlines', 'fixed_assets', 'fixed_asset_categories', 'depreciation_entries',
            'withholding_records', 'api_endpoint_configs',
        ],
        'default' => true,
    ],
    'calendar' => [
        'label' => 'Calendario',
        'description' => 'Agenda operativa ed eventi sincronizzati.',
        'icon' => 'calendar',
        'required_tables' => ['calendar_events'],
        'default' => true,
    ],
    'hr' => [
        'label' => 'Personale',
        'description' => 'Presenze, approvazioni ed elaborazioni paghe.',
        'icon' => 'id-card',
        'required_tables' => ['time_records', 'payroll_runs'],
        'default' => true,
    ],
    'ecommerce' => [
        'label' => 'E-commerce',
        'description' => 'Canali, ordini e sincronizzazioni marketplace.',
        'icon' => 'store',
        'required_tables' => ['ecommerce_channels', 'ecommerce_orders'],
        'default' => false,
    ],
    'rental' => [
        'label' => 'Noleggio',
        'description' => 'Contratti, scadenze e ticket di assistenza.',
        'icon' => 'car',
        'required_tables' => ['rental_contracts', 'rental_tickets'],
        'default' => false,
    ],
    'imports' => [
        'label' => 'Importazioni',
        'description' => 'Migrazione DATEV Koinos e importazioni massive.',
        'icon' => 'upload',
        'required_tables' => ['import_batches', 'import_files', 'import_rows', 'import_records'],
        'default' => true,
    ],
];
