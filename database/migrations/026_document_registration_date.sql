ALTER TABLE documents
    ADD COLUMN registration_date DATE NULL AFTER document_date,
    ADD KEY idx_documents_registration_date (organization_id, registration_date, status);

UPDATE documents
SET registration_date = document_date
WHERE registration_date IS NULL;
