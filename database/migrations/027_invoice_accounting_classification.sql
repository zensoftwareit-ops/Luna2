ALTER TABLE document_lines
    ADD COLUMN deductible_percent DECIMAL(5,2) NOT NULL DEFAULT 100 AFTER vat_amount,
    ADD CONSTRAINT chk_document_lines_deductible CHECK (deductible_percent >= 0 AND deductible_percent <= 100);

UPDATE vat_movements movement
JOIN documents document ON document.id = movement.document_id AND document.organization_id = movement.organization_id
SET movement.protocol_number = CONCAT(
        'IVA-',
        CASE movement.register_type WHEN 'PURCHASES' THEN 'ACQ' WHEN 'SALES' THEN 'VEN' ELSE 'COR' END,
        '-', movement.period_year, '-', LPAD(document.id, 6, '0')
    ),
    movement.document_reference = document.number,
    movement.document_reference_date = document.document_date
WHERE movement.source_type = 'DOCUMENT'
  AND (movement.protocol_number IS NULL OR movement.protocol_number = '' OR movement.protocol_number = document.number);
