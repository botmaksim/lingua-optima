export type SubscriptionTier = 'FREE' | 'PREMIUM' | 'EDUCATOR';

export interface Subscription {
  id: string;
  tier: SubscriptionTier;
  startDate: string;
  endDate?: string;
  status: string;
}

export interface UsageCounter {
  dailyAiEvaluations: number;
  dailyEvaluationsLimit: number;
  dailyOcrScans: number;
  dailyOcrLimit: number;
  resetDate: string;
}
