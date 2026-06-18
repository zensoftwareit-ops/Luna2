-- ============================================================
-- Luna2 – Dati di esempio per simulazioni reali
-- Scenario: Tech Solutions Srl – azienda IT
-- ============================================================
-- Esegui nell'ordine: rispetta le dipendenze FK
-- NOTA: Il script pulisce i dati esistenti prima di inserire
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- PULIZIA DATI ESISTENTI (commenta se vuoi mantenere dati precedenti)
-- ============================================================
-- NOTA: Non tocchiamo users, solo i dati di business
-- Solo le tabelle che esistono veramente nel DB
TRUNCATE TABLE fatture_righe;
TRUNCATE TABLE fatture;
TRUNCATE TABLE ordini_righe;
TRUNCATE TABLE ordini;
TRUNCATE TABLE preventivi_righe;
TRUNCATE TABLE preventivi;
TRUNCATE TABLE prodotti;
TRUNCATE TABLE fornitori;
TRUNCATE TABLE clienti;

-- ============================================================
-- 1. UTENTI (SKIP - manteniamo gli utenti esistenti)
-- ============================================================
-- Saltato per preservare utenti e accessi esistenti

-- ============================================================
-- 2. CLIENTI
-- ============================================================
INSERT IGNORE INTO clienti (id, tipo_anagrafica, ragione_sociale, partita_iva, codice_fiscale, codice_sdi, pec, telefono, email, indirizzo, citta, provincia, cap, paese, codice_cliente, condizioni_pagamento, sconto_percentuale, attivo, data_creazione, data_modifica, created_by) VALUES
(1, 'CLIENTE',  'Alfa Informatica Srl',      '02345678901', '02345678901', 'ABC1234', 'alfainf@pec.it',      '02 1234567',  'info@alfainformatica.it',    'Via Roma 10',         'Milano',  'MI', '20100', 'Italia', 'CLI001', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 1),
(2, 'CLIENTE',  'Beta Consulting SpA',       '03456789012', '03456789012', 'DEF5678', 'betacon@pec.it',      '011 9876543', 'amministrazione@betacon.it', 'Corso Francia 55',    'Torino',  'TO', '10138', 'Italia', 'CLI002', '60 giorni data fattura', 5.00,  true, NOW(), NOW(), 1),
(3, 'CLIENTE',  'Gamma Services Srl',        '04567890123', '04567890123', 'GHI9012', 'gammaserv@pec.it',    '055 3456789', 'ufficio@gammaservices.it',   'Via Nazionale 88',    'Firenze', 'FI', '50100', 'Italia', 'CLI003', 'Immediato',              10.00, true, NOW(), NOW(), 1),
(4, 'CLIENTE',  'Delta Group Srl',           '05678901234', '05678901234', 'JKL3456', 'deltagroup@pec.it',   '06 2345678',  'info@deltagroup.it',         'Via Veneto 23',       'Roma',    'RM', '00187', 'Italia', 'CLI004', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 2),
(5, 'PROSPECT', 'Epsilon Tech Srl',          '06789012345', '06789012345', NULL,      NULL,                  '081 8765432', 'contatti@epsilontech.it',    'Via Toledo 150',      'Napoli',  'NA', '80132', 'Italia', 'CLI005', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 2);

-- ============================================================
-- 3. FORNITORI
-- ============================================================
INSERT IGNORE INTO fornitori (id, ragione_sociale, partita_iva, codice_fiscale, telefono, email, indirizzo, citta, provincia, cap, paese, codice_fornitore, condizioni_pagamento, attivo, data_creazione, data_modifica, created_by) VALUES
(1, 'Microsoft Italia Srl',   '09876543210', '09876543210', '02 70392000', 'partner@microsoft.com',   'Via Lombardia 1',    'Milano',  'MI', '20121', 'Italia', 'FOR001', '30 giorni',  true, NOW(), NOW(), 1),
(2, 'HP Inc Italia Srl',      '08765432109', '08765432109', '02 34567890', 'ordini@hp.it',             'Via Garibaldi 5',    'Segrate', 'MI', '20054', 'Italia', 'FOR002', '60 giorni',  true, NOW(), NOW(), 1),
(3, 'Cloud Host SpA',         '07654321098', '07654321098', '02 12345678', 'billing@cloudhost.it',     'Via Innovazione 99', 'Milano',  'MI', '20126', 'Italia', 'FOR003', 'Mensile',    true, NOW(), NOW(), 1);

-- ============================================================
-- 4. PRODOTTI
-- ============================================================
INSERT IGNORE INTO prodotti (id, codice, nome, descrizione, categoria, tipo_prodotto, unita_misura, prezzo_base, costo_acquisto, iva_percentuale, gestione_magazzino, giacenza_minima, attivo, data_creazione, data_modifica, created_by, fornitore_id) VALUES
(1,  'SW-WIN11',   'Windows 11 Pro – Licenza',         'Licenza OEM Windows 11 Professional 64bit',                    'Software',  'STANDARD', 'PEZZO', 259.00,  180.00, 22.00, false, NULL,  true, NOW(), NOW(), 1, 1),
(2,  'SW-OFF365',  'Microsoft 365 Business – Annuale', 'Abbonamento annuale Microsoft 365 Business Standard per utente','Software',  'STANDARD', 'PEZZO', 149.00,  95.00,  22.00, false, NULL,  true, NOW(), NOW(), 1, 1),
(3,  'HW-NB001',   'Notebook HP EliteBook 840',        'HP EliteBook 840 G10, i7, 16GB RAM, SSD 512GB',               'Hardware',  'STANDARD', 'PEZZO', 1290.00, 890.00, 22.00, true,  3.00,  true, NOW(), NOW(), 1, 2),
(4,  'HW-PC001',   'Desktop HP ProDesk 400',           'HP ProDesk 400 G9, i5, 8GB RAM, SSD 256GB',                   'Hardware',  'STANDARD', 'PEZZO', 790.00,  540.00, 22.00, true,  5.00,  true, NOW(), NOW(), 1, 2),
(5,  'SRV-CLOUD',  'Hosting Cloud – Mensile',          'Server cloud 4 vCPU, 8GB RAM, 100GB SSD',                     'Servizi',   'SERVIZIO', 'ORA',   199.00,  90.00,  22.00, false, NULL,  true, NOW(), NOW(), 1, 3),
(6,  'SRV-ASS',    'Assistenza Tecnica On-site',       'Intervento tecnico on-site – tariffa oraria',                  'Servizi',   'SERVIZIO', 'ORA',   85.00,   0.00,   22.00, false, NULL,  true, NOW(), NOW(), 1, NULL),
(7,  'SRV-FORM',   'Formazione – Giornata',            'Corso di formazione tecnica, giornata intera',                 'Servizi',   'SERVIZIO', 'ORA',   650.00,  0.00,   22.00, false, NULL,  true, NOW(), NOW(), 1, NULL),
(8,  'ACC-MOUSE',  'Mouse Logitech MX Master 3',       'Mouse wireless Logitech MX Master 3 per ufficio',              'Accessori', 'STANDARD', 'PEZZO', 89.00,   55.00,  22.00, true,  10.00, true, NOW(), NOW(), 1, NULL);

-- ============================================================
-- 5. PREVENTIVI
-- ============================================================
INSERT IGNORE INTO preventivi (id, numero, anno, data_preventivo, data_validita, cliente_id, stato, oggetto, note_piede, imponibile, iva, totale, condizioni_pagamento, validita_giorni, data_creazione, data_modifica, created_by) VALUES
(1, 'PREV-2026-001', 2026, '2026-01-10', '2026-02-10', 1, 'CONVERTITO', 'Fornitura notebook e licenze software', 'Prezzi IVA esclusa. Consegna in 5 giorni lavorativi.', 1837.00, 404.14, 2241.14, '30 giorni data fattura', 30, NOW(), NOW(), 2),
(2, 'PREV-2026-002', 2026, '2026-02-05', '2026-03-05', 2, 'ACCETTATO',  'Progetto migrazione cloud e formazione', 'Garanzia soddisfatti o rimborsati 30 giorni.',         3380.00, 743.60, 4123.60, '60 giorni data fattura', 30, NOW(), NOW(), 2),
(3, 'PREV-2026-003', 2026, '2026-03-15', '2026-04-15', 3, 'INVIATO',    'Assistenza tecnica mensile', 'Contratto rinnovabile annualmente.',                              1020.00, 224.40, 1244.40, 'Immediato',              30, NOW(), NOW(), 3);

-- ============================================================
-- 6. RIGHE PREVENTIVI
-- ============================================================
INSERT IGNORE INTO preventivi_righe (id, preventivo_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
-- PREV-2026-001
(1,  1, 1, 'PRODOTTO',    3, 'Notebook HP EliteBook 840 G10 i7 16GB SSD512',   1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2,  1, 2, 'PRODOTTO',    1, 'Windows 11 Pro – Licenza OEM',                   2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3,  1, 3, 'PRODOTTO',    8, 'Mouse Logitech MX Master 3',                     1.000, 'PEZZO',   89.00, 0.00, 0.00,   89.00, 22.00,  19.58,  108.58, NOW()),
-- PREV-2026-002
(4,  2, 1, 'PRODOTTO',    5, 'Hosting Cloud 4vCPU 8GB – 12 mesi',             12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW()),
(5,  2, 2, 'PRODOTTO',    7, 'Formazione migrazione cloud – 1 giornata',        1.000, 'PEZZO',  650.00, 0.00, 0.00,  650.00, 22.00, 143.00,  793.00, NOW()),
(6,  2, 3, 'PRODOTTO',    6, 'Assistenza tecnica configurazione – 4 ore',       4.000, 'ORA',     85.00, 0.00, 0.00,  340.00, 22.00,  74.80,  414.80, NOW()),
-- PREV-2026-003
(7,  3, 1, 'PRODOTTO',    6, 'Assistenza tecnica mensile – 12 ore',            12.000, 'ORA',     85.00, 0.00, 0.00, 1020.00, 22.00, 224.40, 1244.40, NOW());

-- ============================================================
-- 7. ORDINI
-- ============================================================
INSERT IGNORE INTO ordini (id, numero, anno, data_ordine, cliente_id, preventivo_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, data_consegna_prevista, data_creazione, data_modifica, created_by) VALUES
(1, 'ORD-2026-001', 2026, '2026-01-20', 1, 1, 'EVASO',         'Fornitura notebook e licenze software',   1837.00, 404.14, 2241.14, '30 giorni data fattura', '2026-01-27', NOW(), NOW(), 2),
(2, 'ORD-2026-002', 2026, '2026-02-15', 4, NULL, 'CONFERMATO',  'Rinnovo licenze Microsoft 365 – 10 utenti', 1490.00, 327.80, 1817.80, '30 giorni data fattura', '2026-02-20', NOW(), NOW(), 2),
(3, 'ORD-2026-003', 2026, '2026-03-01', 2, NULL, 'IN_LAVORAZIONE', 'Desktop HP ProDesk x3 + accessori',   2460.00, 541.20, 3001.20, '60 giorni data fattura', '2026-03-10', NOW(), NOW(), 3);

-- ============================================================
-- 8. RIGHE ORDINI
-- ============================================================
INSERT IGNORE INTO ordini_righe (id, ordine_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
-- ORD-2026-001
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook 840 G10',         1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro',                        2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 1, 3, 'PRODOTTO', 8, 'Mouse Logitech MX Master 3',            1.000, 'PEZZO',   89.00, 0.00, 0.00,   89.00, 22.00,  19.58,  108.58, NOW()),
-- ORD-2026-002
(4, 2, 1, 'PRODOTTO', 2, 'Microsoft 365 Business – 10 utenti',   10.000, 'PEZZO',  149.00, 0.00, 0.00, 1490.00, 22.00, 327.80, 1817.80, NOW()),
-- ORD-2026-003
(5, 3, 1, 'PRODOTTO', 4, 'Desktop HP ProDesk 400 G9',             3.000, 'PEZZO',  790.00, 0.00, 0.00, 2370.00, 22.00, 521.40, 2891.40, NOW()),
(6, 3, 2, 'PRODOTTO', 8, 'Mouse Logitech MX Master 3',            3.000, 'PEZZO',   89.00, 5.00, 0.00,  253.35, 22.00,  55.74,  309.09, NOW()); -- sconto 5%

-- ============================================================
-- 9. FATTURE ATTIVE
-- ============================================================
INSERT IGNORE INTO fatture (id, numero, anno, data_fattura, tipo_fattura, cliente_id, ordine_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, fattura_elettronica_inviata, data_creazione, data_modifica, created_by) VALUES
-- Gennaio: fattura su ordine 1, PAGATA
(1,  'FT-2026-001', 2026, '2026-01-27', 'REALE',     1, 1, 'Fornitura notebook HP + licenze Windows 11',  1837.00, 404.14, 2241.14, 2241.14, 'PAGATA',           '2026-02-26', '2026-02-20', 'BONIFICO', true,  NOW(), NOW(), 1),
-- Febbraio: fattura licenze Microsoft, PAGATA
(2,  'FT-2026-002', 2026, '2026-02-01', 'REALE',     4, 2, 'Licenze Microsoft 365 Business – 10 utenti', 1490.00, 327.80, 1817.80, 1817.80, 'PAGATA',           '2026-03-03', '2026-03-01', 'BONIFICO', true,  NOW(), NOW(), 1),
-- Febbraio: hosting cloud mensile cliente 2, DA_PAGARE
(3,  'FT-2026-003', 2026, '2026-02-28', 'REALE',     2, NULL, 'Hosting Cloud – Febbraio 2026',            199.00,  43.78,  242.78,  242.78, 'DA_PAGARE',        '2026-03-30', NULL,         NULL,       true,  NOW(), NOW(), 1),
-- Marzo: assistenza tecnica, PARZIALMENTE_PAGATA
(4,  'FT-2026-004', 2026, '2026-03-05', 'REALE',     1, NULL, 'Assistenza tecnica – Febbraio 2026',       680.00,  149.60, 829.60,  829.60, 'PARZIALMENTE_PAGATA','2026-04-04', NULL,        NULL,       true,  NOW(), NOW(), 2),
-- Marzo: desktop HP, DA_PAGARE
(5,  'FT-2026-005', 2026, '2026-03-15', 'REALE',     2, 3,  'Fornitura Desktop HP ProDesk x3 + accessori',2460.00, 541.20, 3001.20, 3001.20, 'DA_PAGARE',       '2026-05-14', NULL,         NULL,       true,  NOW(), NOW(), 2),
-- Gennaio: proforma, DA_PAGARE
(6,  'FT-2026-006', 2026, '2026-01-15', 'PROFORMA',  5, NULL, 'Offerta formazione tecnica',               650.00,  143.00, 793.00,  793.00, 'DA_PAGARE',        '2026-02-14', NULL,         NULL,       false, NOW(), NOW(), 3);

-- ============================================================
-- 10. RIGHE FATTURE ATTIVE
-- ============================================================
INSERT IGNORE INTO fatture_righe (id, fattura_id, riga_numero, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
-- FT-2026-001
(1,  1, 1, 3, 'Notebook HP EliteBook 840 G10 i7 16GB SSD512',  1.000, 1290.00, 1290.00, 22.00, 1573.80),
(2,  1, 2, 1, 'Windows 11 Pro – Licenza OEM',                  2.000,  259.00,  518.00, 22.00,  631.96),
(3,  1, 3, 8, 'Mouse Logitech MX Master 3',                    1.000,   89.00,   89.00, 22.00,  108.58),
-- FT-2026-002
(4,  2, 1, 2, 'Microsoft 365 Business Standard – 10 utenti',  10.000,  149.00, 1490.00, 22.00, 1817.80),
-- FT-2026-003
(5,  3, 1, 5, 'Hosting Cloud 4vCPU 8GB 100GB SSD – Febbraio',  1.000,  199.00,  199.00, 22.00,  242.78),
-- FT-2026-004
(6,  4, 1, 6, 'Assistenza tecnica on-site – 8 ore',            8.000,   85.00,  680.00, 22.00,  829.60),
-- FT-2026-005
(7,  5, 1, 4, 'Desktop HP ProDesk 400 G9 i5 8GB SSD256',       3.000,  790.00, 2370.00, 22.00, 2891.40),
(8,  5, 2, 8, 'Mouse Logitech MX Master 3 (3 pz)',             3.000,   89.00,  253.35, 22.00,  309.09),
-- FT-2026-006
(9,  6, 1, 7, 'Formazione tecnica – 1 giornata',               1.000,  650.00,  650.00, 22.00,  793.00);

-- ============================================================
-- 11. FATTURE PASSIVE (Fornitori)
-- ============================================================
-- ============================================================
-- 12. PAGAMENTI
-- ============================================================
-- ============================================================
-- 13. MOVIMENTI MAGAZZINO
-- ============================================================
-- ============================================================
-- 14. LIQUIDAZIONI IVA
-- ============================================================
-- ============================================================
-- 15. PIANO DEI CONTI (base italiana)
-- ============================================================
-- ============================================================
-- 16. REGISTRAZIONI CONTABILI (Prima Nota)
-- ============================================================
-- ============================================================
-- 17. RIGHE REGISTRAZIONI CONTABILI
-- ============================================================
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- VERIFICA DATI INSERITI
-- ============================================================
SELECT 'UTENTI'            AS tabella, COUNT(*) AS righe FROM users
UNION ALL SELECT 'CLIENTI',            COUNT(*) FROM clienti
UNION ALL SELECT 'FORNITORI',          COUNT(*) FROM fornitori
UNION ALL SELECT 'PRODOTTI',           COUNT(*) FROM prodotti
UNION ALL SELECT 'PREVENTIVI',         COUNT(*) FROM preventivi
UNION ALL SELECT 'ORDINI',             COUNT(*) FROM ordini
UNION ALL SELECT 'FATTURE ATTIVE',     COUNT(*) FROM fatture
UNION ALL SELECT 'FATTURE PASSIVE',    COUNT(*) FROM fatture_passive
UNION ALL SELECT 'PAGAMENTI',          COUNT(*) FROM pagamenti
UNION ALL SELECT 'MOV. MAGAZZINO',     COUNT(*) FROM movimenti_magazzino
UNION ALL SELECT 'LIQUIDAZIONI IVA',   COUNT(*) FROM iva_liquidations
UNION ALL SELECT 'PIANO DEI CONTI',    COUNT(*) FROM accounting_accounts
UNION ALL SELECT 'REGISTRAZIONI',      COUNT(*) FROM accounting_entries
UNION ALL SELECT 'RIGHE REGISTRAZIONI',COUNT(*) FROM accounting_entry_lines;
