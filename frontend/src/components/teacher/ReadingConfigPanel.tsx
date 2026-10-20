/**
 * @file ReadingConfigPanel.tsx
 * @brief Dedicated configuration interface for Reading Comprehension assignments.
 * Allows educators and students to customize reading title, passage length, paragraph guidelines,
 * custom vocabulary keywords, question mode (Random Mix vs Custom Selection), and question breakdown with point allocations.
 */

import React, { useState } from 'react';
import { BookOpen, Award, CheckSquare, AlignLeft, Hash, Sparkles, Shuffle } from 'lucide-react';
import { CefrLevel } from '../../types/user';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { VocabularySelector } from '../common/VocabularySelector';
import { AutoExpandingTextarea } from '../common/AutoExpandingTextarea';
import { NumberStepper } from '../common/NumberStepper';

export interface ReadingConfig {
  readingTitle: string;
  readingLength: string;
  readingParagraphs: string;
  mcqQuestionsCount: number;
  mcqPoints: number;
  trueFalseQuestionsCount: number;
  trueFalsePoints: number;
  openQuestionsCount: number;
  openPoints: number;
  isRandomMode?: boolean;
}

interface ReadingConfigPanelProps {
  config: ReadingConfig;
  onChange: (updated: Partial<ReadingConfig>) => void;
  cefrLevel: CefrLevel;
  customVocabulary: string;
  onCustomVocabularyChange: (vocab: string) => void;
  savedVocabSets?: CustomCurriculumEntry[];
  isStudent?: boolean;
}

const LENGTH_PRESETS = [
  { id: 'Short (150-250 words)', label: 'Short', words: '150–250 words' },
  { id: 'Medium (250-400 words)', label: 'Medium', words: '250–400 words' },
  { id: 'Long (400-600 words)', label: 'Long', words: '400–600 words' },
  { id: 'Custom', label: 'Custom', words: 'Custom words' },
];

export const ReadingConfigPanel: React.FC<ReadingConfigPanelProps> = ({
  config,
  onChange,
  cefrLevel,
  customVocabulary,
  onCustomVocabularyChange,
  savedVocabSets = [],
  isStudent = false,
}) => {
  const [customWordCount, setCustomWordCount] = useState<number>(300);

  const isRandom = config.isRandomMode ?? false;

  const totalQuestions =
    (config.mcqQuestionsCount || 0) +
    (config.trueFalseQuestionsCount || 0) +
    (config.openQuestionsCount || 0);

  const calculatedTotalPoints =
    (config.mcqQuestionsCount || 0) * (config.mcqPoints || 0) +
    (config.trueFalseQuestionsCount || 0) * (config.trueFalsePoints || 0) +
    (config.openQuestionsCount || 0) * (config.openPoints || 0);

  /**
   * @brief Shuffles a randomized question mix across MCQ, True/False, and Open Questions.
   */
  const handleShuffleRandomMix = () => {
    // Generate a random balanced distribution totaling 4-6 questions
    const randomPool = [
      { mcq: 2, tf: 2, open: 1 },
      { mcq: 3, tf: 2, open: 1 },
      { mcq: 2, tf: 1, open: 2 },
      { mcq: 3, tf: 1, open: 1 },
      { mcq: 1, tf: 2, open: 2 },
    ];
    const picked = randomPool[Math.floor(Math.random() * randomPool.length)];
    onChange({
      isRandomMode: true,
      mcqQuestionsCount: picked.mcq,
      trueFalseQuestionsCount: picked.tf,
      openQuestionsCount: picked.open,
    });
  };

  const handleToggleRandomMode = (enableRandom: boolean) => {
    if (enableRandom) {
      handleShuffleRandomMix();
    } else {
      onChange({ isRandomMode: false });
    }
  };

  return (
    <div className="rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-indigo-50/40 via-white to-violet-50/30 p-6 space-y-6 shadow-xs">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-indigo-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-200 flex-shrink-0">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Reading Practice
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                CEFR {cefrLevel}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Choose your preferred question formats or use a random mix'
                : 'Set passage parameters, question types, and scoring weights'}
            </p>
          </div>
        </div>

        {/* Live Points & Question Count Badge */}
        <div className="flex items-center space-x-3 px-3.5 py-1.5 rounded-2xl bg-white border border-indigo-100 shadow-xs self-start sm:self-auto">
          <div className="flex items-center space-x-1 text-xs">
            <Hash className="w-3.5 h-3.5 text-primary" />
            <span className="text-slate-500 font-semibold">Questions:</span>
            <span className="font-bold text-slate-900">{totalQuestions}</span>
          </div>
          <span className="text-slate-300">|</span>
          <div className="flex items-center space-x-1 text-xs">
            <Award className="w-3.5 h-3.5 text-amber-500" />
            <span className="text-slate-500 font-semibold">Total:</span>
            <span className="font-black text-primary">{calculatedTotalPoints} pts</span>
          </div>
        </div>
      </div>

      {/* Row 1: Assignment Title & Target Vocabulary */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
            Assignment Title
          </label>
          <AutoExpandingTextarea
            value={config.readingTitle}
            onChange={(val) => onChange({ readingTitle: val })}
            minRows={1}
            placeholder="e.g. The Green Transition & Clean Energy"
            className="w-full text-xs font-medium text-slate-800 placeholder:text-slate-400"
          />
          <p className="text-[11px] text-slate-400 mt-1">
            Leave blank to use the selected topic name.
          </p>
        </div>

        <div>
          <VocabularySelector
            customVocabulary={customVocabulary}
            onCustomVocabularyChange={onCustomVocabularyChange}
            savedVocabSets={savedVocabSets}
            label="Target Vocabulary"
            placeholder="e.g. breakthrough, sustainable, mitigation, harness"
            helperText="Words to include in the passage, separated by commas."
          />
        </div>
      </div>

      {/* Row 2: Passage Length & Paragraph Structure */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
            Passage Length
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {LENGTH_PRESETS.map((preset) => {
              const isSelected = config.readingLength === preset.id || (preset.id === 'Custom' && config.readingLength.startsWith('Custom'));
              return (
                <button
                  type="button"
                  key={preset.id}
                  onClick={() => {
                    if (preset.id === 'Custom') {
                      onChange({ readingLength: `Custom (${customWordCount} words)` });
                    } else {
                      onChange({ readingLength: preset.id });
                    }
                  }}
                  className={`py-2 px-2 rounded-xl border text-center transition flex flex-col items-center justify-center ${
                    isSelected
                      ? 'border-primary bg-indigo-50/80 text-primary ring-2 ring-primary/20 shadow-xs'
                      : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                  }`}
                >
                  <span className="text-xs font-bold">{preset.label}</span>
                  <span className="text-[10px] text-slate-400">{preset.words}</span>
                </button>
              );
            })}
          </div>

          {/* Custom Length Input if Custom is selected */}
          {config.readingLength && config.readingLength.startsWith('Custom') && (
            <div className="mt-2.5 p-3 rounded-xl bg-indigo-50/60 border border-indigo-200 flex items-center justify-between text-xs animate-in fade-in duration-200">
              <span className="font-semibold text-slate-700">Target Word Count:</span>
              <div>
                <NumberStepper
                  value={customWordCount}
                  onChange={(val) => {
                    setCustomWordCount(val);
                    onChange({ readingLength: `Custom (${val} words)` });
                  }}
                  min={50}
                  max={1000}
                  step={25}
                  unit="words"
                />
              </div>
            </div>
          )}
        </div>

        <div>
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
            Paragraph Structure
          </label>
          <input
            type="text"
            value={config.readingParagraphs}
            onChange={(e) => onChange({ readingParagraphs: e.target.value })}
            placeholder="e.g. 3 paragraphs: Introduction, Key challenges, Future outlook"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
          />
          <p className="text-[11px] text-slate-400 mt-1">
            Guides the structure and flow of the text.
          </p>
        </div>
      </div>

      {/* Row 3: Question Mode Switcher (Random Mix vs Custom Selection) */}
      <div className="space-y-3 pt-2 border-t border-indigo-100">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
              Question Types & Format
            </label>
            <p className="text-[11px] text-slate-500">
              Choose specific questions or enable random surprise mix across formats
            </p>
          </div>

          {/* Mode Switcher Buttons */}
          <div className="flex items-center space-x-1.5 p-1 bg-slate-100 rounded-2xl border border-slate-200 self-start sm:self-auto">
            <button
              type="button"
              onClick={() => handleToggleRandomMode(true)}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                isRandom
                  ? 'bg-white text-primary shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-primary" />
              <span>Random Mix</span>
            </button>

            <button
              type="button"
              onClick={() => handleToggleRandomMode(false)}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                !isRandom
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <CheckSquare className="w-3.5 h-3.5 text-slate-500" />
              <span>Custom Selection</span>
            </button>
          </div>
        </div>

        {/* Random Mode Active Banner */}
        {isRandom ? (
          <div className="p-4 rounded-2xl border border-indigo-200/90 bg-indigo-50/50 flex flex-col sm:flex-row sm:items-center justify-between gap-3 animate-in fade-in duration-200">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-xl bg-indigo-100 text-primary flex items-center justify-center flex-shrink-0">
                <Sparkles className="w-4 h-4" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-slate-900">
                  Random Question Mix
                </h4>
                <p className="text-[11px] text-slate-500">
                  Mix: {config.mcqQuestionsCount} Multiple Choice, {config.trueFalseQuestionsCount} True/False, {config.openQuestionsCount} Sentence Completion · {totalQuestions} questions
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={handleShuffleRandomMix}
              className="flex items-center space-x-1.5 text-xs font-bold text-primary hover:text-primary-hover bg-white hover:bg-slate-50 px-3 py-1.5 rounded-xl border border-indigo-200 transition shadow-xs self-start sm:self-auto"
            >
              <Shuffle className="w-3.5 h-3.5" />
              <span>Shuffle Mix</span>
            </button>
          </div>
        ) : null}

        {/* When Random Mode is Active: Show distribution summary cards (no confusing manual steppers!) */}
        {isRandom ? (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 animate-in fade-in duration-200">
            <div className="p-3.5 rounded-2xl border border-indigo-100 bg-white shadow-2xs flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-indigo-50 text-primary flex items-center justify-center font-bold text-xs">
                  {config.mcqQuestionsCount}
                </div>
                <div>
                  <div className="text-xs font-bold text-slate-800">Multiple Choice</div>
                  <div className="text-[10px] text-slate-400">4 options per item</div>
                </div>
              </div>
              <span className="text-xs font-mono font-bold text-primary">
                {config.mcqQuestionsCount * config.mcqPoints} pts
              </span>
            </div>

            <div className="p-3.5 rounded-2xl border border-emerald-100 bg-white shadow-2xs flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-bold text-xs">
                  {config.trueFalseQuestionsCount}
                </div>
                <div>
                  <div className="text-xs font-bold text-slate-800">True / False / NG</div>
                  <div className="text-[10px] text-slate-400">Factual verification</div>
                </div>
              </div>
              <span className="text-xs font-mono font-bold text-emerald-700">
                {config.trueFalseQuestionsCount * config.trueFalsePoints} pts
              </span>
            </div>

            <div className="p-3.5 rounded-2xl border border-amber-100 bg-white shadow-2xs flex items-center justify-between">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center font-bold text-xs">
                  {config.openQuestionsCount}
                </div>
                <div>
                  <div className="text-xs font-bold text-slate-800">Sentence Completion</div>
                  <div className="text-[10px] text-slate-400">Open-ended input</div>
                </div>
              </div>
              <span className="text-xs font-mono font-bold text-amber-700">
                {config.openQuestionsCount * config.openPoints} pts
              </span>
            </div>
          </div>
        ) : (
          /* Question Type Cards (Only in Custom Selection mode with interactive inputs) */
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 animate-in fade-in duration-200">
            {/* 1. Multiple Choice */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-7 h-7 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center">
                    <CheckSquare className="w-3.5 h-3.5" />
                  </div>
                  <span className="text-xs font-bold text-slate-800">Multiple Choice</span>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                  4 choices
                </span>
              </div>

              <div className={`grid ${isStudent ? 'grid-cols-1' : 'grid-cols-2'} gap-2 text-xs`}>
                <div>
                  <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                    Questions:
                  </span>
                  <NumberStepper
                    value={config.mcqQuestionsCount}
                    onChange={(val) =>
                      onChange({
                        mcqQuestionsCount: val,
                        isRandomMode: false,
                      })
                    }
                    min={0}
                    max={10}
                    step={1}
                  />
                </div>

                {!isStudent && (
                  <div>
                    <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                      Pts / Question:
                    </span>
                    <NumberStepper
                      value={config.mcqPoints}
                      onChange={(val) => onChange({ mcqPoints: val })}
                      min={1}
                      max={100}
                      step={5}
                      unit="pts"
                    />
                  </div>
                )}
              </div>

              <div className="text-[11px] text-slate-400 font-medium text-right">
                Subtotal: <strong className="text-slate-700">{config.mcqQuestionsCount * config.mcqPoints} pts</strong>
              </div>
            </div>

            {/* 2. True / False */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center">
                    <CheckSquare className="w-3.5 h-3.5" />
                  </div>
                  <span className="text-xs font-bold text-slate-800">True / False</span>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                  T / F / NG
                </span>
              </div>

              <div className={`grid ${isStudent ? 'grid-cols-1' : 'grid-cols-2'} gap-2 text-xs`}>
                <div>
                  <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                    Questions:
                  </span>
                  <NumberStepper
                    value={config.trueFalseQuestionsCount}
                    onChange={(val) =>
                      onChange({
                        trueFalseQuestionsCount: val,
                        isRandomMode: false,
                      })
                    }
                    min={0}
                    max={10}
                    step={1}
                  />
                </div>

                {!isStudent && (
                  <div>
                    <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                      Pts / Question:
                    </span>
                    <NumberStepper
                      value={config.trueFalsePoints}
                      onChange={(val) => onChange({ trueFalsePoints: val })}
                      min={1}
                      max={100}
                      step={5}
                      unit="pts"
                    />
                  </div>
                )}
              </div>

              <div className="text-[11px] text-slate-400 font-medium text-right">
                Subtotal: <strong className="text-slate-700">{config.trueFalseQuestionsCount * config.trueFalsePoints} pts</strong>
              </div>
            </div>

            {/* 3. Sentence Completion / Open Questions */}
            <div className="p-4 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center">
                    <AlignLeft className="w-3.5 h-3.5" />
                  </div>
                  <span className="text-xs font-bold text-slate-800">Sentence Completion</span>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                  Open Input
                </span>
              </div>

              <div className={`grid ${isStudent ? 'grid-cols-1' : 'grid-cols-2'} gap-2 text-xs`}>
                <div>
                  <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                    Questions:
                  </span>
                  <NumberStepper
                    value={config.openQuestionsCount}
                    onChange={(val) =>
                      onChange({
                        openQuestionsCount: val,
                        isRandomMode: false,
                      })
                    }
                    min={0}
                    max={10}
                    step={1}
                  />
                </div>

                {!isStudent && (
                  <div>
                    <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                      Pts / Question:
                    </span>
                    <NumberStepper
                      value={config.openPoints}
                      onChange={(val) => onChange({ openPoints: val })}
                      min={1}
                      max={100}
                      step={5}
                      unit="pts"
                    />
                  </div>
                )}
              </div>

              <div className="text-[11px] text-slate-400 font-medium text-right">
                Subtotal: <strong className="text-slate-700">{config.openQuestionsCount * config.openPoints} pts</strong>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
