ALTER TABLE official_print_runs ADD COLUMN snapshot_json LONGTEXT NULL;
ALTER TABLE official_print_runs ADD COLUMN snapshot_sha256 CHAR(64) NULL;
ALTER TABLE official_print_runs ADD COLUMN first_page BIGINT UNSIGNED NULL;
ALTER TABLE official_print_runs ADD COLUMN page_count INT UNSIGNED NULL;
ALTER TABLE vat_settlements ADD COLUMN payment_reference VARCHAR(190) NULL;
ALTER TABLE vat_movements ADD COLUMN document_reference VARCHAR(100) NULL;
ALTER TABLE vat_movements ADD COLUMN document_reference_date DATE NULL;

CREATE TABLE IF NOT EXISTS fixed_asset_register_years (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    fixed_asset_id BIGINT UNSIGNED NOT NULL,
    fiscal_year SMALLINT UNSIGNED NOT NULL,
    original_cost DECIMAL(15,2) NOT NULL,
    revaluations DECIMAL(15,2) NOT NULL DEFAULT 0,
    writedowns DECIMAL(15,2) NOT NULL DEFAULT 0,
    civil_opening_fund DECIMAL(15,2) NOT NULL DEFAULT 0,
    tax_opening_fund DECIMAL(15,2) NOT NULL DEFAULT 0,
    civil_rate DECIMAL(7,4) NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    civil_quota DECIMAL(15,2) NOT NULL DEFAULT 0,
    tax_quota DECIMAL(15,2) NOT NULL DEFAULT 0,
    disposal_date DATE NULL,
    disposal_proceeds DECIMAL(15,2) NULL,
    evidence_reference VARCHAR(500) NOT NULL,
    updated_by BIGINT UNSIGNED NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_asset_register_year (organization_id, fixed_asset_id, fiscal_year),
    CONSTRAINT fk_asset_register_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_asset_register_asset FOREIGN KEY (fixed_asset_id) REFERENCES fixed_assets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
