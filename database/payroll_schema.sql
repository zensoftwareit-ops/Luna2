-- Payroll foundation schema (v1)
CREATE TABLE IF NOT EXISTS payroll_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    revision INT NOT NULL DEFAULT 1,
    total_gross DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_net DECIMAL(12,2) NOT NULL DEFAULT 0,
    confirmed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_payroll_runs_period UNIQUE (company_id, month, year),
    CONSTRAINT fk_payroll_runs_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS revision INT NOT NULL DEFAULT 1;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS payroll_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_run_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_name VARCHAR(200) NULL,
    hours_worked DECIMAL(8,2) NOT NULL DEFAULT 0,
    hours_overtime DECIMAL(8,2) NOT NULL DEFAULT 0,
    gross_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    social_security_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    insurance_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    allowance_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    deduction_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_payroll_details_run_employee UNIQUE (payroll_run_id, employee_id),
    CONSTRAINT fk_payroll_details_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_details_employee FOREIGN KEY (employee_id) REFERENCES users(id),
    INDEX idx_payroll_details_run (payroll_run_id),
    INDEX idx_payroll_details_employee (employee_id)
);

ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS employee_name VARCHAR(200) NULL;
ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS hours_overtime DECIMAL(8,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS allowance_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS deduction_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS social_security_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payroll_details ADD COLUMN IF NOT EXISTS insurance_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS payroll_employee_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 15.00,
    overtime_rate_multiplier DECIMAL(6,2) NOT NULL DEFAULT 1.30,
    tax_rate DECIMAL(5,4) NOT NULL DEFAULT 0.2000,
    social_security_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0919,
    insurance_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0100,
    fixed_allowance DECIMAL(10,2) NOT NULL DEFAULT 0,
    fixed_deduction DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_payroll_config_company_employee UNIQUE (company_id, employee_id),
    CONSTRAINT fk_payroll_config_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_payroll_config_employee FOREIGN KEY (employee_id) REFERENCES users(id)
);

ALTER TABLE payroll_employee_configs ADD COLUMN IF NOT EXISTS social_security_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0919;
ALTER TABLE payroll_employee_configs ADD COLUMN IF NOT EXISTS insurance_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0100;
