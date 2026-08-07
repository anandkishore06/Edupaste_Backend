ALTER TABLE admission_reviews DROP COLUMN reviewed_by_user_id;
ALTER TABLE admission_reviews ADD COLUMN reviewed_by_user_id BIGINT;
