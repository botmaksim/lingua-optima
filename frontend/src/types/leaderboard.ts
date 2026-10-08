export interface LeaderboardEntry {
  rank: number;
  userId: string;
  alias: string;
  weeklyScore: number;
  streakCount: number;
}

export interface GroupLeaderboardResponse {
  groupId: string;
  groupName: string;
  entries: LeaderboardEntry[];
}
