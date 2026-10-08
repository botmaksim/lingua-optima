export type NotificationType = 'TASK' | 'GRADE' | 'STREAK' | 'LEVEL_UP' | 'SYSTEM';

export interface Notification {
  id: string;
  message: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
}
