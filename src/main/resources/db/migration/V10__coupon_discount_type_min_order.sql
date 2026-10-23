-- Coupons can now give either a percentage or a fixed-amount discount, and can require a
-- minimum order amount.
--
--   discount_percent  -> discount_value (widened: a fixed amount can exceed 999.99)
--   + discount_type     'PERCENTAGE' | 'FIXED'; existing rows were all percentages
--   + min_order_amount  NULL = no minimum
--
-- usage_limit / used_count were only ever added by ddl-auto=update, so they are created
-- here too for databases that never had them.

ALTER TABLE coupons ADD COLUMN IF NOT EXISTS usage_limit INTEGER;
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS used_count INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'coupons' AND column_name = 'discount_percent'
    ) THEN
        ALTER TABLE coupons RENAME COLUMN discount_percent TO discount_value;
    END IF;
END;
$$;

ALTER TABLE coupons ALTER COLUMN discount_value TYPE NUMERIC(10, 2);

ALTER TABLE coupons ADD COLUMN IF NOT EXISTS discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE';
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS min_order_amount NUMERIC(10, 2);

ALTER TABLE coupons DROP CONSTRAINT IF EXISTS chk_coupon_discount_type;
ALTER TABLE coupons ADD CONSTRAINT chk_coupon_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED'));
