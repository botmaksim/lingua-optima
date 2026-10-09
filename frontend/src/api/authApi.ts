/**
 * @file authApi.ts
 * @brief Authentication client API module covering login, registration, token refresh, and logout.
 */

import { axiosInstance, setAccessToken } from './axiosInstance';
import { AuthResponse, User, CefrLevel, Role } from '../types/user';

/**
 * @brief Exported const for auth api.
 */
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
   * @param cefrLevel Optional initial CEFR proficiency level.
   * @return Promise resolving to AuthResponse.
   */
  register: async (
    email: string,
    password: string,
    fullName: string,
    role: string,
    cefrLevel?: CefrLevel
  ): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/register', {
      email,
      password,
      fullName,
      role,
      cefrLevel,
    });
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  /**
   * @brief Authenticates or registers a user via a Google OAuth2 ID token.
   * @param idToken Signed Google ID token returned by Google Identity Services.
   * @param role Optional role ('STUDENT' or 'TEACHER') for newly created accounts.
   * @return Promise resolving to AuthResponse containing JWT and user profile.
   */
  googleLogin: async (idToken: string, role?: string): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/google', { idToken, role });
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
   * @param redirectUrl Optional redirect URL.
   */
  forgotPassword: async (email: string, redirectUrl?: string): Promise<void> => {
    await axiosInstance.post('/auth/forgot-password', { email, redirectUrl });
  },

  /**
   * @brief Retrieves latest user profile details.
   * @return Promise resolving to user profile object.
   */
  getMe: async (): Promise<User> => {
    const res = await axiosInstance.get<User>('/users/me');
    return res.data;
  },

  /**
   * @brief Updates user profile details including full name, display alias, CEFR level, or role.
   * @param data Object with optional fullName, displayAlias, cefrLevel, role.
   * @return Promise resolving to updated user profile.
   */
  updateProfile: async (data: {
    fullName?: string;
    displayAlias?: string;
    cefrLevel?: CefrLevel;
    role?: Role;
  }): Promise<User> => {
    const res = await axiosInstance.put<User>('/users/me', data);
    return res.data;
  },
};
