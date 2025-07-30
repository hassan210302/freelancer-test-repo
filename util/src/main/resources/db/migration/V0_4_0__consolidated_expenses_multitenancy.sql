DROP TABLE IF EXISTS expense_attachments CASCADE;
DROP TABLE IF EXISTS costs CASCADE;
DROP TABLE IF EXISTS expenses CASCADE;

CREATE TABLE IF NOT EXISTS expense_categories (
    id SMALLSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    account_number VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

INSERT INTO expense_categories (name, description, account_number) VALUES
('Travel', 'Transportation, accommodation, and travel expenses', '7140'),
('Meals & Entertainment', 'Business meals and client entertainment', '7350'), 
('Office Supplies', 'Stationery, equipment, and office materials', '6560'),
('Marketing', 'Advertising, promotional materials, and marketing', '7320'),
('Professional Services', 'Legal, accounting, consulting services', '6720'),
('Utilities', 'Internet, phone, electricity, and utilities', '6200')
ON CONFLICT (name) DO UPDATE SET
    account_number = EXCLUDED.account_number,
    description = EXCLUDED.description;

CREATE TABLE expenses (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id INTEGER NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id INTEGER NOT NULL REFERENCES expense_categories(id),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    expense_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by VARCHAR(100) NOT NULL,
    amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_expense_status CHECK (status IN ('OPEN', 'DELIVERED', 'APPROVED'))
);

CREATE TABLE expense_attachments (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id INTEGER NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    expense_id INTEGER NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    attachment_id INTEGER NOT NULL REFERENCES attachments(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE costs (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id INTEGER NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    expense_id INTEGER NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    vat INTEGER NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'NOK',
    payment_type VARCHAR(20) NOT NULL,
    chargeable BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT chk_cost_payment_type CHECK (payment_type IN ('PRIVAT_UTLEGG', 'REFUSJON', 'BEDRIFTS_UTLEGG', 'KONTANT_BETALING', 'FAKTURABETALING'))
);

CREATE INDEX idx_expenses_tenant_id ON expenses(tenant_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_status ON expenses(status);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);

CREATE INDEX idx_expense_attachments_tenant_id ON expense_attachments(tenant_id);
CREATE INDEX idx_expense_attachments_expense_id ON expense_attachments(expense_id);
CREATE INDEX idx_expense_attachments_attachment_id ON expense_attachments(attachment_id);

CREATE INDEX idx_costs_tenant_id ON costs(tenant_id);
CREATE INDEX idx_costs_expense_id ON costs(expense_id);
CREATE INDEX idx_costs_date ON costs(date);

CREATE INDEX idx_expense_categories_name ON expense_categories(name);
CREATE INDEX idx_expense_categories_account_number ON expense_categories(account_number);