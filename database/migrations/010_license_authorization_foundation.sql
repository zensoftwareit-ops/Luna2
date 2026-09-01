CREATE TABLE IF NOT EXISTS instance_identity (
    id TINYINT UNSIGNED NOT NULL PRIMARY KEY,
    installation_uuid CHAR(36) NOT NULL,
    instance_id VARCHAR(100) NOT NULL,
    canonical_domain VARCHAR(255) NULL,
    environment VARCHAR(30) NOT NULL DEFAULT 'production',
    software_version VARCHAR(50) NULL,
    state ENUM('ACTIVE','MOVED','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    installed_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_instance_uuid (installation_uuid),
    UNIQUE KEY uq_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS licenses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    license_key_ciphertext TEXT NULL,
    license_key_hash CHAR(64) NULL,
    license_key_fingerprint VARCHAR(32) NULL,
    status ENUM('UNCONFIGURED','ACTIVE','PAST_DUE','SUSPENDED','EXPIRED','REVOKED') NOT NULL DEFAULT 'UNCONFIGURED',
    plan_code VARCHAR(100) NULL,
    max_users INT UNSIGNED NULL,
    max_organizations INT UNSIGNED NOT NULL DEFAULT 1,
    licensed_organization_id BIGINT UNSIGNED NULL,
    modules_json JSON NOT NULL,
    signed_payload_json JSON NULL,
    signature TEXT NULL,
    signature_version VARCHAR(30) NULL,
    bound_instance_uuid CHAR(36) NULL,
    bound_domain VARCHAR(255) NULL,
    issued_at DATETIME NULL,
    valid_from DATETIME NULL,
    valid_until DATETIME NULL,
    last_sync_at DATETIME NULL,
    last_validated_at DATETIME NULL,
    grace_until DATETIME NULL,
    consecutive_sync_failures INT UNSIGNED NOT NULL DEFAULT 0,
    last_sync_error VARCHAR(1000) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_license_active (active, id),
    KEY idx_license_status (status, valid_until),
    CONSTRAINT fk_license_organization FOREIGN KEY (licensed_organization_id) REFERENCES organizations(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_modules (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    license_id BIGINT UNSIGNED NOT NULL,
    module_key VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    limits_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_license_module (license_id, module_key),
    CONSTRAINT fk_license_module_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_sync_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    license_id BIGINT UNSIGNED NULL,
    request_id VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    http_status SMALLINT UNSIGNED NULL,
    result ENUM('SUCCESS','FAILED','REJECTED') NOT NULL,
    duration_ms INT UNSIGNED NULL,
    error_code VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_license_sync_created (created_at, result),
    CONSTRAINT fk_license_sync_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_overrides (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    license_id BIGINT UNSIGNED NULL,
    module_key VARCHAR(100) NULL,
    effect ENUM('ALLOW','DENY') NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    revoked_at DATETIME NULL,
    revoked_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_license_override_validity (valid_from, valid_until, revoked_at),
    CONSTRAINT fk_license_override_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE,
    CONSTRAINT fk_license_override_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_license_override_revoker FOREIGN KEY (revoked_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
