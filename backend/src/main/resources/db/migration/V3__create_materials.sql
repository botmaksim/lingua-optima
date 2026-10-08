-- V3: Create curriculum topics and materials
CREATE TABLE IF NOT EXISTS curriculum_topics (
    id UUID PRIMARY KEY,
    cefr_level VARCHAR(10) NOT NULL,
    grammar_topic VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    description TEXT,
    difficulty_order INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
