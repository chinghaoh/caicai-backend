CREATE TABLE user_favourite_foods
(
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    food_item_id BIGINT NOT NULL REFERENCES food_items (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, food_item_id)
);

CREATE INDEX idx_user_favourite_foods_user_id ON user_favourite_foods (user_id);