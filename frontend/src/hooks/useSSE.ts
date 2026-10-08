import { useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { useNotificationStore } from '../store/notificationStore';
import { notificationApi } from '../api/notificationApi';

export const useSSE = () => {
  const { isAuthenticated } = useAuthStore();
  const { addNotification } = useNotificationStore();

  useEffect(() => {
    if (!isAuthenticated) return;

    let eventSource: EventSource | null = null;
    let reconnectTimeout: any = null;

    const connect = () => {
      eventSource = notificationApi.connectSSE(
        (notification) => {
          addNotification(notification);
        },
        () => {
          // Reconnect on disconnect after 5 seconds
          if (eventSource) {
            eventSource.close();
          }
          reconnectTimeout = setTimeout(connect, 5000);
        }
      );
    };

    connect();

    return () => {
      if (eventSource) eventSource.close();
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
    };
  }, [isAuthenticated, addNotification]);
};
