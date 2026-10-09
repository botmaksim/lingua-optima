/**
 * @file notificationApi.ts
 * @brief REST and Server-Sent Events client API module for notifications and real-time push events.
 */

import { axiosInstance, getAccessToken } from './axiosInstance';
import { Notification } from '../types/notification';

export const notificationApi = {
  /**
   * @brief Retrieves all notifications for authenticated user.
   * @return Promise resolving to array of Notification objects.
   */
  getNotifications: async (): Promise<Notification[]> => {
    const res = await axiosInstance.get<Notification[]>('/notifications');
    return res.data;
  },

  /**
   * @brief Queries unread notification count.
   * @return Promise resolving to object containing unreadCount.
   */
  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const res = await axiosInstance.get<{ unreadCount: number }>('/notifications/unread-count');
    return res.data;
  },

  /**
   * @brief Marks a notification as read by identifier.
   * @param id Notification identifier.
   */
  markAsRead: async (id: string): Promise<void> => {
    await axiosInstance.patch(`/notifications/${id}/read`);
  },

  /**
   * @brief Establishes an EventSource Server-Sent Events stream for real-time notification push delivery.
   * @param onMessage Callback invoked with newly pushed notification.
   * @param onError Optional callback invoked upon connection failure or timeout.
   * @return Active EventSource instance.
   */
  connectSSE: (onMessage: (notification: Notification) => void, onError?: () => void) => {
    const baseURL = import.meta.env.VITE_API_URL || '/api';
    const token = getAccessToken();
    const url = token ? `${baseURL}/notifications/subscribe?token=${token}` : `${baseURL}/notifications/subscribe`;

    const eventSource = new EventSource(url, { withCredentials: true });

    eventSource.addEventListener('NOTIFICATION', (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data);
      } catch (err) {
        console.error('Error parsing SSE event:', err);
      }
    });

    eventSource.onerror = () => {
      if (onError) onError();
    };

    return eventSource;
  },
};
