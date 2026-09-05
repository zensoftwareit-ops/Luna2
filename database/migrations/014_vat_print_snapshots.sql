ALTER TABLE vat_movements
    ADD COLUMN vat_rate DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER vat_code,
    ADD COLUMN vat_nature VARCHAR(8) NOT NULL DEFAULT '' AFTER vat_rate,
    ADD COLUMN vat_description VARCHAR(190) NULL AFTER vat_nature,
    ADD COLUMN vat_legal_reference VARCHAR(255) NULL AFTER vat_description;

UPDATE vat_movements m
LEFT JOIN vat_codes c
    ON c.organization_id = m.organization_id
   AND c.code = m.vat_code
SET m.vat_rate = COALESCE(c.rate,
        CASE
            WHEN REPLACE(m.vat_code, '%', '') REGEXP '^[0-9]+([.,][0-9]+)?$'
                THEN CAST(REPLACE(REPLACE(m.vat_code, '%', ''), ',', '.') AS DECIMAL(5,2))
            ELSE 0
        END),
    m.vat_nature = COALESCE(c.nature, CASE WHEN m.vat_code LIKE 'N%' THEN m.vat_code ELSE '' END),
    m.vat_description = COALESCE(c.description, m.vat_code, 'N/D'),
    m.vat_legal_reference = c.legal_reference;

ALTER TABLE vat_settlement_details
    DROP INDEX uq_vat_settlement_detail,
    ADD COLUMN vat_rate DECIMAL(5,2) NOT NULL DEFAULT 0 AFTER vat_code,
    ADD COLUMN vat_nature VARCHAR(8) NOT NULL DEFAULT '' AFTER vat_rate,
    ADD COLUMN vat_description VARCHAR(190) NULL AFTER vat_nature,
    ADD COLUMN vat_legal_reference VARCHAR(255) NULL AFTER vat_description,
    ADD KEY idx_vat_settlement_group (settlement_id, register_type, vat_code, vat_rate, vat_nature);

UPDATE vat_settlement_details d
LEFT JOIN vat_codes c
    ON c.organization_id = d.organization_id
   AND c.code = d.vat_code
SET d.vat_rate = COALESCE(c.rate,
        CASE
            WHEN REPLACE(d.vat_code, '%', '') REGEXP '^[0-9]+([.,][0-9]+)?$'
                THEN CAST(REPLACE(REPLACE(d.vat_code, '%', ''), ',', '.') AS DECIMAL(5,2))
            ELSE 0
        END),
    d.vat_nature = COALESCE(c.nature, CASE WHEN d.vat_code LIKE 'N%' THEN d.vat_code ELSE '' END),
    d.vat_description = COALESCE(c.description, d.vat_code, 'N/D'),
    d.vat_legal_reference = c.legal_reference;
