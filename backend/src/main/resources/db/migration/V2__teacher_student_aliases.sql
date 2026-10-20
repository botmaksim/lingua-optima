-- ============================================================================
-- V2: Teacher-Student Aliases (educator custom display names)
-- ============================================================================

CREATE TABLE IF NOT EXISTS teacher_student_aliases (
    id UUID PRIMARY KEY,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    custom_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_teacher_student_alias UNIQUE (teacher_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_student_aliases_teacher ON teacher_student_aliases(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_student_aliases_student ON teacher_student_aliases(student_id);
