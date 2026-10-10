/**
 * @file V11__add_task_points_and_question_points.sql
 * @brief Flyway migration V11 adding total_points to tasks and points to task_questions.
 */

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS total_points INT DEFAULT 100;
ALTER TABLE task_questions ADD COLUMN IF NOT EXISTS points INT DEFAULT 10;
