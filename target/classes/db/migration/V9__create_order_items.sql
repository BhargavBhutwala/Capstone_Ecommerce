-- V9: order_items table
-- Products purchased in an order with purchase-time title and price snapshots.

CREATE TABLE order_items (
    id             BIGSERIAL       PRIMARY KEY,
    order_id       BIGINT          NOT NULL,
    product_id     BIGINT          NOT NULL,
    product_title  VARCHAR(255)    NOT NULL,
    quantity       INTEGER         NOT NULL,
    unit_price     NUMERIC(12,2)   NOT NULL,
    subtotal       NUMERIC(12,2)   NOT NULL,
    created_at     TIMESTAMP       NOT NULL,

    CONSTRAINT fk_order_items_order      FOREIGN KEY (order_id)   REFERENCES orders (id),
    CONSTRAINT fk_order_items_product    FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_order_items_quantity  CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price     CHECK (unit_price >= 0),
    CONSTRAINT chk_order_items_subtotal  CHECK (subtotal >= 0)
);
