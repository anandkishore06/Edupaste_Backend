-- Migration to drop legacy email column and ensure all admission_applications columns exist
ALTER TABLE admission_applications DROP COLUMN IF EXISTS email;

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS public_code VARCHAR(100);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS application_number VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS academic_session_id UUID;
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS applying_class_id UUID;

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS date_of_birth DATE;
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS blood_group VARCHAR(10);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS nationality VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS religion VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS category VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS aadhaar_number VARCHAR(20);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS father_name VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS father_mobile VARCHAR(20);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS father_email VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS father_occupation VARCHAR(100);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS mother_name VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS mother_mobile VARCHAR(20);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS mother_email VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS mother_occupation VARCHAR(100);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS guardian_name VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS guardian_relation VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS guardian_mobile VARCHAR(20);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS present_address TEXT;
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS permanent_address TEXT;

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS previous_school VARCHAR(200);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS previous_board VARCHAR(100);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS previous_class VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS previous_percentage VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS transfer_certificate_available VARCHAR(50);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS contact_name VARCHAR(150);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS relation VARCHAR(50);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS mobile VARCHAR(20);
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS alternate_mobile VARCHAR(20);

ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'SUBMITTED';
ALTER TABLE admission_applications ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
