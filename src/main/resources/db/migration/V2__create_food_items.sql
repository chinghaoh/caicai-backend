CREATE TABLE food_items
(
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255)   NOT NULL,
    brand             VARCHAR(255),
    calories_per_100g DECIMAL(7, 2)  NOT NULL,
    protein_per_100g  DECIMAL(7, 2)  NOT NULL,
    carbs_per_100g    DECIMAL(7, 2)  NOT NULL,
    fat_per_100g      DECIMAL(7, 2)  NOT NULL,
    fiber_per_100g    DECIMAL(7, 2),
    sugar_per_100g    DECIMAL(7, 2),
    sodium_per_100g   DECIMAL(7, 2),
    source            VARCHAR(20)    NOT NULL,
    external_id       VARCHAR(255),
    created_by        BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_food_items_name ON food_items (name);
CREATE INDEX idx_food_items_external_id ON food_items (external_id);
CREATE INDEX idx_food_items_created_by ON food_items (created_by);