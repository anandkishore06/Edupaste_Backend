-- Migration to fix legacy document_type and other legacy column constraints on admission_documents table
ALTER TABLE admission_documents DROP COLUMN IF EXISTS document_type;
ALTER TABLE admission_documents DROP COLUMN IF EXISTS file_path;
ALTER TABLE admission_documents DROP COLUMN IF EXISTS document_url;
ALTER TABLE admission_documents DROP COLUMN IF EXISTS url;
ALTER TABLE admission_documents DROP COLUMN IF EXISTS description;
