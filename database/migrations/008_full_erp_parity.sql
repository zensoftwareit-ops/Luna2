ALTER TABLE documents
    ADD COLUMN warehouse_id BIGINT UNSIGNED NULL AFTER source_document_id,
    ADD COLUMN project_id BIGINT UNSIGNED NULL AFTER warehouse_id,
    ADD COLUMN fulfillment_status ENUM('NOT_REQUIRED','OPEN','PARTIAL','FULFILLED','CANCELLED') NOT NULL DEFAULT 'NOT_REQUIRED' AFTER status,
    ADD COLUMN converted_total DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER balance_due,
    ADD KEY idx_documents_workflow (organization_id, document_type, fulfillment_status),
    ADD CONSTRAINT fk_documents_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_documents_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

ALTER TABLE document_lines
    ADD COLUMN source_line_id BIGINT UNSIGNED NULL AFTER document_id,
    ADD COLUMN warehouse_id BIGINT UNSIGNED NULL AFTER product_id,
    ADD COLUMN project_id BIGINT UNSIGNED NULL AFTER warehouse_id,
    ADD COLUMN converted_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER quantity,
    ADD COLUMN reserved_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER converted_quantity,
    ADD COLUMN fulfilled_quantity DECIMAL(15,4) NOT NULL DEFAULT 0 AFTER reserved_quantity,
    ADD KEY idx_document_line_source (organization_id, source_line_id),
    ADD CONSTRAINT fk_document_line_source FOREIGN KEY (source_line_id) REFERENCES document_lines(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_document_line_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_document_line_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS document_links (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    source_document_id BIGINT UNSIGNED NOT NULL,
    target_document_id BIGINT UNSIGNED NOT NULL,
    link_type ENUM('CONVERSION','MERGE','SPLIT','PROJECT_BILLING','ECOMMERCE_IMPORT','RENTAL_BILLING') NOT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_document_link (organization_id, source_document_id, target_document_id, link_type),
    CONSTRAINT fk_document_link_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_link_source FOREIGN KEY (source_document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_link_target FOREIGN KEY (target_document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE inventory_movements
    ADD COLUMN movement_uuid CHAR(36) NULL AFTER id,
    ADD COLUMN source_type VARCHAR(50) NULL AFTER document_number,
    ADD COLUMN source_id BIGINT UNSIGNED NULL AFTER source_type,
    ADD COLUMN reversed_movement_id BIGINT UNSIGNED NULL AFTER source_id,
    ADD UNIQUE KEY uq_inventory_movement_uuid (organization_id, movement_uuid),
    ADD KEY idx_inventory_source (organization_id, source_type, source_id),
    ADD CONSTRAINT fk_inventory_reversal FOREIGN KEY (reversed_movement_id) REFERENCES inventory_movements(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS inventory_transfers (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    transfer_number VARCHAR(100) NOT NULL,
    transfer_date DATE NOT NULL,
    source_warehouse_id BIGINT UNSIGNED NOT NULL,
    destination_warehouse_id BIGINT UNSIGNED NOT NULL,
    status ENUM('DRAFT','CONFIRMED','IN_TRANSIT','RECEIVED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    confirmed_at DATETIME NULL,
    received_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_inventory_transfer (organization_id, transfer_number),
    CONSTRAINT fk_transfer_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_source_warehouse FOREIGN KEY (source_warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_transfer_destination_warehouse FOREIGN KEY (destination_warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_transfer_lines (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    transfer_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    requested_quantity DECIMAL(15,4) NOT NULL,
    shipped_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    received_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(15,4) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_transfer_product (transfer_id, product_id),
    CONSTRAINT fk_transfer_line_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_line_transfer FOREIGN KEY (transfer_id) REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_line_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_pick_lists (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_number VARCHAR(100) NOT NULL,
    warehouse_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    status ENUM('OPEN','PICKING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    assigned_to BIGINT UNSIGNED NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_pick_number (organization_id, pick_number),
    UNIQUE KEY uq_pick_document (organization_id, document_id),
    CONSTRAINT fk_pick_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_pick_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_assignee FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_pick_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_list_id BIGINT UNSIGNED NOT NULL,
    document_line_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    barcode VARCHAR(190) NULL,
    required_quantity DECIMAL(15,4) NOT NULL,
    picked_quantity DECIMAL(15,4) NOT NULL DEFAULT 0,
    status ENUM('OPEN','PARTIAL','PICKED','EXCEPTION') NOT NULL DEFAULT 'OPEN',
    UNIQUE KEY uq_pick_document_line (pick_list_id, document_line_id),
    CONSTRAINT fk_pick_item_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_list FOREIGN KEY (pick_list_id) REFERENCES inventory_pick_lists(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE CASCADE,
    CONSTRAINT fk_pick_item_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_barcode_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    pick_list_id BIGINT UNSIGNED NULL,
    warehouse_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NULL,
    barcode VARCHAR(190) NOT NULL,
    quantity DECIMAL(15,4) NOT NULL DEFAULT 1,
    result ENUM('ACCEPTED','UNKNOWN','EXCESS','WRONG_WAREHOUSE','ERROR') NOT NULL,
    message VARCHAR(500) NULL,
    scanned_by BIGINT UNSIGNED NULL,
    scanned_at DATETIME NOT NULL,
    KEY idx_barcode_scan (organization_id, barcode, scanned_at),
    CONSTRAINT fk_barcode_event_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_barcode_event_pick FOREIGN KEY (pick_list_id) REFERENCES inventory_pick_lists(id) ON DELETE SET NULL,
    CONSTRAINT fk_barcode_event_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_barcode_event_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    CONSTRAINT fk_barcode_event_user FOREIGN KEY (scanned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_time_entries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    work_date DATE NOT NULL,
    description VARCHAR(500) NOT NULL,
    hours DECIMAL(7,2) NOT NULL,
    hourly_cost DECIMAL(12,4) NOT NULL DEFAULT 0,
    hourly_rate DECIMAL(12,4) NOT NULL DEFAULT 0,
    billable TINYINT(1) NOT NULL DEFAULT 1,
    billing_status ENUM('OPEN','INVOICED','NON_BILLABLE','CANCELLED') NOT NULL DEFAULT 'OPEN',
    document_line_id BIGINT UNSIGNED NULL,
    approved_by BIGINT UNSIGNED NULL,
    approved_at DATETIME NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_time_open (organization_id, project_id, billing_status, work_date),
    CONSTRAINT fk_project_time_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_time_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_time_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_time_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_time_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_expenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    expense_date DATE NOT NULL,
    supplier_id BIGINT UNSIGNED NULL,
    description VARCHAR(500) NOT NULL,
    cost_amount DECIMAL(15,2) NOT NULL,
    billable_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    billable TINYINT(1) NOT NULL DEFAULT 0,
    billing_status ENUM('OPEN','INVOICED','NON_BILLABLE','CANCELLED') NOT NULL DEFAULT 'OPEN',
    source_document_id BIGINT UNSIGNED NULL,
    document_line_id BIGINT UNSIGNED NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_expense_open (organization_id, project_id, billing_status, expense_date),
    CONSTRAINT fk_project_expense_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_expense_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_expense_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_expense_document FOREIGN KEY (source_document_id) REFERENCES documents(id) ON DELETE SET NULL,
    CONSTRAINT fk_project_expense_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(255) NOT NULL,
    due_date DATE NULL,
    billing_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    status ENUM('OPEN','READY','INVOICED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
    document_line_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_milestone_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_milestone_document_line FOREIGN KEY (document_line_id) REFERENCES document_lines(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS communication_settings (
    organization_id BIGINT UNSIGNED PRIMARY KEY,
    transport ENUM('SMTP','SENDMAIL','DISABLED') NOT NULL DEFAULT 'DISABLED',
    host VARCHAR(255) NULL,
    port SMALLINT UNSIGNED NOT NULL DEFAULT 587,
    encryption ENUM('NONE','TLS','SMTPS') NOT NULL DEFAULT 'TLS',
    username VARCHAR(190) NULL,
    secret_reference VARCHAR(190) NULL,
    from_email VARCHAR(190) NULL,
    from_name VARCHAR(190) NULL,
    reply_to VARCHAR(190) NULL,
    tracking_base_url VARCHAR(500) NULL,
    tracking_enabled TINYINT(1) NOT NULL DEFAULT 1,
    max_attempts TINYINT UNSIGNED NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_communication_setting_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbound_emails (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    message_uuid CHAR(36) NOT NULL,
    document_id BIGINT UNSIGNED NULL,
    recipient VARCHAR(190) NOT NULL,
    cc VARCHAR(1000) NULL,
    bcc VARCHAR(1000) NULL,
    subject VARCHAR(255) NOT NULL,
    html_body MEDIUMTEXT NOT NULL,
    text_body MEDIUMTEXT NULL,
    attachment_json JSON NULL,
    tracking_token CHAR(64) NOT NULL,
    status ENUM('DRAFT','QUEUED','SENDING','SENT','RETRY','FAILED','CANCELLED') NOT NULL DEFAULT 'QUEUED',
    attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NULL,
    sent_at DATETIME NULL,
    message_id VARCHAR(255) NULL,
    error_message TEXT NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_outbound_message_uuid (organization_id, message_uuid),
    UNIQUE KEY uq_outbound_tracking_token (tracking_token),
    KEY idx_outbound_queue (status, next_attempt_at),
    CONSTRAINT fk_outbound_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    outbound_email_id BIGINT UNSIGNED NOT NULL,
    event_type ENUM('OPEN','CLICK','DOWNLOAD','DELIVERED','BOUNCE','COMPLAINT') NOT NULL,
    target_url VARCHAR(1000) NULL,
    ip_hash CHAR(64) NULL,
    user_agent_hash CHAR(64) NULL,
    occurred_at DATETIME NOT NULL,
    KEY idx_email_event (organization_id, outbound_email_id, event_type, occurred_at),
    CONSTRAINT fk_email_event_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_email_event_message FOREIGN KEY (outbound_email_id) REFERENCES outbound_emails(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    balance_year SMALLINT UNSIGNED NOT NULL,
    leave_type ENUM('HOLIDAY','PERMIT','ROL','OTHER') NOT NULL,
    opening_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    accrued_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    used_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    adjusted_hours DECIMAL(8,2) NOT NULL DEFAULT 0,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_leave_balance (organization_id, user_id, balance_year, leave_type),
    CONSTRAINT fk_leave_balance_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_balance_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS leave_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    leave_type ENUM('HOLIDAY','PERMIT','ROL','SICK','OTHER') NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    requested_hours DECIMAL(8,2) NOT NULL,
    reason TEXT NULL,
    status ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    approval_request_id BIGINT UNSIGNED NULL,
    submitted_at DATETIME NULL,
    decided_at DATETIME NULL,
    decided_by BIGINT UNSIGNED NULL,
    decision_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_leave_request_period (organization_id, user_id, starts_at, ends_at, status),
    CONSTRAINT fk_leave_request_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_approval FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE SET NULL,
    CONSTRAINT fk_leave_request_decider FOREIGN KEY (decided_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS approval_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    approval_request_id BIGINT UNSIGNED NOT NULL,
    action ENUM('CREATE','SUBMIT','APPROVE','REJECT','CANCEL','REOPEN') NOT NULL,
    actor_id BIGINT UNSIGNED NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_history_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_history_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_approval_history_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE ecommerce_channels
    ADD COLUMN secret_reference VARCHAR(190) NULL AFTER encrypted_credentials,
    ADD COLUMN webhook_secret_reference VARCHAR(190) NULL AFTER secret_reference,
    ADD COLUMN settings_json JSON NULL AFTER webhook_secret_reference,
    ADD COLUMN sync_cursor TEXT NULL AFTER settings_json,
    ADD COLUMN last_success_at DATETIME NULL AFTER last_sync_at,
    ADD COLUMN last_error TEXT NULL AFTER last_success_at;

CREATE TABLE IF NOT EXISTS ecommerce_order_lines (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    ecommerce_order_id BIGINT UNSIGNED NOT NULL,
    external_line_id VARCHAR(190) NULL,
    external_product_id VARCHAR(190) NULL,
    product_id BIGINT UNSIGNED NULL,
    sku VARCHAR(100) NULL,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(15,4) NOT NULL,
    unit_price DECIMAL(15,4) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL,
    raw_data_json JSON NULL,
    CONSTRAINT fk_ecommerce_order_line_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ecommerce_order_line_order FOREIGN KEY (ecommerce_order_id) REFERENCES ecommerce_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_ecommerce_order_line_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ecommerce_sync_queue (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    ecommerce_channel_id BIGINT UNSIGNED NOT NULL,
    operation ENUM('PULL_ORDERS','PULL_PRODUCTS','PUSH_STOCK','PUSH_PRICES','ACK_ORDER','PROCESS_WEBHOOK') NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT UNSIGNED NULL,
    payload_json JSON NULL,
    status ENUM('QUEUED','RUNNING','RETRY','DONE','FAILED','CANCELLED') NOT NULL DEFAULT 'QUEUED',
    attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    available_at DATETIME NOT NULL,
    locked_at DATETIME NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ecommerce_queue (status, available_at),
    CONSTRAINT fk_ecommerce_queue_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ecommerce_queue_channel FOREIGN KEY (ecommerce_channel_id) REFERENCES ecommerce_channels(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE rental_contracts
    ADD COLUMN invoice_day TINYINT UNSIGNED NULL AFTER monthly_fee,
    ADD COLUMN next_invoice_date DATE NULL AFTER invoice_day,
    ADD COLUMN included_km INT UNSIGNED NULL AFTER annual_km,
    ADD COLUMN current_km INT UNSIGNED NOT NULL DEFAULT 0 AFTER included_km,
    ADD COLUMN renewal_notice_days SMALLINT UNSIGNED NOT NULL DEFAULT 60 AFTER current_km;

ALTER TABLE rental_tickets
    ADD COLUMN assigned_to BIGINT UNSIGNED NULL AFTER status,
    ADD COLUMN response_due_at DATETIME NULL AFTER sla_due_at,
    ADD COLUMN resolved_at DATETIME NULL AFTER response_due_at,
    ADD COLUMN sla_breached TINYINT(1) NOT NULL DEFAULT 0 AFTER resolved_at,
    ADD CONSTRAINT fk_rental_ticket_assignee FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS rental_meter_readings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    rental_contract_id BIGINT UNSIGNED NOT NULL,
    reading_date DATE NOT NULL,
    kilometers INT UNSIGNED NOT NULL,
    source ENUM('MANUAL','CUSTOMER','SERVICE','IMPORT') NOT NULL DEFAULT 'MANUAL',
    notes VARCHAR(500) NULL,
    created_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rental_meter (rental_contract_id, reading_date),
    CONSTRAINT fk_rental_meter_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_meter_contract FOREIGN KEY (rental_contract_id) REFERENCES rental_contracts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rental_ticket_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    rental_ticket_id BIGINT UNSIGNED NOT NULL,
    event_type ENUM('OPENED','ASSIGNED','COMMENT','STATUS','SLA_BREACH','RESOLVED','CLOSED') NOT NULL,
    old_value VARCHAR(190) NULL,
    new_value VARCHAR(190) NULL,
    notes TEXT NULL,
    actor_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rental_ticket_event_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_ticket_event_ticket FOREIGN KEY (rental_ticket_id) REFERENCES rental_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_ticket_event_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rental_invoice_links (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    rental_contract_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rental_invoice_period (rental_contract_id, period_start, period_end),
    CONSTRAINT fk_rental_invoice_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_invoice_contract FOREIGN KEY (rental_contract_id) REFERENCES rental_contracts(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_invoice_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS report_exports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    report_key VARCHAR(100) NOT NULL,
    period_from DATE NULL,
    period_to DATE NULL,
    parameters_json JSON NULL,
    filename VARCHAR(255) NOT NULL,
    checksum_sha256 CHAR(64) NULL,
    row_count INT UNSIGNED NOT NULL DEFAULT 0,
    generated_by BIGINT UNSIGNED NULL,
    generated_at DATETIME NOT NULL,
    KEY idx_report_export (organization_id, report_key, generated_at),
    CONSTRAINT fk_report_export_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_export_user FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE calendar_accounts
    ADD COLUMN endpoint_url VARCHAR(500) NULL AFTER account_email,
    ADD COLUMN auth_type ENUM('OAUTH2','BASIC','APP_PASSWORD','BEARER') NOT NULL DEFAULT 'OAUTH2' AFTER endpoint_url,
    ADD COLUMN secret_reference VARCHAR(190) NULL AFTER encrypted_credentials,
    ADD COLUMN external_calendar_id VARCHAR(255) NULL AFTER secret_reference,
    ADD COLUMN last_error TEXT NULL AFTER last_sync_at,
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER last_error,
    ADD COLUMN updated_by BIGINT UNSIGNED NULL AFTER created_by;

ALTER TABLE calendar_events
    MODIFY COLUMN provider ENUM('LOCAL','GOOGLE','ICLOUD','CALDAV') NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN etag VARCHAR(255) NULL AFTER provider_event_id,
    ADD COLUMN last_synced_at DATETIME NULL AFTER sync_status,
    ADD COLUMN sync_error TEXT NULL AFTER last_synced_at,
    ADD COLUMN deleted_external TINYINT(1) NOT NULL DEFAULT 0 AFTER sync_error;

CREATE TABLE IF NOT EXISTS calendar_sync_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    calendar_account_id BIGINT UNSIGNED NOT NULL,
    direction ENUM('PUSH','PULL','BIDIRECTIONAL') NOT NULL,
    status ENUM('RUNNING','SUCCESS','PARTIAL','ERROR') NOT NULL,
    processed_count INT UNSIGNED NOT NULL DEFAULT 0,
    error_count INT UNSIGNED NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    message TEXT NULL,
    CONSTRAINT fk_calendar_sync_log_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_calendar_sync_log_account FOREIGN KEY (calendar_account_id) REFERENCES calendar_accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE payroll_runs
    ADD COLUMN calculation_mode ENUM('CONFIGURED_RATES','IMPORTED_PAYSLIPS') NOT NULL DEFAULT 'CONFIGURED_RATES' AFTER period_end,
    ADD COLUMN schema_version VARCHAR(50) NOT NULL DEFAULT 'INTERNAL-1' AFTER calculation_mode,
    ADD COLUMN confirmed_by BIGINT UNSIGNED NULL AFTER status,
    ADD COLUMN confirmed_at DATETIME NULL AFTER confirmed_by,
    ADD COLUMN locked_at DATETIME NULL AFTER confirmed_at,
    ADD COLUMN paid_at DATETIME NULL AFTER locked_at,
    ADD COLUMN professional_validation_reference VARCHAR(190) NULL AFTER paid_at,
    ADD COLUMN validated_at DATETIME NULL AFTER professional_validation_reference,
    ADD COLUMN notes TEXT NULL AFTER validated_at,
    ADD CONSTRAINT fk_payroll_run_confirmer FOREIGN KEY (confirmed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE payroll_runs
    DROP INDEX uq_payroll_period,
    ADD UNIQUE KEY uq_payroll_period_mode (organization_id, period_start, period_end, calculation_mode);

ALTER TABLE payroll_details
    ADD COLUMN employer_contributions_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER contributions_amount,
    ADD COLUMN reimbursements_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER tax_amount,
    ADD COLUMN deductions_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER reimbursements_amount,
    ADD COLUMN employer_cost DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER net_amount,
    ADD COLUMN source_reference VARCHAR(190) NULL AFTER employer_cost;

ALTER TABLE payroll_employee_configs
    ADD COLUMN employee_code VARCHAR(50) NULL AFTER user_id,
    ADD COLUMN standard_weekly_hours DECIMAL(7,2) NOT NULL DEFAULT 40 AFTER employee_code,
    ADD COLUMN employer_contribution_rate DECIMAL(7,4) NOT NULL DEFAULT 0 AFTER inail_rate,
    ADD COLUMN fixed_monthly_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER employer_contribution_rate,
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1 AFTER valid_to,
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER active,
    ADD COLUMN updated_by BIGINT UNSIGNED NULL AFTER created_by;

CREATE TABLE IF NOT EXISTS payroll_components (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    payroll_detail_id BIGINT UNSIGNED NOT NULL,
    component_code VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    component_type ENUM('EARNING','DEDUCTION','EMPLOYEE_CONTRIBUTION','EMPLOYER_CONTRIBUTION','TAX','REIMBURSEMENT') NOT NULL,
    quantity DECIMAL(12,4) NOT NULL DEFAULT 1,
    rate DECIMAL(12,4) NOT NULL DEFAULT 0,
    amount DECIMAL(15,2) NOT NULL,
    source ENUM('CALCULATED','IMPORTED','MANUAL_ADJUSTMENT') NOT NULL DEFAULT 'CALCULATED',
    UNIQUE KEY uq_payroll_component (payroll_detail_id, component_code),
    CONSTRAINT fk_payroll_component_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_component_detail FOREIGN KEY (payroll_detail_id) REFERENCES payroll_details(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payroll_import_rows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    payroll_run_id BIGINT UNSIGNED NOT NULL,
    source_file VARCHAR(255) NOT NULL,
    source_row INT UNSIGNED NOT NULL,
    employee_code VARCHAR(50) NULL,
    normalized_json JSON NOT NULL,
    status ENUM('STAGED','IMPORTED','ERROR') NOT NULL DEFAULT 'STAGED',
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payroll_import (organization_id, payroll_run_id, status),
    CONSTRAINT fk_payroll_import_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_import_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO communication_settings (organization_id)
SELECT id FROM organizations
ON DUPLICATE KEY UPDATE organization_id = VALUES(organization_id);
