-- Parents Table updates
ALTER TABLE parents ADD COLUMN IF NOT EXISTS guardian_relation VARCHAR(50);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS alternate_mobile VARCHAR(20);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS state VARCHAR(100);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE parents ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='parents' AND column_name='phone'
    ) THEN
        ALTER TABLE parents RENAME COLUMN phone TO mobile;
    END IF;
END $$;

-- Students Table updates
ALTER TABLE students DROP COLUMN IF EXISTS class_id;
ALTER TABLE students DROP COLUMN IF EXISTS section_id;
ALTER TABLE students DROP COLUMN IF EXISTS roll_number;

ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_date DATE;
ALTER TABLE students ADD COLUMN IF NOT EXISTS admission_session_id UUID REFERENCES academic_sessions(id) ON DELETE SET NULL;
ALTER TABLE students ADD COLUMN IF NOT EXISTS photo TEXT;

-- Student Enrollments Table updates
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name='student_enrollments' AND column_name='student_id' AND data_type='bigint'
    ) THEN
        -- Add temporary nullable UUID column
        ALTER TABLE student_enrollments ADD COLUMN student_id_new UUID;
        
        -- Populate student_id_new from students table where user_id matches
        UPDATE student_enrollments se 
        SET student_id_new = s.id 
        FROM students s 
        WHERE s.user_id = se.student_id;
        
        -- Delete orphaned enrollments that don't map to a student
        DELETE FROM student_enrollments WHERE student_id_new IS NULL;
        
        -- Drop old student_id
        ALTER TABLE student_enrollments DROP CONSTRAINT IF EXISTS student_enrollments_student_id_fkey;
        ALTER TABLE student_enrollments DROP COLUMN student_id;
        
        -- Rename new column
        ALTER TABLE student_enrollments RENAME COLUMN student_id_new TO student_id;
        
        -- Make it NOT NULL
        ALTER TABLE student_enrollments ALTER COLUMN student_id SET NOT NULL;
    END IF;
END $$;

-- Ensure foreign key constraint is active
ALTER TABLE student_enrollments DROP CONSTRAINT IF EXISTS student_enrollments_student_uuid_fkey;
ALTER TABLE student_enrollments DROP CONSTRAINT IF EXISTS fk_student_enrollments_students;
ALTER TABLE student_enrollments DROP CONSTRAINT IF EXISTS student_enrollments_student_id_fkey;
ALTER TABLE student_enrollments ADD CONSTRAINT student_enrollments_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;

-- Add other enrollment columns
ALTER TABLE student_enrollments ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES classes(id) ON DELETE SET NULL;
ALTER TABLE student_enrollments ADD COLUMN IF NOT EXISTS enrollment_status VARCHAR(20) DEFAULT 'ACTIVE';

-- Populate class_id for existing enrollments from sections table
UPDATE student_enrollments se 
SET class_id = sec.class_id 
FROM sections sec 
WHERE se.section_id = sec.id AND se.class_id IS NULL;
