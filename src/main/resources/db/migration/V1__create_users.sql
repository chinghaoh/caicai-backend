CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    age           INTEGER,
    weight_kg     DECIMAL(5, 2),
    height_cm     DECIMAL(5, 2),
    gender        VARCHAR(20),
    activity_level VARCHAR(20),
    is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_demo       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);