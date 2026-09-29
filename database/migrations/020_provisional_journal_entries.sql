ALTER TABLE journal_entries
    ADD COLUMN is_finalized TINYINT(1) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN revision_number INT UNSIGNED NOT NULL DEFAULT 1 AFTER is_finalized,
    ADD COLUMN finalized_at DATETIME NULL AFTER revision_number,
    ADD COLUMN finalized_by BIGINT UNSIGNED NULL AFTER finalized_at,
    ADD COLUMN finalized_print_run_id BIGINT UNSIGNED NULL AFTER finalized_by,
    ADD KEY idx_journal_finalization (organization_id, entry_date, status, is_finalized);

CREATE TABLE IF NOT EXISTS journal_entry_revisions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT UNSIGNED NOT NULL,
    journal_entry_id BIGINT UNSIGNED NOT NULL,
    revision_number INT UNSIGNED NOT NULL,
    change_reason VARCHAR(500) NOT NULL,
    header_json JSON NOT NULL,
    lines_json JSON NOT NULL,
    changed_by BIGINT UNSIGNED NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_journal_revision (journal_entry_id, revision_number),
    KEY idx_journal_revision_org (organization_id, journal_entry_id),
    CONSTRAINT fk_journal_revision_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_revision_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE journal_entries e
INNER JOIN fiscal_years f ON f.organization_id = e.organization_id AND f.year = YEAR(e.entry_date) AND f.status = 'CLOSED'
SET e.is_finalized = 1, e.finalized_at = COALESCE(e.finalized_at, f.closed_at)
WHERE e.status = 'POSTED';

UPDATE journal_entries e
INNER JOIN official_print_runs p ON p.organization_id = e.organization_id
    AND p.print_type = 'JOURNAL' AND p.status = 'LOCKED'
    AND e.entry_date BETWEEN p.period_start AND p.period_end
SET e.is_finalized = 1, e.finalized_at = COALESCE(e.finalized_at, p.locked_at), e.finalized_print_run_id = p.id
WHERE e.status = 'POSTED';
