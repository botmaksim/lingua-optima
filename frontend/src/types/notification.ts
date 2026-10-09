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
  /** @brief Property representing id in Notification. */
  id: string;
  /** @brief Property representing message in Notification. */
  message: string;
  /** @brief Property representing type in Notification. */
  type: NotificationType;
  /** @brief Property representing is read in Notification. */
  isRead: boolean;
  /** @brief Property representing created at in Notification. */
  createdAt: string;
}
