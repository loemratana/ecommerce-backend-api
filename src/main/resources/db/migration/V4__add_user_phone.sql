-- Adds the "phone" field required by the /users/me profile update endpoint
-- (api-design.md: PATCH /users/me updates name, phone, profile image).

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
