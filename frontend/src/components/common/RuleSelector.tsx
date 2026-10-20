/**
 * @file RuleSelector.tsx
 * @brief Reusable component for selecting syllabus grammar rules, user saved rules,
 * and custom rule formulations with optional AI evaluation enforcement notification.
 */

import React, { useState } from 'react';
import { Bookmark, Check, AlertCircle, HelpCircle } from 'lucide-react';
import { CefrLevel } from '../../types/user';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { DEFAULT_CEFR_TOPICS } from '../../constants/topics';
import { AutoExpandingTextarea } from './AutoExpandingTextarea';

export interface RuleSelectorProps {
  cefrLevel: CefrLevel;
  customRule: string;
  onCustomRuleChange: (rule: string) => void;
  savedRules?: CustomCurriculumEntry[];
  label?: string;
  placeholder?: string;
  showAiEnforcementWarning?: boolean;
  className?: string;
}

export const RuleSelector: React.FC<RuleSelectorProps> = ({
  cefrLevel,
  customRule,
  onCustomRuleChange,
  savedRules = [],
  label = 'Target Grammar Rules (Optional)',
  placeholder = "e.g. Third Conditional; Inversion with 'Not only... but also'; Cleft sentences",
  showAiEnforcementWarning = false,
  className = '',
}) => {
  const [showSavedRulesDropdown, setShowSavedRulesDropdown] = useState<boolean>(false);

  /**
   * @brief Appends or sets a rule as target practice focus.
   */
  const handleSelectRule = (ruleText: string) => {
    if (!customRule.trim()) {
      onCustomRuleChange(ruleText);
    } else if (!customRule.includes(ruleText)) {
      onCustomRuleChange(`${customRule}; ${ruleText}`);
    }
  };

  return (
    <div className={`space-y-3 ${className}`}>
      <div className="flex items-center justify-between">
        <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
          <span>{label}</span>
          <span title="The AI will verify if student actively used these grammar rules and adjust score accordingly">
            <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
          </span>
        </label>
        {customRule.trim() && (
          <button
            type="button"
            onClick={() => onCustomRuleChange('')}
            className="text-[11px] font-semibold text-slate-400 hover:text-rose-600 transition"
          >
            Clear Rules
          </button>
        )}
      </div>

      {/* AI Rule Enforcement Warning Banner if requested */}
      {showAiEnforcementWarning && (
        <div className="p-3 bg-amber-50/80 border border-amber-200/90 rounded-2xl flex items-start space-x-2.5 text-xs text-amber-900">
          <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <strong>AI Rule Enforcement:</strong> If you specify target rules below, the AI grading assistant will actively verify whether the student practiced them. If missing, insufficient, or inaccurate, points will be deducted and diagnostic feedback given.
          </div>
        </div>
      )}

      {/* Quick Rule Suggestions from Level Syllabus */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500">Pick from {cefrLevel} Syllabus:</span>
          {savedRules.length > 0 && (
            <button
              type="button"
              onClick={() => setShowSavedRulesDropdown(!showSavedRulesDropdown)}
              className="text-[11px] font-semibold text-primary hover:underline flex items-center space-x-1"
            >
              <Bookmark className="w-3 h-3" />
              <span>Saved Rules ({savedRules.length})</span>
            </button>
          )}
        </div>

        <div className="flex flex-wrap gap-1.5">
          {(DEFAULT_CEFR_TOPICS[cefrLevel] || []).slice(0, 5).map((rule) => {
            const isIncluded = customRule.includes(rule);
            return (
              <button
                key={rule}
                type="button"
                onClick={() => handleSelectRule(rule)}
                className={`text-xs px-2.5 py-1 rounded-xl border transition flex items-center space-x-1 ${
                  isIncluded
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-900 font-bold'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                }`}
              >
                {isIncluded && <Check className="w-3 h-3 text-indigo-600" />}
                <span>{rule}</span>
              </button>
            );
          })}
        </div>

        {showSavedRulesDropdown && savedRules.length > 0 && (
          <div className="p-3 rounded-2xl bg-indigo-50/50 border border-indigo-200/80 space-y-2 mt-2 animate-in fade-in duration-200">
            <span className="text-xs font-bold text-indigo-900 block">Your Saved Rules:</span>
            <div className="flex flex-wrap gap-1.5">
              {savedRules.map((sr) => (
                <button
                  key={sr.id}
                  type="button"
                  onClick={() => handleSelectRule(sr.title)}
                  className="text-xs px-2.5 py-1 rounded-xl bg-white border border-indigo-200 hover:border-indigo-400 text-slate-700 font-medium transition"
                >
                  + {sr.title}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Target Rule Input */}
      <AutoExpandingTextarea
        minRows={1}
        value={customRule}
        onChange={onCustomRuleChange}
        placeholder={placeholder}
        className="w-full text-xs font-medium text-slate-800 placeholder:text-slate-400"
      />
    </div>
  );
};
