ALTER TABLE import_files ADD COLUMN stored_relative_path VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS datev_reference_records (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    source_file_id BIGINT UNSIGNED NOT NULL,
    source_row_id BIGINT UNSIGNED NOT NULL,
    record_kind VARCHAR(40) NOT NULL,
    source_key VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    payload_json JSON NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    application_status ENUM('APPLIED','REFERENCE','NEEDS_DATA','CONFLICT') NOT NULL,
    application_note TEXT NULL,
    entity_type VARCHAR(64) NULL,
    entity_id BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_datev_source_version (organization_id, record_kind, source_key, content_sha256),
    KEY idx_datev_kind (organization_id, record_kind, application_status),
    CONSTRAINT fk_datev_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_datev_batch FOREIGN KEY (batch_id) REFERENCES import_batches(id) ON DELETE RESTRICT,
    CONSTRAINT fk_datev_file FOREIGN KEY (source_file_id) REFERENCES import_files(id) ON DELETE RESTRICT,
    CONSTRAINT fk_datev_row FOREIGN KEY (source_row_id) REFERENCES import_rows(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS datev_fixed_asset_progressives (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fixed_asset_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    source_key VARCHAR(255) NOT NULL,
    as_of_date DATE NOT NULL,
    historical_cost DECIMAL(15,2) NOT NULL DEFAULT 0,
    current_value DECIMAL(15,2) NOT NULL DEFAULT 0,
    ordinary_fund DECIMAL(15,2) NOT NULL DEFAULT 0,
    accelerated_fund DECIMAL(15,2) NOT NULL DEFAULT 0,
    depreciable_residual DECIMAL(15,2) NOT NULL DEFAULT 0,
    alienation_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    elimination_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_datev_asset_progressive (organization_id, source_key),
    KEY idx_datev_asset_progressive_date (organization_id, fixed_asset_id, as_of_date),
    CONSTRAINT fk_datev_asset_progressive_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_datev_asset_progressive_asset FOREIGN KEY (fixed_asset_id) REFERENCES fixed_assets(id) ON DELETE CASCADE,
    CONSTRAINT fk_datev_asset_progressive_batch FOREIGN KEY (batch_id) REFERENCES import_batches(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS datev_fixed_asset_movements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fixed_asset_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    source_movement_id VARCHAR(100) NOT NULL,
    cause_code VARCHAR(20) NULL,
    movement_date DATE NULL,
    valid_from DATE NULL,
    tax_valid_from DATE NULL,
    document_number VARCHAR(100) NULL,
    description VARCHAR(500) NULL,
    party_code VARCHAR(50) NULL,
    party_name VARCHAR(190) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_datev_asset_movement (organization_id, source_movement_id),
    KEY idx_datev_asset_movement_date (organization_id, fixed_asset_id, movement_date),
    CONSTRAINT fk_datev_asset_movement_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_datev_asset_movement_asset FOREIGN KEY (fixed_asset_id) REFERENCES fixed_assets(id) ON DELETE CASCADE,
    CONSTRAINT fk_datev_asset_movement_batch FOREIGN KEY (batch_id) REFERENCES import_batches(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
