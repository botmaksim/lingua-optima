/**
 * @file ReadingConfigPanel.tsx
 * @brief Dedicated configuration interface for Reading Comprehension assignments.
 * Allows teachers to customize reading title, passage length, paragraph guidelines,
 * CEFR vocabulary keywords, and question breakdown (MCQ, True/False, Completion) with point allocations.
 */

import React from 'react';
import { BookOpen, Award, CheckSquare, AlignLeft, Hash } from 'lucide-react';
import { CefrLevel } from '../../types/user';

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
}

interface ReadingConfigPanelProps {
  config: ReadingConfig;
  onChange: (updated: Partial<ReadingConfig>) => void;
  cefrLevel: CefrLevel;
  customVocabulary: string;
  onCustomVocabularyChange: (vocab: string) => void;
}

const LENGTH_PRESETS = [
  { id: 'Short (150-250 words)', label: 'Short', words: '150–250 words' },
  { id: 'Medium (250-400 words)', label: 'Medium', words: '250–400 words' },
  { id: 'Long (400-600 words)', label: 'Long', words: '400–600 words' },
];

export const ReadingConfigPanel: React.FC<ReadingConfigPanelProps> = ({
  config,
  onChange,
  cefrLevel,
  customVocabulary,
  onCustomVocabularyChange,
}) => {
  const totalQuestions =
    (config.mcqQuestionsCount || 0) +
    (config.trueFalseQuestionsCount || 0) +
    (config.openQuestionsCount || 0);

  const calculatedTotalPoints =
    (config.mcqQuestionsCount || 0) * (config.mcqPoints || 0) +
    (config.trueFalseQuestionsCount || 0) * (config.trueFalsePoints || 0) +
    (config.openQuestionsCount || 0) * (config.openPoints || 0);

  return (
    <div className="rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-indigo-50/40 via-white to-violet-50/30 p-6 space-y-6 shadow-xs">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-indigo-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-200">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Reading Comprehension Studio
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                CEFR {cefrLevel}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              Configure reading passage parameters and question distribution
            </p>
          </div>
        </div>

        {/* Live Points Badge */}
        <div className="flex items-center space-x-3 px-3.5 py-1.5 rounded-2xl bg-white border border-indigo-100 shadow-xs">
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
          <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
            Reading Assignment Title (Optional)
          </label>
          <input
            type="text"
            value={config.readingTitle}
            onChange={(e) => onChange({ readingTitle: e.target.value })}
            placeholder="e.g. The Green Transition & Clean Energy"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
          />
          <p className="text-[11px] text-slate-400 mt-1">
            Leave blank to automatically derive from the grammar/thematic topic.
          </p>
        </div>

        <div>
          <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
            Target Vocabulary & Collocations (Optional)
          </label>
          <input
            type="text"
            value={customVocabulary}
            onChange={(e) => onCustomVocabularyChange(e.target.value)}
            placeholder="e.g. breakthrough, sustainable, mitigation, harness"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
          />
          <p className="text-[11px] text-slate-400 mt-1">
            Comma-separated words that the neural network will weave into the passage.
          </p>
        </div>
      </div>

      {/* Row 2: Passage Length & Paragraph Structure */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
            Passage Length / Volume
          </label>
          <div className="grid grid-cols-3 gap-2">
            {LENGTH_PRESETS.map((preset) => (
              <button
                type="button"
                key={preset.id}
                onClick={() => onChange({ readingLength: preset.id })}
                className={`py-2 px-2 rounded-xl border text-center transition flex flex-col items-center justify-center ${
                  config.readingLength === preset.id
                    ? 'border-primary bg-indigo-50/80 text-primary shadow-xs'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                }`}
              >
                <span className="text-xs font-bold">{preset.label}</span>
                <span className="text-[10px] text-slate-400">{preset.words}</span>
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-1.5">
            Paragraph Structure & Narrative Focus
          </label>
          <input
            type="text"
            value={config.readingParagraphs}
            onChange={(e) => onChange({ readingParagraphs: e.target.value })}
            placeholder="e.g. 3 paragraphs: Introduction, Key challenges, Future outlook"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
          />
          <p className="text-[11px] text-slate-400 mt-1">
            Guides the structure and flow of the generated text paragraphs.
          </p>
        </div>
      </div>

      {/* Row 3: Questions Breakdown and Points Configuration */}
      <div className="space-y-3 pt-2 border-t border-indigo-100">
        <div className="flex items-center justify-between">
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
            Questions Configuration & Point Distribution
          </label>
          <span className="text-[11px] text-slate-500 font-medium">
            Customize question types, counts, and point weights
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
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

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Questions:
                </span>
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={config.mcqQuestionsCount}
                  onChange={(e) =>
                    onChange({ mcqQuestionsCount: Math.max(0, parseInt(e.target.value) || 0) })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-slate-800 text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Pts / Question:
                </span>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={config.mcqPoints}
                  onChange={(e) =>
                    onChange({ mcqPoints: Math.max(1, parseInt(e.target.value) || 1) })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-primary text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
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

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Questions:
                </span>
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={config.trueFalseQuestionsCount}
                  onChange={(e) =>
                    onChange({
                      trueFalseQuestionsCount: Math.max(0, parseInt(e.target.value) || 0),
                    })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-slate-800 text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Pts / Question:
                </span>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={config.trueFalsePoints}
                  onChange={(e) =>
                    onChange({ trueFalsePoints: Math.max(1, parseInt(e.target.value) || 1) })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-primary text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <div className="text-[11px] text-slate-400 font-medium text-right">
              Subtotal: <strong className="text-slate-700">{config.trueFalseQuestionsCount * config.trueFalsePoints} pts</strong>
            </div>
          </div>

          {/* 3. Sentence Completion / Open */}
          <div className="p-4 rounded-2xl border border-slate-200 bg-white shadow-xs space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <div className="w-7 h-7 rounded-lg bg-violet-100 text-violet-700 flex items-center justify-center">
                  <AlignLeft className="w-3.5 h-3.5" />
                </div>
                <span className="text-xs font-bold text-slate-800">Sentence Completion</span>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                Open-ended
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Questions:
                </span>
                <input
                  type="number"
                  min="0"
                  max="10"
                  value={config.openQuestionsCount}
                  onChange={(e) =>
                    onChange({ openQuestionsCount: Math.max(0, parseInt(e.target.value) || 0) })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-slate-800 text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
              <div>
                <span className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Pts / Question:
                </span>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={config.openPoints}
                  onChange={(e) =>
                    onChange({ openPoints: Math.max(1, parseInt(e.target.value) || 1) })
                  }
                  className="w-full px-2.5 py-1.5 rounded-lg border border-slate-200 font-bold text-primary text-center focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>

            <div className="text-[11px] text-slate-400 font-medium text-right">
              Subtotal: <strong className="text-slate-700">{config.openQuestionsCount * config.openPoints} pts</strong>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
