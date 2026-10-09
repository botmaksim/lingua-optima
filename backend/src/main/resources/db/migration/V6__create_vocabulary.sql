/**
 * @file V6__create_vocabulary.sql
 * @brief Flyway migration V6 creating audit_logs and pending_ai_tasks tables.
 */

/**
 * @brief Database table definition for audit_logs.
 */
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for pending_ai_tasks.
 */
CREATE TABLE IF NOT EXISTS pending_ai_tasks (
    id UUID PRIMARY KEY,
    task_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
