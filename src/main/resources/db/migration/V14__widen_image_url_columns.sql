-- 500 chars was too short for CDN / signed image URLs (Cloudinary, S3 presigned, etc.).
ALTER TABLE product_images ALTER COLUMN image_url TYPE VARCHAR(2048);
ALTER TABLE products ALTER COLUMN image_cover TYPE VARCHAR(2048);
