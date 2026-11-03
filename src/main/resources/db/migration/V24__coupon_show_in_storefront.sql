-- Coupons can be listed in the storefront (GET /api/v1/coupons/available). Off by default, so codes
-- made for one customer stay private until an admin chooses to show them.
-- IF NOT EXISTS: ddl-auto=update may already have added the column.
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS show_in_storefront BOOLEAN NOT NULL DEFAULT FALSE;
