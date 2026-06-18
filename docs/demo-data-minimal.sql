-- ============================================================
-- Luna2 – Dati di esempio MINIMALI
-- Basato SOLO su tabelle che veramente esistono nel DB
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Pulizia dati
TRUNCATE TABLE fatture_righe;
TRUNCATE TABLE fatture;
TRUNCATE TABLE ordini_righe;
TRUNCATE TABLE ordini;
TRUNCATE TABLE preventivi_righe;
TRUNCATE TABLE preventivi;
TRUNCATE TABLE ddt_righe;
TRUNCATE TABLE ddt;
TRUNCATE TABLE prodotti;
TRUNCATE TABLE fornitori;
TRUNCATE TABLE clienti;

-- ============================================================
-- CLIENTI
-- ============================================================
INSERT INTO clienti (id, tipo_anagrafica, ragione_sociale, partita_iva, codice_fiscale, codice_sdi, pec, telefono, email, indirizzo, citta, provincia, cap, paese, codice_cliente, condizioni_pagamento, sconto_percentuale, attivo, data_creazione, created_by) VALUES
(1, 'CLIENTE',  'Alfa Informatica Srl',      '02345678901', '02345678901', 'ABC1234', 'alfainf@pec.it',      '02 1234567',  'info@alfainformatica.it',    'Via Roma 10',         'Milano',  'MI', '20100', 'Italia', 'CLI001', '30 giorni', 0.00,  true, NOW(), 1),
(2, 'CLIENTE',  'Beta Consulting SpA',       '03456789012', '03456789012', 'DEF5678', 'betacon@pec.it',      '011 9876543', 'info@betacon.it',            'Corso Francia 55',    'Torino',  'TO', '10138', 'Italia', 'CLI002', '60 giorni', 5.00,  true, NOW(), 1),
(3, 'CLIENTE',  'Gamma Services Srl',        '04567890123', '04567890123', 'GHI9012', 'gammaserv@pec.it',    '055 3456789', 'ufficio@gammaservices.it',   'Via Nazionale 88',    'Firenze', 'FI', '50100', 'Italia', 'CLI003', 'Immediato', 10.00, true, NOW(), 1);

-- ============================================================
-- FORNITORI
-- ============================================================
INSERT INTO fornitori (id, ragione_sociale, partita_iva, codice_fiscale, telefono, email, indirizzo, citta, provincia, cap, paese, codice_fornitore, condizioni_pagamento, attivo, data_creazione, created_by) VALUES
(1, 'Microsoft Italia Srl',   '09876543210', '09876543210', '02 70392000', 'partner@microsoft.com',   'Via Lombardia 1',    'Milano',  'MI', '20121', 'Italia', 'FOR001', '30 giorni',  true, NOW(), 1),
(2, 'HP Inc Italia Srl',      '08765432109', '08765432109', '02 34567890', 'ordini@hp.it',             'Via Garibaldi 5',    'Segrate', 'MI', '20054', 'Italia', 'FOR002', '60 giorni',  true, NOW(), 1);

-- ============================================================
-- PRODOTTI
-- ============================================================
INSERT INTO prodotti (id, codice, nome, descrizione, categoria, tipo_prodotto, unita_misura, prezzo_base, costo_acquisto, iva_percentuale, gestione_magazzino, giacenza_minima, attivo, data_creazione, created_by, fornitore_id) VALUES
(1,  'SW-WIN11',   'Windows 11 Pro',               'Licenza OEM Windows 11',                        'Software',  'STANDARD', 'PEZZO', 259.00,  180.00, 22.00, false, NULL,  true, NOW(), 1, 1),
(2,  'SW-OFF365',  'Microsoft 365 Business',      'Abbonamento annuale',                          'Software',  'STANDARD', 'PEZZO', 149.00,  95.00,  22.00, false, NULL,  true, NOW(), 1, 1),
(3,  'HW-NB001',   'Notebook HP EliteBook 840',   'HP EliteBook 840 G10 i7 16GB',                  'Hardware',  'STANDARD', 'PEZZO', 1290.00, 890.00, 22.00, true,  3.00,  true, NOW(), 1, 2),
(4,  'HW-PC001',   'Desktop HP ProDesk 400',      'HP ProDesk 400 G9 i5 8GB',                      'Hardware',  'STANDARD', 'PEZZO', 790.00,  540.00, 22.00, true,  5.00,  true, NOW(), 1, 2),
(5,  'SRV-CLOUD',  'Hosting Cloud Mensile',       'Server cloud 4vCPU 8GB 100GB SSD',             'Servizi',   'SERVIZIO', 'PEZZO', 199.00,  90.00,  22.00, false, NULL,  true, NOW(), 1, NULL),
(6,  'SRV-ASS',    'Assistenza Tecnica',          'Intervento tecnico on-site',                    'Servizi',   'SERVIZIO', 'ORA',   85.00,   0.00,   22.00, false, NULL,  true, NOW(), 1, NULL);

-- ============================================================
-- PREVENTIVI
-- ============================================================
INSERT INTO preventivi (id, numero, anno, data_preventivo, data_validita, cliente_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, validita_giorni, data_creazione, created_by) VALUES
(1, 'PREV-2026-001', 2026, '2026-01-10', '2026-02-10', 1, 'CONVERTITO', 'Fornitura notebook e software',   1549.00, 340.78, 1889.78, '30 giorni', 30, NOW(), 1),
(2, 'PREV-2026-002', 2026, '2026-02-05', '2026-03-05', 2, 'ACCETTATO',  'Migrazione cloud',                 2380.00, 523.60, 2903.60, '60 giorni', 30, NOW(), 1);

-- ============================================================
-- PREVENTIVI RIGHE
-- ============================================================
INSERT INTO preventivi_righe (id, preventivo_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW());

-- ============================================================
-- ORDINI
-- ============================================================
INSERT INTO ordini (id, numero, anno, data_ordine, cliente_id, preventivo_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, data_consegna_prevista, data_creazione, created_by) VALUES
(1, 'ORD-2026-001', 2026, '2026-01-20', 1, 1, 'EVASO', 'Notebook e software',     1549.00, 340.78, 1889.78, '30 giorni', '2026-01-27', NOW(), 1),
(2, 'ORD-2026-002', 2026, '2026-02-15', 2, 2, 'CONFERMATO', 'Hosting cloud',      2380.00, 523.60, 2903.60, '60 giorni', '2026-03-01', NOW(), 1);

-- ============================================================
-- ORDINI RIGHE
-- ============================================================
INSERT INTO ordini_righe (id, ordine_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW());

-- ============================================================
-- FATTURE
-- ============================================================
INSERT INTO fatture (id, numero, anno, data_fattura, tipo_fattura, cliente_id, ordine_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, fattura_elettronica_inviata, data_creazione, created_by) VALUES
(1, 'FT-2026-001', 2026, '2026-01-27', 'REALE', 1, 1, 'Fornitura notebook e software', 1549.00, 340.78, 1889.78, 1889.78, 'PAGATA', '2026-02-26', '2026-02-20', 'BONIFICO', true, NOW(), 1),
(2, 'FT-2026-002', 2026, '2026-02-28', 'REALE', 2, 2, 'Hosting cloud febbraio',        199.00,  43.78,  242.78,  242.78, 'DA_PAGARE', '2026-03-30', NULL,         NULL,       true, NOW(), 1),
(3, 'FT-2026-003', 2026, '2026-03-15', 'REALE', 1, NULL, 'Assistenza tecnica',           510.00, 112.20,  622.20,  622.20, 'PARZIALMENTE_PAGATA', '2026-04-14', NULL,         NULL,       true, NOW(), 1);

-- ============================================================
-- FATTURE RIGHE
-- ============================================================
INSERT INTO fatture_righe (id, fattura_id, riga_numero, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 3, 'Notebook HP EliteBook', 1.000, 1290.00, 1290.00, 22.00, 1573.80),
(2, 1, 2, 1, 'Windows 11 Pro', 2.000,  259.00,  518.00, 22.00,  631.96),
(3, 2, 1, 5, 'Hosting cloud febbraio',  1.000,  199.00,  199.00, 22.00,  242.78),
(4, 3, 1, 6, 'Assistenza tecnica', 6.000,   85.00,  510.00, 22.00,  622.20);

SET FOREIGN_KEY_CHECKS = 1;

-- Verifica inserimento
SELECT 'Clienti' AS tabella, COUNT(*) AS righe FROM clienti
UNION ALL SELECT 'Fornitori', COUNT(*) FROM fornitori
UNION ALL SELECT 'Prodotti', COUNT(*) FROM prodotti
UNION ALL SELECT 'Preventivi', COUNT(*) FROM preventivi
UNION ALL SELECT 'Ordini', COUNT(*) FROM ordini
UNION ALL SELECT 'Fatture', COUNT(*) FROM fatture;
