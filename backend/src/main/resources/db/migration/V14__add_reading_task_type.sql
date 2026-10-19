/**
 * @file V14__add_reading_task_type.sql
 * @brief Flyway migration V14 updating tasks_type_check constraint to include READING.
 */

ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_type_check;
ALTER TABLE tasks ADD CONSTRAINT tasks_type_check 
    CHECK (type IN ('MCQ', 'GAP_FILL', 'ESSAY', 'REWRITE', 'SHORT_ANSWER', 'OPEN_BRACKETS', 'READING'));
