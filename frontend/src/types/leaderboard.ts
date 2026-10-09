/**
 * @file leaderboard.ts
 * @brief Types for privacy-first, intra-group leaderboards with pseudonymous aliases.
 */

/**
 * @brief Individual ranked entry within an intra-group leaderboard.
 */
export interface LeaderboardEntry {
  rank: number;
  userId: string;
  alias: string;
  weeklyScore: number;
  streakCount: number;
}

/**
 * @brief Leaderboard payload for a specific student group.
 */
export interface GroupLeaderboardResponse {
  groupId: string;
  groupName: string;
  entries: LeaderboardEntry[];
}
