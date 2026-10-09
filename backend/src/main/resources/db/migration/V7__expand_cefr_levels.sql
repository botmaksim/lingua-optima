/**
 * @file V7__expand_cefr_levels.sql
 * @brief Flyway migration V7 expanding CEFR level constraints to include full scale (A1, A2, B1, B2, C1, C2).
 */

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_cefr_level_check;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_cefr_level_check;

ALTER TABLE users ADD CONSTRAINT users_cefr_level_check CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'));
ALTER TABLE tasks ADD CONSTRAINT tasks_cefr_level_check CHECK (cefr_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'));
