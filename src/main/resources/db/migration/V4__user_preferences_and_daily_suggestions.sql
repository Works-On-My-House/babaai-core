-- Taste profile + daily personalized, pantry/expiry-aware suggestions (869dr0a4d).

CREATE TABLE IF NOT EXISTS user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    preferred_ingredients JSONB NOT NULL DEFAULT '[]',
    disliked_ingredients JSONB NOT NULL DEFAULT '[]',
    preferred_categories JSONB NOT NULL DEFAULT '[]',
    dietary_tags JSONB NOT NULL DEFAULT '[]',
    allergens JSONB NOT NULL DEFAULT '[]'
);
CREATE INDEX IF NOT EXISTS ix_user_preferences_user_id ON user_preferences (user_id);

-- Per-user "today's picks": computed once per user/day (scheduled or lazily) and persisted, since a
-- personalized set is not globally cacheable. The unique constraint makes regeneration idempotent.
CREATE TABLE IF NOT EXISTS daily_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    generated_on DATE NOT NULL,
    recipe_id UUID NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    recipe_name VARCHAR(255) NOT NULL,
    rank INTEGER NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    match_percent DOUBLE PRECISION NOT NULL DEFAULT 0,
    can_prepare BOOLEAN NOT NULL DEFAULT false,
    reasons JSONB NOT NULL DEFAULT '[]',
    missing_ingredients JSONB NOT NULL DEFAULT '[]',
    CONSTRAINT uq_daily_suggestions_user_day_recipe UNIQUE (user_id, generated_on, recipe_id)
);
CREATE INDEX IF NOT EXISTS ix_daily_suggestions_user_day ON daily_suggestions (user_id, generated_on);
