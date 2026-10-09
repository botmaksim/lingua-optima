/**
 * @file leaderboardApi.ts
 * @brief REST client API module for cohort-scoped leaderboard rankings.
 *
 * Enforces architectural privacy rule: rankings are strictly scoped within individual teacher groups.
 */

import { axiosInstance } from './axiosInstance';
import { GroupLeaderboardResponse, LeaderboardEntry } from '../types/leaderboard';

/**
 * @brief Client API methods for querying intra-group weekly leaderboards.
 */
export const leaderboardApi = {
  /**
   * @brief Retrieves weekly ranked student standings within a group.
   * @param groupId Group identifier.
   * @return Promise resolving to GroupLeaderboardResponse.
   */
  getGroupLeaderboard: async (groupId: string): Promise<GroupLeaderboardResponse> => {
    const res = await axiosInstance.get<GroupLeaderboardResponse | LeaderboardEntry[]>(
      `/leaderboard/group/${groupId}`
    );
    if (Array.isArray(res.data)) {
      return {
        groupId,
        groupName: 'Class Cohort',
        entries: res.data,
      };
    }
    return res.data;
  },
};
