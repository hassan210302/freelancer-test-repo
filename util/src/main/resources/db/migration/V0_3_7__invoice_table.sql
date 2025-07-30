CREATE TABLE invoices
(
    id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     INTEGER     NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    number        VARCHAR(10) NOT NULL UNIQUE,
    issue_date    DATE        NOT NULL,
    due_date      DATE,
    currency_code VARCHAR(5)  NOT NULL,
    customer_id   INTEGER REFERENCES customers (id) NOT NULL
);

CREATE INDEX idx_invoices_tenant_id ON invoices (tenant_id);

CREATE TABLE invoice_lines
(
    id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id INTEGER        NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    item_name  VARCHAR(25)    NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    discount   INTEGER,
    vat_code   VARCHAR(10)    NOT NULL
);

CREATE TABLE invoice_sequences
(
    tenant_id   INTEGER NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    year        INTEGER NOT NULL,
    next_number INTEGER NOT NULL,
    PRIMARY KEY (tenant_id, year)
);
