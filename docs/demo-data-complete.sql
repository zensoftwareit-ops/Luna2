-- ============================================================
-- Luna2 – Dati di esempio COMPLETI
-- Per simulazioni reali: fatturazione, contabilità, pagamenti
-- ============================================================
-- Esegui dopo aver aggiornato il deploy con le ultime entity registrazioni

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- PULIZIA DATI ESISTENTI
-- ============================================================
TRUNCATE TABLE accounting_entry_lines;
TRUNCATE TABLE accounting_entries;
TRUNCATE TABLE iva_liquidations;
TRUNCATE TABLE pagamenti;
TRUNCATE TABLE fatture_righe;
TRUNCATE TABLE fatture_passive_righe;
TRUNCATE TABLE fatture_passive;
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
-- 1. CLIENTI
-- ============================================================
INSERT INTO clienti (id, tipo_anagrafica, ragione_sociale, partita_iva, codice_fiscale, codice_sdi, pec, telefono, email, indirizzo, citta, provincia, cap, paese, codice_cliente, condizioni_pagamento, sconto_percentuale, attivo, data_creazione, created_by) VALUES
(1, 'CLIENTE',  'Alfa Informatica Srl',      '02345678901', '02345678901', 'ABC1234', 'alfainf@pec.it',      '02 1234567',  'info@alfainformatica.it',    'Via Roma 10',         'Milano',  'MI', '20100', 'Italia', 'CLI001', '30 giorni', 0.00,  true, NOW(), 1),
(2, 'CLIENTE',  'Beta Consulting SpA',       '03456789012', '03456789012', 'DEF5678', 'betacon@pec.it',      '011 9876543', 'info@betacon.it',            'Corso Francia 55',    'Torino',  'TO', '10138', 'Italia', 'CLI002', '60 giorni', 5.00,  true, NOW(), 1),
(3, 'CLIENTE',  'Gamma Services Srl',        '04567890123', '04567890123', 'GHI9012', 'gammaserv@pec.it',    '055 3456789', 'ufficio@gammaservices.it',   'Via Nazionale 88',    'Firenze', 'FI', '50100', 'Italia', 'CLI003', 'Immediato', 10.00, true, NOW(), 1),
(4, 'CLIENTE',  'Delta Group Srl',           '05678901234', '05678901234', 'JKL3456', 'deltagroup@pec.it',   '06 2345678',  'info@deltagroup.it',         'Via Veneto 23',       'Roma',    'RM', '00187', 'Italia', 'CLI004', '30 giorni', 0.00,  true, NOW(), 1);

-- ============================================================
-- 2. FORNITORI
-- ============================================================
INSERT INTO fornitori (id, ragione_sociale, partita_iva, codice_fiscale, telefono, email, indirizzo, citta, provincia, cap, paese, codice_fornitore, condizioni_pagamento, attivo, data_creazione, created_by) VALUES
(1, 'Microsoft Italia Srl',   '09876543210', '09876543210', '02 70392000', 'partner@microsoft.com',   'Via Lombardia 1',    'Milano',  'MI', '20121', 'Italia', 'FOR001', '30 giorni',  true, NOW(), 1),
(2, 'HP Inc Italia Srl',      '08765432109', '08765432109', '02 34567890', 'ordini@hp.it',             'Via Garibaldi 5',    'Segrate', 'MI', '20054', 'Italia', 'FOR002', '60 giorni',  true, NOW(), 1),
(3, 'Cloud Host SpA',         '07654321098', '07654321098', '02 12345678', 'billing@cloudhost.it',     'Via Innovazione 99', 'Milano',  'MI', '20126', 'Italia', 'FOR003', 'Mensile',    true, NOW(), 1);

-- ============================================================
-- 3. PRODOTTI
-- ============================================================
INSERT INTO prodotti (id, codice, nome, descrizione, categoria, tipo_prodotto, unita_misura, prezzo_base, costo_acquisto, iva_percentuale, gestione_magazzino, giacenza_minima, attivo, data_creazione, created_by, fornitore_id) VALUES
(1,  'SW-WIN11',   'Windows 11 Pro',               'Licenza OEM Windows 11',                        'Software',  'STANDARD', 'PEZZO', 259.00,  180.00, 22.00, false, NULL,  true, NOW(), 1, 1),
(2,  'SW-OFF365',  'Microsoft 365 Business',      'Abbonamento annuale',                          'Software',  'STANDARD', 'PEZZO', 149.00,  95.00,  22.00, false, NULL,  true, NOW(), 1, 1),
(3,  'HW-NB001',   'Notebook HP EliteBook 840',   'HP EliteBook 840 G10 i7 16GB',                  'Hardware',  'STANDARD', 'PEZZO', 1290.00, 890.00, 22.00, true,  3.00,  true, NOW(), 1, 2),
(4,  'HW-PC001',   'Desktop HP ProDesk 400',      'HP ProDesk 400 G9 i5 8GB',                      'Hardware',  'STANDARD', 'PEZZO', 790.00,  540.00, 22.00, true,  5.00,  true, NOW(), 1, 2),
(5,  'SRV-CLOUD',  'Hosting Cloud Mensile',       'Server cloud 4vCPU 8GB 100GB SSD',             'Servizi',   'SERVIZIO', 'PEZZO', 199.00,  90.00,  22.00, false, NULL,  true, NOW(), 1, 3),
(6,  'SRV-ASS',    'Assistenza Tecnica',          'Intervento tecnico on-site',                    'Servizi',   'SERVIZIO', 'ORA',   85.00,   0.00,   22.00, false, NULL,  true, NOW(), 1, NULL);

-- ============================================================
-- 4. PREVENTIVI
-- ============================================================
INSERT INTO preventivi (id, numero, anno, data_preventivo, data_validita, cliente_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, validita_giorni, data_creazione, created_by) VALUES
(1, 'PREV-2026-001', 2026, '2026-01-10', '2026-02-10', 1, 'CONVERTITO', 'Fornitura notebook e software',   1549.00, 340.78, 1889.78, '30 giorni', 30, NOW(), 1),
(2, 'PREV-2026-002', 2026, '2026-02-05', '2026-03-05', 2, 'ACCETTATO',  'Migrazione cloud',                 2380.00, 523.60, 2903.60, '60 giorni', 30, NOW(), 1);

-- ============================================================
-- 5. RIGHE PREVENTIVI
-- ============================================================
INSERT INTO preventivi_righe (id, preventivo_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW());

-- ============================================================
-- 6. ORDINI
-- ============================================================
INSERT INTO ordini (id, numero, anno, data_ordine, cliente_id, preventivo_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, data_consegna_prevista, data_creazione, created_by) VALUES
(1, 'ORD-2026-001', 2026, '2026-01-20', 1, 1, 'EVASO', 'Notebook e software',     1549.00, 340.78, 1889.78, '30 giorni', '2026-01-27', NOW(), 1),
(2, 'ORD-2026-002', 2026, '2026-02-15', 2, 2, 'CONFERMATO', 'Hosting cloud',      2380.00, 523.60, 2903.60, '60 giorni', '2026-03-01', NOW(), 1);

-- ============================================================
-- 7. RIGHE ORDINI
-- ============================================================
INSERT INTO ordini_righe (id, ordine_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW());

-- ============================================================
-- 8. FATTURE ATTIVE
-- ============================================================
INSERT INTO fatture (id, numero, anno, data_fattura, tipo_fattura, cliente_id, ordine_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, fattura_elettronica_inviata, data_creazione, created_by) VALUES
(1, 'FT-2026-001', 2026, '2026-01-27', 'REALE', 1, 1, 'Fornitura notebook e software', 1549.00, 340.78, 1889.78, 1889.78, 'PAGATA', '2026-02-26', '2026-02-20', 'BONIFICO', true, NOW(), 1),
(2, 'FT-2026-002', 2026, '2026-02-28', 'REALE', 2, 2, 'Hosting cloud febbraio',        199.00,  43.78,  242.78,  242.78, 'DA_PAGARE', '2026-03-30', NULL,         NULL,       true, NOW(), 1),
(3, 'FT-2026-003', 2026, '2026-03-15', 'REALE', 1, NULL, 'Assistenza tecnica',           510.00, 112.20,  622.20,  622.20, 'PARZIALMENTE_PAGATA', '2026-04-14', NULL,         NULL,       true, NOW(), 1);

-- ============================================================
-- 9. RIGHE FATTURE ATTIVE
-- ============================================================
INSERT INTO fatture_righe (id, fattura_id, riga_numero, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 3, 'Notebook HP EliteBook', 1.000, 1290.00, 1290.00, 22.00, 1573.80),
(2, 1, 2, 1, 'Windows 11 Pro', 2.000,  259.00,  518.00, 22.00,  631.96),
(3, 2, 1, 5, 'Hosting cloud febbraio',  1.000,  199.00,  199.00, 22.00,  242.78),
(4, 3, 1, 6, 'Assistenza tecnica', 6.000,   85.00,  510.00, 22.00,  622.20);

-- ============================================================
-- 10. PAGAMENTI
-- ============================================================
INSERT INTO pagamenti (id, fattura_id, importo, data_pagamento, metodo_pagamento, numero_riferimento, note, data_creazione, created_by) VALUES
(1, 1, 1889.78, '2026-02-20', 'BONIFICO', 'RIF-001', 'Pagamento fattura FT-2026-001', NOW(), 1),
(2, 3, 311.10, '2026-04-01', 'ASSEGNO', 'CHK-001', 'Acconto assistenza tecnica', NOW(), 1);

-- ============================================================
-- 11. FATTURE PASSIVE (acquisti da fornitori)
-- ============================================================
INSERT INTO fatture_passive (id, numero, anno, data_fattura, tipo_fattura, fornitore_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, data_creazione, created_by) VALUES
(1, 'FP-2026-001', 2026, '2026-01-15', 'REALE', 2, 'Notebook HP x5', 6450.00, 1419.00, 7869.00, 7869.00, 'PAGATA', '2026-02-14', '2026-02-10', 'BONIFICO', NOW(), 1),
(2, 'FP-2026-002', 2026, '2026-02-20', 'REALE', 3, 'Hosting cloud – Febbraio', 199.00, 43.78, 242.78, 242.78, 'DA_PAGARE', '2026-03-20', NULL, NULL, NOW(), 1),
(3, 'FP-2026-003', 2026, '2026-03-01', 'REALE', 1, 'Licenze Windows 11 x10', 2590.00, 569.80, 3159.80, 3159.80, 'PARZIALMENTE_PAGATA', '2026-03-31', NULL, NULL, NOW(), 1);

-- ============================================================
-- 12. RIGHE FATTURE PASSIVE
-- ============================================================
INSERT INTO fatture_passive_righe (id, fattura_passiva_id, riga_numero, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 'Notebook HP EliteBook 840 G10', 5.000, 1290.00, 6450.00, 22.00, 7869.00),
(2, 2, 1, 'Hosting cloud – Febbraio 2026', 1.000, 199.00, 199.00, 22.00, 242.78),
(3, 3, 1, 'Windows 11 Pro – Licenza OEM x10', 10.000, 259.00, 2590.00, 22.00, 3159.80);

-- ============================================================
-- 13. DDT (Documenti di Trasporto)
-- ============================================================
INSERT INTO ddt (id, numero, anno, data_ddt, cliente_id, ordine_id, oggetto, causale_trasporto, imponibile, iva, totale, data_creazione, created_by) VALUES
(1, 'DDT-2026-001', 2026, '2026-01-27', 1, 1, 'Consegna notebook e software', 'VENDITA', 1549.00, 340.78, 1889.78, NOW(), 1),
(2, 'DDT-2026-002', 2026, '2026-03-01', 2, 2, 'Consegna hosting cloud', 'VENDITA', 2380.00, 523.60, 2903.60, NOW(), 1);

-- ============================================================
-- 14. RIGHE DDT
-- ============================================================
INSERT INTO ddt_righe (id, ddt_id, riga_numero, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 1290.00, 22.00, 1573.80),
(2, 1, 2, 1, 'Windows 11 Pro', 2.000, 'PEZZO', 259.00, 518.00, 22.00, 631.96),
(3, 2, 1, 5, 'Hosting Cloud – 12 mesi', 12.000, 'PEZZO', 199.00, 2388.00, 22.00, 2913.36);

-- ============================================================
-- 15. IVA LIQUIDATION (Liquidazione IVA)
-- ============================================================
INSERT INTO iva_liquidations (id, period_type, year, month, quarter, iva_due, iva_credit, net_balance, status, payment_due_date, payment_date, notes, data_creazione, created_by) VALUES
(1, 'MONTHLY', 2026, 1, NULL, 1000.50, 150.20, 850.30, 'CALCULATED', '2026-02-15', '2026-02-20', 'Liquidazione IVA gennaio 2026', NOW(), 1),
(2, 'MONTHLY', 2026, 2, NULL, 567.38, 100.00, 467.38, 'DRAFT', '2026-03-15', NULL, 'Liquidazione IVA febbraio 2026', NOW(), 1),
(3, 'QUARTERLY', 2026, NULL, 1, 2500.00, 300.50, 2199.50, 'CALCULATED', '2026-04-30', NULL, 'Liquidazione IVA Q1 2026', NOW(), 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- VERIFICA DATI INSERITI
-- ============================================================
SELECT 'CLIENTI' AS tabella, COUNT(*) AS righe FROM clienti
UNION ALL SELECT 'FORNITORI', COUNT(*) FROM fornitori
UNION ALL SELECT 'PRODOTTI', COUNT(*) FROM prodotti
UNION ALL SELECT 'PREVENTIVI', COUNT(*) FROM preventivi
UNION ALL SELECT 'PREVENTIVI_RIGHE', COUNT(*) FROM preventivi_righe
UNION ALL SELECT 'ORDINI', COUNT(*) FROM ordini
UNION ALL SELECT 'ORDINI_RIGHE', COUNT(*) FROM ordini_righe
UNION ALL SELECT 'FATTURE_ATTIVE', COUNT(*) FROM fatture
UNION ALL SELECT 'FATTURE_RIGHE', COUNT(*) FROM fatture_righe
UNION ALL SELECT 'PAGAMENTI', COUNT(*) FROM pagamenti
UNION ALL SELECT 'FATTURE_PASSIVE', COUNT(*) FROM fatture_passive
UNION ALL SELECT 'FATTURE_PASSIVE_RIGHE', COUNT(*) FROM fatture_passive_righe
UNION ALL SELECT 'DDT', COUNT(*) FROM ddt
UNION ALL SELECT 'DDT_RIGHE', COUNT(*) FROM ddt_righe
UNION ALL SELECT 'IVA_LIQUIDATIONS', COUNT(*) FROM iva_liquidations
ORDER BY tabella;
