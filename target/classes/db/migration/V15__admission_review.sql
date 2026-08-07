CREATE TABLE admission_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES admission_applications(id) ON DELETE CASCADE,
    review_status VARCHAR(50) NOT NULL,
    internal_notes TEXT,
    parent_remarks TEXT,
    reviewed_by_user_id UUID NOT NULL,
    reviewed_by_name VARCHAR(255) NOT NULL,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE admission_status_histories ADD COLUMN event_type VARCHAR(50);
UPDATE admission_status_histories SET event_type = 'STATUS_CHANGED' WHERE event_type IS NULL;
ALTER TABLE admission_status_histories ALTER COLUMN event_type SET NOT NULL;
