/**
 * @file userApi.ts
 * @brief REST client API module for user account management, profile updates, and student renaming.
 */

import { axiosInstance } from './axiosInstance';
import { User } from '../types/user';

/**
 * @brief REST client API module for user profile and student account operations.
 */
export const userApi = {
  /**
   * @brief Renames or customizes the displayed name of an enrolled student.
   * @param studentId Identifier of the student account.
   * @param fullName New full name or custom display label.
   * @return Promise resolving to updated User object.
   */
  updateStudentName: async (studentId: string, fullName: string): Promise<User> => {
    const res = await axiosInstance.put<User>(`/users/${studentId}/name`, { fullName });
    return res.data;
  },

  /**
   * @brief Retrieves profile information for current user.
   * @return Promise resolving to User object.
   */
  getProfile: async (): Promise<User> => {
    const res = await axiosInstance.get<User>('/users/me');
    return res.data;
  },
};
