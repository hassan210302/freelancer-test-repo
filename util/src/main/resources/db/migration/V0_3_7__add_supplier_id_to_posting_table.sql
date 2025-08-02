ALTER TABLE postings
    ADD COLUMN supplier_id INTEGER;

ALTER TABLE postings
    ADD FOREIGN KEY (supplier_id)
        REFERENCES suppliers(id)
        ON DELETE CASCADE;
