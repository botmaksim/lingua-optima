/**
 * @file SubscriptionPage.tsx
 * @brief Subscription management and upgrade pricing tiers page with mock payment processing.
 */

import React, { useEffect, useState } from 'react';
import { Check, Sparkles, Shield, Zap, Users, Camera, CheckCircle2, GraduationCap, Info, Cpu } from 'lucide-react';
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
        '5 AI task generations & practice sets per day',
        '5 AI homework & diagnostic evaluations per day',
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
        'Unlimited AI task generations & exercises',
        'Unlimited AI essay & diagnostic evaluations',
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
        'Everything in Premium (Unlimited AI Tasks & Evals)',
        'Unlimited student groups & class codes',
        'Teacher task deployment with attempt control (1 vs ∞)',
        'Student progress tracking & soft-delete restore',
        'Export reports in CSV and PDF formats',
        'Custom grammar rule & vocabulary context injection',
        'Group-level anonymized ranking settings',
      ],
    },
  ];

  const isUnlimitedEvals = usage
    ? usage.dailyEvaluationsLimit >= 999999 || usage.dailyEvaluationsLimit === -1 || usage.dailyEvaluationsLimit == null
    : false;

  const isUnlimitedOcr = usage
    ? usage.dailyOcrLimit >= 999999 || usage.dailyOcrLimit === -1 || usage.dailyOcrLimit == null
    : false;

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
        <div className="mb-12 bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">Current Plan</span>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-2xl font-black text-slate-900">{currentTier}</span>
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  Active
                </span>
                {(currentTier === 'EDUCATOR' || currentTier === 'PREMIUM') && (
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-primary border border-indigo-200/80 flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-amber-500" />
                    <span>Unlimited Quota</span>
                  </span>
                )}
              </div>
            </div>
            <div className="text-xs font-medium text-slate-500 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-100">
              Usage resets daily at midnight UTC
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Metric 1: Task Generation */}
            <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <Sparkles className="w-4 h-4 text-primary" />
                  <span>Task Generation</span>
                </div>
                {isUnlimitedEvals ? (
                  <span className="text-[11px] font-black text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    Unlimited (∞)
                  </span>
                ) : (
                  <span className="text-xs font-mono font-bold text-slate-900">
                    {usage.dailyAiEvaluations} / {usage.dailyEvaluationsLimit}
                  </span>
                )}
              </div>
              <div className="w-full bg-slate-200/70 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-primary h-2 rounded-full transition-all duration-300"
                  style={{
                    width: isUnlimitedEvals
                      ? '100%'
                      : `${Math.min(100, (usage.dailyAiEvaluations / Math.max(1, usage.dailyEvaluationsLimit)) * 100)}%`,
                  }}
                />
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                Generating CEFR-aligned exercises across all grammar topics & syllabus presets.
              </p>
            </div>

            {/* Metric 2: Token Quota */}
            <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <Cpu className="w-4 h-4 text-violet-600" />
                  <span>Token Quota</span>
                </div>
                {isUnlimitedEvals ? (
                  <span className="text-[11px] font-black text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    Unlimited (∞)
                  </span>
                ) : (
                  <span className="text-xs font-mono font-bold text-slate-900">
                    {(usage.weekTokensUsed || 0).toLocaleString()} / {(usage.tokenLimit || 50000).toLocaleString()}
                  </span>
                )}
              </div>
              <div className="w-full bg-slate-200/70 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-violet-500 h-2 rounded-full transition-all duration-300"
                  style={{
                    width: isUnlimitedEvals
                      ? '100%'
                      : `${Math.min(100, ((usage.weekTokensUsed || 0) / Math.max(1, usage.tokenLimit || 50000)) * 100)}%`,
                  }}
                />
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                Weekly tokens consumed across task generation, grammar diagnostics, and essay grading.
              </p>
            </div>

            {/* Metric 2: AI Evaluations */}
            <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>AI Evaluations</span>
                </div>
                {isUnlimitedEvals ? (
                  <span className="text-[11px] font-black text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    Unlimited (∞)
                  </span>
                ) : (
                  <span className="text-xs font-mono font-bold text-slate-900">
                    {usage.dailyAiEvaluations} / {usage.dailyEvaluationsLimit}
                  </span>
                )}
              </div>
              <div className="w-full bg-slate-200/70 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-emerald-500 h-2 rounded-full transition-all duration-300"
                  style={{
                    width: isUnlimitedEvals
                      ? '100%'
                      : `${Math.min(100, (usage.dailyAiEvaluations / Math.max(1, usage.dailyEvaluationsLimit)) * 100)}%`,
                  }}
                />
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                Sentence breakdowns, essay grading, and diagnostic CEFR feedback.
              </p>
            </div>

            {/* Metric 3: OCR Scans */}
            <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <Camera className="w-4 h-4 text-sky-600" />
                  <span>OCR Scans</span>
                </div>
                {isUnlimitedOcr ? (
                  <span className="text-[11px] font-black text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                    Unlimited (∞)
                  </span>
                ) : (
                  <span className="text-xs font-mono font-bold text-slate-900">
                    {usage.dailyOcrScans} / {usage.dailyOcrLimit}
                  </span>
                )}
              </div>
              <div className="w-full bg-slate-200/70 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-sky-500 h-2 rounded-full transition-all duration-300"
                  style={{
                    width: isUnlimitedOcr
                      ? '100%'
                      : `${Math.min(100, (usage.dailyOcrScans / Math.max(1, usage.dailyOcrLimit)) * 100)}%`,
                  }}
                />
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                Scanning handwritten assignments and photos with zero data retention.
              </p>
            </div>

            {/* Metric 4: Cohort Management */}
            <div className="p-4 rounded-2xl bg-slate-50/70 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-slate-700 uppercase tracking-wider">
                  <GraduationCap className="w-4 h-4 text-purple-600" />
                  <span>Student Cohorts</span>
                </div>
                <span
                  className={`text-[11px] font-black px-2 py-0.5 rounded-md border ${
                    currentTier === 'EDUCATOR'
                      ? 'text-purple-700 bg-purple-50 border-purple-200'
                      : 'text-slate-500 bg-slate-100 border-slate-200'
                  }`}
                >
                  {currentTier === 'EDUCATOR' ? 'Unlimited (∞)' : 'Educator Pro'}
                </span>
              </div>
              <div className="w-full bg-slate-200/70 rounded-full h-2 overflow-hidden">
                <div
                  className="bg-purple-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: currentTier === 'EDUCATOR' ? '100%' : '15%' }}
                />
              </div>
              <p className="text-[11px] text-slate-500 leading-snug">
                {currentTier === 'EDUCATOR'
                  ? 'Unlimited class groups, attempt limits, and PDF/CSV reports.'
                  : 'Upgrade to Educator Pro to unlock cohort management & grading.'}
              </p>
            </div>
          </div>

          <div className="p-4 rounded-2xl bg-indigo-50/60 border border-indigo-100/90 text-xs text-indigo-900 flex items-start gap-3">
            <Info className="w-4 h-4 text-primary shrink-0 mt-0.5" />
            <div className="leading-relaxed">
              <span className="font-bold">Unified AI Quota:</span> Task generation and evaluation share the platform’s high-speed inference pipeline. On <strong>Educator Pro</strong> and <strong>Premium Student</strong> plans, generation and evaluation are completely <strong>unlimited</strong> without daily throttles.
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
