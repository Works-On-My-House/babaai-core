-- BabaAI core schema (PostgreSQL). Vectors live in Qdrant (AI service).

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    hashed_password VARCHAR(255) NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_users_email ON users (email);
CREATE INDEX IF NOT EXISTS ix_users_username ON users (username);

CREATE TABLE IF NOT EXISTS ingredient_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    name VARCHAR(100) NOT NULL UNIQUE,
    keywords JSONB NOT NULL DEFAULT '[]',
    is_default BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS ix_ingredient_categories_name ON ingredient_categories (name);

CREATE TABLE IF NOT EXISTS ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES ingredient_categories (id) ON DELETE RESTRICT,
    name VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL,
    expiration_date DATE,
    notes TEXT
);
CREATE INDEX IF NOT EXISTS ix_ingredients_user_id ON ingredients (user_id);
CREATE INDEX IF NOT EXISTS ix_ingredients_category_id ON ingredients (category_id);

CREATE TABLE IF NOT EXISTS recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'Other',
    preparation TEXT NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'seed',
    source_url VARCHAR(2048),
    is_verified BOOLEAN NOT NULL DEFAULT false,
    servings INTEGER,
    -- Recipe TOTAL nutrition, computed at write time from ingredients x quantity (per-serving
    -- is derived as total / servings in the DTO layer). nutrition_complete is false when one or
    -- more ingredient lines could not be matched/converted to grams (partial totals kept).
    calories DOUBLE PRECISION,
    protein_g DOUBLE PRECISION,
    carbs_g DOUBLE PRECISION,
    fat_g DOUBLE PRECISION,
    nutrition_complete BOOLEAN NOT NULL DEFAULT false,
    view_count INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS ix_recipes_is_verified ON recipes (is_verified);

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_recipe_ingredients_recipe_id ON recipe_ingredients (recipe_id);

CREATE TABLE IF NOT EXISTS suggestion_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes (id) ON DELETE SET NULL,
    recipe_name VARCHAR(255) NOT NULL,
    match_percent DOUBLE PRECISION,
    used_ingredients JSONB NOT NULL DEFAULT '[]',
    missing_ingredients JSONB NOT NULL DEFAULT '[]',
    source VARCHAR(20) NOT NULL DEFAULT 'catalog',
    agent_label VARCHAR(100),
    preparation TEXT,
    ingredients JSONB
);
CREATE INDEX IF NOT EXISTS ix_suggestion_history_user_id ON suggestion_history (user_id);
CREATE INDEX IF NOT EXISTS ix_suggestion_history_recipe_id ON suggestion_history (recipe_id);

CREATE TABLE IF NOT EXISTS recipe_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT uq_recipe_favorites_user_recipe UNIQUE (user_id, recipe_id)
);
CREATE INDEX IF NOT EXISTS ix_recipe_favorites_user_id ON recipe_favorites (user_id);
CREATE INDEX IF NOT EXISTS ix_recipe_favorites_recipe_id ON recipe_favorites (recipe_id);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'info',
    read BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS ix_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS ix_notifications_user_read ON notifications (user_id, read);
