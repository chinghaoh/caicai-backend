CREATE TABLE weight_logs
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg  DECIMAL(5, 2)  NOT NULL,
    logged_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_weight_logs_user_id ON weight_logs (user_id);
CREATE INDEX idx_weight_logs_user_date ON weight_logs (user_id, DATE(logged_at));