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
  /** @brief Property representing user or student ID in LeaderboardEntry. */
  studentId?: string;
  /** @brief Property representing user id (legacy fallback). */
  userId?: string;
  /** @brief Property representing display alias in LeaderboardEntry. */
  displayAlias?: string;
  /** @brief Property representing legacy alias. */
  alias?: string;
  /** @brief Property representing student's full legal name. */
  fullName?: string;
  /** @brief Property representing weekly score in LeaderboardEntry. */
  weeklyScore: number;
  /** @brief Number of completed tasks assigned by group educator. */
  completedTasks?: number;
  /** @brief Total tasks assigned to student by group educator. */
  totalTasks?: number;
  /** @brief Percentage completion rate (0-100). */
  completionRate?: number;
  /** @brief Average percentage score on group tasks. */
  averageScore?: number;
  /** @brief CEFR proficiency level of student. */
  cefrLevel?: string;
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
