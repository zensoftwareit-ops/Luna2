ALTER TABLE vat_movements
    ADD COLUMN document_fiscal_type VARCHAR(10) NULL AFTER source_type,
    ADD KEY idx_vat_movement_fiscal_type (organization_id, document_fiscal_type, period_year, period_month);

UPDATE vat_movements movement
JOIN documents document ON document.id = movement.document_id AND document.organization_id = movement.organization_id
SET movement.document_fiscal_type = COALESCE(NULLIF(document.fatturapa_type, ''),
    CASE WHEN document.document_type = 'CREDIT_NOTE' THEN 'TD04' ELSE 'TD01' END)
WHERE movement.source_type = 'DOCUMENT';

UPDATE vat_movements
SET document_fiscal_type = 'TD04'
WHERE source_type = 'DATEV_PDF' AND (taxable_amount < 0 OR vat_amount < 0);

UPDATE vat_movements
SET document_fiscal_type = 'TD01'
WHERE document_fiscal_type IS NULL OR document_fiscal_type = '';

UPDATE vat_movements
SET taxable_amount = ABS(taxable_amount), vat_amount = ABS(vat_amount),
    vat_due_amount = ABS(vat_due_amount), deductible_vat = ABS(deductible_vat),
    pro_rata_amount = ABS(pro_rata_amount), deductibility_percent = ABS(deductibility_percent)
WHERE document_fiscal_type IN ('TD04','TD08');

DELETE detail
FROM vat_settlement_details detail
JOIN vat_settlements settlement ON settlement.id = detail.settlement_id
WHERE settlement.status IN ('DRAFT','CALCULATED')
  AND EXISTS (
      SELECT 1 FROM vat_movements movement
      WHERE movement.organization_id = settlement.organization_id
        AND movement.period_year = settlement.period_year
        AND movement.document_fiscal_type IN ('TD04','TD08')
        AND ((settlement.period_type = 'MONTHLY' AND settlement.period_number = movement.period_month)
          OR (settlement.period_type = 'QUARTERLY' AND settlement.period_number = CEIL(movement.period_month / 3)))
  );

UPDATE vat_settlements settlement
SET settlement.status = 'DRAFT', settlement.vat_debit = 0, settlement.vat_credit = 0,
    settlement.balance = 0, settlement.calculated_at = NULL, settlement.updated_at = NOW()
WHERE settlement.status IN ('DRAFT','CALCULATED')
  AND EXISTS (
      SELECT 1 FROM vat_movements movement
      WHERE movement.organization_id = settlement.organization_id
        AND movement.period_year = settlement.period_year
        AND movement.document_fiscal_type IN ('TD04','TD08')
        AND ((settlement.period_type = 'MONTHLY' AND settlement.period_number = movement.period_month)
          OR (settlement.period_type = 'QUARTERLY' AND settlement.period_number = CEIL(movement.period_month / 3)))
  );
