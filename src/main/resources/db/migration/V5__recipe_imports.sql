-- User-submitted recipe imports: staging + moderation (ClickUp 869dtx7tj / 869dtx7uq / 869dtx7vk).

CREATE TABLE IF NOT EXISTS recipe_imports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    submitted_by UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    original_filename VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    -- pending -> (parse) -> approved | rejected. Parse + decision land in later slices.
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- Parsed/improved recipe draft (populated by the parse step) + any parse failure detail.
    draft JSONB,
    parse_error TEXT,
    -- Moderation outcome (filled by the admin approve/reject slice).
    review_note TEXT,
    reviewed_by UUID REFERENCES users (id) ON DELETE SET NULL,
    decided_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_recipe_imports_submitted_by ON recipe_imports (submitted_by);
CREATE INDEX IF NOT EXISTS ix_recipe_imports_status ON recipe_imports (status);

-- Raw uploaded bytes kept in a SEPARATE table so listing/metadata reads never load the blob.
-- ImportFileStore abstracts this; the bytea backend can be swapped for object storage (S3/MinIO) later.
CREATE TABLE IF NOT EXISTS recipe_import_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version INTEGER NOT NULL DEFAULT 1,
    import_id UUID NOT NULL UNIQUE REFERENCES recipe_imports (id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_recipe_import_files_import_id ON recipe_import_files (import_id);
