-- "Cooked it" consumption tracking (869dtvyct): records that a user cooked a recipe and what it
-- consumed from their pantry. Basis for cook-rate and £/kg-saved metrics.

CREATE TABLE IF NOT EXISTS cooked_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes (id) ON DELETE SET NULL,
    recipe_name VARCHAR(255) NOT NULL,
    cooked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- snapshot of consumed pantry lines: [{product_name, quantity, unit}, ...]
    consumed JSONB NOT NULL DEFAULT '[]'
);
CREATE INDEX IF NOT EXISTS ix_cooked_events_user_id ON cooked_events (user_id);
CREATE INDEX IF NOT EXISTS ix_cooked_events_recipe_id ON cooked_events (recipe_id);
