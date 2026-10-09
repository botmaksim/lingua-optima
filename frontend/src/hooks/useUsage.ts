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

  const remainingEvaluations = usage
    ? Math.max(0, usage.dailyEvaluationsLimit - usage.dailyAiEvaluations)
    : 0;

  const remainingOcr = usage
    ? Math.max(0, usage.dailyOcrLimit - usage.dailyOcrScans)
    : 0;

  const isQuotaExceeded = usage
    ? usage.dailyAiEvaluations >= usage.dailyEvaluationsLimit
    : false;

  return {
    usage,
    isLoading,
    remainingEvaluations,
    remainingOcr,
    isQuotaExceeded,
    refreshUsage: fetchUsage,
  };
};
