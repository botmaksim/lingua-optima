import { create } from 'zustand';

/**
 * @file uiStore.ts
 * @brief Zustand store managing global UI modal states, mobile sidebar toggling, and paywall popups.
 */

/**
 * @interface UIState
 * @brief UI layout visibility and modal flags.
 */
interface UIState {
  isSidebarOpen: boolean;
  isUpgradeWallOpen: boolean;
  upgradeWallReason: string;
  toggleSidebar: () => void;
  openUpgradeWall: (reason?: string) => void;
  closeUpgradeWall: () => void;
}

/**
 * @brief Global UI state management store hook.
 */
export const useUIStore = create<UIState>((set) => ({
  isSidebarOpen: false,
  isUpgradeWallOpen: false,
  upgradeWallReason: '',

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
  openUpgradeWall: (reason = 'You have reached your daily evaluation limit.') =>
    set({ isUpgradeWallOpen: true, upgradeWallReason: reason }),
  closeUpgradeWall: () => set({ isUpgradeWallOpen: false, upgradeWallReason: '' }),
}));
