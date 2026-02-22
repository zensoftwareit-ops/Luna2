-- ============================================================
-- Luna2 CRM Module - Phase 1 SQL Schema
-- Generated for H2/MySQL/PostgreSQL
-- ============================================================

-- Activity Table
CREATE TABLE IF NOT EXISTS activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    utente_id BIGINT,
    tipo VARCHAR(50),
    titolo VARCHAR(255) NOT NULL,
    descrizione TEXT,
    data_attivita DATETIME NOT NULL,
    stato VARCHAR(50),
    data_prossima_attivita DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id) REFERENCES lead(id),
    FOREIGN KEY (utente_id) REFERENCES users(id),
    INDEX idx_lead_id (lead_id),
    INDEX idx_utente_id (utente_id),
    INDEX idx_stato (stato),
    INDEX idx_data_attivita (data_attivita)
);

-- Task Table
CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    assegnato_a BIGINT,
    creato_da BIGINT,
    descrizione TEXT NOT NULL,
    data_scadenza DATETIME,
    data_completamento DATETIME,
    priorita VARCHAR(50),
    stato VARCHAR(50),
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id) REFERENCES lead(id),
    FOREIGN KEY (assegnato_a) REFERENCES users(id),
    FOREIGN KEY (creato_da) REFERENCES users(id),
    INDEX idx_lead_id (lead_id),
    INDEX idx_assegnato_a (assegnato_a),
    INDEX idx_stato (stato),
    INDEX idx_data_scadenza (data_scadenza)
);

-- StoriaLead Table (Audit trail for lead changes)
CREATE TABLE IF NOT EXISTS storia_lead (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    utente_id BIGINT,
    stage_da VARCHAR(50),
    stage_a VARCHAR(50),
    motivo TEXT,
    data_cambio DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id) REFERENCES lead(id),
    FOREIGN KEY (utente_id) REFERENCES users(id),
    INDEX idx_lead_id (lead_id),
    INDEX idx_data_cambio (data_cambio)
);

-- Reminder Table
CREATE TABLE IF NOT EXISTS reminder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lead_id BIGINT,
    utente_id BIGINT,
    activity_id BIGINT,
    task_id BIGINT,
    tipo VARCHAR(50),
    titolo VARCHAR(255) NOT NULL,
    descrizione TEXT,
    data_notifica DATETIME NOT NULL,
    stato VARCHAR(50),
    letto BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lead_id) REFERENCES lead(id),
    FOREIGN KEY (utente_id) REFERENCES users(id),
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (task_id) REFERENCES task(id),
    INDEX idx_lead_id (lead_id),
    INDEX idx_utente_id (utente_id),
    INDEX idx_data_notifica (data_notifica),
    INDEX idx_stato (stato),
    INDEX idx_letto (letto)
);

-- PipelineStage Table
CREATE TABLE IF NOT EXISTS pipeline_stage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descrizione TEXT,
    sequenza INT,
    colore VARCHAR(20),
    icon_class VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sequenza (sequenza)
);

-- ============================================================
-- Pre-populate pipeline stages
-- ============================================================
INSERT INTO pipeline_stage (nome, descrizione, sequenza, colore, icon_class) VALUES
    ('BOZZA', 'Nuovo lead in fase di raccolta dati', 1, '#FF6B6B', 'fas fa-file-alt'),
    ('QUALIFICATO', 'Lead qualificato e idoneo per proseguire', 2, '#4ECDC4', 'fas fa-check-circle'),
    ('PROPOSTA', 'Proposta commerciale inviata', 3, '#45B7D1', 'fas fa-file-invoice'),
    ('NEGOZIAZIONE', 'In fase di negoziazione', 4, '#FFA07A', 'fas fa-handshake'),
    ('VINTO', 'Chiuso vinto - Contratto acquisito', 5, '#95E1D3', 'fas fa-trophy'),
    ('PERSO', 'Chiuso perso - Opportunità terminata', 6, '#A0AEC0', 'fas fa-times-circle')
ON DUPLICATE KEY UPDATE nome=VALUES(nome);

-- ============================================================
-- Indexes for performance
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_activity_lead_tipo ON activity(lead_id, tipo);
CREATE INDEX IF NOT EXISTS idx_task_stato_scadenza ON task(stato, data_scadenza);
CREATE INDEX IF NOT EXISTS idx_reminder_utente_letto ON reminder(utente_id, letto);
CREATE INDEX IF NOT EXISTS idx_storia_lead_cambio ON storia_lead(lead_id, data_cambio);

-- ============================================================
-- Views for analytics
-- ============================================================
CREATE OR REPLACE VIEW vw_lead_pipeline_summary AS
SELECT 
    ps.nome as stage,
    ps.colore,
    COUNT(DISTINCT l.id) as count_leads,
    AVG(l.probabilita_chiusura) as avg_probability,
    SUM(l.budget_stimato) as total_value
FROM lead l
LEFT JOIN pipeline_stage ps ON l.stato = ps.nome
GROUP BY ps.nome, ps.colore, ps.sequenza
ORDER BY ps.sequenza;

CREATE OR REPLACE VIEW vw_activity_summary AS
SELECT 
    a.tipo,
    COUNT(*) as total_activities,
    SUM(CASE WHEN a.stato = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
    SUM(CASE WHEN a.stato = 'PENDING' THEN 1 ELSE 0 END) as pending
FROM activity a
GROUP BY a.tipo;
