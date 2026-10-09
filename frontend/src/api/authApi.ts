/**
 * @file authApi.ts
 * @brief Authentication client API module covering login, registration, token refresh, and logout.
 */

import { axiosInstance, setAccessToken } from './axiosInstance';
import { AuthResponse } from '../types/user';

export const authApi = {
  /**
   * @brief Authenticates user credentials with backend.
   * @param email User email address.
   * @param password Plaintext password.
   * @return Promise resolving to AuthResponse containing JWT and profile.
   */
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/login', { email, password });
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  /**
   * @brief Registers a new user account.
   * @param email Email address.
   * @param password Password.
   * @param fullName Full legal name.
   * @param role User role ('STUDENT' or 'TEACHER').
   * @return Promise resolving to AuthResponse.
   */
  register: async (email: string, password: string, fullName: string, role: string): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/register', { email, password, fullName, role });
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  /**
   * @brief Exchanges valid HttpOnly cookie refresh token for a new access token.
   * @return Promise resolving to refreshed AuthResponse.
   */
  refreshToken: async (): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/refresh');
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  /**
   * @brief Revokes server session and clears in-memory tokens.
   */
  logout: async (): Promise<void> => {
    try {
      await axiosInstance.post('/auth/logout');
    } finally {
      setAccessToken(null);
    }
  },

  /**
   * @brief Requests password reset email dispatch.
   * @param email User account email.
   */
  forgotPassword: async (email: string): Promise<void> => {
    await axiosInstance.post('/auth/forgot-password', { email });
  },

  /**
   * @brief Retrieves latest user profile details.
   * @return Promise resolving to user profile object.
   */
  getMe: async () => {
    const res = await axiosInstance.get('/users/me');
    return res.data;
  },
};
