CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    value_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_preference (organization_id, user_id, preference_key),
    CONSTRAINT fk_user_preference_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_preference_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS saved_views (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    query_json JSON NOT NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_saved_view (organization_id, user_id, module_key, name),
    KEY idx_saved_view_module (organization_id, user_id, module_key),
    CONSTRAINT fk_saved_view_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_view_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS workspace_notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    category VARCHAR(50) NOT NULL,
    severity ENUM('INFO','SUCCESS','WARNING','DANGER') NOT NULL DEFAULT 'INFO',
    title VARCHAR(190) NOT NULL,
    message VARCHAR(1000) NULL,
    action_url VARCHAR(500) NULL,
    dedupe_key VARCHAR(190) NULL,
    read_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_workspace_notification (organization_id, user_id, dedupe_key),
    KEY idx_workspace_notification_unread (organization_id, user_id, read_at, created_at),
    CONSTRAINT fk_workspace_notification_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS onboarding_progress (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    step_key VARCHAR(100) NOT NULL,
    completed_at DATETIME NULL,
    completed_by BIGINT UNSIGNED NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_onboarding_step (organization_id, step_key),
    CONSTRAINT fk_onboarding_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_onboarding_user FOREIGN KEY (completed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bulk_operations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    selection_json JSON NOT NULL,
    status ENUM('QUEUED','RUNNING','COMPLETED','PARTIAL','FAILED','CANCELLED') NOT NULL DEFAULT 'QUEUED',
    processed_count INT UNSIGNED NOT NULL DEFAULT 0,
    failed_count INT UNSIGNED NOT NULL DEFAULT 0,
    result_json JSON NULL,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_bulk_operations (organization_id, created_at, status),
    CONSTRAINT fk_bulk_operation_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_bulk_operation_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS official_print_runs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    print_type VARCHAR(50) NOT NULL,
    sequence_number BIGINT UNSIGNED NOT NULL,
    title VARCHAR(190) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status ENUM('DRAFT','GENERATED','VALIDATED','LOCKED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    row_count INT UNSIGNED NOT NULL DEFAULT 0,
    file_path VARCHAR(500) NULL,
    file_sha256 CHAR(64) NULL,
    professional_validation_reference VARCHAR(190) NULL,
    generated_by BIGINT UNSIGNED NULL,
    validated_by BIGINT UNSIGNED NULL,
    validated_at DATETIME NULL,
    locked_at DATETIME NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_official_print_sequence (organization_id, print_type, sequence_number),
    KEY idx_official_print_period (organization_id, print_type, period_start, period_end),
    CONSTRAINT fk_official_print_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_official_print_generated_user FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_official_print_validated_user FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS compliance_filing_runs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    filing_type VARCHAR(50) NOT NULL,
    period_year SMALLINT UNSIGNED NOT NULL,
    period_code VARCHAR(30) NULL,
    status ENUM('DRAFT','REVIEW','VALIDATED','READY','SUBMITTED','ACCEPTED','REJECTED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    schema_version VARCHAR(50) NULL,
    data_json JSON NULL,
    anomaly_count INT UNSIGNED NOT NULL DEFAULT 0,
    professional_validation_reference VARCHAR(190) NULL,
    output_path VARCHAR(500) NULL,
    output_sha256 CHAR(64) NULL,
    external_reference VARCHAR(190) NULL,
    submission_endpoint_id BIGINT UNSIGNED NULL,
    created_by BIGINT UNSIGNED NULL,
    validated_by BIGINT UNSIGNED NULL,
    validated_at DATETIME NULL,
    submitted_at DATETIME NULL,
    accepted_at DATETIME NULL,
    locked_at DATETIME NULL,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_compliance_filing (organization_id, filing_type, period_year, status),
    CONSTRAINT fk_compliance_filing_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_compliance_filing_endpoint FOREIGN KEY (submission_endpoint_id) REFERENCES api_endpoint_configs(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_filing_created_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_compliance_filing_validated_user FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bank_statement_imports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    bank_account_id BIGINT UNSIGNED NOT NULL,
    source_format ENUM('CSV','CAMT053','MT940') NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    status ENUM('STAGED','IMPORTED','PARTIAL','FAILED','ROLLED_BACK') NOT NULL DEFAULT 'STAGED',
    rows_count INT UNSIGNED NOT NULL DEFAULT 0,
    imported_count INT UNSIGNED NOT NULL DEFAULT 0,
    duplicate_count INT UNSIGNED NOT NULL DEFAULT 0,
    error_count INT UNSIGNED NOT NULL DEFAULT 0,
    error_json JSON NULL,
    imported_by BIGINT UNSIGNED NULL,
    imported_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bank_statement_checksum (organization_id, bank_account_id, checksum_sha256),
    KEY idx_bank_statement_status (organization_id, status, created_at),
    CONSTRAINT fk_bank_statement_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_statement_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_statement_user FOREIGN KEY (imported_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE bank_transactions
    ADD COLUMN bank_statement_import_id BIGINT UNSIGNED NULL AFTER source_import_batch_id,
    ADD KEY idx_bank_transaction_statement (organization_id, bank_statement_import_id),
    ADD CONSTRAINT fk_bank_transaction_statement FOREIGN KEY (bank_statement_import_id) REFERENCES bank_statement_imports(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS bank_reconciliation_suggestions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    bank_transaction_id BIGINT UNSIGNED NOT NULL,
    payment_id BIGINT UNSIGNED NULL,
    open_item_id BIGINT UNSIGNED NULL,
    confidence_score DECIMAL(5,2) NOT NULL DEFAULT 0,
    reason_json JSON NULL,
    status ENUM('PROPOSED','ACCEPTED','REJECTED','EXPIRED') NOT NULL DEFAULT 'PROPOSED',
    reviewed_by BIGINT UNSIGNED NULL,
    reviewed_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_bank_suggestion_payment (organization_id, bank_transaction_id, payment_id),
    UNIQUE KEY uq_bank_suggestion_open_item (organization_id, bank_transaction_id, open_item_id),
    KEY idx_bank_suggestion_status (organization_id, status, confidence_score),
    CONSTRAINT fk_bank_suggestion_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_suggestion_transaction FOREIGN KEY (bank_transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_suggestion_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_suggestion_open_item FOREIGN KEY (open_item_id) REFERENCES accounting_open_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_bank_suggestion_user FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
