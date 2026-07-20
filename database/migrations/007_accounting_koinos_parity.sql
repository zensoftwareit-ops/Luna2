ALTER TABLE chart_of_accounts
    ADD COLUMN normal_balance ENUM('DEBIT','CREDIT') NOT NULL DEFAULT 'DEBIT' AFTER account_type,
    ADD COLUMN classification_code VARCHAR(50) NULL AFTER system_key,
    ADD COLUMN statement_section VARCHAR(100) NULL AFTER classification_code,
    ADD COLUMN tax_mapping_code VARCHAR(50) NULL AFTER statement_section,
    ADD COLUMN requires_party TINYINT(1) NOT NULL DEFAULT 0 AFTER is_postable,
    ADD COLUMN requires_cost_center TINYINT(1) NOT NULL DEFAULT 0 AFTER requires_party,
    ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 AFTER active,
    ADD KEY idx_accounts_classification (organization_id, classification_code);

UPDATE chart_of_accounts
SET normal_balance = CASE
    WHEN account_type IN ('LIABILITY','EQUITY','REVENUE') THEN 'CREDIT'
    ELSE 'DEBIT'
END;

CREATE TABLE IF NOT EXISTS accounting_settings (
    organization_id BIGINT UNSIGNED PRIMARY KEY,
    accounting_basis ENUM('ACCRUAL','CASH') NOT NULL DEFAULT 'ACCRUAL',
    vat_periodicity ENUM('MONTHLY','QUARTERLY') NOT NULL DEFAULT 'MONTHLY',
    fiscal_year_start_month TINYINT UNSIGNED NOT NULL DEFAULT 1,
    third_party_accounting_lag TINYINT UNSIGNED NOT NULL DEFAULT 0,
    pro_rata_percent DECIMAL(7,4) NOT NULL DEFAULT 100,
    cash_vat_enabled TINYINT(1) NOT NULL DEFAULT 0,
    auto_post_documents TINYINT(1) NOT NULL DEFAULT 1,
    auto_create_open_items TINYINT(1) NOT NULL DEFAULT 1,
    lipe_schema_version VARCHAR(20) NOT NULL DEFAULT '2026-draft',
    vat_return_schema_version VARCHAR(20) NOT NULL DEFAULT 'IVA26-draft',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounting_settings_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_accounting_pro_rata CHECK (pro_rata_percent >= 0 AND pro_rata_percent <= 100),
    CONSTRAINT chk_fiscal_start_month CHECK (fiscal_year_start_month BETWEEN 1 AND 12)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vat_registers (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(190) NOT NULL,
    register_type ENUM('SALES','PURCHASES','CORRISPETTIVI') NOT NULL,
    prefix VARCHAR(32) NOT NULL DEFAULT '',
    suffix VARCHAR(32) NOT NULL DEFAULT '',
    next_protocol BIGINT UNSIGNED NOT NULL DEFAULT 1,
    padding TINYINT UNSIGNED NOT NULL DEFAULT 6,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_vat_register_code (organization_id, code),
    KEY idx_vat_register_type (organization_id, register_type, active),
    CONSTRAINT fk_vat_register_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_causes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(190) NOT NULL,
    category ENUM('GENERAL','SALES','PURCHASE','RECEIPT','PAYMENT','ADJUSTMENT','CLOSING','OPENING','DEPRECIATION','WITHHOLDING') NOT NULL DEFAULT 'GENERAL',
    vat_register_id BIGINT UNSIGNED NULL,
    default_description VARCHAR(500) NULL,
    document_type VARCHAR(50) NULL,
    creates_open_item TINYINT(1) NOT NULL DEFAULT 0,
    automatic TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_accounting_cause (organization_id, code),
    CONSTRAINT fk_accounting_cause_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_accounting_cause_register FOREIGN KEY (vat_register_id) REFERENCES vat_registers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_cause_lines (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    cause_id BIGINT UNSIGNED NOT NULL,
    line_number TINYINT UNSIGNED NOT NULL,
    side ENUM('DEBIT','CREDIT') NOT NULL,
    account_id BIGINT UNSIGNED NULL,
    mapping_key VARCHAR(64) NULL,
    amount_source ENUM('TOTAL','TAXABLE','VAT','GROSS','WITHHOLDING','NET','BALANCE','FIXED') NOT NULL DEFAULT 'BALANCE',
    percentage DECIMAL(7,4) NOT NULL DEFAULT 100,
    fixed_amount DECIMAL(15,2) NULL,
    description VARCHAR(255) NULL,
    UNIQUE KEY uq_cause_line (cause_id, line_number),
    CONSTRAINT fk_cause_line_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_cause_line_cause FOREIGN KEY (cause_id) REFERENCES accounting_causes(id) ON DELETE CASCADE,
    CONSTRAINT fk_cause_line_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_cause_line_account CHECK (account_id IS NOT NULL OR mapping_key IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_account_mappings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    mapping_key VARCHAR(64) NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_account_mapping (organization_id, mapping_key),
    CONSTRAINT fk_account_mapping_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_mapping_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE journal_entries
    ADD COLUMN cause_id BIGINT UNSIGNED NULL AFTER entry_type,
    ADD COLUMN vat_register_id BIGINT UNSIGNED NULL AFTER cause_id,
    ADD COLUMN source_protocol VARCHAR(100) NULL AFTER document_number,
    ADD COLUMN locked_at DATETIME NULL AFTER posted_at,
    ADD KEY idx_journal_cause (organization_id, cause_id),
    ADD CONSTRAINT fk_journal_cause FOREIGN KEY (cause_id) REFERENCES accounting_causes(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_journal_vat_register FOREIGN KEY (vat_register_id) REFERENCES vat_registers(id) ON DELETE SET NULL;

ALTER TABLE vat_movements
    ADD COLUMN vat_register_id BIGINT UNSIGNED NULL AFTER register_type,
    ADD COLUMN operation_type ENUM('DOMESTIC','REVERSE_CHARGE','SPLIT_PAYMENT','CASH','INTRA_EU','EXTRA_EU','MARGIN','EXEMPT','NON_TAXABLE','ADJUSTMENT') NOT NULL DEFAULT 'DOMESTIC' AFTER vat_register_id,
    ADD COLUMN collectability ENUM('IMMEDIATE','DEFERRED','SPLIT','CASH') NOT NULL DEFAULT 'IMMEDIATE' AFTER operation_type,
    ADD COLUMN tax_point_date DATE NULL AFTER movement_date,
    ADD COLUMN vat_due_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER vat_amount,
    ADD COLUMN deductibility_percent DECIMAL(7,4) NOT NULL DEFAULT 100 AFTER deductible_vat,
    ADD COLUMN pro_rata_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER deductibility_percent,
    ADD COLUMN lipe_excluded TINYINT(1) NOT NULL DEFAULT 0 AFTER pro_rata_amount,
    ADD COLUMN source_import_batch_id BIGINT UNSIGNED NULL AFTER lipe_excluded,
    ADD KEY idx_vat_register_period (organization_id, vat_register_id, period_year, period_month),
    ADD CONSTRAINT fk_vat_movement_register FOREIGN KEY (vat_register_id) REFERENCES vat_registers(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_vat_deductibility CHECK (deductibility_percent >= 0 AND deductibility_percent <= 100);

UPDATE vat_movements
SET vat_due_amount = CASE
    WHEN register_type IN ('SALES','CORRISPETTIVI') THEN vat_amount
    ELSE 0
END,
deductibility_percent = CASE
    WHEN register_type = 'PURCHASES' AND vat_amount <> 0 THEN LEAST(100, GREATEST(0, ABS(deductible_vat / vat_amount) * 100))
    ELSE 100
END;

CREATE TABLE IF NOT EXISTS accounting_open_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    direction ENUM('RECEIVABLE','PAYABLE') NOT NULL,
    party_type ENUM('CUSTOMER','SUPPLIER','OTHER') NOT NULL,
    party_id BIGINT UNSIGNED NULL,
    party_name VARCHAR(190) NOT NULL,
    document_id BIGINT UNSIGNED NULL,
    payment_schedule_id BIGINT UNSIGNED NULL,
    journal_entry_id BIGINT UNSIGNED NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    reference VARCHAR(190) NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    original_amount DECIMAL(15,2) NOT NULL,
    settled_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    status ENUM('OPEN','PARTIAL','SETTLED','OVERDUE','DISPUTED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    source_import_batch_id BIGINT UNSIGNED NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_open_item_schedule (organization_id, payment_schedule_id),
    KEY idx_open_item_due (organization_id, direction, status, due_date),
    KEY idx_open_item_party (organization_id, party_type, party_id, status),
    CONSTRAINT fk_open_item_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_open_item_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT fk_open_item_schedule FOREIGN KEY (payment_schedule_id) REFERENCES payment_schedules(id) ON DELETE SET NULL,
    CONSTRAINT fk_open_item_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    CONSTRAINT fk_open_item_account FOREIGN KEY (account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_open_item_amount CHECK (original_amount > 0 AND settled_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE payments
    ADD COLUMN payment_type ENUM('RECEIPT','PAYMENT') NOT NULL DEFAULT 'RECEIPT' AFTER payment_schedule_id,
    ADD COLUMN customer_id BIGINT UNSIGNED NULL AFTER payment_type,
    ADD COLUMN supplier_id BIGINT UNSIGNED NULL AFTER customer_id,
    ADD COLUMN bank_account_id BIGINT UNSIGNED NULL AFTER supplier_id,
    ADD COLUMN journal_entry_id BIGINT UNSIGNED NULL AFTER bank_account_id,
    ADD COLUMN cause_id BIGINT UNSIGNED NULL AFTER journal_entry_id,
    ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'EUR' AFTER amount,
    ADD COLUMN bank_amount DECIMAL(15,2) NULL AFTER currency,
    ADD COLUMN status ENUM('DRAFT','POSTED','REVERSED') NOT NULL DEFAULT 'POSTED' AFTER method,
    ADD KEY idx_payments_party (organization_id, payment_type, customer_id, supplier_id),
    ADD CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_payment_cause FOREIGN KEY (cause_id) REFERENCES accounting_causes(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS payment_allocations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    payment_id BIGINT UNSIGNED NOT NULL,
    open_item_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_payment_allocation (payment_id, open_item_id),
    CONSTRAINT fk_payment_allocation_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_allocation_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_allocation_item FOREIGN KEY (open_item_id) REFERENCES accounting_open_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_allocation_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vat_cash_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    vat_movement_id BIGINT UNSIGNED NOT NULL,
    payment_id BIGINT UNSIGNED NOT NULL,
    recognition_date DATE NOT NULL,
    recognized_taxable DECIMAL(15,2) NOT NULL DEFAULT 0,
    recognized_vat_due DECIMAL(15,2) NOT NULL DEFAULT 0,
    recognized_vat_credit DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_vat_cash_event (vat_movement_id, payment_id),
    KEY idx_vat_cash_period (organization_id, recognition_date),
    CONSTRAINT fk_vat_cash_event_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_vat_cash_event_movement FOREIGN KEY (vat_movement_id) REFERENCES vat_movements(id) ON DELETE CASCADE,
    CONSTRAINT fk_vat_cash_event_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE bank_transactions
    ADD COLUMN journal_entry_id BIGINT UNSIGNED NULL AFTER source_import_batch_id,
    ADD COLUMN import_hash CHAR(64) NULL AFTER external_id,
    ADD COLUMN reconciled_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER reconciliation_status,
    ADD COLUMN notes TEXT NULL AFTER reconciled_amount,
    ADD UNIQUE KEY uq_bank_import_hash (organization_id, bank_account_id, import_hash),
    ADD CONSTRAINT fk_bank_transaction_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS vat_adjustments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fiscal_year SMALLINT UNSIGNED NOT NULL,
    period_month TINYINT UNSIGNED NULL,
    adjustment_type ENUM('PRO_RATA','PRIOR_CREDIT','ROUNDING','ANNUAL_CORRECTION','REFUND','COMPENSATION','OTHER') NOT NULL,
    description VARCHAR(500) NOT NULL,
    vat_debit_delta DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_credit_delta DECIMAL(15,2) NOT NULL DEFAULT 0,
    journal_entry_id BIGINT UNSIGNED NULL,
    status ENUM('DRAFT','APPROVED','POSTED') NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT UNSIGNED NULL,
    approved_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_vat_adjustment_period (organization_id, fiscal_year, period_month, status),
    CONSTRAINT fk_vat_adjustment_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_vat_adjustment_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lipe_communications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fiscal_year SMALLINT UNSIGNED NOT NULL,
    quarter_number TINYINT UNSIGNED NOT NULL,
    schema_version VARCHAR(20) NOT NULL,
    payload_json JSON NOT NULL,
    validation_json JSON NULL,
    status ENUM('DRAFT','REVIEWED','LOCKED','EXPORTED') NOT NULL DEFAULT 'DRAFT',
    export_checksum CHAR(64) NULL,
    reviewed_by BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_lipe_communication (organization_id, fiscal_year, quarter_number),
    CONSTRAINT fk_lipe_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_lipe_quarter CHECK (quarter_number BETWEEN 1 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS vat_annual_summaries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fiscal_year SMALLINT UNSIGNED NOT NULL,
    schema_version VARCHAR(20) NOT NULL,
    sales_taxable DECIMAL(15,2) NOT NULL DEFAULT 0,
    purchases_taxable DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_debit DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_credit DECIMAL(15,2) NOT NULL DEFAULT 0,
    adjustments_debit DECIMAL(15,2) NOT NULL DEFAULT 0,
    adjustments_credit DECIMAL(15,2) NOT NULL DEFAULT 0,
    prior_credit DECIMAL(15,2) NOT NULL DEFAULT 0,
    payments_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    final_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    detail_json JSON NOT NULL,
    validation_json JSON NULL,
    status ENUM('DRAFT','REVIEWED','LOCKED','EXPORTED') NOT NULL DEFAULT 'DRAFT',
    export_checksum CHAR(64) NULL,
    reviewed_by BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_vat_annual (organization_id, fiscal_year),
    CONSTRAINT fk_vat_annual_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_period_locks (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    scope ENUM('ACCOUNTING','VAT','SALES','PURCHASES','BANKING') NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    locked_by BIGINT UNSIGNED NULL,
    unlocked_by BIGINT UNSIGNED NULL,
    unlocked_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_period_lock (organization_id, scope, starts_on, ends_on, unlocked_at),
    CONSTRAINT fk_period_lock_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_period_lock_dates CHECK (ends_on >= starts_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_closing_runs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fiscal_year SMALLINT UNSIGNED NOT NULL,
    status ENUM('DRAFT','VALIDATED','POSTED','REVERSED') NOT NULL DEFAULT 'DRAFT',
    profit_loss_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    closing_journal_entry_id BIGINT UNSIGNED NULL,
    opening_journal_entry_id BIGINT UNSIGNED NULL,
    snapshot_json JSON NOT NULL,
    validation_json JSON NULL,
    created_by BIGINT UNSIGNED NULL,
    posted_by BIGINT UNSIGNED NULL,
    posted_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_closing_run (organization_id, fiscal_year),
    CONSTRAINT fk_closing_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_closing_entry FOREIGN KEY (closing_journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    CONSTRAINT fk_opening_entry FOREIGN KEY (opening_journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accounting_adjustment_schedules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    adjustment_type ENUM('ACCRUAL','DEFERRAL','PREPAID','DEFERRED_INCOME','OTHER') NOT NULL,
    description VARCHAR(500) NOT NULL,
    source_account_id BIGINT UNSIGNED NOT NULL,
    counterpart_account_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    competence_from DATE NOT NULL,
    competence_to DATE NOT NULL,
    posting_date DATE NOT NULL,
    reversal_date DATE NULL,
    journal_entry_id BIGINT UNSIGNED NULL,
    reversal_journal_entry_id BIGINT UNSIGNED NULL,
    status ENUM('DRAFT','POSTED','REVERSED') NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_adjustment_schedule_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_adjustment_source_account FOREIGN KEY (source_account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_counterpart_account FOREIGN KEY (counterpart_account_id) REFERENCES chart_of_accounts(id) ON DELETE RESTRICT,
    CONSTRAINT fk_adjustment_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    CONSTRAINT fk_adjustment_reversal FOREIGN KEY (reversal_journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL,
    CONSTRAINT chk_adjustment_amount CHECK (amount > 0 AND competence_to >= competence_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fixed_asset_categories (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(190) NOT NULL,
    civil_rate DECIMAL(7,4) NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    first_year_percent DECIMAL(7,4) NOT NULL DEFAULT 50,
    asset_account_id BIGINT UNSIGNED NULL,
    depreciation_expense_account_id BIGINT UNSIGNED NULL,
    accumulated_depreciation_account_id BIGINT UNSIGNED NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_asset_category (organization_id, code),
    CONSTRAINT fk_asset_category_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_asset_category_asset_account FOREIGN KEY (asset_account_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_asset_category_expense_account FOREIGN KEY (depreciation_expense_account_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_asset_category_fund_account FOREIGN KEY (accumulated_depreciation_account_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE fixed_assets
    ADD COLUMN category_id BIGINT UNSIGNED NULL AFTER category,
    ADD COLUMN supplier_id BIGINT UNSIGNED NULL AFTER category_id,
    ADD COLUMN document_id BIGINT UNSIGNED NULL AFTER supplier_id,
    ADD COLUMN in_service_date DATE NULL AFTER purchase_date,
    ADD COLUMN residual_value DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER purchase_cost,
    ADD COLUMN civil_depreciation_rate DECIMAL(7,4) NULL AFTER depreciation_rate,
    ADD COLUMN tax_depreciation_rate DECIMAL(7,4) NULL AFTER civil_depreciation_rate,
    ADD COLUMN first_year_percent DECIMAL(7,4) NOT NULL DEFAULT 50 AFTER tax_depreciation_rate,
    ADD COLUMN tax_accumulated_depreciation DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER accumulated_depreciation,
    ADD COLUMN tax_net_value DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER net_book_value,
    ADD COLUMN disposal_proceeds DECIMAL(15,2) NULL AFTER disposal_date,
    ADD CONSTRAINT fk_fixed_asset_category FOREIGN KEY (category_id) REFERENCES fixed_asset_categories(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_fixed_asset_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_fixed_asset_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL;

ALTER TABLE depreciation_entries
    ADD COLUMN civil_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER civil_amount,
    ADD COLUMN period_start DATE NULL AFTER fiscal_year,
    ADD COLUMN period_end DATE NULL AFTER period_start,
    ADD COLUMN posted_at DATETIME NULL AFTER status;

UPDATE fixed_assets
SET in_service_date = COALESCE(in_service_date, purchase_date),
    civil_depreciation_rate = COALESCE(civil_depreciation_rate, depreciation_rate),
    tax_depreciation_rate = COALESCE(tax_depreciation_rate, depreciation_rate),
    tax_accumulated_depreciation = accumulated_depreciation,
    tax_net_value = net_book_value;

UPDATE depreciation_entries
SET civil_amount = amount, tax_amount = amount;

CREATE TABLE IF NOT EXISTS withholding_records (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    supplier_id BIGINT UNSIGNED NULL,
    document_id BIGINT UNSIGNED NULL,
    payment_id BIGINT UNSIGNED NULL,
    record_date DATE NOT NULL,
    due_date DATE NULL,
    withholding_type ENUM('IRPEF','INPS','ENASARCO','OTHER') NOT NULL DEFAULT 'IRPEF',
    gross_amount DECIMAL(15,2) NOT NULL,
    taxable_percent DECIMAL(7,4) NOT NULL DEFAULT 100,
    rate_percent DECIMAL(7,4) NOT NULL,
    withholding_amount DECIMAL(15,2) NOT NULL,
    social_security_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(15,2) NOT NULL,
    status ENUM('DRAFT','WITHHELD','PAID','CERTIFIED') NOT NULL DEFAULT 'DRAFT',
    paid_at DATE NULL,
    journal_entry_id BIGINT UNSIGNED NULL,
    notes TEXT NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_withholding_due (organization_id, status, due_date),
    CONSTRAINT fk_withholding_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_withholding_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    CONSTRAINT fk_withholding_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT fk_withholding_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
    CONSTRAINT fk_withholding_journal FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_endpoint_configs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    service_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(190) NOT NULL,
    environment ENUM('TEST','PRODUCTION') NOT NULL DEFAULT 'TEST',
    base_url VARCHAR(500) NOT NULL,
    send_path VARCHAR(500) NULL,
    receive_path VARCHAR(500) NULL,
    status_path VARCHAR(500) NULL,
    webhook_path VARCHAR(500) NULL,
    auth_type ENUM('NONE','BEARER','API_KEY','OAUTH2','MTLS','CUSTOM') NOT NULL DEFAULT 'NONE',
    secret_reference VARCHAR(190) NULL,
    header_json JSON NULL,
    timeout_seconds SMALLINT UNSIGNED NOT NULL DEFAULT 30,
    verify_tls TINYINT(1) NOT NULL DEFAULT 1,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    notes TEXT NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_api_endpoint (organization_id, service_key, environment),
    CONSTRAINT fk_api_endpoint_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_api_endpoint_timeout CHECK (timeout_seconds BETWEEN 1 AND 300)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO accounting_settings (organization_id)
SELECT id FROM organizations
ON DUPLICATE KEY UPDATE organization_id = VALUES(organization_id);

INSERT INTO vat_registers (organization_id, code, name, register_type, is_default)
SELECT id, 'V1', 'Vendite', 'SALES', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO vat_registers (organization_id, code, name, register_type, is_default)
SELECT id, 'A1', 'Acquisti', 'PURCHASES', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO vat_registers (organization_id, code, name, register_type, is_default)
SELECT id, 'C1', 'Corrispettivi', 'CORRISPETTIVI', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, creates_open_item, automatic)
SELECT id, 'GEN', 'Registrazione generica', 'GENERAL', 'Registrazione contabile', 0, 0 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, creates_open_item, automatic)
SELECT id, 'FAV', 'Fattura di vendita', 'SALES', 'Fattura di vendita', 1, 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, creates_open_item, automatic)
SELECT id, 'FAC', 'Fattura di acquisto', 'PURCHASE', 'Fattura di acquisto', 1, 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'INC', 'Incasso cliente', 'RECEIPT', 'Incasso cliente', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'PAG', 'Pagamento fornitore', 'PAYMENT', 'Pagamento fornitore', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'ASS', 'Scrittura di assestamento', 'ADJUSTMENT', 'Scrittura di assestamento', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'AMM', 'Ammortamento cespiti', 'DEPRECIATION', 'Ammortamento cespiti', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'RIT', 'Ritenute e contributi', 'WITHHOLDING', 'Ritenute e contributi', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'CHI', 'Chiusura esercizio', 'CLOSING', 'Chiusura esercizio', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO accounting_causes (organization_id, code, name, category, default_description, automatic)
SELECT id, 'APE', 'Apertura esercizio', 'OPENING', 'Apertura esercizio', 1 FROM organizations
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO chart_of_accounts
    (organization_id, code, name, account_type, normal_balance, system_key, statement_section, is_postable, active, locked)
SELECT o.id,
    CASE
        WHEN code_conflict.id IS NULL THEN seed.code
        ELSE CONCAT('L2S.', LEFT(seed.system_key, 32), '.', SUBSTRING(SHA2(CONCAT(o.id, ':', seed.system_key), 256), 1, 8))
    END,
    seed.name, seed.account_type, seed.normal_balance, seed.system_key, seed.statement_section, 1, 1, 1
FROM organizations o
CROSS JOIN (
    SELECT 'L2.1000' code, 'Crediti verso clienti' name, 'ASSET' account_type, 'DEBIT' normal_balance, 'TRADE_RECEIVABLES' system_key, 'SP.ATTIVO.CREDITI' statement_section
    UNION ALL SELECT 'L2.1100','Banca c/c','ASSET','DEBIT','BANK','SP.ATTIVO.LIQUIDITA'
    UNION ALL SELECT 'L2.1110','Cassa contanti','ASSET','DEBIT','CASH','SP.ATTIVO.LIQUIDITA'
    UNION ALL SELECT 'L2.1200','IVA a credito','ASSET','DEBIT','VAT_RECEIVABLE','SP.ATTIVO.CREDITI_TRIBUTARI'
    UNION ALL SELECT 'L2.1300','Risconti attivi','ASSET','DEBIT','PREPAID_EXPENSES','SP.ATTIVO.RATEI_RISCONTI'
    UNION ALL SELECT 'L2.1310','Ratei attivi','ASSET','DEBIT','ACCRUED_INCOME','SP.ATTIVO.RATEI_RISCONTI'
    UNION ALL SELECT 'L2.1500','Fondo ammortamento','ASSET','CREDIT','ACCUMULATED_DEPRECIATION','SP.ATTIVO.IMMOBILIZZAZIONI'
    UNION ALL SELECT 'L2.2000','Debiti verso fornitori','LIABILITY','CREDIT','TRADE_PAYABLES','SP.PASSIVO.DEBITI'
    UNION ALL SELECT 'L2.2100','IVA a debito','LIABILITY','CREDIT','VAT_PAYABLE','SP.PASSIVO.DEBITI_TRIBUTARI'
    UNION ALL SELECT 'L2.2200','Erario c/IVA','LIABILITY','CREDIT','VAT_CLEARING','SP.PASSIVO.DEBITI_TRIBUTARI'
    UNION ALL SELECT 'L2.2300','Erario c/ritenute','LIABILITY','CREDIT','WITHHOLDING_PAYABLE','SP.PASSIVO.DEBITI_TRIBUTARI'
    UNION ALL SELECT 'L2.2310','Enti previdenziali','LIABILITY','CREDIT','SOCIAL_SECURITY_PAYABLE','SP.PASSIVO.DEBITI'
    UNION ALL SELECT 'L2.2400','Ratei passivi','LIABILITY','CREDIT','ACCRUED_EXPENSES','SP.PASSIVO.RATEI_RISCONTI'
    UNION ALL SELECT 'L2.2410','Risconti passivi','LIABILITY','CREDIT','DEFERRED_INCOME','SP.PASSIVO.RATEI_RISCONTI'
    UNION ALL SELECT 'L2.3000','Patrimonio netto','EQUITY','CREDIT','EQUITY','SP.PASSIVO.PATRIMONIO_NETTO'
    UNION ALL SELECT 'L2.3100','Utili e perdite portati a nuovo','EQUITY','CREDIT','RETAINED_EARNINGS','SP.PASSIVO.PATRIMONIO_NETTO'
    UNION ALL SELECT 'L2.3900','Risultato d’esercizio','EQUITY','CREDIT','PROFIT_LOSS','SP.PASSIVO.PATRIMONIO_NETTO'
    UNION ALL SELECT 'L2.3990','Bilancio di apertura','EQUITY','CREDIT','OPENING_BALANCE','SP.PASSIVO.PATRIMONIO_NETTO'
    UNION ALL SELECT 'L2.4000','Ricavi vendite e prestazioni','REVENUE','CREDIT','SALES_REVENUE','CE.A.RICAVI'
    UNION ALL SELECT 'L2.4010','Altri ricavi e proventi','REVENUE','CREDIT','OTHER_REVENUE','CE.A.ALTRI_RICAVI'
    UNION ALL SELECT 'L2.5000','Costi per acquisti e servizi','EXPENSE','DEBIT','PURCHASE_COSTS','CE.B.COSTI'
    UNION ALL SELECT 'L2.5010','Abbuoni e differenze passive','EXPENSE','DEBIT','PAYMENT_DIFFERENCES','CE.B.COSTI'
    UNION ALL SELECT 'L2.5100','Ammortamenti','EXPENSE','DEBIT','DEPRECIATION_EXPENSE','CE.B.AMMORTAMENTI'
) seed
LEFT JOIN chart_of_accounts existing ON existing.organization_id = o.id AND existing.system_key = seed.system_key
LEFT JOIN chart_of_accounts code_conflict ON code_conflict.organization_id = o.id AND code_conflict.code = seed.code
WHERE existing.id IS NULL
ON DUPLICATE KEY UPDATE code = chart_of_accounts.code;

INSERT INTO accounting_account_mappings (organization_id, mapping_key, account_id, description)
SELECT organization_id, system_key, id, name
FROM chart_of_accounts
WHERE system_key IS NOT NULL
ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), description = VALUES(description);
