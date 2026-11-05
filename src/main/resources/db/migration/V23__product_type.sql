-- Products get a type (MEN / WOMEN) so the shop can split and filter the catalog.
-- Nullable: existing products have no type until an admin sets one.
-- IF NOT EXISTS: ddl-auto=update may already have added the column.
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_product_type ON products (product_type);
