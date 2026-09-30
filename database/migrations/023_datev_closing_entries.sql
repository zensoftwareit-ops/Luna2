UPDATE journal_entries
SET entry_type = 'CLOSING', updated_at = NOW()
WHERE entry_type = 'DATEV_IMPORT'
  AND description IN ('Bilancio chiusura', 'Chiusura conto economico', 'G/c risultato d''esercizio');

UPDATE journal_entries
SET entry_type = 'OPENING', updated_at = NOW()
WHERE entry_type = 'DATEV_IMPORT'
  AND description = 'Bilancio apertura';
