/**
 * @file V12__add_token_tracking_to_usage_counters.sql
 * @brief Flyway migration adding weekly token usage column to usage_counters table.
 */

ALTER TABLE usage_counters
ADD COLUMN IF NOT EXISTS week_tokens_used BIGINT NOT NULL DEFAULT 0;
