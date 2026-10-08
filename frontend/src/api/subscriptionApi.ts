import { axiosInstance } from './axiosInstance';
import { Subscription, SubscriptionTier, UsageCounter } from '../types/subscription';

export const subscriptionApi = {
  getMySubscription: async (): Promise<Subscription> => {
    const res = await axiosInstance.get<Subscription>('/subscriptions/me');
    return res.data;
  },

  getUsage: async (): Promise<UsageCounter> => {
    const res = await axiosInstance.get<UsageCounter>('/subscriptions/usage');
    return res.data;
  },

  upgradeTier: async (tier: SubscriptionTier): Promise<Subscription> => {
    const res = await axiosInstance.post<Subscription>('/subscriptions/upgrade', { tier });
    return res.data;
  },
};
