ALTER TABLE users
    MODIFY COLUMN role ENUM('SUPERUSER','OWNER','ADMIN','MANAGER','OPERATOR','ACCOUNTANT','SALES','WAREHOUSE','HR','VIEWER') NOT NULL DEFAULT 'VIEWER',
    ADD COLUMN account_type ENUM('HUMAN','SYSTEM','TECHNICAL') NOT NULL DEFAULT 'HUMAN' AFTER role;

UPDATE users SET account_type = 'TECHNICAL' WHERE role = 'SUPERUSER';

CREATE INDEX idx_users_license_count ON users (organization_id, account_type, active);
