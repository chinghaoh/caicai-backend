CREATE TABLE goals
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    calories            INTEGER       NOT NULL,
    protein             INTEGER       NOT NULL,
    carbs               INTEGER       NOT NULL,
    fat                 INTEGER       NOT NULL,
    water_ml            INTEGER       NOT NULL,
    starting_weight_kg  DECIMAL(5, 2) NOT NULL,
    target_weight_kg    DECIMAL(5, 2) NOT NULL,
    effective_from      DATE          NOT NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_user_effective_from ON goals (user_id, effective_from);