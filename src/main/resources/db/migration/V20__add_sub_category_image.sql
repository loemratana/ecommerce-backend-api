-- Sub-categories get an uploaded image/logo URL, like categories.
-- 2048 chars to fit CDN / signed URLs (see V14).
ALTER TABLE sub_categories ADD COLUMN IF NOT EXISTS image VARCHAR(2048);
