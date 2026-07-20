ALTER TABLE vat_movements
    ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER document_id,
    ADD COLUMN description VARCHAR(255) NULL AFTER counterparty_name,
    ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER period_month,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

ALTER TABLE vat_settlements
    ADD COLUMN calculated_at DATETIME NULL AFTER payment_date;

CREATE TABLE IF NOT EXISTS vat_settlement_details (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    settlement_id BIGINT UNSIGNED NOT NULL,
    register_type ENUM('SALES','PURCHASES','CORRISPETTIVI') NOT NULL,
    vat_code VARCHAR(20) NOT NULL,
    taxable_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    deductible_vat DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_vat_settlement_detail (settlement_id, register_type, vat_code),
    KEY idx_vat_settlement_detail_org (organization_id, settlement_id),
    CONSTRAINT fk_vat_settlement_detail_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_vat_settlement_detail_settlement FOREIGN KEY (settlement_id) REFERENCES vat_settlements(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
