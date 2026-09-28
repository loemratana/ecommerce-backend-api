-- ProductSize.status (boolean, not null) has no corresponding column in
-- product_sizes; ddl-auto=update hasn't added it in every environment, and
-- ddl-auto=validate (used elsewhere) requires the column to already exist.
-- Any listing that joins product_sizes (e.g. GET /api/v1/product) fails with
-- "column ps1_0.status does not exist" until this is added.

ALTER TABLE product_sizes ADD COLUMN IF NOT EXISTS status BOOLEAN NOT NULL DEFAULT TRUE;
