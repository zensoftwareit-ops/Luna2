UPDATE chart_of_accounts a
INNER JOIN (
    SELECT organization_id, account_id
    FROM journal_entry_lines
    GROUP BY organization_id, account_id
) used_account
    ON used_account.organization_id = a.organization_id
   AND used_account.account_id = a.id
LEFT JOIN chart_of_accounts child
    ON child.organization_id = a.organization_id
   AND child.parent_id = a.id
SET a.active = 1,
    a.is_postable = 1,
    a.updated_at = NOW()
WHERE child.id IS NULL
  AND (a.active <> 1 OR a.is_postable <> 1);
