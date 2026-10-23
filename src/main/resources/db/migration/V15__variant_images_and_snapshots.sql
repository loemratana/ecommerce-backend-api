-- Images can be tagged with a variant color, so picking a color on the storefront switches
-- the gallery; order items keep a copy of the image of the color that was bought.
--
-- IF [NOT] EXISTS everywhere: ddl-auto=update may already have added the columns.

ALTER TABLE product_images ADD COLUMN IF NOT EXISTS color VARCHAR(50);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS image_url VARCHAR(2048);

-- Backfill existing order items with the product cover (best available guess).
UPDATE order_items oi
SET image_url = p.image_cover
FROM product_variants pv
JOIN products p ON p.id = pv.prouduct_id
WHERE pv.id = oi.product_variant_id
  AND oi.image_url IS NULL;

-- A customer selects a variant by color + size, so that pair must be unique per product.
-- Expression index so NULL color/size still count as duplicates and color is case-insensitive.
-- Skipped (with a notice) if existing data already has duplicates, so the migration never
-- blocks startup; clean the duplicates up and create the index by hand.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM product_variants
        GROUP BY prouduct_id, lower(trim(coalesce(color, ''))), coalesce(product_size_id, 0)
        HAVING count(*) > 1
    ) THEN
        RAISE NOTICE 'uk_product_variant_color_size not created: duplicate color/size variants exist';
    ELSE
        CREATE UNIQUE INDEX IF NOT EXISTS uk_product_variant_color_size
            ON product_variants (prouduct_id, lower(trim(coalesce(color, ''))), coalesce(product_size_id, 0));
    END IF;
END $$;

-- Every variant must belong to a product (same rule: skipped if orphans exist).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM product_variants WHERE prouduct_id IS NULL) THEN
        RAISE NOTICE 'product_variants.prouduct_id left nullable: variants without a product exist';
    ELSE
        ALTER TABLE product_variants ALTER COLUMN prouduct_id SET NOT NULL;
    END IF;
END $$;
