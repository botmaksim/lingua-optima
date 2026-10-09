/**
 * @file V1__create_users.sql
 * @brief Flyway migration V1 creating users, subscriptions, usage_counters, groups, and group_students tables.
 */

/**
 * @brief Database table definition for users.
 */
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    cefr_level VARCHAR(10) NOT NULL DEFAULT 'B1',
    display_alias VARCHAR(100),
    streak_count INT NOT NULL DEFAULT 0,
    last_active_date DATE,
    freeze_tokens INT NOT NULL DEFAULT 0,
    level_up_suggested_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for subscriptions.
 */
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for usage_counters.
 */
CREATE TABLE IF NOT EXISTS usage_counters (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    week_evaluations INT NOT NULL DEFAULT 0,
    week_ocr_uploads INT NOT NULL DEFAULT 0,
    week_reset_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for groups.
 */
CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for group_students.
 */
CREATE TABLE IF NOT EXISTS group_students (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMP,
    CONSTRAINT uq_group_student UNIQUE (group_id, student_id)
);
