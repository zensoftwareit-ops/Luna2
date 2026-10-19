INSERT INTO accounting_causes
    (organization_id, code, name, category, default_description, creates_open_item, automatic, active)
SELECT organization.id, seed.code, seed.name, seed.category, seed.default_description,
       seed.creates_open_item, seed.automatic, 1
FROM organizations organization
CROSS JOIN (
    SELECT 'GEN' code, 'Registrazione generica' name, 'GENERAL' category, 'Registrazione contabile' default_description, 0 creates_open_item, 0 automatic
    UNION ALL SELECT 'FAV', 'Fattura di vendita', 'SALES', 'Fattura di vendita', 1, 1
    UNION ALL SELECT 'FAC', 'Fattura di acquisto', 'PURCHASE', 'Fattura di acquisto', 1, 1
    UNION ALL SELECT 'INC', 'Incasso cliente', 'RECEIPT', 'Incasso cliente', 0, 1
    UNION ALL SELECT 'PAG', 'Pagamento fornitore', 'PAYMENT', 'Pagamento fornitore', 0, 1
    UNION ALL SELECT 'ASS', 'Scrittura di assestamento', 'ADJUSTMENT', 'Scrittura di assestamento', 0, 1
    UNION ALL SELECT 'AMM', 'Ammortamento cespiti', 'DEPRECIATION', 'Ammortamento cespiti', 0, 1
    UNION ALL SELECT 'RIT', 'Ritenute e contributi', 'WITHHOLDING', 'Ritenute e contributi', 0, 1
    UNION ALL SELECT 'CHI', 'Chiusura esercizio', 'CLOSING', 'Chiusura esercizio', 0, 1
    UNION ALL SELECT 'APE', 'Apertura esercizio', 'OPENING', 'Apertura esercizio', 0, 1
) seed
WHERE 1 = 1
ON DUPLICATE KEY UPDATE active = 1;
