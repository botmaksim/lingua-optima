/**
 * @file subscriptionApi.ts
 * @brief REST client API module for user subscription tiers, weekly usage quotas, and tier upgrades.
 */

import { axiosInstance } from './axiosInstance';
import { Subscription, SubscriptionTier, UsageCounter } from '../types/subscription';

/**
 * @brief Client API methods for managing subscription plans and weekly quota usage.
 */
export const subscriptionApi = {
  /**
   * @brief Retrieves active subscription details and tier benefits for current user.
   * @return Promise resolving to Subscription object.
   */
  getMySubscription: async (): Promise<Subscription> => {
    const res = await axiosInstance.get<Subscription>('/subscriptions/me');
    return res.data;
  },

  /**
   * @brief Queries weekly evaluation and OCR usage numbers and remaining limits.
   * @return Promise resolving to UsageCounter object.
   */
  getUsage: async (): Promise<UsageCounter> => {
    const res = await axiosInstance.get<UsageCounter>('/subscriptions/usage');
    return res.data;
  },

  /**
   * @brief Upgrades user subscription to a higher tier and returns updated Subscription state.
   * @param tier Desired target SubscriptionTier.
   * @return Promise resolving to updated Subscription object.
   */
  upgradeTier: async (tier: SubscriptionTier): Promise<Subscription> => {
    const res = await axiosInstance.post<Subscription>('/subscriptions/upgrade', {
      targetTier: tier,
      tier,
      paymentToken: 'stub-token',
    });
    if (res.data && res.data.tier) {
      return res.data;
    }
    return subscriptionApi.getMySubscription();
  },
};
