-- Low-stock threshold becomes an admin setting: the inventory status badge, the dashboard stats and
-- the low-stock notification all use it. 10 = the value that was hard-coded before.
-- IF NOT EXISTS: ddl-auto=update may already have added the column / tables.
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER NOT NULL DEFAULT 10
    CHECK (low_stock_threshold >= 0);

-- Where notifications go (Telegram groups, email addresses), managed in Settings. Credentials are
-- not stored here: the Telegram bot token and SMTP login come from the environment.
CREATE TABLE IF NOT EXISTS notification_destinations (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    channel     VARCHAR(20)   NOT NULL,
    target      VARCHAR(255)  NOT NULL,
    enabled     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,
    CONSTRAINT uk_notification_destinations_channel_target UNIQUE (channel, target)
);

-- Events each destination is subscribed to (NEW_ORDER, LOW_STOCK, NEW_PRODUCT).
CREATE TABLE IF NOT EXISTS notification_destination_events (
    destination_id  BIGINT       NOT NULL REFERENCES notification_destinations (id) ON DELETE CASCADE,
    event           VARCHAR(30)  NOT NULL,
    PRIMARY KEY (destination_id, event)
);
