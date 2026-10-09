/**
 * @file V4__create_exercises.sql
 * @brief Flyway migration V4 creating tasks, task_questions, task_assignments, and session_states tables.
 */

/**
 * @brief Database table definition for tasks.
 */
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    cefr_level VARCHAR(10) NOT NULL,
    grammar_topic VARCHAR(255),
    domain VARCHAR(255),
    difficulty VARCHAR(50) NOT NULL,
    content TEXT,
    answer_key TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for task_questions.
 */
CREATE TABLE IF NOT EXISTS task_questions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    question_order INT NOT NULL,
    question_text TEXT NOT NULL,
    correct_answer TEXT NOT NULL,
    options_json TEXT,
    difficulty INT NOT NULL DEFAULT 2,
    grammar_rule VARCHAR(255)
);

/**
 * @brief Database table definition for task_assignments.
 */
CREATE TABLE IF NOT EXISTS task_assignments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/**
 * @brief Database table definition for session_states.
 */
CREATE TABLE IF NOT EXISTS session_states (
    id UUID PRIMARY KEY,
    assignment_id UUID NOT NULL REFERENCES task_assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_question_index INT NOT NULL DEFAULT 0,
    current_difficulty INT NOT NULL DEFAULT 2,
    answers_json TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
