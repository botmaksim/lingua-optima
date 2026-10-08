-- V5: Create submissions, progress records, and notifications
CREATE TABLE IF NOT EXISTS student_submissions (
    id UUID PRIMARY KEY,
    assignment_id UUID REFERENCES student_assignments(id) ON DELETE SET NULL,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_type VARCHAR(50) NOT NULL,
    original_text TEXT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    override_score DOUBLE PRECISION,
    feedback TEXT,
    rubric JSONB,
    teacher_comment TEXT,
    provider_used VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_progress (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cefr_level VARCHAR(10) NOT NULL,
    grammar_topic VARCHAR(255) NOT NULL,
    mastery_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    attempts_count INT NOT NULL DEFAULT 0,
    is_grammar_gap BOOLEAN NOT NULL DEFAULT FALSE,
    last_practiced_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_progress UNIQUE (student_id, cefr_level, grammar_topic)
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
