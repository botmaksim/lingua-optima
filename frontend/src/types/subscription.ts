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
  id: string;
  tier: SubscriptionTier;
  startDate: string;
  endDate?: string;
  status: string;
}

/**
 * @brief Daily usage metrics and quota tracking for OCR and AI evaluations.
 */
export interface UsageCounter {
  dailyAiEvaluations: number;
  dailyEvaluationsLimit: number;
  dailyOcrScans: number;
  dailyOcrLimit: number;
  resetDate: string;
}
