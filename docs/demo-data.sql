-- ============================================================
-- Luna2 – Dati di esempio per simulazioni reali
-- Scenario: Tech Solutions Srl – azienda IT
-- ============================================================
-- Esegui nell'ordine: rispetta le dipendenze FK
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. UTENTI
-- ============================================================
INSERT INTO users (id, username, password, email, nome, cognome, ruolo, attivo, data_creazione, data_modifica) VALUES
(1, 'admin',   '$2a$10$Kd5lXjfKz3ZTqYJWq5mY3eQfVnHVxGnuVRLvJlpCh6gJvFzPqD3Ey', 'admin@techsolutions.it',    'Mario',    'Rossi',    'ADMIN',   true, NOW(), NOW()),
(2, 'luca',    '$2a$10$Kd5lXjfKz3ZTqYJWq5mY3eQfVnHVxGnuVRLvJlpCh6gJvFzPqD3Ey', 'luca@techsolutions.it',     'Luca',     'Bianchi',  'MANAGER', true, NOW(), NOW()),
(3, 'giulia',  '$2a$10$Kd5lXjfKz3ZTqYJWq5mY3eQfVnHVxGnuVRLvJlpCh6gJvFzPqD3Ey', 'giulia@techsolutions.it',   'Giulia',   'Ferrari',  'USER',    true, NOW(), NOW());

-- ============================================================
-- 2. CLIENTI
-- ============================================================
INSERT INTO clienti (id, tipo_anagrafica, ragione_sociale, partita_iva, codice_fiscale, codice_sdi, pec, telefono, email, indirizzo, citta, provincia, cap, paese, codice_cliente, condizioni_pagamento, sconto_percentuale, attivo, data_creazione, data_modifica, created_by) VALUES
(1, 'CLIENTE',  'Alfa Informatica Srl',      '02345678901', '02345678901', 'ABC1234', 'alfainf@pec.it',      '02 1234567',  'info@alfainformatica.it',    'Via Roma 10',         'Milano',  'MI', '20100', 'Italia', 'CLI001', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 1),
(2, 'CLIENTE',  'Beta Consulting SpA',       '03456789012', '03456789012', 'DEF5678', 'betacon@pec.it',      '011 9876543', 'amministrazione@betacon.it', 'Corso Francia 55',    'Torino',  'TO', '10138', 'Italia', 'CLI002', '60 giorni data fattura', 5.00,  true, NOW(), NOW(), 1),
(3, 'CLIENTE',  'Gamma Services Srl',        '04567890123', '04567890123', 'GHI9012', 'gammaserv@pec.it',    '055 3456789', 'ufficio@gammaservices.it',   'Via Nazionale 88',    'Firenze', 'FI', '50100', 'Italia', 'CLI003', 'Immediato',              10.00, true, NOW(), NOW(), 1),
(4, 'CLIENTE',  'Delta Group Srl',           '05678901234', '05678901234', 'JKL3456', 'deltagroup@pec.it',   '06 2345678',  'info@deltagroup.it',         'Via Veneto 23',       'Roma',    'RM', '00187', 'Italia', 'CLI004', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 2),
(5, 'PROSPECT', 'Epsilon Tech Srl',          '06789012345', '06789012345', NULL,      NULL,                  '081 8765432', 'contatti@epsilontech.it',    'Via Toledo 150',      'Napoli',  'NA', '80132', 'Italia', 'CLI005', '30 giorni data fattura', 0.00,  true, NOW(), NOW(), 2);

-- ============================================================
-- 3. FORNITORI
-- ============================================================
INSERT INTO fornitori (id, ragione_sociale, partita_iva, codice_fiscale, telefono, email, indirizzo, citta, provincia, cap, paese, codice_fornitore, condizioni_pagamento, attivo, data_creazione, data_modifica, created_by) VALUES
(1, 'Microsoft Italia Srl',   '09876543210', '09876543210', '02 70392000', 'partner@microsoft.com',   'Via Lombardia 1',    'Milano',  'MI', '20121', 'Italia', 'FOR001', '30 giorni',  true, NOW(), NOW(), 1),
(2, 'HP Inc Italia Srl',      '08765432109', '08765432109', '02 34567890', 'ordini@hp.it',             'Via Garibaldi 5',    'Segrate', 'MI', '20054', 'Italia', 'FOR002', '60 giorni',  true, NOW(), NOW(), 1),
(3, 'Cloud Host SpA',         '07654321098', '07654321098', '02 12345678', 'billing@cloudhost.it',     'Via Innovazione 99', 'Milano',  'MI', '20126', 'Italia', 'FOR003', 'Mensile',    true, NOW(), NOW(), 1);

-- ============================================================
-- 4. PRODOTTI
-- ============================================================
INSERT INTO prodotti (id, codice, nome, descrizione, categoria, tipo_prodotto, unita_misura, prezzo_base, costo_acquisto, iva_percentuale, gestione_magazzino, giacenza_minima, attivo, data_creazione, data_modifica, created_by, fornitore_id) VALUES
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
INSERT INTO preventivi (id, numero, anno, data_preventivo, data_validita, cliente_id, stato, oggetto, note_piede, imponibile, iva, totale, condizioni_pagamento, validita_giorni, data_creazione, data_modifica, created_by) VALUES
(1, 'PREV-2026-001', 2026, '2026-01-10', '2026-02-10', 1, 'CONVERTITO', 'Fornitura notebook e licenze software', 'Prezzi IVA esclusa. Consegna in 5 giorni lavorativi.', 1837.00, 404.14, 2241.14, '30 giorni data fattura', 30, NOW(), NOW(), 2),
(2, 'PREV-2026-002', 2026, '2026-02-05', '2026-03-05', 2, 'ACCETTATO',  'Progetto migrazione cloud e formazione', 'Garanzia soddisfatti o rimborsati 30 giorni.',         3380.00, 743.60, 4123.60, '60 giorni data fattura', 30, NOW(), NOW(), 2),
(3, 'PREV-2026-003', 2026, '2026-03-15', '2026-04-15', 3, 'INVIATO',    'Assistenza tecnica mensile', 'Contratto rinnovabile annualmente.',                              1020.00, 224.40, 1244.40, 'Immediato',              30, NOW(), NOW(), 3);

-- ============================================================
-- 6. RIGHE PREVENTIVI
-- ============================================================
INSERT INTO preventivi_righe (id, preventivo_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
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
INSERT INTO ordini (id, numero, anno, data_ordine, cliente_id, preventivo_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, data_consegna_prevista, data_creazione, data_modifica, created_by) VALUES
(1, 'ORD-2026-001', 2026, '2026-01-20', 1, 1, 'EVASO',         'Fornitura notebook e licenze software',   1837.00, 404.14, 2241.14, '30 giorni data fattura', '2026-01-27', NOW(), NOW(), 2),
(2, 'ORD-2026-002', 2026, '2026-02-15', 4, NULL, 'CONFERMATO',  'Rinnovo licenze Microsoft 365 – 10 utenti', 1490.00, 327.80, 1817.80, '30 giorni data fattura', '2026-02-20', NOW(), NOW(), 2),
(3, 'ORD-2026-003', 2026, '2026-03-01', 2, NULL, 'IN_LAVORAZIONE', 'Desktop HP ProDesk x3 + accessori',   2460.00, 541.20, 3001.20, '60 giorni data fattura', '2026-03-10', NOW(), NOW(), 3);

-- ============================================================
-- 8. RIGHE ORDINI
-- ============================================================
INSERT INTO ordini_righe (id, ordine_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
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
INSERT INTO fatture (id, numero, anno, data_fattura, tipo_fattura, cliente_id, ordine_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, fattura_elettronica_inviata, data_creazione, data_modifica, created_by) VALUES
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
INSERT INTO fatture_righe (id, fattura_id, riga_numero, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
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
INSERT INTO fatture_passive (id, numero, anno, data_fattura, fornitore_id, fornitore_nome, fornitore_piva, oggetto, imponibile, iva, totale, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, data_creazione, data_modifica, created_by) VALUES
(1, 'MS-2026-0234', 2026, '2026-01-05', 1, 'Microsoft Italia Srl',  '09876543210', 'Licenze Microsoft 365 – Gennaio batch',  2850.00, 627.00, 3477.00, 'PAGATA',    '2026-02-04', '2026-01-28', 'BONIFICO', NOW(), NOW(), 1),
(2, 'HP-2026-1120', 2026, '2026-01-15', 2, 'HP Inc Italia Srl',     '08765432109', 'Notebook EliteBook 840 G10 x2 + ProDesk x3', 3650.00, 803.00, 4453.00, 'PAGATA', '2026-02-14', '2026-02-10', 'BONIFICO', NOW(), NOW(), 1),
(3, 'CH-2026-0089', 2026, '2026-02-01', 3, 'Cloud Host SpA',        '07654321098', 'Canone hosting infrastruttura – Febbraio',    540.00, 118.80,  658.80, 'PAGATA',   '2026-03-03', '2026-02-25', 'BONIFICO', NOW(), NOW(), 1),
(4, 'CH-2026-0145', 2026, '2026-03-01', 3, 'Cloud Host SpA',        '07654321098', 'Canone hosting infrastruttura – Marzo',       540.00, 118.80,  658.80, 'DA_PAGARE','2026-04-01', NULL,         NULL,       NOW(), NOW(), 1);

-- ============================================================
-- 12. PAGAMENTI
-- ============================================================
INSERT INTO pagamenti (id, fattura_id, importo, data_pagamento, metodo_pagamento, riferimento, banca_mittente, causale, riconciliato, data_riconciliazione, data_creazione, data_modifica, created_by) VALUES
-- Pagamento FT-2026-001 (saldo totale)
(1, 1, 2241.14, '2026-02-20', 'BONIFICO', 'BNF-2026-0142', 'Banca Intesa – Alfa Informatica', 'Pagamento fattura FT-2026-001',       true,  '2026-02-21', NOW(), NOW(), 1),
-- Pagamento FT-2026-002 (saldo totale)
(2, 2, 1817.80, '2026-03-01', 'BONIFICO', 'BNF-2026-0198', 'UniCredit – Delta Group',         'Pagamento fattura FT-2026-002',       true,  '2026-03-02', NOW(), NOW(), 1),
-- Pagamento parziale FT-2026-004 (acconto 50%)
(3, 4,  400.00, '2026-03-10', 'BONIFICO', 'BNF-2026-0210', 'Banca Intesa – Alfa Informatica', 'Acconto 50% fattura FT-2026-004',     false, NULL,         NOW(), NOW(), 2),
-- Secondo acconto FT-2026-004
(4, 4,  200.00, '2026-03-25', 'BONIFICO', 'BNF-2026-0267', 'Banca Intesa – Alfa Informatica', 'Secondo acconto fattura FT-2026-004', false, NULL,         NOW(), NOW(), 2);

-- ============================================================
-- 13. MOVIMENTI MAGAZZINO
-- ============================================================
INSERT INTO movimenti_magazzino (id, prodotto_id, tipo_movimento, causale, quantita, costo_unitario, giacenza_prima, giacenza_dopo, documento_tipo, documento_numero, data_movimento, data_creazione, created_by) VALUES
-- Carichi iniziali da fornitore HP
(1, 3, 'CARICO',  'Acquisto da HP Inc Italia – Fattura HP-2026-1120',  5.000, 890.00,  0.000,  5.000, 'FATTURA_PASSIVA', 'HP-2026-1120', '2026-01-16', NOW(), 1),
(2, 4, 'CARICO',  'Acquisto da HP Inc Italia – Fattura HP-2026-1120',  5.000, 540.00,  0.000,  5.000, 'FATTURA_PASSIVA', 'HP-2026-1120', '2026-01-16', NOW(), 1),
(3, 8, 'CARICO',  'Acquisto accessori mouse – carico inventario',      20.000, 55.00,  0.000, 20.000, NULL,              NULL,           '2026-01-05', NOW(), 1),
-- Scarichi per vendite
(4, 3, 'SCARICO', 'Vendita – Ordine ORD-2026-001 a Alfa Informatica',  1.000, 890.00,  5.000,  4.000, 'ORDINE',          'ORD-2026-001', '2026-01-20', NOW(), 2),
(5, 1, 'SCARICO', 'Vendita licenze – Ordine ORD-2026-001',             2.000,   0.00,  0.000,  0.000, 'ORDINE',          'ORD-2026-001', '2026-01-20', NOW(), 2),
(6, 8, 'SCARICO', 'Vendita mouse – Ordine ORD-2026-001',               1.000,  55.00, 20.000, 19.000, 'ORDINE',          'ORD-2026-001', '2026-01-20', NOW(), 2),
(7, 4, 'SCARICO', 'Vendita desktop – Ordine ORD-2026-003',             3.000, 540.00,  5.000,  2.000, 'ORDINE',          'ORD-2026-003', '2026-03-01', NOW(), 2),
(8, 8, 'SCARICO', 'Vendita mouse – Ordine ORD-2026-003',               3.000,  55.00, 19.000, 16.000, 'ORDINE',          'ORD-2026-003', '2026-03-01', NOW(), 2);

-- ============================================================
-- 14. LIQUIDAZIONI IVA
-- ============================================================
INSERT INTO iva_liquidations (id, liquidation_period, liquidation_date, end_date, period_type, iva_invoices_amount, iva_costs_amount, net_iva_amount, deductible_credits_amount, amount_due, due_date, status, notes, created_at, updated_at) VALUES
-- Gennaio 2026
(1, '2026-01', '2026-01-01', '2026-01-31', 'MONTHLY',
   404.14,                       -- IVA fatture attive (FT-2026-001)
   627.00,                       -- IVA fatture passive (Microsoft)
   -222.86,                      -- IVA netta (credito)
   0.00, 0.00,
   '2026-02-16', 'PAID',
   'Mese di avvio: credito IVA da portare a periodo successivo', NOW(), NOW()),
-- Febbraio 2026
(2, '2026-02', '2026-02-01', '2026-02-28', 'MONTHLY',
   371.58,                       -- IVA FT-2026-002 + FT-2026-003
   921.80,                       -- IVA fatture passive HP + Cloud Host Feb
   -550.22,                      -- ancora credito
   222.86, 0.00,
   '2026-03-16', 'PAID',
   'Credito IVA accumulato portato a nuovo', NOW(), NOW()),
-- Marzo 2026
(3, '2026-03', '2026-03-01', '2026-03-31', 'MONTHLY',
   690.80,                       -- IVA FT-2026-004 + FT-2026-005
   118.80,                       -- IVA Cloud Host Marzo
   572.00,
   550.22, 21.78,                -- credito precedente, residuo da pagare
   '2026-04-16', 'CALCULATED',
   'Da verificare e inviare', NOW(), NOW());

-- ============================================================
-- 15. PIANO DEI CONTI (base italiana)
-- ============================================================
INSERT INTO accounting_accounts (id, code, name, category, system_account, enabled, description, created_at, updated_at) VALUES
-- ATTIVO
(1,  '1100', 'Cassa',                              'ATTIVO',          true,  true, 'Denaro contante',                     NOW(), NOW()),
(2,  '1200', 'Banca c/c',                          'ATTIVO',          true,  true, 'Conto corrente bancario principale',  NOW(), NOW()),
(3,  '1300', 'Crediti verso clienti',              'ATTIVO',          true,  true, 'Crediti commerciali attivi',          NOW(), NOW()),
(4,  '1400', 'IVA a credito',                      'ATTIVO',          true,  true, 'IVA sugli acquisti detraibile',       NOW(), NOW()),
(5,  '1500', 'Ratei e risconti attivi',            'ATTIVO',          false, true, 'Costi/ricavi di competenza futura',   NOW(), NOW()),
(6,  '1600', 'Magazzino merci',                    'ATTIVO',          true,  true, 'Valore giacenza magazzino',           NOW(), NOW()),
-- PASSIVO
(7,  '2100', 'Debiti verso fornitori',             'PASSIVO',         true,  true, 'Debiti commerciali passivi',          NOW(), NOW()),
(8,  '2200', 'IVA a debito',                       'PASSIVO',         true,  true, 'IVA sulle vendite da versare',        NOW(), NOW()),
(9,  '2300', 'Debiti verso erario',                'PASSIVO',         true,  true, 'Imposte e tributi da pagare',         NOW(), NOW()),
(10, '2400', 'Debiti per TFR',                     'PASSIVO',         false, true, 'Trattamento fine rapporto',           NOW(), NOW()),
(11, '2500', 'Ratei e risconti passivi',           'PASSIVO',         false, true, 'Ricavi anticipati',                   NOW(), NOW()),
-- PATRIMONIO NETTO
(12, '3100', 'Capitale sociale',                   'PATRIMONIO_NETTO',true,  true, 'Capitale versato dai soci',           NOW(), NOW()),
(13, '3200', 'Riserve',                            'PATRIMONIO_NETTO',false, true, 'Riserve legali e statutarie',         NOW(), NOW()),
(14, '3300', 'Utile/Perdita d''esercizio',         'PATRIMONIO_NETTO',true,  true, 'Risultato economico esercizio',       NOW(), NOW()),
-- RICAVI
(15, '4100', 'Ricavi da vendita prodotti',         'RICAVI',          true,  true, 'Corrispettivi per prodotti venduti',  NOW(), NOW()),
(16, '4200', 'Ricavi da prestazioni di servizi',   'RICAVI',          true,  true, 'Corrispettivi per servizi erogati',   NOW(), NOW()),
(17, '4300', 'Ricavi da hosting e cloud',          'RICAVI',          false, true, 'Ricavi servizi cloud e hosting',      NOW(), NOW()),
-- COSTI
(18, '5100', 'Acquisto merci e prodotti',          'COSTI',           true,  true, 'Costo acquisto prodotti rivenduti',   NOW(), NOW()),
(19, '5200', 'Costi per servizi',                  'COSTI',           true,  true, 'Consulenze, servizi esterni',         NOW(), NOW()),
(20, '5300', 'Costi per hosting e cloud',          'COSTI',           false, true, 'Costi infrastruttura cloud',          NOW(), NOW()),
(21, '5400', 'Costi del personale',                'COSTI',           true,  true, 'Stipendi e oneri sociali',            NOW(), NOW()),
(22, '5500', 'Ammortamenti',                       'COSTI',           false, true, 'Quota ammortamento cespiti',          NOW(), NOW()),
(23, '5600', 'Spese generali e amministrative',    'COSTI',           false, true, 'Affitti, utenze, cancelleria',        NOW(), NOW()),
-- TRIBUTI
(24, '6100', 'IRES',                               'TRIBUTI',         true,  true, 'Imposta reddito società',             NOW(), NOW()),
(25, '6200', 'IRAP',                               'TRIBUTI',         true,  true, 'Imposta regionale attività produttive',NOW(), NOW());

-- ============================================================
-- 16. REGISTRAZIONI CONTABILI (Prima Nota)
-- ============================================================
INSERT INTO accounting_entries (id, protocol_number, entry_date, competence_date, type, status, description, document_number, source_type, source_id, counterparty, total_debit, total_credit, notes, created_by, created_at, updated_at) VALUES
-- Registrazione fattura FT-2026-001 (vendita)
(1, 'REG-2026-001', '2026-01-27', '2026-01-27', 'FATTURA_ATTIVA',  'POSTED', 'Fattura FT-2026-001 – Alfa Informatica Srl',    'FT-2026-001', 'FATTURA', 1, 'Alfa Informatica Srl',  2241.14, 2241.14, NULL, 1, NOW(), NOW()),
-- Registrazione incasso FT-2026-001
(2, 'REG-2026-002', '2026-02-20', '2026-02-20', 'INCASSO',         'POSTED', 'Incasso FT-2026-001 – bonifico Alfa Informatica', 'BNF-2026-0142', 'PAGAMENTO', 1, 'Alfa Informatica Srl', 2241.14, 2241.14, NULL, 1, NOW(), NOW()),
-- Registrazione fattura passiva HP-2026-1120
(3, 'REG-2026-003', '2026-01-15', '2026-01-15', 'FATTURA_PASSIVA', 'POSTED', 'Fattura passiva HP-2026-1120 – HP Inc Italia',  'HP-2026-1120', 'FATTURA_PASSIVA', 2, 'HP Inc Italia Srl',    4453.00, 4453.00, NULL, 1, NOW(), NOW()),
-- Registrazione pagamento fornitore HP
(4, 'REG-2026-004', '2026-02-10', '2026-02-10', 'PAGAMENTO',       'POSTED', 'Pagamento HP-2026-1120 – bonifico a HP Inc',    'HP-2026-1120', 'PAGAMENTO', 2, 'HP Inc Italia Srl',     4453.00, 4453.00, NULL, 1, NOW(), NOW()),
-- Registrazione costi hosting Cloud Host Feb
(5, 'REG-2026-005', '2026-02-01', '2026-02-28', 'FATTURA_PASSIVA', 'POSTED', 'Fattura passiva CH-2026-0089 – Cloud Host SpA', 'CH-2026-0089', 'FATTURA_PASSIVA', 3, 'Cloud Host SpA',        658.80,  658.80, NULL, 1, NOW(), NOW());

-- ============================================================
-- 17. RIGHE REGISTRAZIONI CONTABILI
-- ============================================================
INSERT INTO accounting_entry_lines (id, entry_id, account_id, line_type, amount, description) VALUES
-- REG-2026-001: Fattura attiva FT-2026-001
(1,  1, 3,  'DEBIT',  2241.14, 'Crediti vs. Alfa Informatica – FT-2026-001'),
(2,  1, 15, 'CREDIT', 1837.00, 'Ricavi vendita prodotti HW+SW'),
(3,  1, 8,  'CREDIT',  404.14, 'IVA a debito 22%'),
-- REG-2026-002: Incasso FT-2026-001
(4,  2, 2,  'DEBIT',  2241.14, 'Banca c/c – Bonifico ricevuto Alfa Informatica'),
(5,  2, 3,  'CREDIT', 2241.14, 'Storno crediti vs. Alfa Informatica'),
-- REG-2026-003: Fattura passiva HP
(6,  3, 18, 'DEBIT',  3650.00, 'Acquisto merci HP – notebook e desktop'),
(7,  3, 4,  'DEBIT',   803.00, 'IVA a credito 22%'),
(8,  3, 7,  'CREDIT', 4453.00, 'Debiti vs. HP Inc Italia'),
-- REG-2026-004: Pagamento HP
(9,  4, 7,  'DEBIT',  4453.00, 'Storno debiti vs. HP Inc Italia'),
(10, 4, 2,  'CREDIT', 4453.00, 'Banca c/c – Bonifico inviato a HP'),
-- REG-2026-005: Fattura passiva Cloud Host
(11, 5, 20, 'DEBIT',   540.00, 'Costi hosting cloud – Febbraio 2026'),
(12, 5, 4,  'DEBIT',   118.80, 'IVA a credito 22%'),
(13, 5, 7,  'CREDIT',  658.80, 'Debiti vs. Cloud Host SpA');

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
