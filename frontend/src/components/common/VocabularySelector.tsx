/**
 * @file VocabularySelector.tsx
 * @brief Reusable component for selecting standard CEFR vocabulary presets, saved user sets,
 * and entering custom vocabulary words or collocations.
 */

import React, { useState } from 'react';
import { Sparkles, Bookmark, HelpCircle } from 'lucide-react';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { STANDARD_VOCABULARY_PRESETS, StandardVocabularyPreset } from '../../constants/topics';

export interface VocabularySelectorProps {
  customVocabulary: string;
  onCustomVocabularyChange: (vocab: string) => void;
  savedVocabSets?: CustomCurriculumEntry[];
  label?: string;
  placeholder?: string;
  helperText?: string;
  className?: string;
}

export const VocabularySelector: React.FC<VocabularySelectorProps> = ({
  customVocabulary,
  onCustomVocabularyChange,
  savedVocabSets = [],
  label = 'Target Vocabulary & Lexical Sets',
  placeholder = 'e.g. breakthrough, sustainable, mitigation, harness, paradigm shift',
  helperText = 'Separate words with commas. These will be woven into the passage or practice guidelines.',
  className = '',
}) => {
  const [showSavedVocabDropdown, setShowSavedVocabDropdown] = useState<boolean>(false);

  /**
   * @brief Appends selected preset vocabulary words to the active vocabulary string.
   */
  const handleAddPresetVocabulary = (preset: StandardVocabularyPreset) => {
    const existing = customVocabulary
      .split(',')
      .map((w) => w.trim().toLowerCase())
      .filter(Boolean);
    const newWords = preset.words.filter((w) => !existing.includes(w.toLowerCase()));
    if (newWords.length === 0) return;

    const merged = existing.length > 0 ? `${customVocabulary}, ${newWords.join(', ')}` : newWords.join(', ');
    onCustomVocabularyChange(merged);
  };

  /**
   * @brief Appends a saved user vocabulary set.
   */
  const handleAddSavedSet = (set: CustomCurriculumEntry) => {
    const words = set.content || set.title;
    const merged = customVocabulary.trim() ? `${customVocabulary}, ${words}` : words;
    onCustomVocabularyChange(merged);
  };

  return (
    <div className={`space-y-2 ${className}`}>
      <div className="flex items-center justify-between">
        <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
          <span>{label}</span>
          <span title="Click standard presets or saved sets to quickly insert relevant vocabulary">
            <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
          </span>
        </label>
        {customVocabulary.trim() && (
          <button
            type="button"
            onClick={() => onCustomVocabularyChange('')}
            className="text-[11px] font-semibold text-slate-400 hover:text-rose-600 transition"
          >
            Clear
          </button>
        )}
      </div>

      {/* Preset Chips and Saved Sets Link */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <span className="text-[11px] font-bold text-slate-500">Standard Lexical Presets:</span>
          {savedVocabSets.length > 0 && (
            <button
              type="button"
              onClick={() => setShowSavedVocabDropdown(!showSavedVocabDropdown)}
              className="text-[11px] font-semibold text-primary hover:underline flex items-center space-x-1"
            >
              <Bookmark className="w-3 h-3" />
              <span>Saved Sets ({savedVocabSets.length})</span>
            </button>
          )}
        </div>

        <div className="flex flex-wrap gap-1">
          {STANDARD_VOCABULARY_PRESETS.map((preset) => (
            <button
              key={preset.id}
              type="button"
              onClick={() => handleAddPresetVocabulary(preset)}
              className="text-[11px] px-2.5 py-1 rounded-xl border border-slate-200 bg-white hover:border-indigo-300 hover:bg-indigo-50/50 text-slate-700 font-medium transition flex items-center space-x-1"
              title={`Words: ${preset.words.join(', ')}`}
            >
              <Sparkles className="w-2.5 h-2.5 text-amber-500" />
              <span>{preset.title}</span>
            </button>
          ))}
        </div>

        {showSavedVocabDropdown && savedVocabSets.length > 0 && (
          <div className="p-3 rounded-2xl bg-indigo-50/60 border border-indigo-200 text-xs space-y-1.5 animate-in fade-in duration-200">
            <span className="text-[11px] font-bold text-indigo-900 block">Your Saved Vocabulary:</span>
            <div className="flex flex-wrap gap-1">
              {savedVocabSets.map((sv) => (
                <button
                  key={sv.id}
                  type="button"
                  onClick={() => handleAddSavedSet(sv)}
                  className="text-[11px] px-2 py-0.5 rounded-lg bg-white border border-indigo-200 hover:border-indigo-400 text-slate-700 transition"
                >
                  + {sv.title}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Custom Vocabulary Input */}
      <input
        type="text"
        value={customVocabulary}
        onChange={(e) => onCustomVocabularyChange(e.target.value)}
        placeholder={placeholder}
        className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
      />
      {helperText && <p className="text-[11px] text-slate-400">{helperText}</p>}
    </div>
  );
};
