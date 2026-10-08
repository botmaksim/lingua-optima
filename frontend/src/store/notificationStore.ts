import { create } from 'zustand';
import { Notification } from '../types/notification';
import { notificationApi } from '../api/notificationApi';
import { ToastType } from '../components/common/Toast';

export interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
  title?: string;
}

interface NotificationState {
  notifications: Notification[];
  unreadCount: number;
  isLoading: boolean;
  toasts: ToastItem[];
  fetchNotifications: () => Promise<void>;
  addNotification: (notification: Notification) => void;
  markAsRead: (id: string) => Promise<void>;
  addToast: (toast: Omit<ToastItem, 'id'>) => void;
  removeToast: (id: string) => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  unreadCount: 0,
  isLoading: false,
  toasts: [],

  fetchNotifications: async () => {
    set({ isLoading: true });
    try {
      const [list, countRes] = await Promise.all([
        notificationApi.getNotifications(),
        notificationApi.getUnreadCount(),
      ]);
      set({ notifications: list, unreadCount: countRes.unreadCount, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  addNotification: (notification: Notification) => {
    set((state) => ({
      notifications: [notification, ...state.notifications],
      unreadCount: state.unreadCount + 1,
    }));
  },

  markAsRead: async (id: string) => {
    try {
      await notificationApi.markAsRead(id);
      set((state) => ({
        notifications: state.notifications.map((n) =>
          n.id === id ? { ...n, isRead: true } : n
        ),
        unreadCount: Math.max(0, state.unreadCount - 1),
      }));
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
    }
  },

  addToast: (toast) => {
    const id = Math.random().toString(36).substring(2, 9);
    set((state) => ({
      toasts: [...state.toasts, { ...toast, id }],
    }));
    setTimeout(() => {
      set((state) => ({
        toasts: state.toasts.filter((t) => t.id !== id),
      }));
    }, 4000);
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    }));
  },
}));
