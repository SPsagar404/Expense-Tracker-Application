-- V4: Enhance subscriptions table for auto-billing and categorization
ALTER TABLE subscriptions
    ADD COLUMN category VARCHAR(100),
    ADD COLUMN last_billed_date DATE,
    ADD COLUMN auto_generate_transaction BOOLEAN DEFAULT FALSE;

CREATE INDEX idx_subscriptions_billing ON subscriptions(active, next_billing_date);
