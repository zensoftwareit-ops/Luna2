UPDATE journal_entries
SET source_protocol = SUBSTRING_INDEX(
        SUBSTRING(notes, CHAR_LENGTH('Protocollo origine: ') + 1),
        ' · ',
        1
    ),
    updated_at = NOW()
WHERE (source_protocol IS NULL OR source_protocol = '')
  AND notes LIKE 'Protocollo origine: DATEV-%';

UPDATE journal_entries
SET status = 'POSTED',
    posted_at = COALESCE(posted_at, created_at, NOW()),
    updated_at = NOW()
WHERE status = 'DRAFT'
  AND entry_type IN ('OPENING', 'CLOSING')
  AND source_protocol LIKE 'DATEV-%';
