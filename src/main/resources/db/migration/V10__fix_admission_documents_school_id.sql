-- Migration to fix legacy NOT NULL constraints on admission_documents table
ALTER TABLE admission_documents ALTER COLUMN school_id DROP NOT NULL;
ALTER TABLE admission_documents ALTER COLUMN branch_id DROP NOT NULL;
ALTER TABLE admission_documents ALTER COLUMN status DROP NOT NULL;
