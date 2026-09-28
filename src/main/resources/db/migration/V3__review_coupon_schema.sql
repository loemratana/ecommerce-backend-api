CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255),
    ratings     NUMERIC(2, 1) NOT NULL,
    comment     TEXT,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    product_id  BIGINT NOT NULL REFERENCES products (id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_review_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_review_user ON reviews (user_id);
CREATE INDEX IF NOT EXISTS idx_review_product ON reviews (product_id);

CREATE TABLE IF NOT EXISTS coupons (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(50) NOT NULL UNIQUE,
    discount_percent   NUMERIC(5, 2) NOT NULL,
    expiry_date        TIMESTAMP NOT NULL,
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupons (code);

CREATE TABLE IF NOT EXISTS order_coupons (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL REFERENCES orders (id),
    coupon_id          BIGINT NOT NULL REFERENCES coupons (id),
    discount_percent   NUMERIC(5, 2) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_order_coupon_order_coupon UNIQUE (order_id, coupon_id)
);

CREATE INDEX IF NOT EXISTS idx_order_coupon_order ON order_coupons (order_id);
CREATE INDEX IF NOT EXISTS idx_order_coupon_coupon ON order_coupons (coupon_id);
