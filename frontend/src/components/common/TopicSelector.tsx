/**
 * @file TopicSelector.tsx
 * @brief Reusable unified topic and domain selector component with core syllabus presets, mixed challenges, and custom topic entry.
 */

import React, { useState, useEffect, useRef } from 'react';
import { Edit3, List, BookOpen } from 'lucide-react';
import { CustomSelect, SelectOptionGroup } from './CustomSelect';
import { CefrLevel } from '../../types/user';
import { TopicsCatalogResponse } from '../../types/task';
import { DEFAULT_CEFR_TOPICS, DEFAULT_MIXED_TOPICS, DOMAINS } from '../../constants/topics';

/**
 * @brief Props for the TopicSelector component.
 */
export interface TopicSelectorProps {
  /** @brief Target CEFR proficiency level. */
  cefrLevel: CefrLevel;
  /** @brief Currently selected or entered grammar topic. */
  topic: string;
  /** @brief Callback invoked when grammar topic changes. */
  onTopicChange: (topic: string) => void;
  /** @brief Optional vocabulary domain. */
  domain?: string;
  /** @brief Callback invoked when domain changes. */
  onDomainChange?: (domain: string) => void;
  /** @brief Dynamic topics catalog fetched from backend API. */
  catalog?: TopicsCatalogResponse | null;
  /** @brief Whether to render the grammar topic selector. Defaults to true. */
  showTopic?: boolean;
  /** @brief Whether to render the vocabulary domain selector. Defaults to true. */
  showDomain?: boolean;
  /** @brief Custom label for the topic section. */
  topicLabel?: string;
  /** @brief Custom label for the domain section. */
  domainLabel?: string;
  /** @brief Size variant for the select dropdowns. */
  size?: 'sm' | 'md' | 'lg';
  /** @brief Additional container classes. */
  className?: string;
}

const CUSTOM_TOPIC_TOKEN = '__CUSTOM__';
const CUSTOM_DOMAIN_TOKEN = '__CUSTOM_DOMAIN__';

/**
 * @brief Reusable component for choosing syllabus grammar topics, mixed challenges, or custom topics.
 * @return JSX topic and domain selection UI.
 */
export const TopicSelector: React.FC<TopicSelectorProps> = ({
  cefrLevel,
  topic,
  onTopicChange,
  domain,
  onDomainChange,
  catalog,
  showTopic = true,
  showDomain = true,
  topicLabel = 'Practice Topic & Mixed Challenges',
  domainLabel = 'Vocabulary Domain',
  size = 'md',
  className = '',
}) => {
  const currentLevelTopics =
    catalog?.topicsByLevel?.[cefrLevel] || DEFAULT_CEFR_TOPICS[cefrLevel] || [];
  const currentLevelMixedTopics =
    catalog?.mixedTopicsByLevel?.[cefrLevel] || DEFAULT_MIXED_TOPICS[cefrLevel] || [];
  const crossLevelTopics = catalog?.crossLevelTopics || [];

  // Determine if initial topic is a known preset or custom
  const allKnownPresets = [
    ...currentLevelTopics,
    ...currentLevelMixedTopics,
    ...crossLevelTopics,
  ];

  const [isCustomTopic, setIsCustomTopic] = useState<boolean>(() => {
    return topic ? !allKnownPresets.includes(topic) : false;
  });

  const [customTopicInput, setCustomTopicInput] = useState<string>(() => {
    return allKnownPresets.includes(topic) ? '' : topic;
  });

  const [isCustomDomain, setIsCustomDomain] = useState<boolean>(() => {
    return domain ? !DOMAINS.includes(domain) : false;
  });

  const [customDomainInput, setCustomDomainInput] = useState<string>(() => {
    return domain && !DOMAINS.includes(domain) ? domain : '';
  });

  const lastEmittedTopicRef = useRef<string>(topic);
  const lastEmittedDomainRef = useRef<string | undefined>(domain);
  const prevCefrLevelRef = useRef<CefrLevel>(cefrLevel);

  // Synchronize when topic prop changes externally
  useEffect(() => {
    if (topic !== lastEmittedTopicRef.current) {
      lastEmittedTopicRef.current = topic;
      if (allKnownPresets.includes(topic)) {
        setIsCustomTopic(false);
      } else {
        setIsCustomTopic(true);
        setCustomTopicInput(topic);
      }
    }
  }, [topic, allKnownPresets]);

  // Synchronize when domain prop changes externally
  useEffect(() => {
    if (domain !== undefined && domain !== lastEmittedDomainRef.current) {
      lastEmittedDomainRef.current = domain;
      if (DOMAINS.includes(domain)) {
        setIsCustomDomain(false);
      } else {
        setIsCustomDomain(true);
        setCustomDomainInput(domain);
      }
    }
  }, [domain]);

  // When CEFR level changes and not in custom mode, update topic to first preset if current is not in level
  useEffect(() => {
    if (prevCefrLevelRef.current !== cefrLevel) {
      prevCefrLevelRef.current = cefrLevel;
      if (!isCustomTopic) {
        const inCurrentLevel =
          currentLevelTopics.includes(topic) ||
          currentLevelMixedTopics.includes(topic) ||
          crossLevelTopics.includes(topic);
        if (!inCurrentLevel && currentLevelTopics.length > 0) {
          const fallback = currentLevelTopics[0];
          lastEmittedTopicRef.current = fallback;
          onTopicChange(fallback);
        }
      }
    }
  }, [cefrLevel, isCustomTopic, currentLevelTopics, currentLevelMixedTopics, crossLevelTopics, topic, onTopicChange]);

  /**
   * @brief Switches between syllabus dropdown mode and custom topic input.
   */
  const handleToggleMode = () => {
    if (isCustomTopic) {
      setIsCustomTopic(false);
      const fallback = currentLevelTopics[0] || 'Present Simple';
      lastEmittedTopicRef.current = fallback;
      onTopicChange(fallback);
    } else {
      setIsCustomTopic(true);
      const textToUse = customTopicInput || topic || 'Custom Grammar Topic';
      setCustomTopicInput(textToUse);
      lastEmittedTopicRef.current = textToUse;
      onTopicChange(textToUse);
    }
  };

  /**
   * @brief Handles dropdown change in syllabus mode.
   */
  const handleSelectTopic = (val: string) => {
    if (val === CUSTOM_TOPIC_TOKEN) {
      setIsCustomTopic(true);
      const initialCustom = customTopicInput || topic || 'Custom Grammar Topic';
      setCustomTopicInput(initialCustom);
      lastEmittedTopicRef.current = initialCustom;
      onTopicChange(initialCustom);
    } else {
      lastEmittedTopicRef.current = val;
      onTopicChange(val);
    }
  };

  /**
   * @brief Handles changes in custom topic text input.
   */
  const handleCustomTopicChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setCustomTopicInput(val);
    lastEmittedTopicRef.current = val;
    onTopicChange(val);
  };

  /**
   * @brief Handles domain dropdown selection.
   */
  const handleSelectDomain = (val: string) => {
    if (!onDomainChange) return;
    if (val === CUSTOM_DOMAIN_TOKEN) {
      setIsCustomDomain(true);
      const initial = customDomainInput || 'General';
      setCustomDomainInput(initial);
      lastEmittedDomainRef.current = initial;
      onDomainChange(initial);
    } else {
      setIsCustomDomain(false);
      lastEmittedDomainRef.current = val;
      onDomainChange(val);
    }
  };

  /**
   * @brief Handles custom domain text input change.
   */
  const handleCustomDomainChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setCustomDomainInput(val);
    if (onDomainChange) {
      lastEmittedDomainRef.current = val;
      onDomainChange(val);
    }
  };

  // Build grouped options for CustomSelect
  const topicGroups: SelectOptionGroup[] = [
    {
      label: `Core Syllabus (CEFR ${cefrLevel})`,
      options: currentLevelTopics.map((t) => ({
        value: t,
        label: t,
        badge: cefrLevel,
      })),
    },
    ...(currentLevelMixedTopics.length > 0
      ? [
          {
            label: `🔀 Mixed Challenges (CEFR ${cefrLevel})`,
            options: currentLevelMixedTopics.map((t) => ({
              value: t,
              label: t,
              badge: 'Mixed',
            })),
          },
        ]
      : []),
    ...(crossLevelTopics.length > 0
      ? [
          {
            label: '🌐 Cross-Level & Thematic Challenges',
            options: crossLevelTopics.map((t) => ({
              value: t,
              label: t,
              badge: 'Cross-Level',
            })),
          },
        ]
      : []),
    {
      label: 'Custom Topic',
      options: [
        {
          value: CUSTOM_TOPIC_TOKEN,
          label: '✏️ Enter Custom Topic / Rule...',
          badge: 'Custom',
        },
      ],
    },
  ];

  const domainGroups: SelectOptionGroup[] = [
    {
      label: 'Standard Domains',
      options: DOMAINS.map((d) => ({
        value: d,
        label: d,
      })),
    },
    {
      label: 'Custom Domain',
      options: [
        {
          value: CUSTOM_DOMAIN_TOKEN,
          label: '✏️ Custom Vocabulary Domain...',
          badge: 'Custom',
        },
      ],
    },
  ];

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Grammar Topic Section */}
      {showTopic && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-primary" />
              <span>{topicLabel}</span>
            </label>
            <button
              type="button"
              onClick={handleToggleMode}
              className="text-xs font-semibold text-primary hover:text-primary-hover transition flex items-center space-x-1"
            >
              {isCustomTopic ? (
                <>
                  <List className="w-3.5 h-3.5" />
                  <span>← Switch to Syllabus Presets</span>
                </>
              ) : (
                <>
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>✏️ Enter Custom Topic</span>
                </>
              )}
            </button>
          </div>

          {!isCustomTopic ? (
            <CustomSelect
              size={size}
              value={allKnownPresets.includes(topic) ? topic : currentLevelTopics[0] || ''}
              onChange={handleSelectTopic}
              placeholder="💡 Pick from Syllabus Presets & Mixed Challenges..."
              ariaLabel={topicLabel}
              groups={topicGroups}
            />
          ) : (
            <div className="space-y-2 animate-in fade-in duration-200">
              <input
                type="text"
                value={customTopicInput}
                onChange={handleCustomTopicChange}
                placeholder="e.g. Passive Voice in Technical Reports, Mixed Conditionals in Legal Contracts..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-indigo-200 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-medium"
                autoFocus
              />
              <p className="text-[11px] text-slate-500">
                💡 Tip: Enter any custom topic, specialized grammar structure, or thematic scenario.
              </p>
            </div>
          )}
        </div>
      )}

      {/* Vocabulary Domain Section */}
      {showDomain && onDomainChange && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              {domainLabel}
            </label>
            <button
              type="button"
              onClick={() => {
                if (isCustomDomain) {
                  setIsCustomDomain(false);
                  onDomainChange(DOMAINS[0]);
                } else {
                  setIsCustomDomain(true);
                  const initial = customDomainInput || 'General';
                  setCustomDomainInput(initial);
                  onDomainChange(initial);
                }
              }}
              className="text-xs font-semibold text-primary hover:text-primary-hover transition flex items-center space-x-1"
            >
              <span>{isCustomDomain ? '← Standard Domains' : '✏️ Custom Domain'}</span>
            </button>
          </div>

          {!isCustomDomain ? (
            <CustomSelect
              size={size}
              value={domain && DOMAINS.includes(domain) ? domain : DOMAINS[0]}
              onChange={handleSelectDomain}
              placeholder="Select Vocabulary Domain..."
              ariaLabel={domainLabel}
              groups={domainGroups}
            />
          ) : (
            <input
              type="text"
              value={customDomainInput}
              onChange={handleCustomDomainChange}
              placeholder="e.g. Maritime Law, Biotechnology, Aviation English, Startup Tech..."
              className="w-full px-3.5 py-2.5 rounded-xl border border-indigo-200 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-medium"
              autoFocus
            />
          )}
        </div>
      )}
    </div>
  );
};
