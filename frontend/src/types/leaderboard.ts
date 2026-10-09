/**
 * @file leaderboard.ts
 * @brief Types for privacy-first, intra-group leaderboards with pseudonymous aliases.
 */

/**
 * @brief Individual ranked entry within an intra-group leaderboard.
 */
export interface LeaderboardEntry {
  /** @brief Property representing rank in LeaderboardEntry. */
  rank: number;
  /** @brief Property representing user id in LeaderboardEntry. */
  userId: string;
  /** @brief Property representing alias in LeaderboardEntry. */
  alias: string;
  /** @brief Property representing weekly score in LeaderboardEntry. */
  weeklyScore: number;
  /** @brief Property representing streak count in LeaderboardEntry. */
  streakCount: number;
}

/**
 * @brief Leaderboard payload for a specific student group.
 */
export interface GroupLeaderboardResponse {
  /** @brief Property representing group id in GroupLeaderboardResponse. */
  groupId: string;
  /** @brief Property representing group name in GroupLeaderboardResponse. */
  groupName: string;
  /** @brief Property representing entries in GroupLeaderboardResponse. */
  entries: LeaderboardEntry[];
}
