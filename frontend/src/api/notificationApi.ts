/**
 * @file notificationApi.ts
 * @brief REST and Server-Sent Events client API module for notifications and real-time push events.
 */

import { axiosInstance, getAccessToken } from './axiosInstance';
import { Notification } from '../types/notification';

/**
 * @brief Client API methods for fetching notifications, unread counts, and SSE streams.
 */
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
   * @brief Queries unread notification count, normalizing raw integer or object responses.
   * @return Promise resolving to object containing unreadCount.
   */
  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const res = await axiosInstance.get<number | { unreadCount: number }>('/notifications/unread-count');
    const data = res.data;
    return typeof data === 'number' ? { unreadCount: data } : data;
  },

  /**
   * @brief Marks a notification as read by identifier.
   * @param id Notification identifier.
   */
  markAsRead: async (id: string): Promise<void> => {
    await axiosInstance.patch(`/notifications/${id}/read`);
  },

  /**
   * @brief Marks all notifications as read for authenticated user.
   */
  markAllAsRead: async (): Promise<void> => {
    await axiosInstance.patch('/notifications/read-all');
  },

  /**
   * @brief Establishes an EventSource Server-Sent Events stream for real-time notification push delivery.
   * @param onMessage Callback invoked with newly pushed notification.
   * @param onError Optional callback invoked upon connection failure or timeout.
   * @return Active EventSource instance.
   */
  connectSSE: (onMessage: (notification: Notification) => void, onError?: () => void) => {
    const rawBaseUrl = (import.meta.env.VITE_API_URL || '').trim();
    const baseURL = !rawBaseUrl
      ? '/api'
      : rawBaseUrl.endsWith('/api')
      ? rawBaseUrl
      : `${rawBaseUrl.replace(/\/+$/, '')}/api`;
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
