ALTER TABLE licenses
    ADD COLUMN remote_license_id VARCHAR(190) NULL AFTER id,
    ADD COLUMN next_sync_at DATETIME NULL AFTER last_sync_at,
    ADD COLUMN deactivated_at DATETIME NULL AFTER grace_until;

CREATE UNIQUE INDEX uq_license_sync_request ON license_sync_logs (request_id);

CREATE TABLE IF NOT EXISTS license_module_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    license_id BIGINT UNSIGNED NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    active_in_current_plan TINYINT(1) NOT NULL DEFAULT 1,
    first_entitled_at DATETIME NOT NULL,
    last_entitled_at DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_license_module_history (license_id, module_key),
    KEY idx_license_module_history_read (license_id, module_key, active_in_current_plan),
    CONSTRAINT fk_license_module_history_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
