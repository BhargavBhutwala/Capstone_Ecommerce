-- V5: products table
-- Books/products available in the catalogue.

CREATE TABLE products (
    id                  BIGSERIAL       PRIMARY KEY,
    title               VARCHAR(255)    NOT NULL,
    isbn                VARCHAR(20),
    description         TEXT,
    price               NUMERIC(12,2)   NOT NULL,
    stock_quantity      INTEGER         NOT NULL,
    category_id         BIGINT          NOT NULL,
    brand_id            BIGINT          NOT NULL,
    delivery_days_min   INTEGER,
    delivery_days_max   INTEGER,
    active              BOOLEAN         NOT NULL,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,

    CONSTRAINT uq_products_isbn         UNIQUE (isbn),
    CONSTRAINT fk_products_category     FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_brand        FOREIGN KEY (brand_id)    REFERENCES brands (id),
    CONSTRAINT chk_products_price       CHECK (price >= 0),
    CONSTRAINT chk_products_stock       CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_del_min     CHECK (delivery_days_min >= 0),
    CONSTRAINT chk_products_del_range   CHECK (delivery_days_max >= delivery_days_min)
);
