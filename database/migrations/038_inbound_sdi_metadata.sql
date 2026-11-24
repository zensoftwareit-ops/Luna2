ALTER TABLE documents
    ADD COLUMN sdi_id VARCHAR(20) NULL AFTER registration_date,
    ADD COLUMN sdi_received_at DATETIME NULL AFTER sdi_id,
    ADD KEY idx_documents_sdi_id (organization_id, sdi_id),
    ADD KEY idx_documents_sdi_received (organization_id, sdi_received_at);
