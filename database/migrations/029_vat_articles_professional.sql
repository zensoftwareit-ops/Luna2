ALTER TABLE vat_codes
    ADD COLUMN operation_scope ENUM('SALES','PURCHASES','BOTH') NOT NULL DEFAULT 'BOTH' AFTER deductible_percent,
    ADD COLUMN nature_description VARCHAR(255) NULL AFTER operation_scope,
    ADD COLUMN taxability VARCHAR(32) NULL AFTER nature_description,
    ADD COLUMN vat_regime VARCHAR(80) NULL AFTER taxability,
    ADD COLUMN margin_scheme VARCHAR(80) NULL AFTER vat_regime,
    ADD COLUMN territoriality VARCHAR(32) NULL AFTER margin_scheme,
    ADD COLUMN reverse_charge TINYINT(1) NOT NULL DEFAULT 0 AFTER territoriality,
    ADD COLUMN split_payment TINYINT(1) NOT NULL DEFAULT 0 AFTER reverse_charge,
    ADD COLUMN fatturapa_type VARCHAR(8) NULL AFTER split_payment,
    ADD COLUMN source_system VARCHAR(32) NULL AFTER fatturapa_type,
    ADD COLUMN source_status VARCHAR(32) NULL AFTER source_system,
    ADD KEY idx_vat_codes_usage (organization_id, active, operation_scope, nature, rate);

ALTER TABLE document_lines
    ADD COLUMN vat_description VARCHAR(190) NULL AFTER vat_nature,
    ADD COLUMN vat_legal_reference VARCHAR(255) NULL AFTER vat_description,
    ADD COLUMN vat_reverse_charge TINYINT(1) NOT NULL DEFAULT 0 AFTER vat_legal_reference,
    ADD COLUMN vat_split_payment TINYINT(1) NOT NULL DEFAULT 0 AFTER vat_reverse_charge,
    ADD COLUMN vat_fatturapa_type VARCHAR(8) NULL AFTER vat_split_payment,
    ADD COLUMN vat_article_review_required TINYINT(1) NOT NULL DEFAULT 0 AFTER vat_fatturapa_type,
    ADD KEY idx_document_lines_vat_review (organization_id, vat_article_review_required, document_id);

UPDATE document_lines line_item
LEFT JOIN vat_codes article
  ON article.organization_id = line_item.organization_id
 AND article.code = line_item.vat_code
SET line_item.vat_description = COALESCE(line_item.vat_description, article.description),
    line_item.vat_legal_reference = COALESCE(line_item.vat_legal_reference, article.legal_reference),
    line_item.vat_article_review_required = CASE
        WHEN COALESCE(line_item.vat_code, '') = '' THEN 1
        ELSE line_item.vat_article_review_required
    END;
