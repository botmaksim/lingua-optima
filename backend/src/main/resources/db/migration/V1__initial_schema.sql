/**
 * @file V1__initial_schema.sql
 * @brief Comprehensive baseline relational database schema for Lingua Optima.
 *
 * Defines all core domain tables, constraints, foreign keys, and indexes:
 * - Users, authentication, roles, CEFR continuum (A1-C2)
 * - BYOK API Keys (AES-256-GCM encrypted)
 * - Subscriptions & weekly usage counters (tokens, evaluations, OCR)
 * - Notifications & group cohort orchestration
 * - Tasks (MCQ, GAP_FILL, ESSAY, REWRITE, SHORT_ANSWER, OPEN_BRACKETS, READING, SPEAKING)
 * - Questions, point budgets, assignments, and adaptive CAT session states
 * - Student submissions, diagnostic evaluations, progress mastery tracking
 * - Teacher custom curriculum entries and asynchronous pending AI tasks
 *
 * Copyright (c) 2026 Maksim Manastyrski. All rights reserved.
 */

-- ============================================================================
-- 1. Identity, Access, and Authentication
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    cefr_level VARCHAR(10) NOT NULL DEFAULT 'B1' CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    display_alias VARCHAR(100),
    streak_count INT NOT NULL DEFAULT 0,
    last_active_date DATE,
    freeze_tokens INT NOT NULL DEFAULT 2,
    level_up_suggested_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100),
    encrypted_key VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    price_usd DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);

CREATE TABLE IF NOT EXISTS usage_counters (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_start_date DATE NOT NULL,
    evaluations_count INT NOT NULL DEFAULT 0,
    ocr_count INT NOT NULL DEFAULT 0,
    week_tokens_used BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_week UNIQUE (user_id, week_start_date)
);

CREATE INDEX IF NOT EXISTS idx_usage_user_week ON usage_counters(user_id, week_start_date);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- ============================================================================
-- 2. Educator Cohorts & Curriculum Management
-- ============================================================================

CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_groups_teacher_id ON groups(teacher_id);

CREATE TABLE IF NOT EXISTS group_students (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACCEPTED',
    removed_at TIMESTAMP,
    PRIMARY KEY (group_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_group_students_student_active ON group_students(student_id, is_active);

CREATE TABLE IF NOT EXISTS curriculum_topics (
    id UUID PRIMARY KEY,
    cefr_level VARCHAR(10) NOT NULL CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    grammar_topic VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    description TEXT,
    difficulty_order INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_curriculum_level_topic ON curriculum_topics(cefr_level, grammar_topic);

CREATE TABLE IF NOT EXISTS custom_curriculum_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    cefr_level VARCHAR(10) CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    curriculum_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_custom_curriculum_user_type ON custom_curriculum_entries(user_id, curriculum_type);

-- ============================================================================
-- 3. Tasks, Adaptive Sessions, and Assignments
-- ============================================================================

CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL CONSTRAINT tasks_type_check CHECK (type IN ('MCQ', 'GAP_FILL', 'ESSAY', 'REWRITE', 'SHORT_ANSWER', 'OPEN_BRACKETS', 'READING', 'SPEAKING')),
    cefr_level VARCHAR(10) NOT NULL CONSTRAINT tasks_cefr_level_check CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    grammar_topic VARCHAR(255),
    domain VARCHAR(255),
    difficulty VARCHAR(50) NOT NULL,
    content TEXT,
    answer_key TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_template BOOLEAN NOT NULL DEFAULT FALSE,
    total_points INT DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tasks_type ON tasks(type);
CREATE INDEX IF NOT EXISTS idx_tasks_level ON tasks(cefr_level);
CREATE INDEX IF NOT EXISTS idx_tasks_created_by ON tasks(created_by);

CREATE TABLE IF NOT EXISTS task_questions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    question_order INT NOT NULL,
    question_text TEXT NOT NULL,
    correct_answer TEXT NOT NULL,
    options_json TEXT,
    difficulty INT NOT NULL DEFAULT 2,
    grammar_rule VARCHAR(255),
    points INT DEFAULT 10
);

CREATE INDEX IF NOT EXISTS idx_task_questions_task_order ON task_questions(task_id, question_order);

CREATE TABLE IF NOT EXISTS task_assignments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SUBMITTED', 'GRADED')),
    max_attempts INT DEFAULT 1,
    attempts_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_assignments_student_status ON task_assignments(student_id, status);
CREATE INDEX IF NOT EXISTS idx_task_assignments_task ON task_assignments(task_id);

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

CREATE INDEX IF NOT EXISTS idx_session_states_assignment ON session_states(assignment_id);
CREATE INDEX IF NOT EXISTS idx_session_states_student ON session_states(student_id);

-- ============================================================================
-- 4. Submissions, Diagnostics, and Mastery Progression
-- ============================================================================

CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID REFERENCES task_assignments(id) ON DELETE SET NULL,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_type VARCHAR(50) NOT NULL CHECK (submission_type IN ('TEXT', 'IMAGE')),
    student_text TEXT NOT NULL,
    ai_score DOUBLE PRECISION,
    ai_feedback TEXT,
    override_score DOUBLE PRECISION,
    teacher_comment TEXT,
    provider_used VARCHAR(255),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_submissions_student ON submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON submissions(assignment_id);

CREATE TABLE IF NOT EXISTS progress_records (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cefr_level VARCHAR(10) NOT NULL CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    grammar_topic VARCHAR(255) NOT NULL,
    mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    attempts_count INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    last_practiced_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_level_topic UNIQUE (student_id, cefr_level, grammar_topic)
);

CREATE INDEX IF NOT EXISTS idx_progress_student_topic ON progress_records(student_id, cefr_level, grammar_topic);

CREATE TABLE IF NOT EXISTS pending_ai_tasks (
    id UUID PRIMARY KEY,
    task_type VARCHAR(100) NOT NULL,
    payload_json TEXT NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pending_ai_tasks_status ON pending_ai_tasks(status);
