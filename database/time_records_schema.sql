-- ============================================================
-- TIME RECORDS / PRESENZE SCHEMA
-- Sprint HR v1
-- ============================================================

CREATE TABLE IF NOT EXISTS time_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  record_date DATE NOT NULL,

  check_in_time DATETIME NULL,
  check_out_time DATETIME NULL,

  absence_type VARCHAR(20) NOT NULL DEFAULT 'NONE',
  hours_worked DECIMAL(5,2) DEFAULT 0,
  hours_overtime DECIMAL(5,2) DEFAULT 0,
  notes VARCHAR(500),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_time_records_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT fk_time_records_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,

  UNIQUE KEY uk_employee_date (employee_id, record_date),
  INDEX idx_time_records_company_employee (company_id, employee_id),
  INDEX idx_time_records_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
