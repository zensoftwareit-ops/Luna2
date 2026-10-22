-- Riallinea le partite non ancora movimentate delle fatture con ritenuta.
-- Il debito verso il percipiente e la relativa scadenza sono pari al netto
-- da pagare, mentre la ritenuta resta sul distinto debito verso l'Erario.

UPDATE payment_schedules schedule_row
JOIN documents document_row
  ON document_row.id = schedule_row.document_id
 AND document_row.organization_id = schedule_row.organization_id
JOIN (
    SELECT organization_id, document_id, COUNT(*) AS schedule_count, SUM(paid_amount) AS paid_total
    FROM payment_schedules
    GROUP BY organization_id, document_id
) schedule_totals
  ON schedule_totals.organization_id = schedule_row.organization_id
 AND schedule_totals.document_id = schedule_row.document_id
SET schedule_row.amount = GREATEST(0, ABS(document_row.total) - ABS(document_row.withholding_total)),
    schedule_row.status = CASE
        WHEN GREATEST(0, ABS(document_row.total) - ABS(document_row.withholding_total)) <= 0.005 THEN 'PAID'
        WHEN schedule_row.due_date < CURRENT_DATE() THEN 'OVERDUE'
        ELSE 'OPEN'
    END,
    schedule_row.updated_at = NOW()
WHERE document_row.withholding_total > 0.005
  AND schedule_totals.schedule_count = 1
  AND schedule_totals.paid_total <= 0.005
  AND ABS(schedule_row.amount - GREATEST(0, ABS(document_row.total) - ABS(document_row.withholding_total))) > 0.02;

UPDATE accounting_open_items item
JOIN payment_schedules schedule
  ON schedule.id = item.payment_schedule_id
 AND schedule.organization_id = item.organization_id
JOIN documents document
  ON document.id = item.document_id
 AND document.organization_id = item.organization_id
SET item.original_amount = schedule.amount,
    item.due_date = schedule.due_date,
    item.status = CASE
        WHEN schedule.amount <= 0.005 THEN 'SETTLED'
        WHEN schedule.due_date < CURRENT_DATE() THEN 'OVERDUE'
        ELSE 'OPEN'
    END,
    item.updated_at = NOW()
WHERE document.withholding_total > 0.005
  AND item.settled_amount <= 0.005
  AND ABS(item.original_amount - schedule.amount) > 0.02;

UPDATE documents document
SET document.balance_due = GREATEST(0, ABS(document.total) - ABS(document.withholding_total)),
    document.updated_at = NOW()
WHERE document.withholding_total > 0.005
  AND NOT EXISTS (
      SELECT 1
      FROM payment_schedules schedule
      WHERE schedule.organization_id = document.organization_id
        AND schedule.document_id = document.id
        AND schedule.paid_amount > 0.005
  )
  AND ABS(document.balance_due - GREATEST(0, ABS(document.total) - ABS(document.withholding_total))) > 0.02;
