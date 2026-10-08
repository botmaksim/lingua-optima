import { describe, it, expect, beforeEach } from 'vitest';
import { useUIStore } from '../store/uiStore';
import { useNotificationStore } from '../store/notificationStore';

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

  it('adds notifications and increments unread count', () => {
    useNotificationStore.getState().addNotification({
      id: '123',
      type: 'TASK',
      message: 'New task assigned',
      isRead: false,
      createdAt: new Date().toISOString(),
    });

    const state = useNotificationStore.getState();
    expect(state.notifications.length).toBe(1);
    expect(state.unreadCount).toBe(1);
    expect(state.notifications[0].message).toBe('New task assigned');
  });
});
