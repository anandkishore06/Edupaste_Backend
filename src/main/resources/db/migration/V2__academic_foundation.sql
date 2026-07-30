CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS academic_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    description TEXT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS academic_terms (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES academic_sessions(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    name VARCHAR(100) NOT NULL,
    display_order INT DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES academic_sessions(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    name VARCHAR(100) NOT NULL,
    display_order INT DEFAULT 0,
    description TEXT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_id UUID NOT NULL REFERENCES classes(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    name VARCHAR(50) NOT NULL,
    capacity INT DEFAULT 30,
    room VARCHAR(50),
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    subject_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) DEFAULT 'THEORY',
    is_elective BOOLEAN DEFAULT FALSE,
    is_language BOOLEAN DEFAULT FALSE,
    credits INT DEFAULT 1,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0,
    UNIQUE(school_id, subject_code)
);

CREATE TABLE IF NOT EXISTS class_subjects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id UUID NOT NULL REFERENCES sections(id),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    weekly_periods INT DEFAULT 0,
    is_mandatory BOOLEAN DEFAULT TRUE,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS teacher_assignments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    class_subject_id UUID NOT NULL REFERENCES class_subjects(id),
    teacher_id BIGINT NOT NULL REFERENCES users(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS student_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    section_id UUID NOT NULL REFERENCES sections(id),
    student_id BIGINT NOT NULL REFERENCES users(id),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    roll_number VARCHAR(50),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);
