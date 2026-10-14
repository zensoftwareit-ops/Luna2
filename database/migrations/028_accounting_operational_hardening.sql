ALTER TABLE vat_settlements
    ADD COLUMN previous_credit_mode ENUM('AUTO','MANUAL') NOT NULL DEFAULT 'MANUAL' AFTER previous_credit,
    ADD COLUMN previous_credit_source_settlement_id BIGINT UNSIGNED NULL AFTER previous_credit_mode,
    ADD KEY idx_vat_previous_credit_source (organization_id, previous_credit_source_settlement_id);

ALTER TABLE document_lines
    ADD COLUMN purchase_classification ENUM('ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE') NOT NULL DEFAULT 'ORDINARY' AFTER deductible_percent;

ALTER TABLE vat_movements
    ADD COLUMN purchase_classification ENUM('ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE') NOT NULL DEFAULT 'ORDINARY' AFTER deductibility_percent;

ALTER TABLE vat_settlement_details
    ADD COLUMN purchase_classification ENUM('ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE') NOT NULL DEFAULT 'ORDINARY' AFTER vat_legal_reference;

ALTER TABLE vat_annual_summaries
    ADD COLUMN capital_goods_taxable DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER purchases_taxable,
    ADD COLUMN capital_goods_vat DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER capital_goods_taxable;

ALTER TABLE fixed_assets
    ADD COLUMN purchase_document_number VARCHAR(100) NULL AFTER document_id,
    ADD COLUMN purchase_document_date DATE NULL AFTER purchase_document_number,
    ADD COLUMN acquisition_condition ENUM('NEW','USED') NOT NULL DEFAULT 'NEW' AFTER purchase_document_date;

INSERT INTO vat_codes (organization_id, code, description, rate, nature, legal_reference)
SELECT organization.id, catalog.code, catalog.description, 0, catalog.code, catalog.legal_reference
FROM organizations organization
CROSS JOIN (
    SELECT 'N1' code, 'Esclusa IVA: somme fuori base imponibile' description, 'Art. 15 DPR 633/72' legal_reference
    UNION ALL SELECT 'N2.1', 'Non soggetta IVA: carenza territorialita', 'Artt. 7-7-septies DPR 633/72'
    UNION ALL SELECT 'N2.2', 'Non soggetta IVA: altri casi', NULL
    UNION ALL SELECT 'N3.1', 'Non imponibile: esportazioni', 'Art. 8 DPR 633/72'
    UNION ALL SELECT 'N3.2', 'Non imponibile: cessioni intracomunitarie', 'Art. 41 DL 331/93'
    UNION ALL SELECT 'N3.3', 'Non imponibile: cessioni verso San Marino', NULL
    UNION ALL SELECT 'N3.4', 'Non imponibile: operazioni assimilate alle esportazioni', 'Art. 8-bis DPR 633/72'
    UNION ALL SELECT 'N3.5', 'Non imponibile: dichiarazione d’intento', 'Art. 8 c.1 lett. c DPR 633/72'
    UNION ALL SELECT 'N3.6', 'Non imponibile: altre operazioni', NULL
    UNION ALL SELECT 'N4', 'Esente IVA', 'Art. 10 DPR 633/72'
    UNION ALL SELECT 'N5', 'Regime del margine / IVA non esposta', NULL
    UNION ALL SELECT 'N6.1', 'Reverse charge: rottami e materiali di recupero', 'Art. 74 c.7-8 DPR 633/72'
    UNION ALL SELECT 'N6.2', 'Reverse charge: oro e argento puro', 'Art. 17 c.5 DPR 633/72'
    UNION ALL SELECT 'N6.3', 'Reverse charge: subappalto edile', 'Art. 17 c.6 lett. a DPR 633/72'
    UNION ALL SELECT 'N6.4', 'Reverse charge: cessione fabbricati', 'Art. 17 c.6 lett. a-bis DPR 633/72'
    UNION ALL SELECT 'N6.5', 'Reverse charge: telefoni cellulari', 'Art. 17 c.6 lett. b DPR 633/72'
    UNION ALL SELECT 'N6.6', 'Reverse charge: prodotti elettronici', 'Art. 17 c.6 lett. c DPR 633/72'
    UNION ALL SELECT 'N6.7', 'Reverse charge: servizi edilizia e comparti connessi', 'Art. 17 c.6 lett. a-ter DPR 633/72'
    UNION ALL SELECT 'N6.8', 'Reverse charge: settore energetico', 'Art. 17 c.6 lett. d-bis/d-ter/d-quater DPR 633/72'
    UNION ALL SELECT 'N6.9', 'Reverse charge: altri casi', NULL
    UNION ALL SELECT 'N7', 'IVA assolta in altro Stato UE', NULL
) catalog
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    description = IF(vat_codes.description = vat_codes.code OR vat_codes.description = '', VALUES(description), vat_codes.description),
    nature = VALUES(nature),
    legal_reference = COALESCE(vat_codes.legal_reference, VALUES(legal_reference));
