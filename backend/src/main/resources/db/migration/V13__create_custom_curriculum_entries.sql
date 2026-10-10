CREATE TABLE IF NOT EXISTS custom_curriculum_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    cefr_level VARCHAR(10),
    curriculum_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_custom_curriculum_user_type ON custom_curriculum_entries(user_id, curriculum_type);
