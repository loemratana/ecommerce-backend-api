-- Reworks orders / order_items / order_coupons to match the redesigned Order entities.
--
-- orders
--   + order_number      public identifier (ORD-yyyyMMdd-XXXXXXXX), unique
--   + version           optimistic locking (@Version)
--   + subtotal, discount_amount; tax_price / shipping_price become NOT NULL DEFAULT 0
--   + ship_*            copy of the delivery address taken when the order is placed
--   status gains PAID; idx_order_user_id -> idx_order_user_date (user_id, order_date)
--
-- order_items
--   + product_name, sku, color, size_name   copied from the variant at order time
--   product_variant_id becomes nullable, FK ON DELETE SET NULL, so variants can be deleted
--
-- order_coupons
--   discount_percent -> discount_amount (money; a FIXED coupon has no percent)
--   + coupon_code      copied from the coupon at order time
--
-- Existing rows are backfilled where there is a source (variants, coupons, the user's
-- default address). As in V9, a legacy order whose user has no default address makes the
-- ship_* SET NOT NULL fail loudly rather than inventing an address.

-- order_items --------------------------------------------------------------------------------

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS sku VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS color VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS size_name VARCHAR(50);

UPDATE order_items oi
SET product_name = COALESCE(oi.product_name, p.title, pv.sku),
    sku          = COALESCE(oi.sku, pv.sku),
    color        = COALESCE(oi.color, pv.color),
    size_name    = COALESCE(oi.size_name, ps.name)
FROM product_variants pv
LEFT JOIN products p ON p.id = pv.prouduct_id
LEFT JOIN product_sizes ps ON ps.id = pv.product_size_id
WHERE pv.id = oi.product_variant_id;

ALTER TABLE order_items
    ALTER COLUMN product_name SET NOT NULL,
    ALTER COLUMN sku SET NOT NULL,
    ALTER COLUMN product_variant_id DROP NOT NULL;

-- Replace whatever FK exists on product_variant_id (V9's or one added by ddl-auto=update)
-- with one that lets the variant be deleted.
DO $$
DECLARE
    fk RECORD;
BEGIN
    FOR fk IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema
        WHERE tc.table_schema = current_schema() AND tc.table_name = 'order_items'
          AND tc.constraint_type = 'FOREIGN KEY' AND kcu.column_name = 'product_variant_id'
    LOOP
        EXECUTE format('ALTER TABLE order_items DROP CONSTRAINT %I', fk.constraint_name);
    END LOOP;
END;
$$;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_product_variant
    FOREIGN KEY (product_variant_id) REFERENCES product_variants (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_order_item_order_id ON order_items (order_id);

-- order_coupons ------------------------------------------------------------------------------

ALTER TABLE order_coupons ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);
ALTER TABLE order_coupons ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 2);

UPDATE order_coupons oc
SET coupon_code = c.code
FROM coupons c
WHERE c.id = oc.coupon_id AND oc.coupon_code IS NULL;

-- Legacy rows only stored a percent; turn it into money using the order's item total.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'order_coupons' AND column_name = 'discount_percent'
    ) THEN
        UPDATE order_coupons oc
        SET discount_amount = ROUND(
                COALESCE((SELECT SUM(oi.price_after_discount * oi.quantity)
                          FROM order_items oi WHERE oi.order_id = oc.order_id), 0)
                * oc.discount_percent / 100, 2)
        WHERE oc.discount_amount IS NULL;

        ALTER TABLE order_coupons DROP COLUMN discount_percent;
    END IF;
END;
$$;

ALTER TABLE order_coupons
    ALTER COLUMN coupon_code SET NOT NULL,
    ALTER COLUMN discount_amount SET NOT NULL;

-- orders -------------------------------------------------------------------------------------

ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_number VARCHAR(32);
UPDATE orders
SET order_number = 'ORD-' || to_char(order_date, 'YYYYMMDD') || '-' || lpad(id::TEXT, 8, '0')
WHERE order_number IS NULL;
ALTER TABLE orders ALTER COLUMN order_number SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_order_number') THEN
        ALTER TABLE orders ADD CONSTRAINT uk_order_number UNIQUE (order_number);
    END IF;
END;
$$;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Money columns
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal NUMERIC(19, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 2);

UPDATE orders o
SET subtotal = COALESCE((SELECT SUM(oi.price_after_discount * oi.quantity)
                         FROM order_items oi WHERE oi.order_id = o.id), 0)
WHERE o.subtotal IS NULL;

UPDATE orders o
SET discount_amount = LEAST(
        COALESCE((SELECT SUM(oc.discount_amount)
                  FROM order_coupons oc WHERE oc.order_id = o.id), 0),
        o.subtotal)
WHERE o.discount_amount IS NULL;

UPDATE orders SET tax_price = 0 WHERE tax_price IS NULL;
UPDATE orders SET shipping_price = 0 WHERE shipping_price IS NULL;

ALTER TABLE orders
    ALTER COLUMN subtotal SET DEFAULT 0,
    ALTER COLUMN subtotal SET NOT NULL,
    ALTER COLUMN discount_amount SET DEFAULT 0,
    ALTER COLUMN discount_amount SET NOT NULL,
    ALTER COLUMN tax_price SET DEFAULT 0,
    ALTER COLUMN tax_price SET NOT NULL,
    ALTER COLUMN shipping_price SET DEFAULT 0,
    ALTER COLUMN shipping_price SET NOT NULL;

-- Shipping address copy, backfilled from the user's most recent default address.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_recipient_name VARCHAR(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_phone VARCHAR(30);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_street VARCHAR(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_city VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_state VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_zip_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_country VARCHAR(100);

UPDATE orders o
SET ship_recipient_name = u.first_name || ' ' || u.last_name,
    ship_phone          = u.phone,
    ship_street         = a.street,
    ship_city           = a.city,
    ship_state          = a.state,
    ship_zip_code       = a.zip_code,
    ship_country        = a.country
FROM users u
JOIN LATERAL (
    SELECT * FROM addresses ad
    WHERE ad.user_id = u.id AND ad.is_default
    ORDER BY ad.updated_at DESC
    LIMIT 1
) a ON TRUE
WHERE u.id = o.user_id AND o.ship_street IS NULL;

ALTER TABLE orders
    ALTER COLUMN ship_recipient_name SET NOT NULL,
    ALTER COLUMN ship_street SET NOT NULL,
    ALTER COLUMN ship_city SET NOT NULL,
    ALTER COLUMN ship_state SET NOT NULL,
    ALTER COLUMN ship_zip_code SET NOT NULL,
    ALTER COLUMN ship_country SET NOT NULL;

-- Status: V2 created an unnamed CHECK (orders_status_check) without PAID, and ddl-auto=update
-- may have added its own; drop every CHECK that mentions status and add one named constraint.
DO $$
DECLARE
    chk RECORD;
BEGIN
    FOR chk IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'orders'::regclass AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE orders DROP CONSTRAINT %I', chk.conname);
    END LOOP;
END;
$$;

ALTER TABLE orders ADD CONSTRAINT chk_order_status
    CHECK (status IN ('PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- "My orders, newest first" filters on user_id and sorts by order_date.
DROP INDEX IF EXISTS idx_order_user_id;
CREATE INDEX IF NOT EXISTS idx_order_user_date ON orders (user_id, order_date);
