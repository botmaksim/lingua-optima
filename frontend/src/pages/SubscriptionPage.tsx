/**
 * @file SubscriptionPage.tsx
 * @brief Subscription management and upgrade pricing tiers page with mock payment processing.
 */

import React, { useEffect, useState } from 'react';
import { Check, Sparkles, Shield, Zap, Users } from 'lucide-react';
import { subscriptionApi } from '../api/subscriptionApi';
import { Subscription, SubscriptionTier, UsageCounter } from '../types/subscription';
import { useNotificationStore } from '../store/notificationStore';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

/**
 * @brief Renders the subscription plans, daily quota usage meters, and sandbox upgrade actions.
 * @return JSX subscription management view.
 */
export const SubscriptionPage: React.FC = () => {
  const { addToast } = useNotificationStore();
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [usage, setUsage] = useState<UsageCounter | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpgrading, setIsUpgrading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  /**
   * @brief Event handler or helper executing fetch data.
   */
  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [subData, usageData] = await Promise.all([
        subscriptionApi.getMySubscription().catch(() => null),
        subscriptionApi.getUsage().catch(() => null),
      ]);
      setSubscription(subData);
      setUsage(usageData);
    } catch {
      addToast({
        title: 'Error',
        message: 'Failed to load subscription details.',
        type: 'error',
      });
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle upgrade.
   */
  const handleUpgrade = async (tier: SubscriptionTier) => {
    try {
      setIsUpgrading(true);
      const updated = await subscriptionApi.upgradeTier(tier);
      setSubscription(updated);
      addToast({
        title: 'Success!',
        message: `Plan changed to ${tier} successfully. (Mock Payment Processed)`,
        type: 'success',
      });
      await fetchData();
    } catch {
      addToast({
        title: 'Upgrade Failed',
        message: 'Could not process tier change. Please try again.',
        type: 'error',
      });
    } finally {
      setIsUpgrading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  const currentTier = subscription?.tier || 'FREE';

  const plans: Array<{
    tier: SubscriptionTier;
    name: string;
    price: string;
    period: string;
    description: string;
    features: string[];
    highlighted?: boolean;
    badge?: string;
  }> = [
    {
      tier: 'FREE',
      name: 'Free Learner',
      price: '$0',
      period: 'forever',
      description: 'Ideal for self-paced beginners getting started with adaptive English learning.',
      features: [
        '5 AI evaluations per day',
        '3 OCR scans per day',
        'Standard CEFR adaptive CAT sessions',
        'Group leaderboards participation',
        'Zero-retention OCR privacy',
      ],
    },
    {
      tier: 'PREMIUM',
      name: 'Premium Student',
      price: '$9.99',
      period: 'per month',
      description: 'For dedicated learners aiming for rapid IELTS, TOEFL, or Cambridge fluency.',
      features: [
        'Unlimited AI evaluations',
        'Unlimited handwritten OCR scans',
        'Deep CEFR rubric & lexical diversity feedback',
        'Priority Groq & Gemini AI fallback pipeline',
        'Bring-Your-Own-Key (BYOK) AES-256 fallback',
        'Offline submission queue & auto-sync',
      ],
      highlighted: true,
      badge: 'Most Popular',
    },
    {
      tier: 'EDUCATOR',
      name: 'Educator Pro',
      price: '$29.99',
      period: 'per month',
      description: 'Designed for teachers, schools, and tutors managing multiple student cohorts.',
      features: [
        'Everything in Premium',
        'Unlimited student groups & class codes',
        'Student progress tracking & soft-delete restore',
        'Export reports in CSV and PDF formats',
        'Custom task & essay topic generation',
        'Group-level anonymized ranking settings',
      ],
    },
  ];

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      <div className="text-center max-w-3xl mx-auto mb-12">
        <h1 className="text-3xl font-extrabold text-slate-900 sm:text-4xl tracking-tight">
          Flexible Plans for Every English Learner
        </h1>
        <p className="mt-4 text-base text-slate-600">
          Level up your English with AI-driven adaptive sessions, real-time CEFR metrics, and zero-retention privacy.
        </p>
      </div>

      {usage && (
        <div className="mb-12 bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Current Plan</span>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-2xl font-bold text-slate-900">{currentTier}</span>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  Active
                </span>
              </div>
            </div>
            <div className="text-sm text-slate-500">
              Usage resets daily at midnight UTC
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mt-6">
            <div>
              <div className="flex justify-between text-sm font-medium mb-1.5">
                <span className="text-slate-600">Daily AI Evaluations</span>
                <span className="text-slate-900 font-semibold">
                  {usage.dailyAiEvaluations} / {usage.dailyEvaluationsLimit === -1 ? '∞' : usage.dailyEvaluationsLimit}
                </span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-primary h-2.5 rounded-full transition-all duration-300"
                  style={{
                    width: usage.dailyEvaluationsLimit === -1
                      ? '10%'
                      : `${Math.min(100, (usage.dailyAiEvaluations / usage.dailyEvaluationsLimit) * 100)}%`,
                  }}
                />
              </div>
            </div>

            <div>
              <div className="flex justify-between text-sm font-medium mb-1.5">
                <span className="text-slate-600">Daily OCR Scans</span>
                <span className="text-slate-900 font-semibold">
                  {usage.dailyOcrScans} / {usage.dailyOcrLimit === -1 ? '∞' : usage.dailyOcrLimit}
                </span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-sky-500 h-2.5 rounded-full transition-all duration-300"
                  style={{
                    width: usage.dailyOcrLimit === -1
                      ? '10%'
                      : `${Math.min(100, (usage.dailyOcrScans / usage.dailyOcrLimit) * 100)}%`,
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {plans.map((plan) => {
          const isCurrent = currentTier === plan.tier;

          return (
            <div
              key={plan.tier}
              className={`relative rounded-3xl p-8 flex flex-col justify-between transition-all duration-200 ${
                plan.highlighted
                  ? 'bg-white border-2 border-primary shadow-xl shadow-indigo-100/50 scale-[1.02]'
                  : 'bg-white border border-slate-200/80 shadow-sm hover:shadow-md'
              }`}
            >
              {plan.badge && (
                <div className="absolute -top-3.5 left-1/2 -translate-x-1/2">
                  <span className="bg-primary text-white text-xs font-semibold px-3.5 py-1 rounded-full shadow-sm tracking-wide">
                    {plan.badge}
                  </span>
                </div>
              )}

              <div>
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-xl font-bold text-slate-900">{plan.name}</h3>
                    <p className="text-xs text-slate-500 mt-1">{plan.description}</p>
                  </div>
                  {plan.tier === 'PREMIUM' && <Sparkles className="w-6 h-6 text-primary" />}
                  {plan.tier === 'EDUCATOR' && <Users className="w-6 h-6 text-sky-600" />}
                  {plan.tier === 'FREE' && <Shield className="w-6 h-6 text-slate-400" />}
                </div>

                <div className="my-6">
                  <span className="text-4xl font-extrabold text-slate-900">{plan.price}</span>
                  <span className="text-slate-500 text-sm ml-2">/{plan.period}</span>
                </div>

                <ul className="space-y-3.5 mb-8">
                  {plan.features.map((feature, i) => (
                    <li key={i} className="flex items-start text-sm text-slate-600">
                      <Check className="w-4 h-4 text-emerald-500 mr-3 mt-0.5 flex-shrink-0" />
                      <span>{feature}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div>
                {isCurrent ? (
                  <button
                    disabled
                    className="w-full py-3 px-4 rounded-xl bg-slate-100 text-slate-500 font-medium text-sm cursor-default"
                  >
                    Current Plan
                  </button>
                ) : (
                  <button
                    disabled={isUpgrading}
                    onClick={() => handleUpgrade(plan.tier)}
                    className={`w-full py-3 px-4 rounded-xl font-semibold text-sm transition shadow-sm ${
                      plan.highlighted
                        ? 'bg-primary hover:bg-primary-hover text-white shadow-indigo-100'
                        : 'bg-slate-900 hover:bg-slate-800 text-white'
                    }`}
                  >
                    {isUpgrading ? 'Processing...' : plan.tier === 'FREE' ? 'Downgrade to Free' : `Upgrade to ${plan.name}`}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-12 bg-indigo-50/50 border border-indigo-100 rounded-2xl p-6 flex items-start gap-4">
        <Zap className="w-6 h-6 text-primary flex-shrink-0 mt-0.5" />
        <div className="text-sm text-slate-700">
          <h4 className="font-semibold text-slate-900 mb-1">Mock Payment Sandbox</h4>
          <p>
            Lingua Optima is currently operating in demonstrator sandbox mode. Clicking upgrade instantly applies the new tier
            via the backend payment stub provider with zero billing charges.
          </p>
        </div>
      </div>
    </div>
  );
};
