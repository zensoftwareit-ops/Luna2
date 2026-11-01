ALTER TABLE sdi_transmissions
    MODIFY COLUMN status ENUM('QUEUED','SENDING','SENT','ACCEPTED_BY_SDI','DELIVERED','NOT_DELIVERED','REJECTED','REJECTED_BY_SDI','TERMS_EXPIRED','DELIVERY_IMPOSSIBLE','SEND_FAILED','SDI_ERROR','ERROR') NOT NULL DEFAULT 'QUEUED',
    ADD COLUMN external_id CHAR(36) NULL AFTER checksum_sha256,
    ADD COLUMN sdi_id VARCHAR(20) NULL AFTER external_id,
    ADD COLUMN sdi_received_at DATETIME NULL AFTER sent_at,
    ADD COLUMN last_synced_at DATETIME NULL AFTER sdi_received_at,
    ADD UNIQUE KEY uq_sdi_external (organization_id, external_id),
    ADD KEY idx_sdi_identifier (organization_id, sdi_id);

ALTER TABLE sdi_notifications
    ADD COLUMN external_id CHAR(36) NULL AFTER sdi_transmission_id,
    ADD COLUMN resulting_status VARCHAR(50) NULL AFTER notification_type,
    ADD UNIQUE KEY uq_sdi_notification_external (organization_id, external_id);
