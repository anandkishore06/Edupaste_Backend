CREATE TABLE IF NOT EXISTS teachers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(50) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    gender VARCHAR(20),
    date_of_birth DATE,
    qualification VARCHAR(100),
    experience VARCHAR(50),
    joining_date DATE,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS parents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    father_name VARCHAR(100),
    mother_name VARCHAR(100),
    guardian_name VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(150) NOT NULL,
    occupation VARCHAR(100),
    address TEXT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id BIGINT NOT NULL,
    branch_id BIGINT,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES parents(id) ON DELETE SET NULL,
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    section_id UUID REFERENCES sections(id) ON DELETE SET NULL,
    admission_number VARCHAR(50) NOT NULL,
    roll_number VARCHAR(50),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    gender VARCHAR(20),
    date_of_birth DATE,
    blood_group VARCHAR(10),
    email VARCHAR(150),
    mobile VARCHAR(20),
    address TEXT,
    
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    deleted_at TIMESTAMP,
    version INT DEFAULT 0
);
