-- Settings that admins edit from the dashboard. Always exactly one row (id = 1); the app creates
-- it on first use from the app.* values in application.yaml, which are now only starting values.
--
-- IF NOT EXISTS: ddl-auto=update may already have created the table.
CREATE TABLE IF NOT EXISTS store_settings (
    id                        BIGINT        PRIMARY KEY CHECK (id = 1),
    version                   BIGINT        NOT NULL DEFAULT 0,

    store_name                VARCHAR(150)  NOT NULL,
    store_email               VARCHAR(150),
    store_phone               VARCHAR(30),
    store_address             VARCHAR(255),
    store_description         VARCHAR(1000),

    tax_rate                  NUMERIC(5, 4) NOT NULL CHECK (tax_rate >= 0 AND tax_rate <= 1),
    shipping_fee              NUMERIC(19, 2) NOT NULL CHECK (shipping_fee >= 0),
    pending_timeout_minutes   INTEGER       NOT NULL CHECK (pending_timeout_minutes > 0),
    return_window_days        INTEGER       NOT NULL CHECK (return_window_days >= 0),

    currency                  VARCHAR(3)    NOT NULL,
    timezone                  VARCHAR(64)   NOT NULL,

    access_token_ttl_minutes  INTEGER       NOT NULL CHECK (access_token_ttl_minutes > 0),

    updated_at                TIMESTAMP     NOT NULL,
    updated_by                VARCHAR(150)
);
