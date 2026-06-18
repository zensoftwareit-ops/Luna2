-- ============================================================
-- Luna2 – Dati di esempio ESTESI e COMPLETI
-- Include: fatture passive, pagamenti, movimenti magazzino,
--          iva liquidation, contabilità generale completa
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- TRUNCATE EXISTING DATA
-- ============================================================
TRUNCATE TABLE accounting_entry_lines;
TRUNCATE TABLE accounting_entries;
TRUNCATE TABLE iva_liquidations;
TRUNCATE TABLE pagamenti;
TRUNCATE TABLE movimenti_magazzino;
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
(4, 'CLIENTE',  'Delta Group Srl',           '05678901234', '05678901234', 'JKL3456', 'deltagroup@pec.it',   '06 2345678',  'info@deltagroup.it',         'Via Veneto 23',       'Roma',    'RM', '00187', 'Italia', 'CLI004', '30 giorni', 0.00,  true, NOW(), 1),
(5, 'CLIENTE',  'Epsilon Tech Srl',          '06789012345', '06789012345', 'MNO7890', 'info@epsilontech.it', '081 8765432', 'contatti@epsilontech.it',    'Via Toledo 150',      'Napoli',  'NA', '80132', 'Italia', 'CLI005', '30 giorni', 0.00,  true, NOW(), 1);

-- ============================================================
-- 2. FORNITORI
-- ============================================================
INSERT INTO fornitori (id, ragione_sociale, partita_iva, codice_fiscale, telefono, email, indirizzo, citta, provincia, cap, paese, codice_fornitore, condizioni_pagamento, attivo, data_creazione, created_by) VALUES
(1, 'Microsoft Italia Srl',   '09876543210', '09876543210', '02 70392000', 'partner@microsoft.com',   'Via Lombardia 1',    'Milano',  'MI', '20121', 'Italia', 'FOR001', '30 giorni',  true, NOW(), 1),
(2, 'HP Inc Italia Srl',      '08765432109', '08765432109', '02 34567890', 'ordini@hp.it',             'Via Garibaldi 5',    'Segrate', 'MI', '20054', 'Italia', 'FOR002', '60 giorni',  true, NOW(), 1),
(3, 'Cloud Host SpA',         '07654321098', '07654321098', '02 12345678', 'billing@cloudhost.it',     'Via Innovazione 99', 'Milano',  'MI', '20126', 'Italia', 'FOR003', 'Mensile',    true, NOW(), 1);

-- ============================================================
-- 3. PRODOTTI (8 items)
-- ============================================================
INSERT INTO prodotti (id, codice, nome, descrizione, categoria, tipo_prodotto, unita_misura, prezzo_base, costo_acquisto, iva_percentuale, gestione_magazzino, giacenza_minima, attivo, data_creazione, created_by, fornitore_id) VALUES
(1,  'SW-WIN11',   'Windows 11 Pro',               'Licenza OEM Windows 11',                        'Software',  'STANDARD', 'PEZZO', 259.00,  180.00, 22.00, true,  5.00,  true, NOW(), 1, 1),
(2,  'SW-OFF365',  'Microsoft 365 Business',      'Abbonamento annuale',                          'Software',  'STANDARD', 'PEZZO', 149.00,  95.00,  22.00, true,  10.00, true, NOW(), 1, 1),
(3,  'HW-NB001',   'Notebook HP EliteBook 840',   'HP EliteBook 840 G10 i7 16GB',                  'Hardware',  'STANDARD', 'PEZZO', 1290.00, 890.00, 22.00, true,  3.00,  true, NOW(), 1, 2),
(4,  'HW-PC001',   'Desktop HP ProDesk 400',      'HP ProDesk 400 G9 i5 8GB',                      'Hardware',  'STANDARD', 'PEZZO', 790.00,  540.00, 22.00, true,  5.00,  true, NOW(), 1, 2),
(5,  'SRV-CLOUD',  'Hosting Cloud Mensile',       'Server cloud 4vCPU 8GB 100GB SSD',             'Servizi',   'SERVIZIO', 'PEZZO', 199.00,  90.00,  22.00, false, NULL,  true, NOW(), 1, 3),
(6,  'SRV-ASS',    'Assistenza Tecnica',          'Intervento tecnico on-site',                    'Servizi',   'SERVIZIO', 'ORA',   85.00,   0.00,   22.00, false, NULL,  true, NOW(), 1, NULL),
(7,  'HW-MOUSE',   'Mouse Logitech MX Master',    'Mouse wireless per ufficio',                    'Hardware',  'STANDARD', 'PEZZO', 89.00,   55.00,  22.00, true,  20.00, true, NOW(), 1, 2),
(8,  'ACC-MONITOR','Monitor Dell 27 inch 4K',     'Monitor Ultra HD per postazione',                'Hardware',  'STANDARD', 'PEZZO', 450.00,  280.00, 22.00, true,  2.00,  true, NOW(), 1, 2);

-- ============================================================
-- 4. PREVENTIVI (3)
-- ============================================================
INSERT INTO preventivi (id, numero, anno, data_preventivo, data_validita, cliente_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, validita_giorni, data_creazione, created_by) VALUES
(1, 'PREV-2026-001', 2026, '2026-01-10', '2026-02-10', 1, 'CONVERTITO', 'Fornitura notebook e software',   1549.00, 340.78, 1889.78, '30 giorni', 30, NOW(), 1),
(2, 'PREV-2026-002', 2026, '2026-02-05', '2026-03-05', 2, 'ACCETTATO',  'Migrazione cloud',                 2380.00, 523.60, 2903.60, '60 giorni', 30, NOW(), 1),
(3, 'PREV-2026-003', 2026, '2026-03-01', '2026-04-01', 3, 'INVIATO',    'Progetto assistenza annuale',      5100.00, 1122.00, 6222.00, 'Immediato', 30, NOW(), 1);

-- ============================================================
-- 5. RIGHE PREVENTIVI
-- ============================================================
INSERT INTO preventivi_righe (id, preventivo_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW());

-- ============================================================
-- 6. ORDINI (3)
-- ============================================================
INSERT INTO ordini (id, numero, anno, data_ordine, cliente_id, preventivo_id, stato, oggetto, imponibile, iva, totale, condizioni_pagamento, data_consegna_prevista, data_creazione, created_by) VALUES
(1, 'ORD-2026-001', 2026, '2026-01-20', 1, 1, 'EVASO', 'Notebook e software',     1549.00, 340.78, 1889.78, '30 giorni', '2026-01-27', NOW(), 1),
(2, 'ORD-2026-002', 2026, '2026-02-15', 2, 2, 'CONFERMATO', 'Hosting cloud',      2380.00, 523.60, 2903.60, '60 giorni', '2026-03-01', NOW(), 1),
(3, 'ORD-2026-003', 2026, '2026-03-10', 3, NULL, 'IN_LAVORAZIONE', 'Monitor e mouse', 539.00, 118.58, 657.58, 'Immediato', '2026-03-20', NOW(), 1);

-- ============================================================
-- 7. RIGHE ORDINI
-- ============================================================
INSERT INTO ordini_righe (id, ordine_id, riga_numero, tipo_riga, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, sconto_percentuale, sconto_importo, imponibile_riga, iva_percentuale, iva_importo, totale_riga, data_creazione) VALUES
(1, 1, 1, 'PRODOTTO', 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 0.00, 0.00, 1290.00, 22.00, 283.80, 1573.80, NOW()),
(2, 1, 2, 'PRODOTTO', 1, 'Windows 11 Pro', 2.000, 'PEZZO',  259.00, 0.00, 0.00,  518.00, 22.00, 113.96,  631.96, NOW()),
(3, 2, 1, 'PRODOTTO', 5, 'Hosting Cloud 12 mesi', 12.000, 'PEZZO',  199.00, 0.00, 0.00, 2388.00, 22.00, 525.36, 2913.36, NOW()),
(4, 3, 1, 'PRODOTTO', 8, 'Monitor Dell 27 inch 4K', 1.000, 'PEZZO', 450.00, 0.00, 0.00, 450.00, 22.00, 99.00, 549.00, NOW()),
(5, 3, 2, 'PRODOTTO', 7, 'Mouse Logitech MX Master', 1.000, 'PEZZO', 89.00, 0.00, 0.00, 89.00, 22.00, 19.58, 108.58, NOW());

-- ============================================================
-- 8. FATTURE ATTIVE (6 fatture con vari stati)
-- ============================================================
INSERT INTO fatture (id, numero, anno, data_fattura, tipo_fattura, cliente_id, ordine_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, fattura_elettronica_inviata, data_creazione, created_by) VALUES
(1, 'FT-2026-001', 2026, '2026-01-27', 'REALE', 1, 1, 'Fornitura notebook e software', 1549.00, 340.78, 1889.78, 1889.78, 'PAGATA', '2026-02-26', '2026-02-20', 'BONIFICO', true, NOW(), 1),
(2, 'FT-2026-002', 2026, '2026-02-28', 'REALE', 2, 2, 'Hosting cloud – Febbraio',      199.00,  43.78,  242.78,  242.78, 'PAGATA', '2026-03-30', '2026-03-28', 'BONIFICO', true, NOW(), 1),
(3, 'FT-2026-003', 2026, '2026-03-15', 'REALE', 1, NULL, 'Assistenza tecnica – Marzo',   510.00, 112.20,  622.20,  622.20, 'PARZIALMENTE_PAGATA', '2026-04-14', NULL, NULL, true, NOW(), 1),
(4, 'FT-2026-004', 2026, '2026-03-20', 'REALE', 3, 3, 'Monitor e mouse',               539.00, 118.58,  657.58,  657.58, 'DA_PAGARE', '2026-04-19', NULL, NULL, true, NOW(), 1),
(5, 'FT-2026-005', 2026, '2026-04-01', 'REALE', 2, NULL, 'Hosting cloud – Aprile',       199.00,  43.78,  242.78,  242.78, 'DA_PAGARE', '2026-05-01', NULL, NULL, true, NOW(), 1),
(6, 'FT-2026-006', 2026, '2026-04-10', 'PROFORMA', 4, NULL, 'Proposta formazione IT',      1550.00, 341.00, 1891.00, 1891.00, 'DA_PAGARE', '2026-05-10', NULL, NULL, false, NOW(), 1);

-- ============================================================
-- 9. RIGHE FATTURE ATTIVE
-- ============================================================
INSERT INTO fatture_righe (id, fattura_id, riga_numero, prodotto_id, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 3, 'Notebook HP EliteBook', 1.000, 1290.00, 1290.00, 22.00, 1573.80),
(2, 1, 2, 1, 'Windows 11 Pro', 2.000,  259.00,  518.00, 22.00,  631.96),
(3, 2, 1, 5, 'Hosting cloud – Febbraio',  1.000,  199.00,  199.00, 22.00,  242.78),
(4, 3, 1, 6, 'Assistenza tecnica – 6 ore', 6.000,   85.00,  510.00, 22.00,  622.20),
(5, 4, 1, 8, 'Monitor Dell 27 inch 4K', 1.000, 450.00, 450.00, 22.00, 549.00),
(6, 4, 2, 7, 'Mouse Logitech MX Master', 1.000, 89.00, 89.00, 22.00, 108.58);

-- ============================================================
-- 10. PAGAMENTI (6 pagamenti su varie fatture)
-- ============================================================
INSERT INTO pagamenti (id, fattura_id, importo, data_pagamento, metodo_pagamento, numero_riferimento, note, data_creazione, created_by) VALUES
(1, 1, 1889.78, '2026-02-20', 'BONIFICO', 'RIF-BNK-001', 'Pagamento completo fattura FT-2026-001', NOW(), 1),
(2, 2, 242.78, '2026-03-28', 'BONIFICO', 'RIF-BNK-002', 'Pagamento hosting cloud febbraio', NOW(), 1),
(3, 3, 311.10, '2026-04-05', 'ASSEGNO', 'CHK-00123', 'Primo acconto assistenza tecnica', NOW(), 1),
(4, 3, 311.10, '2026-05-10', 'BONIFICO', 'RIF-BNK-003', 'Saldo assistenza tecnica', NOW(), 1),
(5, 5, 242.78, '2026-04-30', 'CARTA_CREDITO', 'CC-45678', 'Hosting cloud aprile', NOW(), 1),
(6, 6, 945.50, '2026-05-08', 'BONIFICO', 'RIF-BNK-004', 'Acconto 50% formazione', NOW(), 1);

-- ============================================================
-- 11. FATTURE PASSIVE (3 acquisti da fornitori)
-- ============================================================
INSERT INTO fatture_passive (id, numero, anno, data_fattura, tipo_fattura, fornitore_id, oggetto, imponibile, iva, totale, totale_netto, stato_pagamento, data_scadenza, data_pagamento, metodo_pagamento, data_creazione, created_by) VALUES
(1, 'FP-2026-001', 2026, '2026-01-15', 'REALE', 2, 'Notebook HP x5 + accessori', 6450.00, 1419.00, 7869.00, 7869.00, 'PAGATA', '2026-02-14', '2026-02-10', 'BONIFICO', NOW(), 1),
(2, 'FP-2026-002', 2026, '2026-02-20', 'REALE', 3, 'Hosting cloud – Febbraio', 199.00, 43.78, 242.78, 242.78, 'PAGATA', '2026-03-20', '2026-03-15', 'BONIFICO', NOW(), 1),
(3, 'FP-2026-003', 2026, '2026-03-01', 'REALE', 1, 'Licenze Windows 11 x10', 2590.00, 569.80, 3159.80, 3159.80, 'PARZIALMENTE_PAGATA', '2026-03-31', NULL, NULL, NOW(), 1);

-- ============================================================
-- 12. RIGHE FATTURE PASSIVE
-- ============================================================
INSERT INTO fatture_passive_righe (id, fattura_passiva_id, riga_numero, descrizione, quantita, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 'Notebook HP EliteBook 840 G10 x5', 5.000, 1290.00, 6450.00, 22.00, 7869.00),
(2, 2, 1, 'Hosting cloud – Febbraio 2026', 1.000, 199.00, 199.00, 22.00, 242.78),
(3, 3, 1, 'Windows 11 Pro – Licenza OEM x10', 10.000, 259.00, 2590.00, 22.00, 3159.80);

-- ============================================================
-- 13. MOVIMENTI MAGAZZINO (Inventory movements)
-- ============================================================
INSERT INTO movimenti_magazzino (id, prodotto_id, fornitore_id, tipo_movimento, causale, quantita, costo_unitario, giacenza_prima, giacenza_dopo, documento_tipo, documento_id, documento_numero, data_movimento, data_creazione, created_by) VALUES
-- Acquisti (IN) da fornitori
(1, 1, 1, 'ENTRATA_ACQUISTO', 'Acquisto Windows 11 licenze', 10.000, 180.00, 2.000, 12.000, 'FATTURA_PASSIVA', 3, 'FP-2026-003', '2026-03-02', NOW(), 1),
(2, 3, 2, 'ENTRATA_ACQUISTO', 'Acquisto Notebook HP EliteBook', 5.000, 890.00, 1.000, 6.000, 'FATTURA_PASSIVA', 1, 'FP-2026-001', '2026-01-16', NOW(), 1),
(3, 7, 2, 'ENTRATA_ACQUISTO', 'Acquisto Mouse Logitech', 20.000, 55.00, 5.000, 25.000, 'ORDINE_FORNITORE', 1, 'ORD-FOR-001', '2026-02-05', NOW(), 1),
(4, 8, 2, 'ENTRATA_ACQUISTO', 'Acquisto Monitor Dell 27 inch', 5.000, 280.00, 0.000, 5.000, 'ORDINE_FORNITORE', 2, 'ORD-FOR-002', '2026-03-05', NOW(), 1),

-- Vendite (OUT) a clienti
(5, 3, NULL, 'USCITA_VENDITA', 'Vendita cliente Alfa Informatica', 1.000, 890.00, 6.000, 5.000, 'FATTURA_ATTIVA', 1, 'FT-2026-001', '2026-01-27', NOW(), 1),
(6, 1, NULL, 'USCITA_VENDITA', 'Vendita licenze a cliente Alfa', 2.000, 180.00, 12.000, 10.000, 'FATTURA_ATTIVA', 1, 'FT-2026-001', '2026-01-27', NOW(), 1),
(7, 7, NULL, 'USCITA_VENDITA', 'Vendita Mouse a cliente Gamma Services', 1.000, 55.00, 25.000, 24.000, 'FATTURA_ATTIVA', 4, 'FT-2026-004', '2026-03-20', NOW(), 1),
(8, 8, NULL, 'USCITA_VENDITA', 'Vendita Monitor a cliente Gamma Services', 1.000, 280.00, 5.000, 4.000, 'FATTURA_ATTIVA', 4, 'FT-2026-004', '2026-03-20', NOW(), 1),

-- Rimanenze (adjustments)
(9, 2, NULL, 'RETTIFICA', 'Inventario Microsoft 365 (conteggio fisico)', 0.000, 95.00, 8.000, 8.000, NULL, NULL, NULL, '2026-03-15', NOW(), 1);

-- ============================================================
-- 14. IVA LIQUIDATION (Liquidazione IVA trimestrale/mensile)
-- ============================================================
INSERT INTO iva_liquidations (id, period_type, year, month, quarter, iva_due, iva_credit, net_balance, status, payment_due_date, payment_date, notes, data_creazione, created_by) VALUES
-- Gennaio 2026
(1, 'MONTHLY', 2026, 1, NULL, 1089.18, 150.00, 939.18, 'PAID', '2026-02-15', '2026-02-15', 'Liquidazione IVA Gennaio 2026 – PAGATA', NOW(), 1),
-- Febbraio 2026
(2, 'MONTHLY', 2026, 2, NULL, 667.56, 100.00, 567.56, 'SUBMITTED', '2026-03-15', NULL, 'Liquidazione IVA Febbraio 2026 – DICHIARATA', NOW(), 1),
-- Marzo 2026
(3, 'MONTHLY', 2026, 3, NULL, 1234.78, 200.50, 1034.28, 'CALCULATED', '2026-04-15', NULL, 'Liquidazione IVA Marzo 2026 – CALCOLATA (in bozza)', NOW(), 1),
-- Q1 2026 (riepiloga i tre mesi)
(4, 'QUARTERLY', 2026, NULL, 1, 2991.52, 450.50, 2541.02, 'CALCULATED', '2026-05-30', NULL, 'Liquidazione IVA Q1 2026 – Riepilogo trimestrale', NOW(), 1);

-- ============================================================
-- 15. DDT (Documenti di Trasporto)
-- ============================================================
INSERT INTO ddt (id, numero, anno, data_ddt, cliente_id, ordine_id, oggetto, causale_trasporto, imponibile, iva, totale, data_creazione, created_by) VALUES
(1, 'DDT-2026-001', 2026, '2026-01-27', 1, 1, 'Consegna notebook e software', 'VENDITA', 1549.00, 340.78, 1889.78, NOW(), 1),
(2, 'DDT-2026-002', 2026, '2026-03-01', 2, 2, 'Consegna hosting cloud – configurazione', 'VENDITA', 2380.00, 523.60, 2903.60, NOW(), 1),
(3, 'DDT-2026-003', 2026, '2026-03-20', 3, 3, 'Consegna monitor e mouse', 'VENDITA', 539.00, 118.58, 657.58, NOW(), 1);

-- ============================================================
-- 16. RIGHE DDT
-- ============================================================
INSERT INTO ddt_righe (id, ddt_id, riga_numero, prodotto_id, descrizione, quantita, unita_misura, prezzo_unitario, imponibile_riga, iva_percentuale, totale_riga) VALUES
(1, 1, 1, 3, 'Notebook HP EliteBook', 1.000, 'PEZZO', 1290.00, 1290.00, 22.00, 1573.80),
(2, 1, 2, 1, 'Windows 11 Pro', 2.000, 'PEZZO', 259.00, 518.00, 22.00, 631.96),
(3, 2, 1, 5, 'Hosting Cloud – 12 mesi', 12.000, 'PEZZO', 199.00, 2388.00, 22.00, 2913.36),
(4, 3, 1, 8, 'Monitor Dell 27 inch 4K', 1.000, 'PEZZO', 450.00, 450.00, 22.00, 549.00),
(5, 3, 2, 7, 'Mouse Logitech MX Master', 1.000, 'PEZZO', 89.00, 89.00, 22.00, 108.58);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- VERIFICA DATI COMPLETI
-- ============================================================
SELECT 'UTENTI' AS tabella, COUNT(*) AS righe FROM users
UNION ALL SELECT 'CLIENTI', COUNT(*) FROM clienti
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
UNION ALL SELECT 'MOVIMENTI_MAGAZZINO', COUNT(*) FROM movimenti_magazzino
UNION ALL SELECT 'IVA_LIQUIDATIONS', COUNT(*) FROM iva_liquidations
ORDER BY tabella;
