-- Aligns carts/cart_items/orders/order_items with the current entity mappings.
--
-- V2 captured these tables as the entities looked at the time; the entities have since been
-- fixed (typo'd column names renamed, OrderItem joined via order_id, Order.user added,
-- Cart.orderItems removed) but no migration followed, so ddl-auto=validate failed with
-- "missing column [order_id] in table [order_items]".
--
-- Two starting states have to work:
--   - fresh DB: tables are exactly as V2/V7 left them (Flyway runs before Hibernate);
--   - existing dev DB: ddl-auto=update has already added the new columns next to the old
--     ones (e.g. both tax_pice and tax_price), plus its own copies of some constraints.
-- So each old column is renamed if the new one doesn't exist yet, and otherwise merged into
-- it and dropped.
--
-- No data is invented: orders.user_id has no source to backfill from, so SET NOT NULL fails
-- loudly if legacy rows exist. At the time of writing these tables are empty everywhere.

CREATE FUNCTION pg_temp.column_exists(tbl TEXT, col TEXT) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = tbl AND column_name = col
    );
$$ LANGUAGE sql;

CREATE FUNCTION pg_temp.rename_or_merge(tbl TEXT, old_col TEXT, new_col TEXT) RETURNS VOID AS $$
BEGIN
    IF NOT pg_temp.column_exists(tbl, old_col) THEN
        RETURN;
    END IF;
    IF pg_temp.column_exists(tbl, new_col) THEN
        EXECUTE format('UPDATE %I SET %I = COALESCE(%I, %I)', tbl, new_col, new_col, old_col);
        EXECUTE format('ALTER TABLE %I DROP COLUMN %I', tbl, old_col);
    ELSE
        EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', tbl, old_col, new_col);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.add_fk_if_missing(tbl TEXT, col TEXT, ref_tbl TEXT, fk_name TEXT) RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = current_schema() AND tc.table_name = tbl
          AND tc.constraint_type = 'FOREIGN KEY' AND kcu.column_name = col
    ) THEN
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I (id)',
                       tbl, fk_name, col, ref_tbl);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Cart.orderItems was removed; its implicit join table is unused.
DROP TABLE IF EXISTS carts_order_items;

-- orders -------------------------------------------------------------------------------------

SELECT pg_temp.rename_or_merge('orders', 'tax_pice', 'tax_price');
SELECT pg_temp.rename_or_merge('orders', 'shopping_price', 'shipping_price');
SELECT pg_temp.rename_or_merge('orders', 'total_order_pice', 'total_order_price');

ALTER TABLE orders ADD COLUMN IF NOT EXISTS user_id BIGINT;

ALTER TABLE orders
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN tax_price TYPE NUMERIC(19, 2),
    ALTER COLUMN shipping_price TYPE NUMERIC(19, 2),
    ALTER COLUMN total_order_price TYPE NUMERIC(19, 2);

UPDATE orders SET update_date = order_date WHERE update_date IS NULL;

ALTER TABLE orders
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN total_order_price SET NOT NULL,
    ALTER COLUMN order_date SET NOT NULL,
    ALTER COLUMN update_date SET NOT NULL;

SELECT pg_temp.add_fk_if_missing('orders', 'user_id', 'users', 'fk_orders_user');

-- V2's idx_user_id was on orders(id), not user_id; both V2 indexes are replaced by the
-- entity-named ones.
DROP INDEX IF EXISTS idx_user_id;
DROP INDEX IF EXISTS idx_status;
CREATE INDEX IF NOT EXISTS idx_order_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders (status);

-- order_items --------------------------------------------------------------------------------

-- V2's user_id already referenced orders(id) (see the note in V2), so its FK carries over.
SELECT pg_temp.rename_or_merge('order_items', 'user_id', 'order_id');

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_variant_id BIGINT;
ALTER TABLE order_items
    ALTER COLUMN order_id SET NOT NULL,
    ALTER COLUMN product_variant_id SET NOT NULL;

SELECT pg_temp.add_fk_if_missing('order_items', 'order_id', 'orders', 'fk_order_items_order');
SELECT pg_temp.add_fk_if_missing('order_items', 'product_variant_id', 'product_variants', 'fk_order_items_product_variant');

-- carts --------------------------------------------------------------------------------------

ALTER TABLE carts ALTER COLUMN total_price_after_discount TYPE NUMERIC(19, 2);
UPDATE carts SET total_price_after_discount = COALESCE(total_cart_price, 0)
    WHERE total_price_after_discount IS NULL;
ALTER TABLE carts
    ALTER COLUMN total_price_after_discount SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE carts DROP CONSTRAINT IF EXISTS uk_carts_user_id;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cart_user') THEN
        ALTER TABLE carts ADD CONSTRAINT uk_cart_user UNIQUE (user_id);
    END IF;
END;
$$;
CREATE INDEX IF NOT EXISTS idx_cart_user_id ON carts (user_id);

-- cart_items ---------------------------------------------------------------------------------

ALTER TABLE cart_items ALTER COLUMN price TYPE NUMERIC(19, 2);

-- V7 named this constraint uk_cart_items_cart_product; the entity calls it uk_cart_product.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cart_product') THEN
        ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_items_cart_product;
    ELSIF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_cart_items_cart_product') THEN
        ALTER TABLE cart_items RENAME CONSTRAINT uk_cart_items_cart_product TO uk_cart_product;
    ELSE
        ALTER TABLE cart_items ADD CONSTRAINT uk_cart_product UNIQUE (cart_id, product_id);
    END IF;
END;
$$;
CREATE INDEX IF NOT EXISTS idx_cart_item_cart_id ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_item_product_id ON cart_items (product_id);
