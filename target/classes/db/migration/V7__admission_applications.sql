CREATE TABLE IF NOT EXISTS admission_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    public_code VARCHAR(100) NOT NULL,
    application_number VARCHAR(50) NOT NULL UNIQUE,
    academic_session_id UUID,
    applying_class_id UUID,
    
    -- Student Info
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_group VARCHAR(10),
    nationality VARCHAR(50),
    religion VARCHAR(50),
    category VARCHAR(50),
    aadhaar_number VARCHAR(20),
    
    -- Father Info
    father_name VARCHAR(150) NOT NULL,
    father_mobile VARCHAR(20) NOT NULL,
    father_email VARCHAR(150) NOT NULL,
    father_occupation VARCHAR(100),
    
    -- Mother Info
    mother_name VARCHAR(150),
    mother_mobile VARCHAR(20),
    mother_email VARCHAR(150),
    mother_occupation VARCHAR(100),
    
    -- Guardian Info
    guardian_name VARCHAR(150),
    guardian_relation VARCHAR(50),
    guardian_mobile VARCHAR(20),
    
    -- Address
    present_address TEXT NOT NULL,
    permanent_address TEXT NOT NULL,
    
    -- Previous School
    previous_school VARCHAR(200),
    previous_board VARCHAR(100),
    previous_class VARCHAR(50),
    previous_percentage VARCHAR(50),
    transfer_certificate_available VARCHAR(50),
    
    -- Emergency Contact
    contact_name VARCHAR(150) NOT NULL,
    relation VARCHAR(50) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    alternate_mobile VARCHAR(20),
    
    -- Application Status
    status VARCHAR(50) DEFAULT 'SUBMITTED',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admission_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admission_application_id UUID NOT NULL REFERENCES admission_applications(id) ON DELETE CASCADE,
    document_key VARCHAR(100) NOT NULL,
    document_name VARCHAR(150) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admission_status_histories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID NOT NULL REFERENCES admission_applications(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    remarks TEXT,
    changed_by VARCHAR(100) DEFAULT 'APPLICANT',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS school_application_sequences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    current_year INT NOT NULL,
    last_sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_school_year UNIQUE (school_id, current_year)
);
