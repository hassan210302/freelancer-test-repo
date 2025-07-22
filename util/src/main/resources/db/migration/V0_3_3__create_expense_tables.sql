-- Create expense categories table
CREATE TABLE expense_categories (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create expenses table
CREATE TABLE expenses (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    description VARCHAR(500) NOT NULL,
    expense_date DATE NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    receipt_path VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Foreign key constraints
    CONSTRAINT fk_expense_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES expense_categories(id)
);

-- Insert default expense categories
INSERT INTO expense_categories (name, description) VALUES
('Meals & Entertainment', 'Business meals and client entertainment'),
('Travel', 'Transportation, accommodation, and travel expenses'),
('Office Supplies', 'Stationery, equipment, and office materials'),
('Marketing', 'Advertising, promotional materials, and marketing'),
('Professional Services', 'Legal, accounting, consulting services'),
('Utilities', 'Internet, phone, electricity, and utilities');

-- Create indexes for performance
CREATE INDEX idx_expenses_tenant_id ON expenses(tenant_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);