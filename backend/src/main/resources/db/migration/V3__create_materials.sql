/**
 * @file V3__create_materials.sql
 * @brief Flyway migration V3 creating curriculum topics reference table.
 */

/**
 * @brief Database table definition for curriculum_topics.
 */
CREATE TABLE IF NOT EXISTS curriculum_topics (
    id UUID PRIMARY KEY,
    cefr_level VARCHAR(10) NOT NULL,
    grammar_topic VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    description TEXT,
    difficulty_order INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
