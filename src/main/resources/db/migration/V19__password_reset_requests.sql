CREATE TABLE IF NOT EXISTS password_reset_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    verification_channel VARCHAR(20) NOT NULL,
    recipient_masked VARCHAR(150) NOT NULL,
    hashed_code VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    failed_attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 5,
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    reset_token VARCHAR(255),
    reset_token_expires_at TIMESTAMP,
    is_used BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_user ON password_reset_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_token ON password_reset_requests(reset_token);
