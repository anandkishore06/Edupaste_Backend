CREATE TABLE IF NOT EXISTS admission_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL UNIQUE,
    branch_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    is_admission_open BOOLEAN DEFAULT FALSE,
    start_date DATE,
    end_date DATE,
    academic_session_id UUID REFERENCES academic_sessions(id) ON DELETE SET NULL,
    admission_email VARCHAR(150),
    admission_phone VARCHAR(30),
    office_hours VARCHAR(100),
    admission_instructions TEXT,
    public_code VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS admission_allowed_classes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admission_settings_id UUID NOT NULL REFERENCES admission_settings(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_settings_class UNIQUE (admission_settings_id, class_id)
);

CREATE TABLE IF NOT EXISTS admission_required_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admission_settings_id UUID NOT NULL REFERENCES admission_settings(id) ON DELETE CASCADE,
    document_name VARCHAR(100) NOT NULL,
    document_key VARCHAR(100) NOT NULL,
    is_required BOOLEAN DEFAULT TRUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
