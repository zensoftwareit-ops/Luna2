ALTER TABLE accounting_settings
    ADD COLUMN datev_party_reconciliation_version INT UNSIGNED NOT NULL DEFAULT 0 AFTER auto_create_open_items;
