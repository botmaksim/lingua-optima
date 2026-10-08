import { create } from 'zustand';

interface UIState {
  isSidebarOpen: boolean;
  isUpgradeWallOpen: boolean;
  upgradeWallReason: string;
  toggleSidebar: () => void;
  openUpgradeWall: (reason?: string) => void;
  closeUpgradeWall: () => void;
}

export const useUIStore = create<UIState>((set) => ({
  isSidebarOpen: false,
  isUpgradeWallOpen: false,
  upgradeWallReason: '',

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),
  openUpgradeWall: (reason = 'You have reached your daily evaluation limit.') =>
    set({ isUpgradeWallOpen: true, upgradeWallReason: reason }),
  closeUpgradeWall: () => set({ isUpgradeWallOpen: false, upgradeWallReason: '' }),
}));
