-- Luna2 Gestionale - Database Schema
-- MySQL 8.0+

-- Create Database
CREATE DATABASE IF NOT EXISTS luna2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE luna2;

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    ruolo ENUM('ADMIN', 'USER', 'MANAGER') NOT NULL DEFAULT 'USER',
    attivo BOOLEAN DEFAULT TRUE,
    ultimo_accesso DATETIME,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Clienti Table
CREATE TABLE clienti (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_anagrafica ENUM('CLIENTE', 'PROSPECT') NOT NULL DEFAULT 'CLIENTE',
    ragione_sociale VARCHAR(255) NOT NULL,
    partita_iva VARCHAR(20),
    codice_fiscale VARCHAR(20),
    codice_sdi VARCHAR(10),
    pec VARCHAR(100),
    telefono VARCHAR(30),
    email VARCHAR(100),
    sito_web VARCHAR(255),
    indirizzo VARCHAR(255),
    citta VARCHAR(100),
    provincia VARCHAR(2),
    cap VARCHAR(10),
    paese VARCHAR(100) DEFAULT 'Italia',
    codice_cliente VARCHAR(50) UNIQUE,
    condizioni_pagamento VARCHAR(100),
    listino_default BIGINT,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    fido_massimo DECIMAL(15,2),
    note TEXT,
    attivo BOOLEAN DEFAULT TRUE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_ragione_sociale (ragione_sociale),
    INDEX idx_partita_iva (partita_iva),
    INDEX idx_codice_cliente (codice_cliente),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Fornitori Table
CREATE TABLE fornitori (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ragione_sociale VARCHAR(255) NOT NULL,
    partita_iva VARCHAR(20),
    codice_fiscale VARCHAR(20),
    telefono VARCHAR(30),
    email VARCHAR(100),
    sito_web VARCHAR(255),
    indirizzo VARCHAR(255),
    citta VARCHAR(100),
    provincia VARCHAR(2),
    cap VARCHAR(10),
    paese VARCHAR(100) DEFAULT 'Italia',
    codice_fornitore VARCHAR(50) UNIQUE,
    condizioni_pagamento VARCHAR(100),
    note TEXT,
    attivo BOOLEAN DEFAULT TRUE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_ragione_sociale (ragione_sociale),
    INDEX idx_partita_iva (partita_iva),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Contatti Table
CREATE TABLE contatti (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    fornitore_id BIGINT,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    ruolo VARCHAR(100),
    telefono VARCHAR(30),
    email VARCHAR(100),
    note TEXT,
    principale BOOLEAN DEFAULT FALSE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES clienti(id) ON DELETE CASCADE,
    FOREIGN KEY (fornitore_id) REFERENCES fornitori(id) ON DELETE CASCADE,
    INDEX idx_cliente (cliente_id),
    INDEX idx_fornitore (fornitore_id),
    CHECK ((cliente_id IS NOT NULL AND fornitore_id IS NULL) OR (cliente_id IS NULL AND fornitore_id IS NOT NULL))
) ENGINE=InnoDB;

-- Prodotti Table
CREATE TABLE prodotti (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codice VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    descrizione TEXT,
    categoria VARCHAR(100),
    unita_misura ENUM('PEZZO', 'KG', 'LITRO', 'METRO', 'MQ', 'MC', 'ORA') NOT NULL DEFAULT 'PEZZO',
    prezzo_base DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    costo_acquisto DECIMAL(15,2),
    iva_percentuale DECIMAL(5,2) NOT NULL DEFAULT 22.00,
    sconto_massimo DECIMAL(5,2) DEFAULT 0.00,
    peso DECIMAL(10,3),
    volume DECIMAL(10,3),
    codice_ean VARCHAR(50),
    fornitore_id BIGINT,
    gestione_magazzino BOOLEAN DEFAULT TRUE,
    giacenza_minima DECIMAL(10,2),
    note TEXT,
    attivo BOOLEAN DEFAULT TRUE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_codice (codice),
    INDEX idx_nome (nome),
    INDEX idx_categoria (categoria),
    FOREIGN KEY (fornitore_id) REFERENCES fornitori(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Listini Table
CREATE TABLE listini (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descrizione TEXT,
    tipo_sconto ENUM('PERCENTUALE', 'IMPORTO') DEFAULT 'PERCENTUALE',
    valore_sconto DECIMAL(15,2) DEFAULT 0.00,
    data_inizio DATE,
    data_fine DATE,
    attivo BOOLEAN DEFAULT TRUE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nome (nome)
) ENGINE=InnoDB;

-- Listini Dettagli Table
CREATE TABLE listini_dettagli (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listino_id BIGINT NOT NULL,
    prodotto_id BIGINT NOT NULL,
    prezzo DECIMAL(15,2) NOT NULL,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listino_id) REFERENCES listini(id) ON DELETE CASCADE,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id) ON DELETE CASCADE,
    UNIQUE KEY uk_listino_prodotto (listino_id, prodotto_id)
) ENGINE=InnoDB;

-- Magazzino Table
CREATE TABLE magazzino (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prodotto_id BIGINT NOT NULL UNIQUE,
    giacenza_attuale DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    giacenza_disponibile DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    giacenza_impegnata DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    giacenza_in_ordine DECIMAL(15,3) NOT NULL DEFAULT 0.000,
    valore_magazzino DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ultima_valorizzazione TIMESTAMP,
    data_aggiornamento TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Movimenti Magazzino Table
CREATE TABLE movimenti_magazzino (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prodotto_id BIGINT NOT NULL,
    tipo_movimento ENUM('CARICO', 'SCARICO', 'RETTIFICA', 'INVENTARIO') NOT NULL,
    causale VARCHAR(100),
    quantita DECIMAL(15,3) NOT NULL,
    costo_unitario DECIMAL(15,2),
    valore_totale DECIMAL(15,2),
    giacenza_dopo DECIMAL(15,3),
    documento_tipo VARCHAR(50),
    documento_id BIGINT,
    documento_numero VARCHAR(50),
    note TEXT,
    data_movimento DATE NOT NULL,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    INDEX idx_prodotto (prodotto_id),
    INDEX idx_data_movimento (data_movimento),
    INDEX idx_tipo (tipo_movimento),
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Lead Table
CREATE TABLE lead (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    origine ENUM('FIERA', 'CAMPAGNA', 'PASSAPAROLA', 'WEBSITE', 'EMAIL', 'TELEFONO', 'ALTRO') NOT NULL,
    stato ENUM('NUOVO', 'CONTATTATO', 'QUALIFICATO', 'PREVENTIVO', 'NEGOZIAZIONE', 'VINTO', 'PERSO') NOT NULL DEFAULT 'NUOVO',
    azienda VARCHAR(255) NOT NULL,
    nome_contatto VARCHAR(100),
    cognome_contatto VARCHAR(100),
    telefono VARCHAR(30),
    email VARCHAR(100),
    indirizzo VARCHAR(255),
    citta VARCHAR(100),
    provincia VARCHAR(2),
    cap VARCHAR(10),
    esigenza TEXT,
    budget_stimato DECIMAL(15,2),
    probabilita_chiusura INT DEFAULT 0,
    data_contatto DATE,
    data_prossimo_followup DATE,
    note TEXT,
    cliente_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_stato (stato),
    INDEX idx_origine (origine),
    INDEX idx_azienda (azienda),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Tag Table
CREATE TABLE tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    colore VARCHAR(20),
    descrizione TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Lead Tag Association Table
CREATE TABLE lead_tag (
    lead_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (lead_id, tag_id),
    FOREIGN KEY (lead_id) REFERENCES lead(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Preventivi Table
CREATE TABLE preventivi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) NOT NULL UNIQUE,
    anno INT NOT NULL,
    data_preventivo DATE NOT NULL,
    data_validita DATE,
    cliente_id BIGINT NOT NULL,
    lead_id BIGINT,
    stato ENUM('BOZZA', 'INVIATO', 'ACCETTATO', 'RIFIUTATO', 'SCADUTO') NOT NULL DEFAULT 'BOZZA',
    oggetto VARCHAR(255),
    note_intestazione TEXT,
    note_piede TEXT,
    imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    iva DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    totale DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    spese_trasporto DECIMAL(15,2) DEFAULT 0.00,
    condizioni_pagamento VARCHAR(255),
    tempi_consegna VARCHAR(255),
    validita_giorni INT DEFAULT 30,
    ordine_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_numero (numero),
    INDEX idx_anno (anno),
    INDEX idx_cliente (cliente_id),
    INDEX idx_stato (stato),
    INDEX idx_data (data_preventivo),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (lead_id) REFERENCES lead(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Preventivi Righe Table
CREATE TABLE preventivi_righe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    preventivo_id BIGINT NOT NULL,
    riga_numero INT NOT NULL,
    tipo_riga ENUM('PRODOTTO', 'DESCRIZIONE', 'SUBTOTALE') NOT NULL DEFAULT 'PRODOTTO',
    prodotto_id BIGINT,
    descrizione TEXT NOT NULL,
    quantita DECIMAL(15,3) DEFAULT 1.000,
    unita_misura VARCHAR(20),
    prezzo_unitario DECIMAL(15,2) DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    imponibile_riga DECIMAL(15,2) DEFAULT 0.00,
    iva_percentuale DECIMAL(5,2) DEFAULT 22.00,
    iva_importo DECIMAL(15,2) DEFAULT 0.00,
    totale_riga DECIMAL(15,2) DEFAULT 0.00,
    note TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (preventivo_id) REFERENCES preventivi(id) ON DELETE CASCADE,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
    INDEX idx_preventivo (preventivo_id)
) ENGINE=InnoDB;

-- Ordini Table
CREATE TABLE ordini (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) NOT NULL UNIQUE,
    anno INT NOT NULL,
    data_ordine DATE NOT NULL,
    cliente_id BIGINT NOT NULL,
    preventivo_id BIGINT,
    stato ENUM('CONFERMATO', 'IN_LAVORAZIONE', 'PARZIALMENTE_EVASO', 'EVASO', 'ANNULLATO') NOT NULL DEFAULT 'CONFERMATO',
    oggetto VARCHAR(255),
    riferimento_cliente VARCHAR(100),
    note_intestazione TEXT,
    note_piede TEXT,
    imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    iva DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    totale DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    spese_trasporto DECIMAL(15,2) DEFAULT 0.00,
    condizioni_pagamento VARCHAR(255),
    data_consegna_prevista DATE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_numero (numero),
    INDEX idx_anno (anno),
    INDEX idx_cliente (cliente_id),
    INDEX idx_stato (stato),
    INDEX idx_data (data_ordine),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (preventivo_id) REFERENCES preventivi(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Ordini Righe Table
CREATE TABLE ordini_righe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ordine_id BIGINT NOT NULL,
    riga_numero INT NOT NULL,
    tipo_riga ENUM('PRODOTTO', 'DESCRIZIONE', 'SUBTOTALE') NOT NULL DEFAULT 'PRODOTTO',
    prodotto_id BIGINT,
    descrizione TEXT NOT NULL,
    quantita DECIMAL(15,3) DEFAULT 1.000,
    quantita_evasa DECIMAL(15,3) DEFAULT 0.000,
    unita_misura VARCHAR(20),
    prezzo_unitario DECIMAL(15,2) DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    imponibile_riga DECIMAL(15,2) DEFAULT 0.00,
    iva_percentuale DECIMAL(5,2) DEFAULT 22.00,
    iva_importo DECIMAL(15,2) DEFAULT 0.00,
    totale_riga DECIMAL(15,2) DEFAULT 0.00,
    note TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ordine_id) REFERENCES ordini(id) ON DELETE CASCADE,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
    INDEX idx_ordine (ordine_id)
) ENGINE=InnoDB;

-- DDT Table
CREATE TABLE ddt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) NOT NULL UNIQUE,
    anno INT NOT NULL,
    data_ddt DATE NOT NULL,
    cliente_id BIGINT NOT NULL,
    ordine_id BIGINT,
    causale_trasporto VARCHAR(100),
    aspetto_beni VARCHAR(100),
    numero_colli INT,
    peso DECIMAL(10,2),
    trasportatore VARCHAR(255),
    indirizzo_destinazione TEXT,
    note TEXT,
    imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    iva DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    totale DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    fattura_id BIGINT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_numero (numero),
    INDEX idx_anno (anno),
    INDEX idx_cliente (cliente_id),
    INDEX idx_data (data_ddt),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (ordine_id) REFERENCES ordini(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- DDT Righe Table
CREATE TABLE ddt_righe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ddt_id BIGINT NOT NULL,
    riga_numero INT NOT NULL,
    tipo_riga ENUM('PRODOTTO', 'DESCRIZIONE') NOT NULL DEFAULT 'PRODOTTO',
    prodotto_id BIGINT,
    descrizione TEXT NOT NULL,
    quantita DECIMAL(15,3) DEFAULT 1.000,
    unita_misura VARCHAR(20),
    prezzo_unitario DECIMAL(15,2) DEFAULT 0.00,
    imponibile_riga DECIMAL(15,2) DEFAULT 0.00,
    iva_percentuale DECIMAL(5,2) DEFAULT 22.00,
    note TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ddt_id) REFERENCES ddt(id) ON DELETE CASCADE,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
    INDEX idx_ddt (ddt_id)
) ENGINE=InnoDB;

-- Fatture Table
CREATE TABLE fatture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) NOT NULL UNIQUE,
    anno INT NOT NULL,
    data_fattura DATE NOT NULL,
    tipo_fattura ENUM('VENDITA', 'ACCONTO', 'SALDO', 'NOTA_CREDITO') NOT NULL DEFAULT 'VENDITA',
    cliente_id BIGINT NOT NULL,
    ordine_id BIGINT,
    oggetto VARCHAR(255),
    note_intestazione TEXT,
    note_piede TEXT,
    imponibile DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    iva DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    totale DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    ritenuta_acconto DECIMAL(15,2) DEFAULT 0.00,
    totale_netto DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    spese_trasporto DECIMAL(15,2) DEFAULT 0.00,
    marca_bollo DECIMAL(10,2) DEFAULT 0.00,
    condizioni_pagamento VARCHAR(255),
    stato_pagamento ENUM('DA_PAGARE', 'PARZIALMENTE_PAGATA', 'PAGATA', 'SCADUTA') NOT NULL DEFAULT 'DA_PAGARE',
    data_scadenza DATE,
    data_pagamento DATE,
    metodo_pagamento VARCHAR(100),
    fattura_elettronica_inviata BOOLEAN DEFAULT FALSE,
    fattura_elettronica_id VARCHAR(100),
    data_invio_sdi DATETIME,
    note_fattura_elettronica TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_numero (numero),
    INDEX idx_anno (anno),
    INDEX idx_cliente (cliente_id),
    INDEX idx_data (data_fattura),
    INDEX idx_stato_pagamento (stato_pagamento),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (ordine_id) REFERENCES ordini(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Fatture Righe Table
CREATE TABLE fatture_righe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fattura_id BIGINT NOT NULL,
    riga_numero INT NOT NULL,
    tipo_riga ENUM('PRODOTTO', 'DESCRIZIONE', 'SUBTOTALE') NOT NULL DEFAULT 'PRODOTTO',
    prodotto_id BIGINT,
    descrizione TEXT NOT NULL,
    quantita DECIMAL(15,3) DEFAULT 1.000,
    unita_misura VARCHAR(20),
    prezzo_unitario DECIMAL(15,2) DEFAULT 0.00,
    sconto_percentuale DECIMAL(5,2) DEFAULT 0.00,
    sconto_importo DECIMAL(15,2) DEFAULT 0.00,
    imponibile_riga DECIMAL(15,2) DEFAULT 0.00,
    iva_percentuale DECIMAL(5,2) DEFAULT 22.00,
    iva_importo DECIMAL(15,2) DEFAULT 0.00,
    totale_riga DECIMAL(15,2) DEFAULT 0.00,
    note TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (fattura_id) REFERENCES fatture(id) ON DELETE CASCADE,
    FOREIGN KEY (prodotto_id) REFERENCES prodotti(id),
    INDEX idx_fattura (fattura_id)
) ENGINE=InnoDB;

-- Note Table
CREATE TABLE note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_entita ENUM('CLIENTE', 'FORNITORE', 'LEAD', 'PREVENTIVO', 'ORDINE', 'DDT', 'FATTURA') NOT NULL,
    entita_id BIGINT NOT NULL,
    titolo VARCHAR(255),
    contenuto TEXT NOT NULL,
    importante BOOLEAN DEFAULT FALSE,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    modified_by BIGINT,
    INDEX idx_entita (tipo_entita, entita_id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (modified_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Scadenze Table
CREATE TABLE scadenze (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo ENUM('FATTURA', 'PAGAMENTO', 'ATTIVITA', 'PROMEMORIA', 'FOLLOWUP') NOT NULL,
    tipo_entita VARCHAR(50),
    entita_id BIGINT,
    titolo VARCHAR(255) NOT NULL,
    descrizione TEXT,
    data_scadenza DATE NOT NULL,
    completata BOOLEAN DEFAULT FALSE,
    data_completamento DATE,
    priorita ENUM('BASSA', 'MEDIA', 'ALTA', 'URGENTE') DEFAULT 'MEDIA',
    assegnata_a BIGINT,
    cliente_id BIGINT,
    importo DECIMAL(15,2),
    note TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    INDEX idx_data_scadenza (data_scadenza),
    INDEX idx_tipo (tipo),
    INDEX idx_completata (completata),
    INDEX idx_assegnata (assegnata_a),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (assegnata_a) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

-- Azienda Table (configurazione)
CREATE TABLE azienda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ragione_sociale VARCHAR(255) NOT NULL,
    partita_iva VARCHAR(20) NOT NULL,
    codice_fiscale VARCHAR(20),
    indirizzo VARCHAR(255),
    citta VARCHAR(100),
    provincia VARCHAR(2),
    cap VARCHAR(10),
    paese VARCHAR(100) DEFAULT 'Italia',
    telefono VARCHAR(30),
    email VARCHAR(100),
    pec VARCHAR(100),
    sito_web VARCHAR(255),
    codice_sdi VARCHAR(10),
    logo_path VARCHAR(255),
    iban VARCHAR(34),
    banca VARCHAR(255),
    capitale_sociale DECIMAL(15,2),
    registro_imprese VARCHAR(255),
    rea VARCHAR(50),
    note_fattura TEXT,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Numerazioni Table (gestione numerazioni documenti)
CREATE TABLE numerazioni (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('PREVENTIVO', 'ORDINE', 'DDT', 'FATTURA') NOT NULL,
    anno INT NOT NULL,
    ultimo_numero INT NOT NULL DEFAULT 0,
    prefisso VARCHAR(20),
    suffisso VARCHAR(20),
    formato VARCHAR(50) DEFAULT '{ANNO}/{NUMERO}',
    lunghezza_numero INT DEFAULT 4,
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_modifica TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tipo_anno (tipo_documento, anno)
) ENGINE=InnoDB;

-- Notification Preferences Table
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    digest_frequency ENUM('IMMEDIATE', 'HOURLY', 'DAILY', 'WEEKLY', 'NEVER') NOT NULL DEFAULT 'IMMEDIATE',
    enabled_event_types TEXT,
    quiet_start_time VARCHAR(5) NOT NULL DEFAULT '22:00',
    quiet_end_time VARCHAR(5) NOT NULL DEFAULT '08:00',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_user (user_id)
) ENGINE=InnoDB;

-- Notification History Table
CREATE TABLE notification_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    channel ENUM('EMAIL', 'PUSH', 'SMS', 'SLACK', 'DATABASE') NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT,
    status ENUM('SENT', 'FAILED', 'BOUNCED', 'DELIVERED', 'READ', 'CLICKED') NOT NULL DEFAULT 'SENT',
    error_message TEXT,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME,
    clicked_at DATETIME,
    INDEX idx_notification_history_user (user_id),
    INDEX idx_notification_history_event (event_type),
    INDEX idx_notification_history_sent (sent_at)
) ENGINE=InnoDB;

-- Calendar Accounts Table
CREATE TABLE calendar_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    provider ENUM('GOOGLE', 'ICLOUD') NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    caldav_url VARCHAR(512),
    calendar_id VARCHAR(255),
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Europe/Rome',
    sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_calendar_account_user_provider (user_id, provider),
    INDEX idx_calendar_account_user (user_id)
) ENGINE=InnoDB;

-- Calendar Events Table
CREATE TABLE calendar_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    provider ENUM('GOOGLE', 'ICLOUD') NOT NULL,
    external_event_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    source_type ENUM('REMINDER', 'TASK', 'MEETING') NOT NULL,
    source_id BIGINT NOT NULL,
    status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_calendar_event_user (user_id),
    INDEX idx_calendar_event_provider (provider),
    INDEX idx_calendar_event_source (source_type, source_id)
) ENGINE=InnoDB;

-- Insert default admin user (password: admin123)
INSERT INTO users (username, password, email, nome, cognome, ruolo, attivo)
VALUES ('admin', '$2a$10$7PJKF5IXtYt5LVLnBd/XB.b3lZ9.kUlp9p5oFJZHx9qSrKVJ3KjNi', 'admin@luna2.local', 'Admin', 'Luna2', 'ADMIN', TRUE);

-- Insert default configuration
INSERT INTO azienda (ragione_sociale, partita_iva, email)
VALUES ('La Tua Azienda Srl', '12345678901', 'info@tuaazienda.it');

-- Insert default numerazioni
INSERT INTO numerazioni (tipo_documento, anno, ultimo_numero)
VALUES 
    ('PREVENTIVO', YEAR(CURDATE()), 0),
    ('ORDINE', YEAR(CURDATE()), 0),
    ('DDT', YEAR(CURDATE()), 0),
    ('FATTURA', YEAR(CURDATE()), 0);

-- Insert sample tags
INSERT INTO tag (nome, colore, descrizione)
VALUES 
    ('Urgente', 'danger', 'Richiesta urgente'),
    ('VIP', 'warning', 'Cliente VIP'),
    ('Nuovo', 'success', 'Nuovo cliente'),
    ('Follow-up', 'info', 'Richiede follow-up'),
    ('Interessato', 'primary', 'Molto interessato');
