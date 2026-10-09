/**
 * @file V8__add_assignment_attempts.sql
 * @brief Flyway migration V8 adding configurable attempt limits (max_attempts and attempts_used) to task_assignments.
 */

ALTER TABLE task_assignments
    ADD COLUMN IF NOT EXISTS max_attempts INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS attempts_used INT NOT NULL DEFAULT 0;
