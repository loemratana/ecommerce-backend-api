-- Stock lives only in inventories (per variant: quantity - reserved_quantity). This product-level
-- copy was never updated by orders or restocks, so stats and New Arrivals built on it were wrong.
ALTER TABLE products DROP COLUMN IF EXISTS stock_quantity;
