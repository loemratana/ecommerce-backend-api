-- Cart items now point at a product variant (size/color) instead of a product, so checkout
-- can turn them straight into order items.
--
-- Existing rows can't be migrated: a product has several variants and there's no way to
-- know which one the user meant. Carts are throwaway data, so they're simply emptied.
--
-- Has to work on both a fresh DB and a dev DB where ddl-auto=update already added
-- product_variant_id next to product_id, hence the IF [NOT] EXISTS everywhere.

DELETE FROM cart_items;
UPDATE carts SET total_cart_price = 0, total_price_after_discount = 0;

-- old product-based column + its constraint/index (DROP COLUMN also removes its FK)
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_product;
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_items_cart_product;
DROP INDEX IF EXISTS idx_cart_item_product_id;
ALTER TABLE cart_items DROP COLUMN IF EXISTS product_id;

-- new variant-based column
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS product_variant_id BIGINT;
ALTER TABLE cart_items ALTER COLUMN product_variant_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = current_schema() AND tc.table_name = 'cart_items'
          AND tc.constraint_type = 'FOREIGN KEY' AND kcu.column_name = 'product_variant_id'
    ) THEN
        ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_product_variant
            FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cart_product_variant') THEN
        ALTER TABLE cart_items ADD CONSTRAINT uk_cart_product_variant
            UNIQUE (cart_id, product_variant_id);
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_cart_item_product_variant_id ON cart_items (product_variant_id);
