ALTER TABLE postings
    ADD COLUMN company_id INTEGER;

ALTER TABLE postings
    ADD CONSTRAINT fk__postings__company
        FOREIGN KEY (company_id)
            REFERENCES companies (id)
            ON DELETE CASCADE;
