import { axiosInstance, getAccessToken } from './axiosInstance';
import { Notification } from '../types/notification';

export const notificationApi = {
  getNotifications: async (): Promise<Notification[]> => {
    const res = await axiosInstance.get<Notification[]>('/notifications');
    return res.data;
  },

  getUnreadCount: async (): Promise<{ unreadCount: number }> => {
    const res = await axiosInstance.get<{ unreadCount: number }>('/notifications/unread-count');
    return res.data;
  },

  markAsRead: async (id: string): Promise<void> => {
    await axiosInstance.patch(`/notifications/${id}/read`);
  },

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
