-- UAT seed data for approval workflow
-- Eseguire dopo approval_workflow_schema.sql

INSERT INTO approval_requests (
  company_id,
  requester_id,
  approver_id,
  request_type,
  description,
  start_date,
  end_date,
  days_requested,
  status,
  created_date,
  submitted_date,
  updated_date
)
VALUES
(1, 1, 2, 'FERIE', 'UAT - ferie estive', DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), 3, 'SUBMITTED', NOW(), NOW(), NOW()),
(1, 1, 2, 'FERIE', 'UAT - ferie approvate', DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 24 DAY), 5, 'APPROVED', NOW(), NOW(), NOW()),
(1, 1, 2, 'FERIE', 'UAT - ferie rifiutate', DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 31 DAY), 2, 'REJECTED', NOW(), NOW(), NOW());
