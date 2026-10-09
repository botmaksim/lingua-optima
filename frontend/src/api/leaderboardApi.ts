/**
 * @file leaderboardApi.ts
 * @brief REST client API module for cohort-scoped leaderboard rankings.
 *
 * Enforces architectural privacy rule: rankings are strictly scoped within individual teacher groups.
 */

import { axiosInstance } from './axiosInstance';
import { GroupLeaderboardResponse } from '../types/leaderboard';

export const leaderboardApi = {
  /**
   * @brief Retrieves weekly ranked student standings within a group.
   * @param groupId Group identifier.
   * @return Promise resolving to GroupLeaderboardResponse.
   */
  getGroupLeaderboard: async (groupId: string): Promise<GroupLeaderboardResponse> => {
    const res = await axiosInstance.get<GroupLeaderboardResponse>(`/leaderboard/group/${groupId}`);
    return res.data;
  },
};
