/**
 * @file useUsage.ts
 * @brief Custom React hook querying and computing remaining free-tier evaluation and OCR quotas.
 */

import { useState, useEffect } from 'react';
import { subscriptionApi } from '../api/subscriptionApi';
import { UsageCounter } from '../types/subscription';
import { useAuthStore } from '../store/authStore';

/**
 * @brief Hook monitoring quota consumption and limit warnings.
 * @return Object containing usage data, remaining allowances, and refresh trigger.
 */
export const useUsage = () => {
  const { isAuthenticated } = useAuthStore();
  const [usage, setUsage] = useState<UsageCounter | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  /**
   * @brief Event handler or helper executing fetch usage.
   */
  const fetchUsage = async () => {
    if (!isAuthenticated) return;
    setIsLoading(true);
    try {
      const data = await subscriptionApi.getUsage();
      setUsage(data);
    } catch (err) {
      console.error('Failed to load usage:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsage();
  }, [isAuthenticated]);

  const isUnlimited = usage
    ? usage.dailyEvaluationsLimit >= 999999 || usage.dailyEvaluationsLimit === -1
    : false;

  const isUnlimitedOcr = usage
    ? usage.dailyOcrLimit >= 999999 || usage.dailyOcrLimit === -1
    : false;

  const remainingEvaluations = usage
    ? isUnlimited
      ? Infinity
      : Math.max(0, usage.dailyEvaluationsLimit - usage.dailyAiEvaluations)
    : 0;

  const remainingOcr = usage
    ? isUnlimitedOcr
      ? Infinity
      : Math.max(0, usage.dailyOcrLimit - usage.dailyOcrScans)
    : 0;

  const isQuotaExceeded = usage
    ? isUnlimited
      ? false
      : usage.dailyAiEvaluations >= usage.dailyEvaluationsLimit ||
        (usage.tokensRemaining !== undefined && usage.tokensRemaining <= 0)
    : false;

  const tokensUsed = usage?.weekTokensUsed || 0;
  const tokenLimit = usage?.tokenLimit || 50000;
  const tokensRemaining = usage?.tokensRemaining ?? Math.max(0, tokenLimit - tokensUsed);

  return {
    usage,
    isLoading,
    remainingEvaluations,
    remainingOcr,
    tokensUsed,
    tokenLimit,
    tokensRemaining,
    isUnlimited,
    isUnlimitedOcr,
    isQuotaExceeded,
    refreshUsage: fetchUsage,
  };
};
