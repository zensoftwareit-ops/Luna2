ALTER TABLE vat_settlement_details
    ADD COLUMN vat_due_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER vat_amount,
    ADD COLUMN non_deductible_vat DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER deductible_vat,
    ADD COLUMN suspended_vat DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER non_deductible_vat;
