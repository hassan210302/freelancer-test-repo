CREATE TABLE invoices
(
    id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id     INTEGER     NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    number        VARCHAR(10) NOT NULL UNIQUE,
    issue_date    DATE        NOT NULL,
    due_date      DATE,
    currency_code VARCHAR(5)  NOT NULL,
    supplier_id   INTEGER REFERENCES suppliers (id),
    customer_id   INTEGER REFERENCES customers (id),

    CONSTRAINT chk_exactly_one_party
        CHECK (
            (supplier_id IS NULL AND customer_id IS NOT NULL)
                OR (supplier_id IS NOT NULL AND customer_id IS NULL)
            )
);

CREATE INDEX idx_invoices_tenant_id ON invoices (tenant_id);

CREATE TABLE invoice_lines
(
    id         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id INTEGER        NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    item_name  VARCHAR(25)    NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    discount   NUMERIC(10, 2),
    vat_code   VARCHAR(10)    NOT NULL
);
