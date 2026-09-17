ALTER TABLE vat_movements
    MODIFY COLUMN operation_type ENUM('DOMESTIC','REVERSE_CHARGE','SELF_INVOICE','SPLIT_PAYMENT','CASH','INTRA_EU','EXTRA_EU','MARGIN','EXEMPT','NON_TAXABLE','ADJUSTMENT') NOT NULL DEFAULT 'DOMESTIC',
    ADD COLUMN source_key CHAR(64) NULL AFTER source_import_batch_id,
    ADD UNIQUE KEY uq_vat_movement_source (organization_id, source_type, source_key);
