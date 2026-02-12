-- V2: Seed data with demo user and sample transactions
-- Password is BCrypt hash of 'demo1234'
INSERT INTO users (email, password, name) VALUES
('demo@expensemanager.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Demo User');

-- Demo account
INSERT INTO accounts (user_id, name, type, currency) VALUES
(1, 'Checking Account', 'CHECKING', 'USD'),
(1, 'Credit Card', 'CREDIT', 'USD');

-- 30 sample transactions across various categories
INSERT INTO transactions (user_id, account_id, amount, currency, merchant, category, transaction_date, notes) VALUES
(1, 1, 45.99, 'USD', 'Whole Foods', 'Food & Groceries', '2026-01-03', 'Weekly groceries'),
(1, 2, 12.50, 'USD', 'Starbucks', 'Food & Groceries', '2026-01-05', 'Coffee and pastry'),
(1, 1, 1200.00, 'USD', 'Landlord', 'Housing', '2026-01-01', 'Monthly rent'),
(1, 2, 65.00, 'USD', 'Shell Gas Station', 'Transportation', '2026-01-04', 'Gas fill up'),
(1, 2, 150.00, 'USD', 'Amazon', 'Shopping', '2026-01-06', 'Electronics purchase'),
(1, 1, 89.99, 'USD', 'Verizon', 'Utilities', '2026-01-07', 'Phone bill'),
(1, 2, 35.00, 'USD', 'Netflix', 'Entertainment', '2026-01-08', 'Annual plan monthly'),
(1, 1, 200.00, 'USD', 'Blue Cross', 'Healthcare', '2026-01-10', 'Insurance premium'),
(1, 2, 55.00, 'USD', 'Uber', 'Transportation', '2026-01-11', 'Airport ride'),
(1, 1, 78.50, 'USD', 'Target', 'Shopping', '2026-01-12', 'Household items'),
(1, 2, 22.00, 'USD', 'Spotify', 'Entertainment', '2026-01-13', 'Music subscription'),
(1, 1, 95.00, 'USD', 'Electric Company', 'Utilities', '2026-01-15', 'Electricity bill'),
(1, 2, 42.50, 'USD', 'Chipotle', 'Food & Groceries', '2026-01-16', 'Dinner out'),
(1, 1, 30.00, 'USD', 'Planet Fitness', 'Healthcare', '2026-01-17', 'Gym membership'),
(1, 2, 120.00, 'USD', 'Best Buy', 'Shopping', '2026-01-18', 'Headphones'),
(1, 1, 67.00, 'USD', 'Trader Joes', 'Food & Groceries', '2026-01-20', 'Groceries'),
(1, 2, 15.99, 'USD', 'Hulu', 'Entertainment', '2026-01-21', 'Streaming'),
(1, 1, 250.00, 'USD', 'Auto Insurance', 'Transportation', '2026-01-22', 'Car insurance'),
(1, 2, 38.00, 'USD', 'CVS Pharmacy', 'Healthcare', '2026-01-23', 'Prescriptions'),
(1, 1, 85.00, 'USD', 'Water Company', 'Utilities', '2026-01-25', 'Water bill'),
(1, 2, 110.00, 'USD', 'Nike', 'Shopping', '2026-01-26', 'Running shoes'),
(1, 1, 55.00, 'USD', 'Costco', 'Food & Groceries', '2026-01-27', 'Bulk groceries'),
(1, 2, 25.00, 'USD', 'AMC Theatres', 'Entertainment', '2026-01-28', 'Movie night'),
(1, 1, 180.00, 'USD', 'Dentist', 'Healthcare', '2026-01-29', 'Dental checkup'),
(1, 2, 45.00, 'USD', 'Lyft', 'Transportation', '2026-01-30', 'Work commute'),
(1, 1, 32.00, 'USD', 'Panera Bread', 'Food & Groceries', '2026-02-01', 'Lunch meeting'),
(1, 2, 99.99, 'USD', 'Adobe', 'Utilities', '2026-02-02', 'Creative Cloud'),
(1, 1, 160.00, 'USD', 'Nordstrom', 'Shopping', '2026-02-03', 'Winter jacket'),
(1, 2, 28.00, 'USD', 'DoorDash', 'Food & Groceries', '2026-02-05', 'Delivery order'),
(1, 1, 75.00, 'USD', 'Internet Provider', 'Utilities', '2026-02-06', 'Internet bill');

-- Sample budgets for demo user
INSERT INTO budgets (user_id, category, month, year, limit_amount) VALUES
(1, 'Food & Groceries', 1, 2026, 500.00),
(1, 'Transportation', 1, 2026, 400.00),
(1, 'Entertainment', 1, 2026, 150.00),
(1, 'Shopping', 1, 2026, 300.00),
(1, 'Utilities', 1, 2026, 350.00),
(1, 'Healthcare', 1, 2026, 500.00),
(1, 'Housing', 1, 2026, 1500.00);

-- Sample subscriptions
INSERT INTO subscriptions (user_id, merchant, amount, interval, next_billing_date, active) VALUES
(1, 'Netflix', 35.00, 'MONTHLY', '2026-02-08', true),
(1, 'Spotify', 22.00, 'MONTHLY', '2026-02-13', true),
(1, 'Hulu', 15.99, 'MONTHLY', '2026-02-21', true),
(1, 'Adobe Creative Cloud', 99.99, 'MONTHLY', '2026-03-02', true),
(1, 'Planet Fitness', 30.00, 'MONTHLY', '2026-02-17', true);
