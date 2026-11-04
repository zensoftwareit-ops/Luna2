ALTER TABLE accounting_adjustment_schedules
    MODIFY COLUMN adjustment_type ENUM(
        'ACCRUAL','DEFERRAL','PREPAID','DEFERRED_INCOME',
        'ACCRUED_INCOME','INVOICE_TO_RECEIVE','INVOICE_TO_ISSUE','OTHER'
    ) NOT NULL,
    ADD COLUMN source_journal_entry_id BIGINT UNSIGNED NULL AFTER organization_id,
    ADD COLUMN source_line_number INT UNSIGNED NULL AFTER source_journal_entry_id,
    ADD COLUMN calculation_method ENUM('DAILY','MANUAL') NOT NULL DEFAULT 'DAILY' AFTER amount,
    ADD UNIQUE KEY uq_adjustment_source_line (organization_id, source_journal_entry_id, source_line_number),
    ADD KEY idx_adjustment_source_entry (source_journal_entry_id),
    ADD CONSTRAINT fk_adjustment_source_entry FOREIGN KEY (source_journal_entry_id)
        REFERENCES journal_entries(id) ON DELETE CASCADE;
