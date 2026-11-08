-- ============================================================================
-- V3: Group Tasks (Explicit Cohort Assignment Mapping)
-- ============================================================================

CREATE TABLE IF NOT EXISTS group_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    assigned_by_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    due_date TIMESTAMP,
    max_attempts INT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_group_task UNIQUE (group_id, task_id)
);

CREATE INDEX IF NOT EXISTS idx_group_tasks_group_id ON group_tasks(group_id);
CREATE INDEX IF NOT EXISTS idx_group_tasks_task_id ON group_tasks(task_id);
CREATE INDEX IF NOT EXISTS idx_group_tasks_created_at ON group_tasks(created_at DESC);

-- Backfill group_tasks from existing task_assignments where student belongs to the group
INSERT INTO group_tasks (id, group_id, task_id, assigned_by_id, due_date, max_attempts, created_at)
SELECT
    gen_random_uuid(),
    gs.group_id,
    ta.task_id,
    ta.assigned_by,
    MIN(ta.due_date),
    COALESCE(MIN(ta.max_attempts), 1),
    MIN(ta.created_at)
FROM task_assignments ta
JOIN group_students gs ON gs.student_id = ta.student_id
JOIN groups g ON g.id = gs.group_id AND g.teacher_id = ta.assigned_by
GROUP BY gs.group_id, ta.task_id, ta.assigned_by
ON CONFLICT (group_id, task_id) DO NOTHING;

-- Explicitly attach user task e922856f-fdaa-42f2-8449-5cb4d82085ff to cohort 50cc75eb-877a-4f94-b1e3-413f36b63089 if exists
INSERT INTO group_tasks (id, group_id, task_id, assigned_by_id, due_date, max_attempts, created_at)
SELECT
    gen_random_uuid(),
    '50cc75eb-877a-4f94-b1e3-413f36b63089'::uuid,
    'e922856f-fdaa-42f2-8449-5cb4d82085ff'::uuid,
    t.created_by,
    NULL,
    1,
    t.created_at
FROM tasks t
WHERE t.id = 'e922856f-fdaa-42f2-8449-5cb4d82085ff'::uuid
ON CONFLICT (group_id, task_id) DO NOTHING;
