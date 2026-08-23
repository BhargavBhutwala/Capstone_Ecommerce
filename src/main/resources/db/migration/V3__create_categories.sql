-- V3: categories table
-- Product catalogue categories.

CREATE TABLE categories (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    active      BOOLEAN       NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,

    CONSTRAINT uq_categories_name UNIQUE (name)
);
