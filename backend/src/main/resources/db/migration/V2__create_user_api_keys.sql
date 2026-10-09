/**
 * @file V2__create_user_api_keys.sql
 * @brief Flyway migration V2 creating the api_keys table for AES-256-GCM encrypted BYOK credentials.
 */

/**
 * @brief Database table definition for api_keys.
 */
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    encrypted_key VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
