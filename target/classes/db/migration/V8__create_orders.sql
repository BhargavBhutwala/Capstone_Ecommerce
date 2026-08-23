-- V8: orders table
-- Customer purchase transactions with shipping-address snapshot.

CREATE TABLE orders (
    id                    BIGSERIAL       PRIMARY KEY,
    order_number          VARCHAR(50)     NOT NULL,
    user_id               BIGINT          NOT NULL,
    shipping_name         VARCHAR(200)    NOT NULL,
    shipping_line1        VARCHAR(255)    NOT NULL,
    shipping_line2        VARCHAR(255),
    shipping_city         VARCHAR(100)    NOT NULL,
    shipping_state        VARCHAR(100)    NOT NULL,
    shipping_postal_code  VARCHAR(20)     NOT NULL,
    shipping_country      VARCHAR(100)    NOT NULL,
    status                VARCHAR(40)     NOT NULL,
    subtotal              NUMERIC(12,2)   NOT NULL,
    shipping_amount       NUMERIC(12,2)   NOT NULL,
    discount_amount       NUMERIC(12,2)   NOT NULL DEFAULT 0,
    gift_points_used      INTEGER         NOT NULL DEFAULT 0,
    total_amount          NUMERIC(12,2)   NOT NULL,
    placed_at             TIMESTAMP,
    cancellation_deadline TIMESTAMP,
    created_at            TIMESTAMP       NOT NULL,
    updated_at            TIMESTAMP       NOT NULL,

    CONSTRAINT uq_orders_order_number      UNIQUE (order_number),
    CONSTRAINT fk_orders_user              FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_orders_subtotal         CHECK (subtotal >= 0),
    CONSTRAINT chk_orders_shipping_amount  CHECK (shipping_amount >= 0),
    CONSTRAINT chk_orders_discount_amount  CHECK (discount_amount >= 0),
    CONSTRAINT chk_orders_gift_points_used CHECK (gift_points_used >= 0),
    CONSTRAINT chk_orders_total_amount     CHECK (total_amount >= 0)
);
