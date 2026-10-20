/**
 * @file leaderboard.ts
 * @brief Types for privacy-first, intra-group leaderboards with pseudonymous aliases.
 */

/**
 * @brief Individual ranked entry within an intra-group leaderboard.
 */
export interface LeaderboardEntry {
  rank: number;
  studentId?: string;
  userId?: string;
  displayAlias?: string;
  alias?: string;
  fullName?: string;
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
