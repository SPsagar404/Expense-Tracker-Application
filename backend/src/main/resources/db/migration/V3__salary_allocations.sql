-- V3: Salary Allocations table for monthly financial planning
CREATE TABLE salary_allocations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    year INTEGER NOT NULL CHECK (year >= 2020),
    total_salary DECIMAL(12,2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    allocation_type VARCHAR(20) NOT NULL CHECK (allocation_type IN ('PERCENTAGE', 'FIXED')),
    allocation_value DECIMAL(12,2) NOT NULL,
    allocated_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, month, year, category)
);

CREATE INDEX idx_salary_allocations_user ON salary_allocations(user_id);
CREATE INDEX idx_salary_allocations_period ON salary_allocations(user_id, year, month);
