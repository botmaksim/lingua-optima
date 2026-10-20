/**
 * @file EssayConfigPanel.tsx
 * @brief Dedicated configuration interface for Essay Writing tasks and assignments.
 * Supports curated CEFR prompt pools, random prompt shuffle, target grammar rules with AI scoring verification,
 * and modular standard/saved vocabulary integration.
 */

import React from 'react';
import { PenTool, Shuffle, HelpCircle, Award } from 'lucide-react';
import { CefrLevel } from '../../types/user';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { DEFAULT_ESSAY_TOPICS, DOMAINS } from '../../constants/topics';
import { VocabularySelector } from '../common/VocabularySelector';
import { RuleSelector } from '../common/RuleSelector';
import { AutoExpandingTextarea } from '../common/AutoExpandingTextarea';
import { NumberStepper } from '../common/NumberStepper';

export interface EssayConfigPanelProps {
  cefrLevel: CefrLevel;
  topic: string;
  onTopicChange: (topic: string) => void;
  domain: string;
  onDomainChange: (domain: string) => void;
  customRule: string;
  onCustomRuleChange: (rule: string) => void;
  customVocabulary: string;
  onCustomVocabularyChange: (vocab: string) => void;
  totalPoints?: number;
  onTotalPointsChange?: (points: number) => void;
  savedRules?: CustomCurriculumEntry[];
  savedVocabSets?: CustomCurriculumEntry[];
  isStudent?: boolean;
}

export const EssayConfigPanel: React.FC<EssayConfigPanelProps> = ({
  cefrLevel,
  topic,
  onTopicChange,
  domain,
  onDomainChange,
  customRule,
  onCustomRuleChange,
  customVocabulary,
  onCustomVocabularyChange,
  totalPoints = 100,
  onTotalPointsChange,
  savedRules = [],
  savedVocabSets = [],
  isStudent = false,
}) => {
  // Recommended word count ranges based on CEFR benchmark
  const wordCountGuideline = React.useMemo(() => {
    switch (cefrLevel) {
      case 'A1':
        return '60–100 words (Simple sentences & routines)';
      case 'A2':
        return '100–150 words (Personal narratives & descriptions)';
      case 'B1':
        return '150–220 words (Opinion & structured paragraphs)';
      case 'B2':
        return '250–350 words (Discursive & balanced arguments)';
      case 'C1':
        return '350–450 words (Complex thesis & nuanced stance)';
      case 'C2':
        return '450–600 words (Academic rigor & stylistic mastery)';
      default:
        return '250–350 words';
    }
  }, [cefrLevel]);

  /**
   * @brief Shuffles a random curated essay prompt from the level pool.
   */
  const handleShuffleTopic = () => {
    const pool = DEFAULT_ESSAY_TOPICS[cefrLevel] || DEFAULT_ESSAY_TOPICS.B2;
    const filtered = pool.filter((t) => t !== topic);
    const selected = (filtered.length > 0 ? filtered : pool)[
      Math.floor(Math.random() * (filtered.length > 0 ? filtered.length : pool.length))
    ];
    onTopicChange(selected);
  };

  return (
    <div className="rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-indigo-50/40 via-white to-violet-50/30 p-6 space-y-6 shadow-xs animate-in fade-in duration-200">
      {/* Module Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-indigo-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-200 flex-shrink-0">
            <PenTool className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Essay Studio & Discursive Writing
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                CEFR {cefrLevel}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Craft essays with real-time word counting, target rules, and IELTS/CEFR 4-pillar rubric grading'
                : 'Configure essay topic, target grammar rules for AI verification, and vocabulary requirements'}
            </p>
          </div>
        </div>

        {/* Guideline Pill */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-2xl bg-white border border-indigo-100 shadow-xs self-start sm:self-auto text-xs font-semibold text-slate-700">
          <Award className="w-3.5 h-3.5 text-amber-500" />
          <span>Rubric: 4 Criteria (10 pts) · {totalPoints} pts total</span>
        </div>
      </div>

      {/* 1. Essay Title & Topic Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
            <span>Essay Topic & Writing Prompt</span>
            <span title="Pick a curated CEFR discursive question or write your own custom assignment title">
              <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
            </span>
          </label>
          <button
            type="button"
            onClick={handleShuffleTopic}
            className="flex items-center space-x-1.5 text-xs font-bold text-primary hover:text-primary-hover bg-indigo-50 hover:bg-indigo-100 px-3 py-1 rounded-xl transition cursor-pointer"
          >
            <Shuffle className="w-3.5 h-3.5" />
            <span>Shuffle Prompt</span>
          </button>
        </div>

        {/* Curated Prompt Suggestions Chips */}
        <div className="flex flex-wrap gap-1.5">
          {(DEFAULT_ESSAY_TOPICS[cefrLevel] || []).map((preset) => {
            const isSelected = topic === preset;
            return (
              <button
                key={preset}
                type="button"
                onClick={() => onTopicChange(preset)}
                className={`text-left text-xs px-3 py-1.5 rounded-xl border transition cursor-pointer ${
                  isSelected
                    ? 'border-primary bg-indigo-50 text-primary font-bold shadow-2xs'
                    : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                }`}
              >
                {preset}
              </button>
            );
          })}
        </div>

        {/* Custom Editable Topic Input */}
        <div>
          <AutoExpandingTextarea
            minRows={2}
            value={topic}
            onChange={onTopicChange}
            placeholder="e.g. Discuss the advantages and disadvantages of remote work..."
            className="w-full text-xs font-medium text-slate-800 placeholder:text-slate-400"
          />
          <div className="flex flex-wrap items-center justify-between gap-2 mt-1.5 text-[11px] text-slate-400">
            <span>Recommended Length: <strong className="text-slate-600">{wordCountGuideline}</strong></span>
            <span>You can freely edit the prompt above.</span>
          </div>
        </div>

        {/* Domain Context Pills */}
        <div className="pt-1 flex flex-wrap items-center gap-1.5">
          <span className="text-[11px] font-semibold text-slate-500 mr-1">Domain:</span>
          {DOMAINS.map((d) => {
            const isSelected = domain === d;
            return (
              <button
                key={d}
                type="button"
                onClick={() => onDomainChange(d)}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold border transition cursor-pointer ${
                  isSelected
                    ? 'border-indigo-500 bg-indigo-50 text-indigo-700 shadow-2xs'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                }`}
              >
                {d}
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Target Grammar Rules for Practice (AI-Verified) */}
      <div className="pt-3 border-t border-indigo-100">
        <RuleSelector
          cefrLevel={cefrLevel}
          customRule={customRule}
          onCustomRuleChange={onCustomRuleChange}
          savedRules={savedRules}
          showAiEnforcementWarning={true}
          label="Target Grammar Rules for Practice (AI-Verified)"
        />
      </div>

      {/* 3. Target Vocabulary Requirements */}
      <div className="pt-3 border-t border-indigo-100">
        <VocabularySelector
          customVocabulary={customVocabulary}
          onCustomVocabularyChange={onCustomVocabularyChange}
          savedVocabSets={savedVocabSets}
          label="Target Vocabulary & Lexical Sets (Optional)"
          placeholder="e.g. furthermore, paradigm shift, mitigation, corroborate, feasibility"
          helperText="Separate words with commas. The AI writing tutor will encourage usage in paragraphs."
        />
      </div>

      {/* 4. Total Points (For Educators) */}
      {!isStudent && onTotalPointsChange && (
        <div className="pt-3 border-t border-indigo-100 flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-slate-700 uppercase tracking-wider block">
              Grading Points Total
            </span>
            <span className="text-[11px] text-slate-400">
              Allocated score for this essay assignment in gradebook
            </span>
          </div>
          <div>
            <NumberStepper
              value={totalPoints}
              onChange={onTotalPointsChange}
              min={10}
              max={100}
              step={5}
              unit="pts"
            />
          </div>
        </div>
      )}
    </div>
  );
};
