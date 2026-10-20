/**
 * @file notification.ts
 * @brief Types for user notifications and event categories.
 */

/**
 * @brief Supported notification event types.
 */
export type NotificationType = 'TASK' | 'GRADE' | 'STREAK' | 'LEVEL_UP' | 'SYSTEM' | 'GROUP_INVITATION';

/**
 * @brief User notification record.
 */
export interface Notification {
  id: string;
  message: string;
  type: NotificationType;
  /** @brief Optional referenced entity identifier (e.g. group or task UUID). */
  referenceId?: string;
  isRead: boolean;
  createdAt: string;
}
