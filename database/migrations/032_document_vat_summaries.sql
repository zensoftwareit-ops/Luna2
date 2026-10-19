CREATE TABLE IF NOT EXISTS document_vat_summaries (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    line_number INT UNSIGNED NOT NULL,
    vat_code VARCHAR(20) NOT NULL,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    vat_nature VARCHAR(8) NULL,
    vat_description VARCHAR(255) NULL,
    vat_legal_reference VARCHAR(255) NULL,
    vat_reverse_charge TINYINT(1) NOT NULL DEFAULT 0,
    vat_split_payment TINYINT(1) NOT NULL DEFAULT 0,
    vat_fatturapa_type VARCHAR(8) NULL,
    deductible_percent DECIMAL(5,2) NOT NULL DEFAULT 100,
    purchase_classification ENUM('ORDINARY','DEPRECIABLE_ASSET','NON_DEPRECIABLE_ASSET','LEASING','RESALE') NOT NULL DEFAULT 'ORDINARY',
    taxable_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    vat_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    source_type ENUM('GENERATED','MANUAL') NOT NULL DEFAULT 'GENERATED',
    created_by BIGINT UNSIGNED NULL,
    updated_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_document_vat_summary_line (document_id, line_number),
    KEY idx_document_vat_summary_org (organization_id, document_id),
    CONSTRAINT fk_document_vat_summary_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_document_vat_summary_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO document_vat_summaries
    (organization_id, document_id, line_number, vat_code, vat_rate, vat_nature, vat_description,
     vat_legal_reference, vat_reverse_charge, vat_split_payment, vat_fatturapa_type,
     deductible_percent, purchase_classification, taxable_amount, vat_amount, source_type)
SELECT grouped.organization_id, grouped.document_id,
       ROW_NUMBER() OVER (PARTITION BY grouped.document_id ORDER BY grouped.vat_rate DESC, grouped.vat_code, grouped.purchase_classification),
       grouped.vat_code, grouped.vat_rate, NULLIF(grouped.vat_nature, ''), NULLIF(grouped.vat_description, ''),
       NULLIF(grouped.vat_legal_reference, ''), grouped.vat_reverse_charge, grouped.vat_split_payment,
       NULLIF(grouped.vat_fatturapa_type, ''), grouped.deductible_percent, grouped.purchase_classification,
       ABS(grouped.taxable_amount), ABS(grouped.vat_amount), 'GENERATED'
FROM (
    SELECT line.organization_id, line.document_id,
           COALESCE(NULLIF(line.vat_code, ''), NULLIF(line.vat_nature, ''), CONCAT(REPLACE(FORMAT(line.vat_rate, 2), '.00', ''), '%')) AS vat_code,
           line.vat_rate, COALESCE(line.vat_nature, '') AS vat_nature,
           MAX(COALESCE(line.vat_description, '')) AS vat_description,
           MAX(COALESCE(line.vat_legal_reference, '')) AS vat_legal_reference,
           MAX(COALESCE(line.vat_reverse_charge, 0)) AS vat_reverse_charge,
           MAX(COALESCE(line.vat_split_payment, 0)) AS vat_split_payment,
           MAX(COALESCE(line.vat_fatturapa_type, '')) AS vat_fatturapa_type,
           COALESCE(line.deductible_percent, 100) AS deductible_percent,
           COALESCE(line.purchase_classification, 'ORDINARY') AS purchase_classification,
           SUM(line.taxable_amount) AS taxable_amount, SUM(line.vat_amount) AS vat_amount
    FROM document_lines line
    JOIN documents document ON document.id = line.document_id AND document.organization_id = line.organization_id
    WHERE document.document_type IN ('SALES_INVOICE','PURCHASE_INVOICE','CREDIT_NOTE')
    GROUP BY line.organization_id, line.document_id,
             COALESCE(NULLIF(line.vat_code, ''), NULLIF(line.vat_nature, ''), CONCAT(REPLACE(FORMAT(line.vat_rate, 2), '.00', ''), '%')),
             line.vat_rate, COALESCE(line.vat_nature, ''), COALESCE(line.deductible_percent, 100),
             COALESCE(line.purchase_classification, 'ORDINARY')
) grouped
WHERE ABS(grouped.taxable_amount) > 0.005 OR ABS(grouped.vat_amount) > 0.005
ON DUPLICATE KEY UPDATE document_id = VALUES(document_id);
