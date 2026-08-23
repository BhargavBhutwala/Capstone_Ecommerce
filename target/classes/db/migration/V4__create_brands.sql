-- V4: brands table
-- Product brands / publishers.

CREATE TABLE brands (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    description TEXT,
    active      BOOLEAN       NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,

    CONSTRAINT uq_brands_name UNIQUE (name)
);
