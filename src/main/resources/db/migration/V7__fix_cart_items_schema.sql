-- V2 predates CartItem.product and Cart.version. With ddl-auto=validate, Hibernate needs
-- every mapped column to exist, so startup fails as soon as the cart feature is touched:
--   - cart_items has no product_id column (CartItem.product, nullable = false)
--   - carts has no version column (Cart.version, @Version)
-- cart_items.user_id / uk_cart_items_user_id are leftovers from before the product_id FK was
-- added; they aren't mapped by CartItem and only get in the way (the unique constraint would
-- limit a user to a single cart item ever), so they're dropped here too.
-- Both tables are still empty (nothing could have inserted successfully against this schema).

ALTER TABLE carts ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_items_user_id;
ALTER TABLE cart_items DROP COLUMN IF EXISTS user_id;

ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS product_id BIGINT REFERENCES products (id);
ALTER TABLE cart_items ALTER COLUMN product_id SET NOT NULL;
ALTER TABLE cart_items ALTER COLUMN cart_id SET NOT NULL;
ALTER TABLE cart_items ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE cart_items ALTER COLUMN price SET NOT NULL;

ALTER TABLE cart_items ADD CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id);
