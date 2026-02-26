-- Luna2 Broker Noleggio Auto - Database Schema
-- MySQL 8.0+ - Complete rental car management system
-- 5 Phases + NBT (short-term rental) module

USE luna2;

-- ============== PHASE 1: PREVENTIVAZIONE (quotation) ==============

CREATE TABLE IF NOT EXISTS noleggio_lead (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_pratica VARCHAR(50) NOT NULL UNIQUE,
    
    -- Customer Info
    cliente_id BIGINT,
    ragione_sociale VARCHAR(255) NOT NULL,
    nome_contatto VARCHAR(100),
    cognome_contatto VARCHAR(100),
    email VARCHAR(100),
    telefono VARCHAR(30),
    
    -- Vehicle Requirements
    utilizzo_previsto VARCHAR(100),
    km_annuali_previsti INT,
    data_inizio_noleggio DATE,
    durata_mesi INT,
    priorita_marca VARCHAR(100),
    priorita_modello VARCHAR(100),
    budget_massimo DECIMAL(15,2),
    
    -- Lead Status (Phase tracking)
    fase ENUM('PREVENTIVAZIONE', 'ISTRUTTORIA', 'ORDINE', 'POST_VENDITA') NOT NULL DEFAULT 'PREVENTIVAZIONE',
    referrer VARCHAR(100),
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_assegnato_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_numero_pratica (numero_pratica),
    INDEX idx_fase (fase),
    INDEX idx_data_creazione (data_creazione),
    INDEX idx_cliente_id (cliente_id),
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_assegnato_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Quotations (preventivi)
CREATE TABLE IF NOT EXISTS noleggio_preventivo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    numero_preventivo VARCHAR(50) NOT NULL UNIQUE,
    
    -- Vehicle Details
    marca VARCHAR(100),
    modello VARCHAR(100),
    versione VARCHAR(100),
    colore VARCHAR(50),
    
    -- Quote Pricing
    prezzo_acquisto DECIMAL(15,2),
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    prezzo_netto DECIMAL(15,2),
    tassa_immatricolazione DECIMAL(15,2),
    costo_trasporto DECIMAL(15,2),
    costo_allestimenti DECIMAL(15,2),
    
    -- Rental Terms
    durata_mesi INT,
    rata_mensile DECIMAL(15,2),
    chilometri_inclusi INT,
    sovrapprezzo_km DECIMAL(5,2),
    costo_manutenzione_mensile DECIMAL(10,2),
    costo_assicurazione_mensile DECIMAL(10,2),
    costo_pneumatici DECIMAL(10,2),
    
    -- Quote Status
    status ENUM('BOZZA', 'INVIATO', 'ACCETTATO', 'RIFIUTATO', 'SOSTITUITO') NOT NULL DEFAULT 'BOZZA',
    data_invio DATETIME,
    data_accettazione DATETIME,
    data_scadenza DATE,
    versione_numero INT DEFAULT 1,
    follow_up_48h_inviato BOOLEAN DEFAULT FALSE,
    
    -- Audit
    utente_creazione_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_lead_id (lead_id),
    INDEX idx_numero_preventivo (numero_preventivo),
    INDEX idx_status (status),
    INDEX idx_data_creazione (data_creazione),
    FOREIGN KEY (lead_id) REFERENCES noleggio_lead(id) ON DELETE RESTRICT,
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== PHASE 2: ISTRUTTORIA (document review & evaluation) ==============

-- Required Documents Checklist
CREATE TABLE IF NOT EXISTS noleggio_documento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    
    -- Document Type
    tipo_documento VARCHAR(100) NOT NULL,
    descrizione VARCHAR(255),
    obbligatorio BOOLEAN DEFAULT TRUE,
    
    -- File Content
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    
    -- Document Status
    status ENUM('RICHIESTO', 'CARICATO', 'VALIDATO', 'RIFIUTATO') NOT NULL DEFAULT 'RICHIESTO',
    data_richiesta DATETIME,
    data_caricamento DATETIME,
    data_validazione DATETIME,
    motivo_rifiuto TEXT,
    reminder_spedito_giorni INT,
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_validazione_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_lead_id (lead_id),
    INDEX idx_status (status),
    INDEX idx_tipo_documento (tipo_documento),
    INDEX idx_data_creazione (data_creazione),
    FOREIGN KEY (lead_id) REFERENCES noleggio_lead(id) ON DELETE CASCADE,
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_validazione_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Financial Evaluation & Risk Scoring
CREATE TABLE IF NOT EXISTS noleggio_valutazione (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BIGINT NOT NULL UNIQUE,
    
    -- Customer Creditworthiness
    rating_creditizio VARCHAR(10),
    score_creditizio INT,
    storico_pagamenti TEXT,
    esposizioni_bancarie BOOLEAN,
    importo_max_fideiussione DECIMAL(15,2),
    
    -- Financial Assessment
    fatturato_annuale DECIMAL(15,2),
    utile_netto_annuale DECIMAL(15,2),
    rating_solvibilita ENUM('ECCELLENTE', 'BUONO', 'ACCETTABILE', 'INSUFFICIENTE') NOT NULL DEFAULT 'BUONO',
    
    -- Risk Evaluation
    fattori_rischio TEXT,
    valutazione_complessiva VARCHAR(100),
    opinione_gestore TEXT,
    
    -- Solicitation Tracking (4-5 day cycle)
    numero_solleciti INT DEFAULT 0,
    data_ultimo_sollecito DATETIME,
    
    -- Approval Status
    status ENUM('BOZZA', 'IN_REVISIONE', 'APPROVATO', 'RIFIUTATO') NOT NULL DEFAULT 'IN_REVISIONE',
    data_approvazione DATETIME,
    data_rifiuto DATETIME,
    motivo_rifiuto TEXT,
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_approvatore_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_lead_id (lead_id),
    INDEX idx_status (status),
    INDEX idx_rating_solvibilita (rating_solvibilita),
    INDEX idx_data_creazione (data_creazione),
    FOREIGN KEY (lead_id) REFERENCES noleggio_lead(id) ON DELETE RESTRICT,
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_approvatore_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== PHASE 3: ORDINE (order management) ==============

CREATE TABLE IF NOT EXISTS noleggio_ordine (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_id BIGINT NOT NULL,
    numero_ordine VARCHAR(50) NOT NULL UNIQUE,
    preventivo_id BIGINT,
    
    -- Vehicle Assignment
    targa VARCHAR(20),
    marca_modello VARCHAR(255),
    vin VARCHAR(50),
    data_immatricolazione DATE,
    colore VARCHAR(50),
    km_attuali INT,
    
    -- Delivery
    eta_consegna DATE,
    data_consegna DATETIME,
    note_consegna TEXT,
    
    -- Order Status (20-30 day care call cycle)
    status ENUM('ORDINE_CREATO', 'IN_LAVORAZIONE', 'CONFERMATO', 'CONSEGNATO', 'ANNULLATO') NOT NULL DEFAULT 'ORDINE_CREATO',
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Care Call Automation (20-30 day cycle)
    ultima_care_call DATETIME,
    prossima_care_call_prevista DATETIME,
    numero_care_call INT DEFAULT 0,
    note_care_call LONGTEXT,
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_assegnato_id BIGINT,
    data_creazione_record TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_lead_id (lead_id),
    INDEX idx_numero_ordine (numero_ordine),
    INDEX idx_status (status),
    INDEX idx_targa (targa),
    INDEX idx_data_creazione (data_creazione),
    INDEX idx_prossima_care_call (prossima_care_call_prevista),
    FOREIGN KEY (lead_id) REFERENCES noleggio_lead(id) ON DELETE RESTRICT,
    FOREIGN KEY (preventivo_id) REFERENCES noleggio_preventivo(id),
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_assegnato_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== PHASE 4: POST-VENDITA (after-sales support) ==============

CREATE TABLE IF NOT EXISTS noleggio_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_ticket VARCHAR(50) NOT NULL UNIQUE,
    contratto_id BIGINT,
    ordine_id BIGINT,
    
    -- Issue Description
    oggetto VARCHAR(255) NOT NULL,
    descrizione LONGTEXT,
    priorita ENUM('BASSA', 'MEDIA', 'ALTA', 'CRITICA') NOT NULL DEFAULT 'MEDIA',
    categoria VARCHAR(100),
    
    -- Assignment & Status
    status ENUM('APERTO', 'IN_LAVORAZIONE', 'IN_ATTESA_CLIENTE', 'RISOLTO', 'CHIUSO') NOT NULL DEFAULT 'APERTO',
    operatore_assegnato_id BIGINT,
    data_assegnazione DATETIME,
    
    -- Resolution
    soluzione_adottata LONGTEXT,
    data_risoluzione DATETIME,
    data_chiusura DATETIME,
    
    -- SLA Tracking
    sla_risposta_ore INT,
    data_scadenza_risposta DATETIME,
    sla_risoluzione_ore INT,
    data_scadenza_risoluzione DATETIME,
    violazione_sla_risposta BOOLEAN DEFAULT FALSE,
    violazione_sla_risoluzione BOOLEAN DEFAULT FALSE,
    
    -- Anti-Bounce Prevention (48-72h window)
    flag_anti_rimbalzo BOOLEAN DEFAULT FALSE,
    data_chiusura_precedente DATETIME,
    motivo_riapertura TEXT,
    numero_riaperture INT DEFAULT 0,
    
    -- Audit
    utente_creazione_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_numero_ticket (numero_ticket),
    INDEX idx_contratto_id (contratto_id),
    INDEX idx_status (status),
    INDEX idx_priorita (priorita),
    INDEX idx_operatore_assegnato_id (operatore_assegnato_id),
    INDEX idx_data_creazione (data_creazione),
    INDEX idx_flag_anti_rimbalzo (flag_anti_rimbalzo),
    -- FOREIGN KEY (contratto_id) REFERENCES noleggio_contratto(id), -- Rimosso per evitare dipendenza circolare
    FOREIGN KEY (ordine_id) REFERENCES noleggio_ordine(id),
    FOREIGN KEY (operatore_assegnato_id) REFERENCES users(id),
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== PHASE 5: SCADENZARIO (maintenance & renewal scheduling) ==============

CREATE TABLE IF NOT EXISTS noleggio_contratto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_contratto VARCHAR(50) NOT NULL UNIQUE,
    lead_id BIGINT NOT NULL,
    ordine_id BIGINT,
    
    -- Contract Terms
    data_inizio DATE NOT NULL,
    data_fine DATE NOT NULL,
    durata_mesi INT,
    
    -- Vehicle Info
    targa VARCHAR(20) NOT NULL,
    vin VARCHAR(50),
    marca_modello VARCHAR(255),
    colore VARCHAR(50),
    potenza_cv INT,
    cilindrata INT,
    emissioni_co2 DECIMAL(5,2),
    
    -- Rental Conditions
    chilometri_inclusi_annuali INT,
    costo_km_extra DECIMAL(5,2),
    scatto_minimo_km DECIMAL(15,2),
    tasso_usura DECIMAL(5,2),
    
    -- Maintenance Schedule
    data_ultima_revisione DATE,
    data_prossima_revisione DATE,
    km_ultima_revisione INT,
    km_prossima_revisione INT,
    
    data_ultimo_tagliando DATE,
    data_prossimo_tagliando DATE,
    km_ultimo_tagliando INT,
    km_prossimo_tagliando INT,
    
    -- Insurance & License
    data_scadenza_assicurazione DATE,
    numero_polizza_assicurazione VARCHAR(50),
    data_scadenza_patente DATE,
    
    -- Contract Status
    status ENUM('ATTIVO', 'IN_SCADENZA', 'SCADUTO', 'RINNOVATO', 'RISOLTO') NOT NULL DEFAULT 'ATTIVO',
    km_attuali INT,
    km_limite INT,
    km_ecceduti INT DEFAULT 0,
    
    -- Calendar Sync (Phase 5)
    evento_rinnovo_id BIGINT,
    evento_revisione_id BIGINT,
    evento_tagliando_id BIGINT,
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_assegnato_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_numero_contratto (numero_contratto),
    INDEX idx_lead_id (lead_id),
    INDEX idx_targa (targa),
    INDEX idx_status (status),
    INDEX idx_data_fine (data_fine),
    INDEX idx_data_prossima_revisione (data_prossima_revisione),
    INDEX idx_data_prossimo_tagliando (data_prossimo_tagliando),
    INDEX idx_data_scadenza_assicurazione (data_scadenza_assicurazione),
    INDEX idx_data_creazione (data_creazione),
    FOREIGN KEY (lead_id) REFERENCES noleggio_lead(id) ON DELETE RESTRICT,
    FOREIGN KEY (ordine_id) REFERENCES noleggio_ordine(id),
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_assegnato_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== NBT MODULE: Short-term rental (1-30 days) ==============

CREATE TABLE IF NOT EXISTS noleggio_nbt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_pratica VARCHAR(50) NOT NULL UNIQUE,
    
    -- Customer & Client
    cliente_id BIGINT,
    ragione_sociale VARCHAR(255),
    nome_contatto VARCHAR(100),
    cognome_contatto VARCHAR(100),
    email VARCHAR(100),
    telefono VARCHAR(30),
    
    -- Rental Period
    data_inizio_prevista DATE,
    data_fine_prevista DATE,
    numero_giorni INT,
    
    -- Vehicle Requirements
    marca_preferita VARCHAR(100),
    modello_preferito VARCHAR(100),
    numero_posti INT,
    aria_condizionata BOOLEAN DEFAULT FALSE,
    automatico BOOLEAN DEFAULT FALSE,
    
    -- Quote & Pricing
    tariffa_giornaliera DECIMAL(10,2),
    numero_giorni_noleggio INT,
    importo_totale DECIMAL(15,2),
    deposito_cauzione DECIMAL(15,2),
    assicurazione_supplementare BOOLEAN DEFAULT FALSE,
    costo_assicurazione DECIMAL(10,2),
    
    -- Vehicle Assigned
    marca_modello_veicolo_assegnato VARCHAR(255),
    targa_veicolo_assegnato VARCHAR(20),
    vin_veicolo_assegnato VARCHAR(50),
    km_iniziali INT,
    km_finali INT,
    costo_extra_km DECIMAL(10,2),
    
    -- Reservation Status
    status ENUM('RICHIESTA', 'PREVENTIVO_INVIATO', 'CONFERMATO', 'VEICOLO_PRONTO', 'AFFIDATA', 'RESTITUITA', 'COMPLETATO', 'ANNULLATO') NOT NULL DEFAULT 'RICHIESTA',
    data_creazione_richiesta DATETIME,
    data_invio_preventivo DATETIME,
    data_conferma_prenotazione DATETIME,
    data_consegna DATETIME,
    data_restituzione_prevista DATETIME,
    data_restituzione_effettiva DATETIME,
    
    -- Delivery Notes
    note_consegna TEXT,
    note_restituzione TEXT,
    
    -- Payment Info
    importo_pagato DECIMAL(15,2),
    metodo_pagamento VARCHAR(50),
    data_pagamento DATETIME,
    nota_pagamento TEXT,
    
    -- Cancellation Tracking
    motivo_annullamento TEXT,
    data_annullamento DATETIME,
    indennizzo_cancellazione DECIMAL(10,2),
    
    -- Follow-up Automation (24h quote validity, return alerts)
    preventivo_valido_fino DATETIME,
    reminder_24h_inviato BOOLEAN DEFAULT FALSE,
    reminder_restituzione_inviato BOOLEAN DEFAULT FALSE,
    
    -- Audit
    utente_creazione_id BIGINT,
    utente_assegnato_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_numero_pratica (numero_pratica),
    INDEX idx_status (status),
    INDEX idx_cliente_id (cliente_id),
    INDEX idx_data_inizio_prevista (data_inizio_prevista),
    INDEX idx_data_fine_prevista (data_fine_prevista),
    INDEX idx_data_creazione (data_creazione),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (utente_creazione_id) REFERENCES users(id),
    FOREIGN KEY (utente_assegnato_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== MODULE ACTIVATION ==============

-- Ensure Broker Auto module is enabled
INSERT INTO module_settings (code, name, description, enabled, updated_at)
VALUES 
    ('CRM_BROKER_AUTO', 'CRM Broker Auto', 'Broker Rental Car Management - 5 Phases + NBT', TRUE, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    enabled = VALUES(enabled),
    updated_at = CURRENT_TIMESTAMP;

-- ============== INDEXES FOR AUTOMATION QUERIES ==============

-- Optimization indexes for automation cron jobs
CREATE INDEX idx_preventivo_follow_up ON noleggio_preventivo(follow_up_48h_inviato, data_invio);
CREATE INDEX idx_documento_status_reminder ON noleggio_documento(status, data_richiesta);
CREATE INDEX idx_valutazione_solleciti ON noleggio_valutazione(numero_solleciti, data_ultimo_sollecito);
CREATE INDEX idx_ordine_care_call ON noleggio_ordine(prossima_care_call_prevista, status);
CREATE INDEX idx_ticket_anti_rimbalzo_finestra ON noleggio_ticket(flag_anti_rimbalzo, data_chiusura_precedente);
CREATE INDEX idx_nbt_followup ON noleggio_nbt(reminder_24h_inviato, data_creazione_richiesta);
CREATE INDEX idx_nbt_restituzione ON noleggio_nbt(reminder_restituzione_inviato, data_fine_prevista);
CREATE INDEX idx_contratto_scadenzario ON noleggio_contratto(data_fine, data_prossima_revisione, data_prossimo_tagliando, data_scadenza_assicurazione);

-- ============== AUDIT TRAIL ==============

-- Create audit log table for all noleggio operations
CREATE TABLE IF NOT EXISTS noleggio_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entita_tipo VARCHAR(100) NOT NULL,
    entita_id BIGINT NOT NULL,
    operazione VARCHAR(50) NOT NULL,
    valori_precedenti LONGTEXT,
    valori_nuovi LONGTEXT,
    utente_id BIGINT,
    data_operazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_entita_tipo (entita_tipo),
    INDEX idx_entita_id (entita_id),
    INDEX idx_operazione (operazione),
    INDEX idx_data_operazione (data_operazione),
    FOREIGN KEY (utente_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============== VIEWS FOR DASHBOARD ==============

-- Pipeline visualization
CREATE OR REPLACE VIEW noleggio_lead_pipeline AS
SELECT 
    fase,
    COUNT(*) as count,
    DATE_FORMAT(data_creazione, '%Y-%m') as mese
FROM noleggio_lead
WHERE data_creazione >= DATE_SUB(NOW(), INTERVAL 90 DAY)
GROUP BY fase, mese
ORDER BY mese DESC, fase;

-- Revenue Analytics
CREATE OR REPLACE VIEW noleggio_revenue_analytics AS
SELECT 
    DATE_FORMAT(n.data_consegna, '%Y-%m') as mese,
    COUNT(DISTINCT n.id) as ordini_consegnati,
    SUM(np.rata_mensile * np.durata_mesi) as fatturato_totale,
    AVG(np.rata_mensile) as rata_media
FROM noleggio_ordine n
LEFT JOIN noleggio_preventivo np ON n.preventivo_id = np.id
WHERE n.status = 'CONSEGNATO'
GROUP BY DATE_FORMAT(n.data_consegna, '%Y-%m')
ORDER BY mese DESC;

-- NBT Short-term Rental Analytics
CREATE OR REPLACE VIEW noleggio_nbt_analytics AS
SELECT 
    DATE_FORMAT(n.data_creazione, '%Y-%m') as mese,
    COUNT(DISTINCT n.id) as richieste,
    SUM(CASE WHEN n.status = 'COMPLETATO' THEN 1 ELSE 0 END) as completati,
    SUM(CASE WHEN n.status = 'ANNULLATO' THEN 1 ELSE 0 END) as annullati,
    SUM(n.importo_totale) as fatturato_nbt
FROM noleggio_nbt n
GROUP BY DATE_FORMAT(n.data_creazione, '%Y-%m')
ORDER BY mese DESC;
