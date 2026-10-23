-- Sample coupons covering every rule findValidCoupon/Order.applyCoupon checks:
-- percentage vs fixed, minimum order amount, usage limit, expired, inactive.
-- ON CONFLICT keeps this safe on DBs where DataSeeder (or an admin) already made the code.

INSERT INTO coupons (code, discount_type, discount_value, min_order_amount,
                     usage_limit, used_count, expiry_date, active, created_at, updated_at)
VALUES
    -- valid
    ('WELCOME10',   'PERCENTAGE', 10.00, NULL,   NULL, 0, now() + INTERVAL '1 year',   TRUE,  now(), now()),
    ('SAVE15',      'PERCENTAGE', 15.00, 50.00,  NULL, 0, now() + INTERVAL '6 months', TRUE,  now(), now()),
    ('FLAT5',       'FIXED',       5.00, 20.00,  NULL, 0, now() + INTERVAL '6 months', TRUE,  now(), now()),
    ('BIG50',       'FIXED',      50.00, 300.00, 100,  0, now() + INTERVAL '3 months', TRUE,  now(), now()),
    ('VIP25',       'PERCENTAGE', 25.00, 100.00, 10,   0, now() + INTERVAL '1 year',   TRUE,  now(), now()),

    -- for testing the rejection paths
    ('LASTONE',     'PERCENTAGE', 20.00, NULL,   1,    0, now() + INTERVAL '1 year',   TRUE,  now(), now()), -- 2nd use fails
    ('USEDUP',      'PERCENTAGE', 20.00, NULL,   5,    5, now() + INTERVAL '1 year',   TRUE,  now(), now()), -- limit reached
    ('EXPIRED20',   'PERCENTAGE', 20.00, NULL,   NULL, 0, now() - INTERVAL '1 day',    TRUE,  now(), now()), -- expired
    ('DISABLED30',  'PERCENTAGE', 30.00, NULL,   NULL, 0, now() + INTERVAL '1 year',   FALSE, now(), now())  -- inactive
ON CONFLICT (code) DO NOTHING;
