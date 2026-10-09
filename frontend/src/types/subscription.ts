/**
 * @file subscription.ts
 * @brief Types for subscription tiers, active licenses, and daily rate limit counters.
 */

/**
 * @brief Subscription tiers available in LinguaOptima.
 */
export type SubscriptionTier = 'FREE' | 'PREMIUM' | 'EDUCATOR';

/**
 * @brief User subscription details.
 */
export interface Subscription {
  /** @brief Property representing id in Subscription. */
  id: string;
  /** @brief Property representing tier in Subscription. */
  tier: SubscriptionTier;
  /** @brief Property representing start date in Subscription. */
  startDate: string;
  /** @brief Property representing end date in Subscription. */
  endDate?: string;
  /** @brief Property representing status in Subscription. */
  status: string;
}

/**
 * @brief Daily usage metrics and quota tracking for OCR and AI evaluations.
 */
export interface UsageCounter {
  /** @brief Property representing daily ai evaluations in UsageCounter. */
  dailyAiEvaluations: number;
  /** @brief Property representing daily evaluations limit in UsageCounter. */
  dailyEvaluationsLimit: number;
  /** @brief Property representing daily ocr scans in UsageCounter. */
  dailyOcrScans: number;
  /** @brief Property representing daily ocr limit in UsageCounter. */
  dailyOcrLimit: number;
  /** @brief Property representing reset date in UsageCounter. */
  resetDate: string;
}
