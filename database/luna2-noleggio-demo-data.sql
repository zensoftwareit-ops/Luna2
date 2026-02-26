-- Luna2 Broker Noleggio Auto - Demo Data
-- Script per popolare il database con dati demo realistici
-- Basato su scenari reali descritti nella guida utente

USE luna2;

-- ============== PREREQUISITI ==============
-- Assicura che ci siano utenti di sistema

-- Crea utenti broker se non esistono
INSERT INTO users (id, username, password, email, nome, cognome, ruolo, attivo, data_creazione)
VALUES 
    (1, 'admin', '$2a$10$Dt/TJ065SLL2PaboT7ZdcuO0ugvG1uAxSM8SVfts9zdR0C7eUtbrC', 'admin@luna2.local', 'Admin', 'Sistema', 'ADMIN', 1, NOW()),
    (2, 'paolo.verdi', '$2a$10$Bpu4gKi3vwFWaJKOp7yw6OSpm6d/E7UTlvvKQ130A9h73hHIHqnQS', 'paolo.verdi@luna2.local', 'Paolo', 'Verdi', 'USER', 1, NOW()),
    (3, 'marco.rossi', '$2a$10$Bpu4gKi3vwFWaJKOp7yw6OSpm6d/E7UTlvvKQ130A9h73hHIHqnQS', 'marco.rossi@luna2.local', 'Marco', 'Rossi', 'USER', 1, NOW())
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    attivo = VALUES(attivo),
    ruolo = VALUES(ruolo);

-- Assicura che il modulo sia attivato
INSERT INTO module_settings (code, name, description, enabled, updated_at)
VALUES 
    ('CRM_BROKER_AUTO', 'CRM Broker Auto', 'Broker Auto - Gestione completa noleggio auto', TRUE, NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    enabled = VALUES(enabled),
    updated_at = NOW();

-- ============== SCENARIO 1: MARIO ROSSI - LEAD COMPLETO IN POST VENDITA ==============

-- Lead CRM principale
INSERT INTO `lead` (id, azienda, nome_contatto, cognome_contatto, email, telefono, origine, stato, 
    esigenza, budget_stimato, data_contatto, note, created_by, data_creazione)
VALUES 
    (1001, 'Costruzioni Rossi Srl', 'Mario', 'Rossi', 'mario.rossi@costruzionirossi.it', 
     '+39 328 1234567', 'WEBSITE', 'VINTO', 
     '3 auto Fiat Ducato per cantieri', 7410.00, 
     DATE_SUB(NOW(), INTERVAL 45 DAY), 
     'Cliente affidabile, bisogno urgente per cantieri Milano', 
     2, DATE_SUB(NOW(), INTERVAL 45 DAY));

-- Noleggio Lead - FASE POST_VENDITA
INSERT INTO noleggio_lead (id, numero_pratica, cliente_id, ragione_sociale, nome_contatto, cognome_contatto,
    email, telefono, utilizzo_previsto, km_annuali_previsti, data_inizio_noleggio, durata_mesi,
    priorita_marca, priorita_modello, budget_massimo, fase, referrer,
    utente_creazione_id, utente_assegnato_id, data_creazione, data_modifica)
VALUES 
    (1001, 'NL-2026-001', 1001, 'Costruzioni Rossi Srl', 'Mario', 'Rossi',
     'mario.rossi@costruzionirossi.it', '+39 328 1234567', 
     'Trasporto materiali edili per cantieri', 30000, 
     DATE_SUB(NOW(), INTERVAL 23 DAY), 36,
     'Fiat', 'Ducato', 7500.00, 'POST_VENDITA', 'Campagna Google Ads',
     2, 2, DATE_SUB(NOW(), INTERVAL 45 DAY), NOW());

-- Preventivo Mario Rossi - ACCETTATO
INSERT INTO noleggio_preventivo (id, lead_id, numero_preventivo, marca, modello, versione, colore,
    prezzo_acquisto, sconto_percentuale, prezzo_netto, tassa_immatricolazione, costo_trasporto, costo_allestimenti,
    durata_mesi, rata_mensile, chilometri_inclusi, sovrapprezzo_km, 
    costo_manutenzione_mensile, costo_assicurazione_mensile, costo_pneumatici,
    status, data_invio, data_accettazione, data_scadenza, versione_numero, follow_up_48h_inviato,
    utente_creazione_id, data_creazione)
VALUES 
    (1001, 1001, 'PREV-2026-001', 'Fiat', 'Ducato', '2.3 Multijet 140cv', 'Bianco',
     28000.00, 5.00, 26600.00, 500.00, 200.00, 800.00,
     36, 2470.00, 30000, 0.15,
     150.00, 280.00, 80.00,
     'ACCETTATO', DATE_SUB(NOW(), INTERVAL 44 DAY), DATE_SUB(NOW(), INTERVAL 42 DAY), 
     DATE_SUB(NOW(), INTERVAL 14 DAY), 1, TRUE,
     2, DATE_SUB(NOW(), INTERVAL 45 DAY));

-- Documenti Mario Rossi - TUTTI VALIDATI
INSERT INTO noleggio_documento (lead_id, tipo_documento, descrizione, obbligatorio, 
    file_name, file_path, file_size, mime_type, status, 
    data_richiesta, data_caricamento, data_validazione,
    utente_creazione_id, utente_validazione_id, data_creazione)
VALUES 
    (1001, 'CARTA_IDENTITA', 'Carta identità titolare Mario Rossi', TRUE,
     'CI_Mario_Rossi.pdf', '/uploads/noleggio/1001/ci_mario_rossi.pdf', 2048000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 40 DAY)),
    (1001, 'PATENTE', 'Patente Mario Rossi', TRUE,
     'Patente_Mario_Rossi.pdf', '/uploads/noleggio/1001/patente_mario.pdf', 1843200, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 40 DAY)),
    (1001, 'CERTIFICATO_SINISTRI', 'Certificato storico sinistri', TRUE,
     'Certificato_Sinistri.pdf', '/uploads/noleggio/1001/cert_sinistri.pdf', 870400, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 37 DAY), DATE_SUB(NOW(), INTERVAL 36 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 40 DAY)),
    (1001, 'ESTRATTO_CONTO', 'Estratto conto ultimi 3 mesi', TRUE,
     'Estratto_Conto_3mesi.pdf', '/uploads/noleggio/1001/estratto_conto.pdf', 4300000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 39 DAY), DATE_SUB(NOW(), INTERVAL 36 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 39 DAY)),
    (1001, 'BILANCIO_AZIENDA', 'Bilancio Costruzioni Rossi 2023', TRUE,
     'Bilancio_2023_Rossi.pdf', '/uploads/noleggio/1001/bilancio_2023.pdf', 2560000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 39 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 34 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 39 DAY));

-- Valutazione finanziaria Mario Rossi - APPROVATO
INSERT INTO noleggio_valutazione (lead_id, rating_creditizio, score_creditizio, storico_pagamenti,
    esposizioni_bancarie, importo_max_fideiussione, fatturato_annuale, utile_netto_annuale,
    rating_solvibilita, fattori_rischio, valutazione_complessiva, opinione_gestore,
    numero_solleciti, status, data_approvazione, utente_creazione_id, utente_approvatore_id, data_creazione)
VALUES 
    (1001, 'BBB+', 750, 'Eccellente storico pagamenti, nessun ritardo negli ultimi 5 anni',
     FALSE, 50000.00, 1200000.00, 85000.00,
     'ECCELLENTE', 'Nessun fattore di rischio significativo', 
     'Cliente solido, ditta presente da 20 anni, finanziamento approvato senza problemi',
     'Ditta affidabile, documenti completi, finanziamento approvato immediatamente',
     0, 'APPROVATO', DATE_SUB(NOW(), INTERVAL 33 DAY), 2, 2, DATE_SUB(NOW(), INTERVAL 35 DAY));

-- Ordine Mario Rossi - CONSEGNATO
INSERT INTO noleggio_ordine (id, lead_id, numero_ordine, preventivo_id, 
    targa, marca_modello, vin, data_immatricolazione, colore, km_attuali,
    eta_consegna, data_consegna, note_consegna,
    status, ultima_care_call, prossima_care_call_prevista, numero_care_call, note_care_call,
    utente_creazione_id, utente_assegnato_id, data_creazione_record)
VALUES 
    (1001, 1001, 'ORD-2026-001', 1001,
     'MI500ABC', 'Fiat Ducato 2.3 Multijet', 'ZFA25000001234567', DATE_SUB(NOW(), INTERVAL 22 DAY), 'Bianco', 2450,
     DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), 
     'Consegnate 3 auto presso Via Roma 10, Milano. Cliente molto soddisfatto',
     'CONSEGNATO', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), 1,
     'Care call eseguita - Cliente soddisfatto 8/10, nessun problema riscontrato, auto utilizzate per cantieri',
     2, 2, DATE_SUB(NOW(), INTERVAL 32 DAY)),
     -- Auto 2 e 3 dello stesso ordine
    (1002, 1001, 'ORD-2026-001B', 1001,
     'MI500ABD', 'Fiat Ducato 2.3 Multijet', 'ZFA25000001234568', DATE_SUB(NOW(), INTERVAL 22 DAY), 'Bianco', 1890,
     DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), 'Auto 2 dello stesso ordine',
     'CONSEGNATO', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), 1, 'Care call ok',
     2, 2, DATE_SUB(NOW(), INTERVAL 32 DAY)),
    (1003, 1001, 'ORD-2026-001C', 1001,
     'MI500ABE', 'Fiat Ducato 2.3 Multijet', 'ZFA25000001234569', DATE_SUB(NOW(), INTERVAL 22 DAY), 'Bianco', 2120,
     DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY), 'Auto 3 dello stesso ordine',
     'CONSEGNATO', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 23 DAY), 1, 'Care call ok',
     2, 2, DATE_SUB(NOW(), INTERVAL 32 DAY));


-- Ticket di supporto Mario Rossi - RISOLTO
INSERT INTO noleggio_ticket (numero_ticket, contratto_id, ordine_id, oggetto, descrizione,
    priorita, categoria, status, operatore_assegnato_id, data_assegnazione,
    soluzione_adottata, data_risoluzione, data_chiusura,
    flag_anti_rimbalzo, data_chiusura_precedente,
    utente_creazione_id, data_creazione)
VALUES 
    ('TICK-2026-001', 1001, 1001, 'Problema freno anteriore sinistro', 
     'Uno dei tre Ducato (targa MI500ABC) ha strani rumori quando freno forte. Può controllare? Auto usata tutti i giorni per trasporti.',
     'MEDIA', 'MANUTENZIONE', 'RISOLTO', 2, DATE_SUB(NOW(), INTERVAL 3 DAY),
     'Pastiglie anteriori leggermente sporche di polvere. Pulite + lubricate. Freni ok. Test drive positivo. Nessun costo (garanzia noleggio).',
     DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
     FALSE, DATE_SUB(NOW(), INTERVAL 2 DAY),
     2, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ============== SCENARIO 2: ANNA FERRARI - GESTIONE TELEFONICA IN PREVENTIVAZIONE ==============

-- Lead CRM principale
INSERT INTO `lead` (id, azienda, nome_contatto, cognome_contatto, email, telefono, origine, stato,
    esigenza, budget_stimato, data_contatto, note, created_by, data_creazione)
VALUES 
    (1002, 'Privato', 'Anna', 'Ferrari', 'anna.ferrari@email.com', '+39 334 5555555',
     'TELEFONO', 'CONTATTATO', 
     'Auto diesel Milano, 36 mesi', 800.00,
     DATE_SUB(NOW(), INTERVAL 2 DAY),
     'Contatto telefonico, interessata diesel, budget max 800€/mese',
     3, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Noleggio Lead - FASE PREVENTIVAZIONE
INSERT INTO noleggio_lead (id, numero_pratica, cliente_id, ragione_sociale, nome_contatto, cognome_contatto,
    email, telefono, utilizzo_previsto, km_annuali_previsti, data_inizio_noleggio, durata_mesi,
    priorita_marca, priorita_modello, budget_massimo, fase, referrer,
    utente_creazione_id, utente_assegnato_id, data_creazione, data_modifica)
VALUES 
    (1002, 'NL-2026-002', 1002, 'Privato', 'Anna', 'Ferrari',
     'anna.ferrari@email.com', '+39 334 5555555',
     'Uso privato quotidiano', 20000,
     DATE_ADD(NOW(), INTERVAL 15 DAY), 36,
     'Qualsiasi', 'Diesel', 800.00, 'PREVENTIVAZIONE', 'Chiamata diretta',
     3, 3, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

-- Preventivo Anna Ferrari - INVIATO (in attesa risposta)
INSERT INTO noleggio_preventivo (id, lead_id, numero_preventivo, marca, modello, versione, colore,
    prezzo_acquisto, sconto_percentuale, prezzo_netto, tassa_immatricolazione, costo_trasporto, costo_allestimenti,
    durata_mesi, rata_mensile, chilometri_inclusi, sovrapprezzo_km,
    costo_manutenzione_mensile, costo_assicurazione_mensile, costo_pneumatici,
    status, data_invio, data_scadenza, versione_numero, follow_up_48h_inviato,
    utente_creazione_id, data_creazione)
VALUES 
    (1002, 1002, 'PREV-2026-002', 'Volkswagen', 'Golf', '1.6 TDI 115cv', 'Grigio metallizzato',
     22000.00, 3.00, 21340.00, 450.00, 150.00, 400.00,
     36, 780.00, 20000, 0.12,
     120.00, 200.00, 60.00,
     'INVIATO', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 13 DAY), 1, FALSE,
     3, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============== SCENARIO 3: LORENZO VERDI - BMW IN ISTRUTTORIA ==============

-- Lead CRM principale
INSERT INTO `lead` (id, azienda, nome_contatto, cognome_contatto, email, telefono, origine, stato,
    esigenza, budget_stimato, data_contatto, note, created_by, data_creazione)
VALUES 
    (1003, 'Consulting LV Srl', 'Lorenzo', 'Verdi', 'lorenzo.verdi@consulting.com', '+39 340 7777777',
     'WEBSITE', 'QUALIFICATO',
     'BMW 320d per rappresentanza aziendale', 1248.00,
     DATE_SUB(NOW(), INTERVAL 10 DAY),
     'Professionista, richiede BMW Serie 3 diesel per rappresentanza',
     2, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- Noleggio Lead - FASE ISTRUTTORIA
INSERT INTO noleggio_lead (id, numero_pratica, cliente_id, ragione_sociale, nome_contatto, cognome_contatto,
    email, telefono, utilizzo_previsto, km_annuali_previsti, data_inizio_noleggio, durata_mesi,
    priorita_marca, priorita_modello, budget_massimo, fase, referrer,
    utente_creazione_id, utente_assegnato_id, data_creazione, data_modifica)
VALUES 
    (1003, 'NL-2026-003', 1003, 'Consulting LV Srl', 'Lorenzo', 'Verdi',
     'lorenzo.verdi@consulting.com', '+39 340 7777777',
     'Rappresentanza aziendale e trasferte', 25000,
     DATE_ADD(NOW(), INTERVAL 20 DAY), 36,
     'BMW', '320d', 1300.00, 'ISTRUTTORIA', 'Form contatto sito web',
     2, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW());

-- Preventivo Lorenzo Verdi - ACCETTATO
INSERT INTO noleggio_preventivo (id, lead_id, numero_preventivo, marca, modello, versione, colore,
    prezzo_acquisto, sconto_percentuale, prezzo_netto, tassa_immatricolazione, costo_trasporto, costo_allestimenti,
    durata_mesi, rata_mensile, chilometri_inclusi, sovrapprezzo_km,
    costo_manutenzione_mensile, costo_assicurazione_mensile, costo_pneumatici,
    status, data_invio, data_accettazione, data_scadenza, versione_numero, follow_up_48h_inviato,
    utente_creazione_id, data_creazione)
VALUES 
    (1003, 1003, 'PREV-2026-003', 'BMW', '320d', 'xDrive Business Advantage', 'Blu sophisto',
     42000.00, 5.00, 39900.00, 650.00, 250.00, 1200.00,
     36, 1248.00, 25000, 0.18,
     180.00, 320.00, 100.00,
     'ACCETTATO', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
     DATE_ADD(NOW(), INTERVAL 6 DAY), 1, TRUE,
     2, DATE_SUB(NOW(), INTERVAL 9 DAY));

-- Documenti Lorenzo Verdi - ALCUNI CARICATI, IN ATTESA DI ALTRI
INSERT INTO noleggio_documento (lead_id, tipo_documento, descrizione, obbligatorio,
    file_name, file_path, file_size, mime_type, status,
    data_richiesta, data_caricamento, data_validazione,
    utente_creazione_id, utente_validazione_id, data_creazione)
VALUES 
    (1003, 'CARTA_IDENTITA', 'Carta identità Lorenzo Verdi', TRUE,
     'CI_Lorenzo_Verdi.pdf', '/uploads/noleggio/1003/ci_lorenzo.pdf', 1950000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1003, 'PATENTE', 'Patente Lorenzo Verdi', TRUE,
     'Patente_Lorenzo_Verdi.pdf', '/uploads/noleggio/1003/patente_lorenzo.pdf', 1750000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1003, 'CERTIFICATO_SINISTRI', 'Certificato storico sinistri', TRUE,
     NULL, NULL, NULL, NULL, 'CARICATO',
     DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL,
     2, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1003, 'ESTRATTO_CONTO', 'Estratto conto ultimi 3 mesi', TRUE,
     NULL, NULL, NULL, NULL, 'RICHIESTO',
     DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL,
     2, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
    (1003, 'BILANCIO_AZIENDA', 'Bilancio Consulting LV 2023', TRUE,
     NULL, NULL, NULL, NULL, 'RICHIESTO',
     DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, NULL,
     2, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- Valutazione Lorenzo Verdi - IN REVISIONE
INSERT INTO noleggio_valutazione (lead_id, rating_creditizio, score_creditizio, storico_pagamenti,
    esposizioni_bancarie, importo_max_fideiussione, fatturato_annuale, utile_netto_annuale,
    rating_solvibilita, fattori_rischio, valutazione_complessiva, opinione_gestore,
    numero_solleciti, data_ultimo_sollecito, status, utente_creazione_id, data_creazione)
VALUES 
    (1003, 'BBB', 680, 'Buono storico pagamenti',
     FALSE, 30000.00, 450000.00, 55000.00,
     'BUONO', 'Nessun fattore di rischio particolare',
     'In attesa di documenti finanziari completi per approvazione finale',
     'Cliente promettente, attività consolidata. In attesa di bilancio e estratto conto.',
     1, DATE_SUB(NOW(), INTERVAL 1 DAY), 'IN_REVISIONE', 2, DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ============== LEAD AGGIUNTIVI PER ALTRE FASI ==============

-- Lead in fase ORDINE
INSERT INTO `lead` (id, azienda, nome_contatto, cognome_contatto, email, telefono, origine, stato,
    esigenza, budget_stimato, data_contatto, note, created_by, data_creazione)
VALUES 
    (1004, 'Tech Solutions SpA', 'Giulia', 'Bianchi', 'giulia.bianchi@techsol.it', '+39 345 8888888',
     'REFERRAL', 'VINTO',
     'Fleet aziendale 5 auto Audi A4', 6500.00,
     DATE_SUB(NOW(), INTERVAL 15 DAY),
     'Azienda IT, richiede 5 auto premium per dirigenti',
     2, DATE_SUB(NOW(), INTERVAL 15 DAY));

INSERT INTO noleggio_lead (id, numero_pratica, cliente_id, ragione_sociale, nome_contatto, cognome_contatto,
    email, telefono, utilizzo_previsto, km_annuali_previsti, data_inizio_noleggio, durata_mesi,
    priorita_marca, priorita_modello, budget_massimo, fase, referrer,
    utente_creazione_id, utente_assegnato_id, data_creazione, data_modifica)
VALUES 
    (1004, 'NL-2026-004', 1004, 'Tech Solutions SpA', 'Giulia', 'Bianchi',
     'giulia.bianchi@techsol.it', '+39 345 8888888',
     'Fleet aziendale dirigenza', 28000,
     DATE_ADD(NOW(), INTERVAL 10 DAY), 36,
     'Audi', 'A4', 6500.00, 'ORDINE', 'Cliente esistente',
     2, 2, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW());

-- Preventivo in fase ordine
INSERT INTO noleggio_preventivo (id, lead_id, numero_preventivo, marca, modello, versione, colore,
    prezzo_acquisto, sconto_percentuale, prezzo_netto, tassa_immatricolazione, costo_trasporto, costo_allestimenti,
    durata_mesi, rata_mensile, chilometri_inclusi, sovrapprezzo_km,
    costo_manutenzione_mensile, costo_assicurazione_mensile, costo_pneumatici,
    status, data_invio, data_accettazione, data_scadenza, versione_numero, follow_up_48h_inviato,
    utente_creazione_id, data_creazione)
VALUES 
    (1004, 1004, 'PREV-2026-004', 'Audi', 'A4', '2.0 TDI 190cv S-tronic Business Sport', 'Grigio Daytona',
     45000.00, 7.00, 41850.00, 700.00, 250.00, 2000.00,
     36, 1300.00, 28000, 0.20,
     200.00, 350.00, 120.00,
     'ACCETTATO', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY),
     DATE_ADD(NOW(), INTERVAL 4 DAY), 1, TRUE,
     2, DATE_SUB(NOW(), INTERVAL 13 DAY));

-- Documenti e valutazione approvata per lead 1004
INSERT INTO noleggio_documento (lead_id, tipo_documento, descrizione, obbligatorio,
    file_name, file_path, file_size, mime_type, status,
    data_richiesta, data_caricamento, data_validazione,
    utente_creazione_id, utente_validazione_id, data_creazione)
VALUES 
    (1004, 'CARTA_IDENTITA', 'Carta identità Giulia Bianchi', TRUE,
     'CI_Giulia_Bianchi.pdf', '/uploads/noleggio/1004/ci_giulia.pdf', 2100000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 8 DAY)),
    (1004, 'BILANCIO_AZIENDA', 'Bilancio Tech Solutions 2023', TRUE,
     'Bilancio_TechSol_2023.pdf', '/uploads/noleggio/1004/bilancio_2023.pdf', 3500000, 'application/pdf', 'VALIDATO',
     DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY),
     2, 2, DATE_SUB(NOW(), INTERVAL 8 DAY));

INSERT INTO noleggio_valutazione (lead_id, rating_creditizio, score_creditizio, storico_pagamenti,
    esposizioni_bancarie, importo_max_fideiussione, fatturato_annuale, utile_netto_annuale,
    rating_solvibilita, status, data_approvazione, utente_creazione_id, utente_approvatore_id, data_creazione)
VALUES 
    (1004, 'A-', 820, 'Eccellente storico, cliente esistente',
     FALSE, 100000.00, 5500000.00, 450000.00,
     'ECCELLENTE', 'APPROVATO', DATE_SUB(NOW(), INTERVAL 4 DAY), 2, 2, DATE_SUB(NOW(), INTERVAL 6 DAY));

-- Ordine creato, in attesa consegna
INSERT INTO noleggio_ordine (id, lead_id, numero_ordine, preventivo_id,
    eta_consegna, note_consegna, status,
    utente_creazione_id, utente_assegnato_id, data_creazione_record)
VALUES 
    (1004, 1004, 'ORD-2026-004', 1004,
     DATE_ADD(NOW(), INTERVAL 5 DAY), 
     'Ordine confermato, 5 Audi A4 in arrivo da Germania, consegna prevista per 5 marzo',
     'CONFERMATO', 2, 2, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- ============== RIEPILOGO DEMO DATA INSERITI ==============

-- FASE PREVENTIVAZIONE: 1 lead (Anna Ferrari)
-- FASE ISTRUTTORIA: 1 lead (Lorenzo Verdi) 
-- FASE ORDINE: 1 lead (Giulia Bianchi)
-- FASE POST_VENDITA: 1 lead (Mario Rossi) con care call, ticket risolto
-- NBT: 1 richiesta breve termine (Sara Conti)

-- Totale: 4 lead noleggio lungo termine + 1 NBT
-- Dashboard KPI:
-- - Preventivazione: 1
-- - Istruttoria: 1
-- - Ordine: 1
-- - Post-Vendita: 1

SELECT 'Demo data inserted successfully' as status,
       (SELECT COUNT(*) FROM noleggio_lead) as total_leads,
       (SELECT COUNT(*) FROM noleggio_preventivo) as total_preventivi,
       (SELECT COUNT(*) FROM noleggio_ordine) as total_ordini,
       (SELECT COUNT(*) FROM noleggio_ticket) as total_ticket,
       (SELECT COUNT(*) FROM noleggio_nbt) as total_nbt;
