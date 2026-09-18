CREATE TABLE IF NOT EXISTS custom_domains (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hostname VARCHAR(253) NOT NULL,
    cname_target VARCHAR(253) NOT NULL,
    canonical_domain VARCHAR(253) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_DNS',
    plesk_alias_id BIGINT UNSIGNED NULL,
    dns_verified_at DATETIME NULL,
    alias_provisioned_at DATETIME NULL,
    ssl_issued_at DATETIME NULL,
    ssl_expires_at DATETIME NULL,
    certificate_issuer VARCHAR(255) NULL,
    last_checked_at DATETIME NULL,
    last_error TEXT NULL,
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_custom_domains_hostname (hostname),
    KEY idx_custom_domains_status (status),
    CONSTRAINT fk_custom_domains_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_custom_domains_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS custom_domain_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    custom_domain_id BIGINT UNSIGNED NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    detail TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_custom_domain_events_domain (custom_domain_id, id),
    CONSTRAINT fk_custom_domain_events_domain FOREIGN KEY (custom_domain_id) REFERENCES custom_domains(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
