-- ================================================================
-- MIGRAZIONE ESEMPIO: Aggiunta colonna status_notes
-- ================================================================
-- Questo file è un esempio di migrazione idempotente.
-- Durante il deploy, viene applicato automaticamente a tutte le istanze.
-- 
-- IMPORTANTE: Usa sempre IF NOT EXISTS / IF EXISTS per idempotenza
-- (così se la migrazione è già stata applicata, non dà errore)
-- ================================================================

-- Aggiungi colonna status_notes alla tabella noleggio_lead
ALTER TABLE noleggio_lead 
ADD COLUMN IF NOT EXISTS status_notes TEXT COMMENT 'Note sullo stato del lead';

-- Aggiungi indice su campo email (se non esiste già)
CREATE INDEX IF NOT EXISTS idx_noleggio_lead_email 
ON noleggio_lead(email);

-- Esempio: Aggiungi tabella di audit
CREATE TABLE IF NOT EXISTS noleggio_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL COMMENT 'Tipo entità (lead, preventivo, ordine)',
    entity_id BIGINT NOT NULL COMMENT 'ID entità',
    action VARCHAR(50) NOT NULL COMMENT 'Azione (created, updated, deleted)',
    user_id BIGINT COMMENT 'ID utente che ha fatto azione',
    changes JSON COMMENT 'Dettagli modifiche in formato JSON',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Log di audit per tracciare modifiche';

-- ================================================================
-- NOTA: Durante il deploy, questo file viene eseguito con:
--   docker exec mysql-{domain} mysql -uluna2 -pluna2pass luna2 < 001_example_migration.sql
--
-- Se fallisce (es. colonna già esistente), viene loggato come WARN ma il deploy continua.
-- ================================================================
