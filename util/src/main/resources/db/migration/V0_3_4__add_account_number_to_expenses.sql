-- Add account_number column to expenses table
ALTER TABLE expenses
ADD COLUMN account_number VARCHAR(10);

-- Create index for performance
CREATE INDEX idx_expenses_account_number ON expenses(account_number);

-- Update existing expenses with default account numbers based on category
-- Travel expenses -> 7140 (Travel Costs – Non-taxable)
UPDATE expenses
SET account_number = '7140'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Travel');

-- Meals & Entertainment -> 7350 (Representation – Deductible)
UPDATE expenses
SET account_number = '7350'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Meals & Entertainment');

-- Office Supplies -> 6560 (Office Supplies)
UPDATE expenses
SET account_number = '6560'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Office Supplies');

-- Marketing -> 7320 (Advertising Costs)
UPDATE expenses
SET account_number = '7320'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Marketing');

-- Professional Services -> 6720 (Economic and Legal Assistance)
UPDATE expenses
SET account_number = '6720'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Professional Services');

-- Utilities -> 6200 (Electricity) - default utility account
UPDATE expenses
SET account_number = '6200'
WHERE category_id = (SELECT id FROM expense_categories WHERE name = 'Utilities');

-- Default fallback for any remaining expenses
UPDATE expenses
SET account_number = '7790'
WHERE account_number IS NULL;