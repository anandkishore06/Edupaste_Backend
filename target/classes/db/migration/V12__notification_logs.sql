CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipient VARCHAR(150) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) DEFAULT 'EMAIL',
    application_number VARCHAR(50),
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
