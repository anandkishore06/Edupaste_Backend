-- Migration to drop NOT NULL constraint on legacy channel column in notification_logs
ALTER TABLE notification_logs ALTER COLUMN channel DROP NOT NULL;
ALTER TABLE notification_logs ADD COLUMN IF NOT EXISTS channel VARCHAR(50) DEFAULT 'EMAIL';
