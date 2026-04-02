-- ============================================================
-- APPROVAL WORKFLOW DATABASE SCHEMA
-- Sprint 1: Q2 2026 - Workflow Approvazione
-- ============================================================

-- Tabella: approval_requests (Richieste di approvazione)
CREATE TABLE IF NOT EXISTS approval_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  requester_id BIGINT NOT NULL COMMENT 'ID dipendente che richiede',
  approver_id BIGINT COMMENT 'ID manager che approva',
  request_type VARCHAR(50) NOT NULL COMMENT 'FERIE, PERMESSO, SPESA',
  
  -- Dettagli richiesta
  description TEXT,
  
  -- Date specifiche per tipo
  start_date DATE COMMENT 'Data inizio ferie/permesso',
  end_date DATE COMMENT 'Data fine ferie/permesso',
  days_requested INT COMMENT 'Giorni richiesti',
  amount_requested DECIMAL(10,2) COMMENT 'Importo per spese',
  
  -- Status workflow
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT, SUBMITTED, APPROVED, REJECTED, COMPLETED',
  approval_date DATETIME COMMENT 'Timestamp approvazione/rifiuto',
  rejection_reason TEXT COMMENT 'Motivo rifiuto',
  
  -- Tracking
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  submitted_date DATETIME COMMENT 'Quando sottoposto per approvazione',
  updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (approver_id) REFERENCES users(id) ON DELETE SET NULL,
  
  INDEX idx_status (status),
  INDEX idx_requester (requester_id),
  INDEX idx_approver (approver_id),
  INDEX idx_company_status (company_id, status),
  INDEX idx_dates (start_date, end_date),
  INDEX idx_created (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Richieste di approvazione: ferie, permessi, spese';

-- Tabella: approval_history (Audit trail approvazioni)
CREATE TABLE IF NOT EXISTS approval_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  action VARCHAR(50) COMMENT 'CREATED, SUBMITTED, APPROVED, REJECTED, CANCELLED',
  actor_id BIGINT NOT NULL COMMENT 'ID utente che ha eseguito l''azione',
  action_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  notes TEXT COMMENT 'Note sull''azione',
  
  FOREIGN KEY (request_id) REFERENCES approval_requests(id) ON DELETE CASCADE,
  FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE,
  
  INDEX idx_request (request_id),
  INDEX idx_actor (actor_id),
  INDEX idx_action_date (action_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Storico di tutte le azioni su approval_requests';

-- Tabella: vacation_balance (Saldo ferie per dipendente/anno)
CREATE TABLE IF NOT EXISTS vacation_balance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  year INT NOT NULL,
  
  -- Ferie da contratto (es. CCNL Commercio = 26 gg)
  total_days_entitled INT COMMENT 'Giorni ferie totali anno',
  
  -- Usate (calcolate da approvazioni)
  days_used INT DEFAULT 0 COMMENT 'Giorni già usati',
  
  -- Ferie riportate da anno precedente
  carried_over_days INT DEFAULT 0 COMMENT 'Giorni riportati da anno precedente',
  
  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE,
  
  UNIQUE KEY uk_employee_year (employee_id, year),
  INDEX idx_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Saldo ferie per dipendente per anno';

-- Tabella: approval_templates (Template di approvazione per tipi)
CREATE TABLE IF NOT EXISTS approval_templates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT NOT NULL,
  request_type VARCHAR(50) NOT NULL COMMENT 'FERIE, PERMESSO, SPESA',
  
  -- Regole template
  requires_reason BOOLEAN DEFAULT FALSE COMMENT 'Richiede motivazione',
  max_days_per_request INT COMMENT 'Max giorni per richiesta (ferie)',
  max_amount DECIMAL(10,2) COMMENT 'Max importo (spese)',
  auto_approve_within_days INT COMMENT 'Auto-approva se within X giorni',
  requires_director_approval BOOLEAN DEFAULT FALSE COMMENT 'Richiede approvazione direttore',
  
  created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  
  UNIQUE KEY uk_template (company_id, request_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Template regole approvazione per tipo di richiesta';

-- ============================================================
-- SAMPLE DATA (SEEDING)
-- ============================================================

-- Popola vacation_balance per CCNL Commercio (26 giorni ferie)
INSERT IGNORE INTO vacation_balance (company_id, employee_id, year, total_days_entitled, days_used, carried_over_days)
SELECT 
    1 as company_id,
    u.id as employee_id,
    YEAR(CURDATE()) as year,
    26 as total_days_entitled,
    0 as days_used,
    0 as carried_over_days
FROM users u
WHERE u.role = 'EMPLOYEE' 
LIMIT 50;

-- Template FERIE
INSERT IGNORE INTO approval_templates (company_id, request_type, requires_reason, max_days_per_request, auto_approve_within_days, requires_director_approval)
VALUES (1, 'FERIE', FALSE, 30, NULL, FALSE);

-- Template PERMESSO
INSERT IGNORE INTO approval_templates (company_id, request_type, requires_reason, max_days_per_request, auto_approve_within_days, requires_director_approval)
VALUES (1, 'PERMESSO', TRUE, 3, NULL, FALSE);

-- Template SPESA
INSERT IGNORE INTO approval_templates (company_id, request_type, requires_reason, max_amount, auto_approve_within_days, requires_director_approval)
VALUES (1, 'SPESA', TRUE, 500.00, 5, FALSE);

-- ============================================================
-- VIEWS (Optional - per report e dashboard)
-- ============================================================

-- View: Richieste pending per manager
CREATE OR REPLACE VIEW v_pending_approvals AS
SELECT 
    ar.id,
    ar.company_id,
    ar.requester_id,
    ar.approver_id,
    ar.request_type,
    ar.days_requested,
    ar.amount_requested,
    ar.start_date,
    ar.end_date,
    ar.submitted_date,
    DATEDIFF(CURDATE(), DATE(ar.submitted_date)) as days_pending,
    u.name as requester_name,
    u.email as requester_email
FROM approval_requests ar
JOIN users u ON ar.requester_id = u.id
WHERE ar.status = 'SUBMITTED'
ORDER BY ar.submitted_date ASC;

-- View: Saldo ferie per dipendente (anno corrente)
CREATE OR REPLACE VIEW v_vacation_status AS
SELECT 
    vb.company_id,
    vb.employee_id,
    u.name as employee_name,
    vb.total_days_entitled,
    vb.days_used,
    (vb.total_days_entitled - vb.days_used + vb.carried_over_days) as days_remaining,
    vb.year,
    CASE 
        WHEN (vb.total_days_entitled - vb.days_used) <= 0 THEN 'ESAURITO'
        WHEN (vb.total_days_entitled - vb.days_used) <= 5 THEN 'CRITICO'
        ELSE 'OK'
    END as status
FROM vacation_balance vb
JOIN users u ON vb.employee_id = u.id
WHERE vb.year = YEAR(CURDATE())
ORDER BY days_remaining ASC;

-- View: Statistics approvazioni per manager
CREATE OR REPLACE VIEW v_approval_stats AS
SELECT 
    ar.approver_id,
    u.name as manager_name,
    COUNT(*) as total_requests,
    SUM(CASE WHEN ar.status = 'SUBMITTED' THEN 1 ELSE 0 END) as pending_count,
    SUM(CASE WHEN ar.status = 'APPROVED' THEN 1 ELSE 0 END) as approved_count,
    SUM(CASE WHEN ar.status = 'REJECTED' THEN 1 ELSE 0 END) as rejected_count,
    ROUND(SUM(CASE WHEN ar.status = 'APPROVED' THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) as approval_rate
FROM approval_requests ar
JOIN users u ON ar.approver_id = u.id
WHERE ar.status IN ('APPROVED', 'REJECTED', 'SUBMITTED')
GROUP BY ar.approver_id, u.name
ORDER BY pending_count DESC;

-- ============================================================
-- INDEXES AGGIUNTIVI PER PERFORMANCE
-- ============================================================

-- Indice composito per query comuni
ALTER TABLE approval_requests ADD INDEX idx_company_requester_status (company_id, requester_id, status);
ALTER TABLE approval_requests ADD INDEX idx_company_approver_status (company_id, approver_id, status);

-- ============================================================
-- END OF SCHEMA
-- ============================================================
