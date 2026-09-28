-- products/product_sizes carry legacy columns from before the entities were renamed
-- to created_at/updated_at/ratings_quantity (create_at, update_at, rating_count on
-- products; create_at on product_sizes). They are NOT NULL with no default and are not
-- populated by the current entity mapping, so every insert into these tables currently
-- fails - both tables are still empty. Safe to drop: no data exists in either table yet.

ALTER TABLE products DROP COLUMN IF EXISTS create_at;
ALTER TABLE products DROP COLUMN IF EXISTS update_at;
ALTER TABLE products DROP COLUMN IF EXISTS rating_count;

ALTER TABLE product_sizes DROP COLUMN IF EXISTS create_at;
