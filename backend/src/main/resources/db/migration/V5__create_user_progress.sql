/**
 * @file V5__create_user_progress.sql
 * @brief Flyway migration V5 creating submissions, progress_records, and notifications tables.
 */

/**
 * @brief Database table definition for submissions.
 */
CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID REFERENCES task_assignments(id) ON DELETE SET NULL,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_type VARCHAR(50) NOT NULL,
    student_text TEXT,
    ai_score DOUBLE PRECISION,
    ai_feedback TEXT,
    override_score DOUBLE PRECISION,
    teacher_comment TEXT,
    provider_used VARCHAR(255),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for progress_records.
 */
CREATE TABLE IF NOT EXISTS progress_records (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    grammar_topic VARCHAR(255) NOT NULL,
    total_attempts INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for notifications.
 */
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
