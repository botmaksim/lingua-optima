-- V4: Create tasks, questions, and adaptive sessions
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    cefr_level VARCHAR(10) NOT NULL,
    grammar_topic VARCHAR(255) NOT NULL,
    domain VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    prompt TEXT NOT NULL,
    rubric JSONB,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    is_template BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_questions (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    options JSONB,
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    difficulty_level VARCHAR(50) DEFAULT 'MEDIUM',
    question_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS student_assignments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES student_groups(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    due_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cat_sessions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id UUID REFERENCES student_assignments(id) ON DELETE CASCADE,
    current_difficulty VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    questions_answered INT DEFAULT 0,
    correct_answers INT DEFAULT 0,
    current_theta DOUBLE PRECISION DEFAULT 0.0,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
