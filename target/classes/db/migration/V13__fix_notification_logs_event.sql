-- Migration to drop NOT NULL constraint on legacy event column in notification_logs
ALTER TABLE notification_logs ALTER COLUMN event DROP NOT NULL;
ALTER TABLE notification_logs ADD COLUMN IF NOT EXISTS event VARCHAR(100) DEFAULT 'ADMISSION_SUBMITTED';
