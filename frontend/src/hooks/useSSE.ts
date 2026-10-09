/**
 * @file useSSE.ts
 * @brief Custom React hook managing real-time Server-Sent Events (SSE) notification connection and automatic reconnection.
 */

import { useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { useNotificationStore } from '../store/notificationStore';
import { notificationApi } from '../api/notificationApi';

/**
 * @brief React hook subscribing to real-time notification push events when authenticated.
 */
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
