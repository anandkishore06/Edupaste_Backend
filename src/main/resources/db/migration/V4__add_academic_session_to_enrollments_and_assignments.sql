ALTER TABLE student_enrollments 
ADD COLUMN IF NOT EXISTS academic_session_id UUID REFERENCES academic_sessions(id) ON DELETE SET NULL;

ALTER TABLE teacher_assignments 
ADD COLUMN IF NOT EXISTS academic_session_id UUID REFERENCES academic_sessions(id) ON DELETE SET NULL;
