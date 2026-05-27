CREATE TABLE water_logs
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount_ml  INTEGER   NOT NULL,
    logged_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_water_logs_user_id ON water_logs (user_id);
CREATE INDEX idx_water_logs_user_date ON water_logs (user_id, DATE(logged_at));