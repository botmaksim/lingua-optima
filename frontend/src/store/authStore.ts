/**
 * @file authStore.ts
 * @brief Zustand store managing client authentication state, user identity, and session restoration.
 */

import { create } from 'zustand';
import { User } from '../types/user';
import { authApi } from '../api/authApi';
import { setAccessToken } from '../api/axiosInstance';

/**
 * @interface AuthState
 * @brief Reactive state and actions for user authentication and session management.
 */
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  /** @brief Sets current user identity */
  setUser: (user: User | null) => void;
  /** @brief Authenticates user with email and password */
  login: (email: string, password: string) => Promise<void>;
  /** @brief Registers a new user account */
  register: (email: string, password: string, fullName: string, role: string) => Promise<void>;
  /** @brief Authenticates or registers user via Google OAuth2 ID token */
  googleLogin: (idToken: string, role?: string) => Promise<void>;
  /** @brief Terminates the active session and clears tokens */
  logout: () => Promise<void>;
  /** @brief Restores active session on app startup via refresh token */
  initAuth: () => Promise<void>;
}

/**
 * @brief Global authentication store hook.
 */
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

  googleLogin: async (idToken, role) => {
    set({ isLoading: true });
    try {
      const data = await authApi.googleLogin(idToken, role);
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

if (typeof window !== 'undefined') {
  window.addEventListener('auth:expired', () => {
    useAuthStore.getState().logout();
  });
}
