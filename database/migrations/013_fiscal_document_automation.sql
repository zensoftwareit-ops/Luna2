ALTER TABLE customers ADD COLUMN payment_days SMALLINT UNSIGNED NOT NULL DEFAULT 30 AFTER payment_terms;
ALTER TABLE customers ADD COLUMN payment_month_end TINYINT(1) NOT NULL DEFAULT 0 AFTER payment_days;
ALTER TABLE customers ADD COLUMN payment_method_code VARCHAR(8) NULL AFTER payment_month_end;
ALTER TABLE customers ADD COLUMN bank_name VARCHAR(190) NULL AFTER iban;
ALTER TABLE customers ADD COLUMN bank_abi VARCHAR(5) NULL AFTER bank_name;
ALTER TABLE customers ADD COLUMN bank_cab VARCHAR(5) NULL AFTER bank_abi;

ALTER TABLE suppliers ADD COLUMN payment_days SMALLINT UNSIGNED NOT NULL DEFAULT 30 AFTER payment_terms;
ALTER TABLE suppliers ADD COLUMN payment_month_end TINYINT(1) NOT NULL DEFAULT 0 AFTER payment_days;
ALTER TABLE suppliers ADD COLUMN payment_method_code VARCHAR(8) NULL AFTER payment_month_end;
ALTER TABLE suppliers ADD COLUMN bank_name VARCHAR(190) NULL AFTER iban;
ALTER TABLE suppliers ADD COLUMN bank_abi VARCHAR(5) NULL AFTER bank_name;
ALTER TABLE suppliers ADD COLUMN bank_cab VARCHAR(5) NULL AFTER bank_abi;
ALTER TABLE suppliers ADD COLUMN withholding_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER payment_method_code;
ALTER TABLE suppliers ADD COLUMN withholding_type VARCHAR(16) NOT NULL DEFAULT 'IRPEF' AFTER withholding_enabled;
ALTER TABLE suppliers ADD COLUMN withholding_rate DECIMAL(7,4) NOT NULL DEFAULT 20 AFTER withholding_type;
ALTER TABLE suppliers ADD COLUMN withholding_taxable_percent DECIMAL(7,4) NOT NULL DEFAULT 100 AFTER withholding_rate;
ALTER TABLE suppliers ADD COLUMN withholding_cause VARCHAR(8) NULL AFTER withholding_taxable_percent;

ALTER TABLE organizations ADD COLUMN withholding_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER iban;
ALTER TABLE organizations ADD COLUMN withholding_type VARCHAR(16) NOT NULL DEFAULT 'RT01' AFTER withholding_enabled;
ALTER TABLE organizations ADD COLUMN withholding_rate DECIMAL(7,4) NOT NULL DEFAULT 20 AFTER withholding_type;
ALTER TABLE organizations ADD COLUMN withholding_taxable_percent DECIMAL(7,4) NOT NULL DEFAULT 100 AFTER withholding_rate;
ALTER TABLE organizations ADD COLUMN withholding_cause VARCHAR(8) NULL AFTER withholding_taxable_percent;

ALTER TABLE documents ADD COLUMN payment_terms_label VARCHAR(100) NULL AFTER payment_method_code;
ALTER TABLE documents ADD COLUMN bank_name VARCHAR(190) NULL AFTER payment_terms_label;
ALTER TABLE documents ADD COLUMN bank_abi VARCHAR(5) NULL AFTER bank_name;
ALTER TABLE documents ADD COLUMN bank_cab VARCHAR(5) NULL AFTER bank_abi;
ALTER TABLE documents ADD COLUMN bank_iban VARCHAR(34) NULL AFTER bank_cab;
ALTER TABLE documents ADD COLUMN withholding_type VARCHAR(16) NULL AFTER withholding_total;
ALTER TABLE documents ADD COLUMN withholding_rate DECIMAL(7,4) NOT NULL DEFAULT 0 AFTER withholding_type;
ALTER TABLE documents ADD COLUMN withholding_taxable_percent DECIMAL(7,4) NOT NULL DEFAULT 100 AFTER withholding_rate;
ALTER TABLE documents ADD COLUMN withholding_cause VARCHAR(8) NULL AFTER withholding_taxable_percent;

ALTER TABLE documents DROP INDEX uq_documents_number;
CREATE UNIQUE INDEX uq_documents_number_party ON documents (organization_id, document_type, fiscal_year, number, counterparty_id);
