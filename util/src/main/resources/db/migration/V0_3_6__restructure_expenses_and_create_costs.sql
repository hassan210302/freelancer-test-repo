-- Drop existing expenses table as structure has changed completely
DROP TABLE IF EXISTS expenses CASCADE;

-- Create new expenses table with updated structure
CREATE TABLE expenses (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    expense_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    receipt_path VARCHAR(255),
    created_by VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    account_number VARCHAR(10),

    
    -- Check constraints
    CONSTRAINT chk_expense_status CHECK (status IN ('OPEN', 'DELIVERED', 'APPROVED'))
);

-- Create expense_attachments table with BYTEA storage and expense relationship
CREATE TABLE expense_attachments (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expense_id INTEGER NOT NULL,
    file_data BYTEA NOT NULL,

    -- Foreign key constraints
    CONSTRAINT fk_expense_attachment_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE
);

-- Create costs table
CREATE TABLE costs (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    expense_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    vat INTEGER NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'NOK',
    payment_type VARCHAR(20) NOT NULL,
    chargeable BOOLEAN NOT NULL DEFAULT true,

    -- Foreign key constraints
    CONSTRAINT fk_cost_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT chk_cost_payment_type CHECK (payment_type IN ('PRIVAT_UTLEGG', 'REFUSJON', 'BEDRIFTS_UTLEGG', 'KONTANT_BETALING', 'FAKTURABETALING'))
);

-- Create indexes for performance
CREATE INDEX idx_expenses_tenant_id ON expenses(tenant_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_status ON expenses(status);

CREATE INDEX idx_expense_attachments_expense_id ON expense_attachments(expense_id);

CREATE INDEX idx_costs_expense_id ON costs(expense_id);
CREATE INDEX idx_costs_date ON costs(date);