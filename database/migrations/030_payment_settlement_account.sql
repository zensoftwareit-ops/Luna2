ALTER TABLE payments
    ADD COLUMN settlement_account_id BIGINT UNSIGNED NULL AFTER bank_account_id,
    ADD KEY idx_payments_settlement_account (organization_id, settlement_account_id),
    ADD CONSTRAINT fk_payment_settlement_account
        FOREIGN KEY (settlement_account_id) REFERENCES chart_of_accounts(id) ON DELETE SET NULL;

UPDATE payments payment
JOIN bank_accounts bank
  ON bank.id = payment.bank_account_id
 AND bank.organization_id = payment.organization_id
SET payment.settlement_account_id = bank.chart_account_id
WHERE payment.settlement_account_id IS NULL
  AND bank.chart_account_id IS NOT NULL;
