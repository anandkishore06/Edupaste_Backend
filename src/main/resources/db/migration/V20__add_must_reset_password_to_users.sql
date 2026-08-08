ALTER TABLE users ADD COLUMN IF NOT EXISTS must_reset_password BOOLEAN DEFAULT TRUE NOT NULL;

-- Keep Super Admin accounts exempt from mandatory first-login password reset by default
UPDATE users SET must_reset_password = FALSE WHERE role = 'SUPER_ADMIN';
