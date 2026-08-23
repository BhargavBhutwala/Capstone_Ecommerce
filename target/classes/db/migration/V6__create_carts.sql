-- V6: carts table
-- Customer shopping carts; one per user (enforced by UNIQUE on user_id).

CREATE TABLE carts (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT uq_carts_user_id UNIQUE (user_id),
    CONSTRAINT fk_carts_user    FOREIGN KEY (user_id) REFERENCES users (id)
);
