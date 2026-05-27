CREATE TABLE food_logs
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    food_item_id BIGINT        NOT NULL REFERENCES food_items (id) ON DELETE RESTRICT,
    amount_grams INTEGER       NOT NULL,
    meal_type    VARCHAR(20)   NOT NULL,
    logged_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_food_logs_user_id ON food_logs (user_id);
CREATE INDEX idx_food_logs_logged_at ON food_logs (logged_at);
CREATE INDEX idx_food_logs_user_date ON food_logs (user_id, DATE(logged_at));