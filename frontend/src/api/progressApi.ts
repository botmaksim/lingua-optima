/**
 * @file progressApi.ts
 * @brief REST client API module for student topic progress records, learning gaps, and CEFR level-up confirmations.
 */

import { axiosInstance } from './axiosInstance';
import { ProgressRecord } from '../types/progress';
import { User } from '../types/user';

/**
 * @brief Client API methods for querying topic mastery, grammar gaps, and CEFR level promotions.
 */
export const progressApi = {
  /**
   * @brief Retrieves topic mastery progress records for the current student.
   * @return Promise resolving to array of ProgressRecord objects.
   */
  getMyProgress: async (): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>('/progress/me');
    return res.data;
  },

  /**
   * @brief Retrieves knowledge gaps (mastery below 60%) for the current student.
   * @return Promise resolving to array of ProgressRecord objects.
   */
  getGaps: async (): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>('/progress/gaps');
    return res.data;
  },

  /**
   * @brief Retrieves aggregate progress records across all active students in a cohort group.
   * @param groupId Cohort group identifier.
   * @return Promise resolving to array of ProgressRecord objects.
   */
  getGroupProgress: async (groupId: string): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>(`/progress/group/${groupId}`);
    return res.data;
  },

  /**
   * @brief Confirms student acceptance of a suggested CEFR level upgrade and returns updated profile.
   * @return Promise resolving to updated User profile object.
   */
  confirmLevelUp: async (): Promise<User> => {
    const res = await axiosInstance.post<User>('/progress/level-up/confirm');
    if (res.data && res.data.id) {
      return res.data;
    }
    const userRes = await axiosInstance.get<User>('/users/me');
    return userRes.data;
  },
};
