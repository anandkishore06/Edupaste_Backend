-- Migration to fix legacy admission_documents columns from prior auto-DDL runs
ALTER TABLE admission_documents DROP COLUMN IF EXISTS application_id;

ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS admission_application_id UUID REFERENCES admission_applications(id) ON DELETE CASCADE;
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS document_key VARCHAR(100);
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS document_name VARCHAR(150);
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS file_name VARCHAR(255);
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE admission_documents ADD COLUMN IF NOT EXISTS uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
