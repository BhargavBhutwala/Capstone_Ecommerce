-- V2: addresses table
-- Stores reusable customer delivery addresses.

CREATE TABLE addresses (
    id            BIGSERIAL     PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    label         VARCHAR(50),
    address_line1 VARCHAR(255)  NOT NULL,
    address_line2 VARCHAR(255),
    city          VARCHAR(100)  NOT NULL,
    state         VARCHAR(100)  NOT NULL,
    postal_code   VARCHAR(20)   NOT NULL,
    country       VARCHAR(100)  NOT NULL,
    is_default    BOOLEAN       NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,

    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id)
);
