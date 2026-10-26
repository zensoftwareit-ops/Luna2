-- Le causali DATEV importate erano visibili nell'elenco di configurazione ma
-- disattivate, quindi non selezionabili in prima nota. La configurazione resta
-- modificabile dall'utente e consente di disattivare singole causali in seguito.
UPDATE accounting_causes
SET active = 1, updated_at = NOW()
WHERE active = 0;
