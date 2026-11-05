-- Product-wide discount: applies to every variant that has no discount_percent of its own.
-- Nullable: NULL = no product discount.
-- The variant / order-item columns were only ever added by ddl-auto=update, so make sure they exist too.
-- IF NOT EXISTS everywhere: ddl-auto=update may already have added these columns.
ALTER TABLE products         ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2);
ALTER TABLE order_items      ADD COLUMN IF NOT EXISTS discount_percent NUMERIC(5, 2);
