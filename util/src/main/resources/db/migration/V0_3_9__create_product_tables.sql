CREATE TABLE IF NOT EXISTS products (
                                        id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        tenant_id     INTEGER      NOT NULL,
                                        has_variants  BOOLEAN      NOT NULL DEFAULT false,
                                        name          VARCHAR(255) NOT NULL,
                                        description   VARCHAR(1000),
                                        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                        updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                        CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE IF NOT EXISTS product_options (
                                               id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                               product_id  INTEGER      NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                               position    INTEGER      NOT NULL DEFAULT 0,
                                               name        VARCHAR(100) NOT NULL,
                                               CONSTRAINT uk_product_options_product_name UNIQUE (product_id, name)
);

CREATE TABLE IF NOT EXISTS product_option_values (
                                                     id                 INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                     product_option_id  INTEGER      NOT NULL REFERENCES product_options(id) ON DELETE CASCADE,
                                                     position           INTEGER      NOT NULL DEFAULT 0,
                                                     value              VARCHAR(100) NOT NULL,
                                                     CONSTRAINT uk_product_option_values_option_value UNIQUE (product_option_id, value)
);

CREATE TABLE IF NOT EXISTS variants (
                                        id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        product_id   INTEGER      NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                        tenant_id    INTEGER      NOT NULL REFERENCES tenants(id),
                                        price_cents  INTEGER      NOT NULL,
                                        stock_qty    INTEGER      NOT NULL DEFAULT 0,
                                        is_default   BOOLEAN GENERATED ALWAYS AS (option_map = '{}'::jsonb) STORED,
                                        sku          VARCHAR(100) NOT NULL,
                                        option_map   JSONB        NOT NULL DEFAULT '{}'::jsonb,
                                        created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                        updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                        CONSTRAINT uk_variants_sku_tenant UNIQUE (tenant_id, sku)
);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_default_variant_per_product
    ON variants(product_id)
    WHERE (option_map = '{}'::jsonb);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_variant_option_combo
    ON variants(product_id, option_map);

CREATE INDEX IF NOT EXISTS idx_variant_option_map_gin
    ON variants USING gin (option_map jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products (tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_options_product_id ON product_options (product_id);
CREATE INDEX IF NOT EXISTS idx_product_option_values_option_id ON product_option_values (product_option_id);
CREATE INDEX IF NOT EXISTS idx_variants_product_id ON variants (product_id);
CREATE INDEX IF NOT EXISTS idx_variants_tenant_id ON variants (tenant_id);