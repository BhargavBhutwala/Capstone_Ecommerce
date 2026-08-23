-- V1: users table
-- Stores registered user accounts.

CREATE TABLE users (
    id             BIGSERIAL     PRIMARY KEY,
    first_name     VARCHAR(100)  NOT NULL,
    last_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    role           VARCHAR(50)   NOT NULL,
    status         VARCHAR(30)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,

    CONSTRAINT uq_users_email UNIQUE (email)
);
