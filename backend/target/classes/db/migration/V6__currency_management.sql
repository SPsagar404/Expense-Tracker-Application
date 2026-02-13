-- V6: Global currency management

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS preferred_currency VARCHAR(10) NOT NULL DEFAULT 'INR';

CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    base_currency VARCHAR(10) NOT NULL,
    target_currency VARCHAR(10) NOT NULL,
    rate DECIMAL(18,6) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exchange_rate UNIQUE (base_currency, target_currency)
);

CREATE INDEX IF NOT EXISTS idx_exchange_rates_base_target
    ON exchange_rates(base_currency, target_currency);

