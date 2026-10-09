/**
 * @file notification.ts
 * @brief Types for user notifications and event categories.
 */

/**
 * @brief Supported notification event types.
 */
export type NotificationType = 'TASK' | 'GRADE' | 'STREAK' | 'LEVEL_UP' | 'SYSTEM';

/**
 * @brief User notification record.
 */
export interface Notification {
  id: string;
  message: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
}
