/**
 * @file stores.test.ts
 * @brief Unit tests for Zustand state stores (useUIStore, useNotificationStore, useAuthStore).
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useUIStore } from '../store/uiStore';
import { useNotificationStore } from '../store/notificationStore';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { notificationApi } from '../api/notificationApi';
import { getAccessToken, setAccessToken } from '../api/axiosInstance';

describe('useUIStore', () => {
  beforeEach(() => {
    useUIStore.setState({
      isSidebarOpen: false,
      isUpgradeWallOpen: false,
      upgradeWallReason: '',
    });
  });

  it('toggles sidebar correctly', () => {
    expect(useUIStore.getState().isSidebarOpen).toBe(false);
    useUIStore.getState().toggleSidebar();
    expect(useUIStore.getState().isSidebarOpen).toBe(true);
    useUIStore.getState().toggleSidebar();
    expect(useUIStore.getState().isSidebarOpen).toBe(false);
  });

  it('opens and closes upgrade wall with reason', () => {
    useUIStore.getState().openUpgradeWall('Limit reached for evaluations');
    expect(useUIStore.getState().isUpgradeWallOpen).toBe(true);
    expect(useUIStore.getState().upgradeWallReason).toBe('Limit reached for evaluations');

    useUIStore.getState().closeUpgradeWall();
    expect(useUIStore.getState().isUpgradeWallOpen).toBe(false);
    expect(useUIStore.getState().upgradeWallReason).toBe('');
  });
});

describe('useNotificationStore', () => {
  beforeEach(() => {
    useNotificationStore.setState({
      notifications: [],
      unreadCount: 0,
      isLoading: false,
      toasts: [],
    });
    vi.restoreAllMocks();
  });

  it('manages toasts correctly', () => {
    useNotificationStore.getState().addToast({
      type: 'success',
      message: 'Evaluation completed',
    });

    const state = useNotificationStore.getState();
    expect(state.toasts.length).toBe(1);
    expect(state.toasts[0].message).toBe('Evaluation completed');
    expect(state.toasts[0].type).toBe('success');

    const toastId = state.toasts[0].id;
    useNotificationStore.getState().removeToast(toastId);
    expect(useNotificationStore.getState().toasts.length).toBe(0);
  });

  it('adds notifications and clamps unreadCount at zero on repeated markAsRead calls', async () => {
    vi.spyOn(notificationApi, 'markAsRead').mockResolvedValue(undefined);

    useNotificationStore.getState().addNotification({
      id: '123',
      type: 'TASK',
      message: 'New task assigned',
      isRead: false,
      createdAt: new Date().toISOString(),
    });

    expect(useNotificationStore.getState().unreadCount).toBe(1);

    await useNotificationStore.getState().markAsRead('123');
    expect(useNotificationStore.getState().unreadCount).toBe(0);
    expect(useNotificationStore.getState().notifications[0].isRead).toBe(true);

    await useNotificationStore.getState().markAsRead('123');
    expect(useNotificationStore.getState().unreadCount).toBe(0);
  });
});

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
    vi.restoreAllMocks();
  });

  it('authenticates user via email and password and stores in-memory JWT', async () => {
    vi.spyOn(authApi, 'login').mockImplementation(async () => {
      const res = {
        accessToken: 'jwt-email',
        user: {
          id: 'u-1',
          email: 'user@example.com',
          fullName: 'Email User',
          role: 'STUDENT' as const,
          cefrLevel: 'B1' as const,
          streakCount: 1,
          createdAt: new Date().toISOString(),
        },
      };
      setAccessToken(res.accessToken);
      return res;
    });

    await useAuthStore.getState().login('user@example.com', 'secret123');
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.email).toBe('user@example.com');
    expect(getAccessToken()).toBe('jwt-email');
  });

  it('authenticates user via Google OAuth2 ID token', async () => {
    vi.spyOn(authApi, 'googleLogin').mockImplementation(async () => {
      const res = {
        accessToken: 'jwt-google',
        user: {
          id: 'u-2',
          email: 'google@example.com',
          fullName: 'Google User',
          role: 'STUDENT' as const,
          cefrLevel: 'B2' as const,
          streakCount: 3,
          createdAt: new Date().toISOString(),
        },
      };
      setAccessToken(res.accessToken);
      return res;
    });

    await useAuthStore.getState().googleLogin('google-id-token-xyz', 'STUDENT');
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.email).toBe('google@example.com');
    expect(getAccessToken()).toBe('jwt-google');
  });

  it('wipes in-memory JWT and user state on logout even when backend logout endpoint fails', async () => {
    setAccessToken('active-jwt-token');
    useAuthStore.setState({
      user: {
        id: 'u-1',
        email: 'user@example.com',
        fullName: 'User',
        role: 'STUDENT',
        cefrLevel: 'B1',
        streakCount: 1,
        createdAt: new Date().toISOString(),
      },
      isAuthenticated: true,
      isLoading: false,
    });
    vi.spyOn(authApi, 'logout').mockRejectedValue(new Error('Network offline'));

    await useAuthStore.getState().logout();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(getAccessToken()).toBeNull();
  });

  it('handles expired refresh token during initAuth without leaving store in loading state', async () => {
    setAccessToken('stale-jwt');
    vi.spyOn(authApi, 'refreshToken').mockRejectedValue(new Error('401 Unauthorized'));

    await useAuthStore.getState().initAuth();
    expect(useAuthStore.getState().isLoading).toBe(false);
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(getAccessToken()).toBeNull();
  });
});


