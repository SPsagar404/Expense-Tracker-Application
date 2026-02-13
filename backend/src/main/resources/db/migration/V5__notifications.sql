-- V5: Notification system tables

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_id BIGINT,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    scheduled_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, read);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_at ON notifications(scheduled_at);

CREATE TABLE IF NOT EXISTS user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    budget_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    subscription_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    goal_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    large_expense_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_user_notification_prefs_user ON user_notification_preferences(user_id);

CREATE TABLE IF NOT EXISTS user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_device_tokens_user ON user_device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_user_device_tokens_token ON user_device_tokens(device_token);

-- Seed demo notification preferences and example notifications for demo user (id = 1)

INSERT INTO user_notification_preferences (
    user_id,
    email_enabled,
    push_enabled,
    in_app_enabled,
    budget_alert_enabled,
    subscription_alert_enabled,
    goal_alert_enabled,
    large_expense_alert_enabled
) VALUES (
    1,  -- demo user from V2 seed
    TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, TRUE
)
ON CONFLICT (user_id) DO NOTHING;

-- Example in-app notifications for the demo user
INSERT INTO notifications (user_id, type, title, message, reference_id, read, sent, scheduled_at) VALUES
-- Budget alert demo
(1, 'BUDGET_ALERT',
 'Budget limit exceeded',
 'You have spent 520.00 in ''Food & Groceries'' for 01/2026, exceeding your budget limit of 500.00.',
 NULL,
 FALSE,
 FALSE,
 NULL),
-- Upcoming subscription reminder demo
(1, 'SUBSCRIPTION_REMINDER',
 'Upcoming subscription billing',
 'Your subscription ''Netflix'' will be billed in 2 days.',
 NULL,
 FALSE,
 FALSE,
 NULL),
-- Large expense alert demo
(1, 'LARGE_EXPENSE_ALERT',
 'Large expense detected',
 'A large transaction of 1200.00 was recorded for ''Housing'' (Landlord).',
 NULL,
 TRUE,
 TRUE,
 NULL),
-- Custom reminder demo, scheduled in the near future
(1, 'CUSTOM_REMINDER',
 'Pay Electricity Bill',
 'Electricity bill is due soon. Don''t forget to pay it on time.',
 NULL,
 FALSE,
 FALSE,
 CURRENT_TIMESTAMP + INTERVAL '1 day');

