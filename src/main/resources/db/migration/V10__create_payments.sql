-- V10: payments table
-- Payment transaction state and reference; one payment per order (enforced by UNIQUE on order_id).

CREATE TABLE payments (
    id                 BIGSERIAL       PRIMARY KEY,
    order_id           BIGINT          NOT NULL,
    payment_reference  VARCHAR(100),
    payment_method     VARCHAR(30)     NOT NULL,
    amount             NUMERIC(12,2)   NOT NULL,
    status             VARCHAR(30)     NOT NULL,
    paid_at            TIMESTAMP,
    created_at         TIMESTAMP       NOT NULL,
    updated_at         TIMESTAMP       NOT NULL,

    CONSTRAINT uq_payments_order_id           UNIQUE (order_id),
    CONSTRAINT uq_payments_payment_reference  UNIQUE (payment_reference),
    CONSTRAINT fk_payments_order              FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_payments_amount            CHECK (amount >= 0)
);
