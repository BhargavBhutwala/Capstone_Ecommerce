-- V7: cart_items table
-- Individual products and quantities inside a cart.

CREATE TABLE cart_items (
    id          BIGSERIAL       PRIMARY KEY,
    cart_id     BIGINT          NOT NULL,
    product_id  BIGINT          NOT NULL,
    quantity    INTEGER         NOT NULL,
    unit_price  NUMERIC(12,2)   NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL,

    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart         FOREIGN KEY (cart_id)    REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product      FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_cart_items_quantity    CHECK (quantity > 0),
    CONSTRAINT chk_cart_items_unit_price  CHECK (unit_price >= 0)
);
