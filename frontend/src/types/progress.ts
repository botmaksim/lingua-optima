/**
 * @file progress.ts
 * @brief Types for tracking CEFR grammar topic progress and mastery metrics.
 */

/**
 * @brief Student mastery progress record for a single grammar topic.
 */
export interface ProgressRecord {
  id: string;
  grammarTopic: string;
  totalAttempts: number;
  errorCount: number;
  masteryScore: number;
  lastPracticedAt: string;
}

/**
 * @brief Aggregated progress records across all students in a group.
 */
export interface GroupProgress {
  groupId: string;
  records: ProgressRecord[];
}
