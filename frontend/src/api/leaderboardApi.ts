import { axiosInstance } from './axiosInstance';
import { GroupLeaderboardResponse } from '../types/leaderboard';

export const leaderboardApi = {
  getGroupLeaderboard: async (groupId: string): Promise<GroupLeaderboardResponse> => {
    const res = await axiosInstance.get<GroupLeaderboardResponse>(`/leaderboard/group/${groupId}`);
    return res.data;
  },
};
