/**
 * @file V10__add_group_enrollment_status_and_notification_reference.sql
 * @brief Adds enrollment status to group_students and reference_id to notifications.
 */

-- Add status column to group_students for invitation acceptance/rejection flow
ALTER TABLE group_students ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACCEPTED';

-- Add reference_id to notifications to link notifications to entities (e.g. group invitations)
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_id UUID;
