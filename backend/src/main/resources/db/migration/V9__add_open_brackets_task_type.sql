/**
 * @file V9__add_open_brackets_task_type.sql
 * @brief Flyway migration V9 updating tasks_type_check constraint to include OPEN_BRACKETS.
 */

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_type_check;
ALTER TABLE tasks ADD CONSTRAINT tasks_type_check 
    CHECK (type IN ('MCQ', 'GAP_FILL', 'ESSAY', 'REWRITE', 'SHORT_ANSWER', 'OPEN_BRACKETS'));
