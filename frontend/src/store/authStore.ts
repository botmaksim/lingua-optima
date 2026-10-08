import { create } from 'zustand';
import { User } from '../types/user';
import { authApi } from '../api/authApi';
import { setAccessToken } from '../api/axiosInstance';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setUser: (user: User | null) => void;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string, role: string) => Promise<void>;
  logout: () => Promise<void>;
  initAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  setUser: (user) => set({ user, isAuthenticated: !!user, isLoading: false }),

  login: async (email, password) => {
    set({ isLoading: true });
    try {
      const data = await authApi.login(email, password);
      set({ user: data.user, isAuthenticated: true, isLoading: false });
    } catch (err) {
      set({ isLoading: false });
      throw err;
    }
  },

  register: async (email, password, fullName, role) => {
    set({ isLoading: true });
    try {
      const data = await authApi.register(email, password, fullName, role);
      set({ user: data.user, isAuthenticated: true, isLoading: false });
    } catch (err) {
      set({ isLoading: false });
      throw err;
    }
  },

  logout: async () => {
    try {
      await authApi.logout();
    } finally {
      setAccessToken(null);
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },

  initAuth: async () => {
    set({ isLoading: true });
    try {
      const data = await authApi.refreshToken();
      set({ user: data.user, isAuthenticated: true, isLoading: false });
    } catch {
      setAccessToken(null);
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },
}));

// Listen to custom expiration event from axios interceptor
if (typeof window !== 'undefined') {
  window.addEventListener('auth:expired', () => {
    useAuthStore.getState().logout();
  });
}
