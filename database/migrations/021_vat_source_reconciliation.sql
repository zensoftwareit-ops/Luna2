ALTER TABLE vat_settlements
    ADD COLUMN source_reconciliation_mode VARCHAR(30) NOT NULL DEFAULT 'STANDARD' AFTER notes,
    ADD COLUMN excluded_document_rows INT UNSIGNED NOT NULL DEFAULT 0 AFTER source_reconciliation_mode,
    ADD COLUMN excluded_document_vat_debit DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER excluded_document_rows,
    ADD COLUMN excluded_document_vat_credit DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER excluded_document_vat_debit,
    ADD COLUMN excluded_document_purchase_vat DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER excluded_document_vat_credit;

-- Le prime versioni dell'estrattore DATEV attribuivano detraibilità zero alle
-- note di credito acquisti perché confrontavano importi negativi con zero.
-- Il profilo positivo dello stesso articolo IVA consente di correggere in modo
-- deterministico soltanto le righe generate da quell'estrattore. I periodi già
-- resi definitivi o pagati non vengono modificati automaticamente.
CREATE TEMPORARY TABLE vat_credit_note_repairs (
    movement_id BIGINT UNSIGNED PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    period_year SMALLINT UNSIGNED NOT NULL,
    period_month TINYINT UNSIGNED NOT NULL,
    deductibility_percent DECIMAL(7,4) NOT NULL
);

INSERT INTO vat_credit_note_repairs
    (movement_id, organization_id, period_year, period_month, deductibility_percent)
SELECT movement.id, movement.organization_id, movement.period_year, movement.period_month,
       profile.deductibility_percent
FROM vat_movements movement
JOIN (
    SELECT organization_id, vat_code, MAX(deductibility_percent) AS deductibility_percent
    FROM vat_movements
    WHERE source_type = 'DATEV_PDF' AND register_type = 'PURCHASES' AND vat_amount > 0
    GROUP BY organization_id, vat_code
) profile
  ON profile.organization_id = movement.organization_id AND profile.vat_code = movement.vat_code
WHERE movement.source_type = 'DATEV_PDF'
  AND movement.register_type = 'PURCHASES'
  AND movement.vat_amount < 0
  AND movement.deductible_vat = 0
  AND movement.deductibility_percent = 0
  AND NOT EXISTS (
      SELECT 1 FROM vat_settlements settlement
      WHERE settlement.organization_id = movement.organization_id
        AND settlement.period_year = movement.period_year
        AND settlement.status IN ('SUBMITTED','PAID')
        AND ((settlement.period_type = 'MONTHLY' AND settlement.period_number = movement.period_month)
          OR (settlement.period_type = 'QUARTERLY' AND settlement.period_number = CEIL(movement.period_month / 3)))
  );

UPDATE vat_movements movement
JOIN vat_credit_note_repairs repair ON repair.movement_id = movement.id
SET movement.deductibility_percent = repair.deductibility_percent,
    movement.deductible_vat = ROUND(movement.vat_amount * repair.deductibility_percent / 100, 2),
    movement.pro_rata_amount = movement.vat_amount - ROUND(movement.vat_amount * repair.deductibility_percent / 100, 2);

DELETE detail
FROM vat_settlement_details detail
JOIN vat_settlements settlement ON settlement.id = detail.settlement_id
JOIN vat_credit_note_repairs repair
  ON repair.organization_id = settlement.organization_id AND repair.period_year = settlement.period_year
 AND ((settlement.period_type = 'MONTHLY' AND settlement.period_number = repair.period_month)
   OR (settlement.period_type = 'QUARTERLY' AND settlement.period_number = CEIL(repair.period_month / 3)))
WHERE settlement.status IN ('DRAFT','CALCULATED');

UPDATE vat_settlements settlement
JOIN vat_credit_note_repairs repair
  ON repair.organization_id = settlement.organization_id AND repair.period_year = settlement.period_year
 AND ((settlement.period_type = 'MONTHLY' AND settlement.period_number = repair.period_month)
   OR (settlement.period_type = 'QUARTERLY' AND settlement.period_number = CEIL(repair.period_month / 3)))
SET settlement.status = 'DRAFT', settlement.vat_debit = 0, settlement.vat_credit = 0,
    settlement.balance = 0, settlement.source_reconciliation_mode = 'STANDARD',
    settlement.excluded_document_rows = 0, settlement.excluded_document_vat_debit = 0,
    settlement.excluded_document_vat_credit = 0, settlement.excluded_document_purchase_vat = 0,
    settlement.calculated_at = NULL, settlement.updated_at = NOW()
WHERE settlement.status IN ('DRAFT','CALCULATED');

DROP TEMPORARY TABLE vat_credit_note_repairs;
