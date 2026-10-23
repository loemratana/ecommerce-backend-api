-- Every variant needs an inventory row: stock adjustments, add-to-cart and checkout all look it
-- up and fail with "Inventory not found" otherwise. Variants inserted outside ProductService
-- (seed data, manual SQL) never got one. Start them at 0 - real stock is set from the admin.
INSERT INTO inventories (product_variant_id, quantity, reserved_quantity, version, created_at, updated_at)
SELECT v.id, 0, 0, 0, now(), now()
FROM product_variants v
WHERE NOT EXISTS (SELECT 1 FROM inventories i WHERE i.product_variant_id = v.id);
