-- Cart/order schema for the Cart -> CartItem and Order -> OrderItem models.
--
-- These tables were never created by any migration (ddl-auto was already "validate" by the
-- time these entities were added), so Hibernate schema validation failed with
-- "missing table [cart_items]" on startup. Shapes below match exactly what Hibernate expects
-- for the current entity mappings, verified by diffing against a Hibernate-generated schema.
--
-- roles.active is backfilled here too: the Role entity has a non-nullable "active" column
-- that was never applied to the existing roles table for the same reason.

ALTER TABLE roles ADD COLUMN IF NOT EXISTS active BOOLEAN;
UPDATE roles SET active = TRUE WHERE active IS NULL;
ALTER TABLE roles ALTER COLUMN active SET NOT NULL;

CREATE TABLE IF NOT EXISTS carts (
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT REFERENCES users (id),
    total_cart_price            NUMERIC(38, 2),
    total_price_after_discount  NUMERIC(38, 2),
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_carts_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGSERIAL PRIMARY KEY,
    cart_id     BIGINT REFERENCES carts (id),
    user_id     BIGINT REFERENCES users (id),
    quantity    INTEGER,
    price       NUMERIC(38, 2),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_cart_items_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id                BIGSERIAL PRIMARY KEY,
    status            VARCHAR(255) CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    tax_pice          NUMERIC(38, 2),
    shopping_price    NUMERIC(38, 2),
    total_order_pice  NUMERIC(38, 2),
    order_date        TIMESTAMP,
    update_date       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_id ON orders (id);
CREATE INDEX IF NOT EXISTS idx_status ON orders (status);

-- NOTE: OrderItem.order is mapped via @JoinColumn(name = "user_id"), so despite the name this
-- column is a foreign key to orders(id), not to users. Matches the existing entity mapping as-is.
CREATE TABLE IF NOT EXISTS order_items (
    id                    BIGSERIAL PRIMARY KEY,
    quantity              INTEGER NOT NULL CHECK (quantity >= 1),
    price                 NUMERIC(19, 2) NOT NULL,
    price_after_discount  NUMERIC(19, 2) NOT NULL,
    user_id               BIGINT NOT NULL REFERENCES orders (id),
    created_at            TIMESTAMP NOT NULL DEFAULT now()
);

-- Implicit join table for Cart.orderItems (unidirectional @OneToMany, no mappedBy/@JoinColumn).
CREATE TABLE IF NOT EXISTS carts_order_items (
    cart_id         BIGINT NOT NULL REFERENCES carts (id),
    order_items_id  BIGINT NOT NULL UNIQUE REFERENCES order_items (id)
);
