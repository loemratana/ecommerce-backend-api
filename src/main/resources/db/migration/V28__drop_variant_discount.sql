-- Discounts live on the product only (products.discount_percent). Variants keep just their price;
-- the price after discount is calculated, never stored.
-- IF EXISTS: these columns were only ever created by ddl-auto=update, so some databases lack them.
ALTER TABLE product_variants DROP COLUMN IF EXISTS discount_percent;
ALTER TABLE product_variants DROP COLUMN IF EXISTS price_after_discount;
