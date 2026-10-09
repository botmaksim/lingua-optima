/**
 * @file progress.ts
 * @brief Types for tracking CEFR grammar topic progress and mastery metrics.
 */

/**
 * @brief Student mastery progress record for a single grammar topic.
 */
export interface ProgressRecord {
  /** @brief Property representing id in ProgressRecord. */
  id: string;
  /** @brief Property representing grammar topic in ProgressRecord. */
  grammarTopic: string;
  /** @brief Property representing total attempts in ProgressRecord. */
  totalAttempts: number;
  /** @brief Property representing error count in ProgressRecord. */
  errorCount: number;
  /** @brief Property representing mastery score in ProgressRecord. */
  masteryScore: number;
  /** @brief Property representing last practiced at in ProgressRecord. */
  lastPracticedAt: string;
}

/**
 * @brief Aggregated progress records across all students in a group.
 */
export interface GroupProgress {
  /** @brief Property representing group id in GroupProgress. */
  groupId: string;
  /** @brief Property representing records in GroupProgress. */
  records: ProgressRecord[];
}
