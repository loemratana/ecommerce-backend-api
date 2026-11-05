-- Whether customers get an order-confirmation email when they place an order; admins switch it in
-- Settings -> Notifications. On by default (it is only sent while SMTP is configured).
-- IF NOT EXISTS: ddl-auto=update may already have added the column.
ALTER TABLE store_settings ADD COLUMN IF NOT EXISTS customer_order_email BOOLEAN NOT NULL DEFAULT TRUE;
