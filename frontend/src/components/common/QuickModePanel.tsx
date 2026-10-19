/**
 * @file QuickModePanel.tsx
 * @brief Streamlined, zero-friction task generation panel for rapid student and teacher practice.
 *
 * Allows users to choose only the essential parameters (CEFR level, practice format, and topic),
 * while intelligently randomizing and balancing all fine-grained parameters.
 */

import React, { useState, useEffect } from 'react';
import {
  Zap,
  Dices,
  BookOpen,
  Mic,
  Sliders,
  Sparkles,
  ArrowRight,
  Shuffle,
  Check,
} from 'lucide-react';
import { DifficultyLevel, TaskType } from '../../types/task';
import { CefrLevel } from '../../types/user';
import { CefrBadge } from './CefrBadge';
import { DEFAULT_CEFR_TOPICS, DEFAULT_MIXED_TOPICS, DOMAINS } from '../../constants/topics';

export type QuickPracticeFormat = 'SURPRISE' | 'GRAMMAR' | 'READING' | 'SPEAKING';

export interface QuickGenerateParams {
  taskType: TaskType;
  grammarTopic: string;
  domain: string;
  difficulty: DifficultyLevel;
  numberOfQuestions: number;
  ecoMode: boolean;
  readingConfig?: {
    readingTitle: string;
    readingLength: string;
    readingParagraphs: string;
    mcqQuestionsCount: number;
    mcqPoints: number;
    trueFalseQuestionsCount: number;
    trueFalsePoints: number;
    openQuestionsCount: number;
    openPoints: number;
  };
  speakingConfig?: {
    speakingTitle: string;
    targetSounds: string;
    speakingLength: string;
    pronunciationPackId?: string;
  };
}

interface QuickModePanelProps {
  cefrLevel: CefrLevel;
  onCefrLevelChange: (level: CefrLevel) => void;
  onQuickGenerate: (params: QuickGenerateParams) => void;
  isLoading: boolean;
  isStudent?: boolean;
}

export const QuickModePanel: React.FC<QuickModePanelProps> = ({
  cefrLevel,
  onCefrLevelChange,
  onQuickGenerate,
  isLoading,
  isStudent = false,
}) => {
  const [format, setFormat] = useState<QuickPracticeFormat>('SURPRISE');
  const [topic, setTopic] = useState<string>('');
  const [isCustomTopic, setIsCustomTopic] = useState<boolean>(false);
  const [customTopicInput, setCustomTopicInput] = useState<string>('');

  // Pick a fresh random topic whenever CEFR level changes
  useEffect(() => {
    shuffleTopic();
  }, [cefrLevel]);

  const shuffleTopic = () => {
    const list = [
      ...(DEFAULT_CEFR_TOPICS[cefrLevel] || []),
      ...(DEFAULT_MIXED_TOPICS[cefrLevel] || []),
    ];
    if (list.length > 0) {
      const randomTopic = list[Math.floor(Math.random() * list.length)];
      setTopic(randomTopic);
      setIsCustomTopic(false);
      setCustomTopicInput('');
    }
  };

  const handleFormatChange = (selectedFormat: QuickPracticeFormat) => {
    setFormat(selectedFormat);
  };

  const handleStartPractice = () => {
    const activeTopic = isCustomTopic && customTopicInput.trim()
      ? customTopicInput.trim()
      : topic || DEFAULT_CEFR_TOPICS[cefrLevel]?.[0] || 'General Practice';

    // Pick a random real-world domain context
    const randomDomain = DOMAINS[Math.floor(Math.random() * DOMAINS.length)] || 'Daily Life';

    // Determine final task type
    let chosenTaskType: TaskType;
    if (format === 'SURPRISE') {
      const candidates: TaskType[] = ['MCQ', 'GAP_FILL', 'REWRITE', 'READING', 'SPEAKING'];
      chosenTaskType = candidates[Math.floor(Math.random() * candidates.length)];
    } else if (format === 'GRAMMAR') {
      const grammarTypes: TaskType[] = ['MCQ', 'GAP_FILL', 'OPEN_BRACKETS', 'REWRITE'];
      chosenTaskType = grammarTypes[Math.floor(Math.random() * grammarTypes.length)];
    } else if (format === 'READING') {
      chosenTaskType = 'READING';
    } else {
      chosenTaskType = 'SPEAKING';
    }

    const quickParams: QuickGenerateParams = {
      taskType: chosenTaskType,
      grammarTopic: activeTopic,
      domain: randomDomain,
      difficulty: 'MEDIUM',
      numberOfQuestions: chosenTaskType === 'SPEAKING' ? 1 : 5,
      ecoMode: true, // Quick mode prioritizes rapid generation and low latency
      ...(chosenTaskType === 'READING'
        ? {
            readingConfig: {
              readingTitle: activeTopic,
              readingLength: 'Medium (250-400 words)',
              readingParagraphs: '3 clear paragraphs with narrative flow',
              mcqQuestionsCount: 2,
              mcqPoints: 10,
              trueFalseQuestionsCount: 2,
              trueFalsePoints: 10,
              openQuestionsCount: 1,
              openPoints: 15,
            },
          }
        : {}),
      ...(chosenTaskType === 'SPEAKING'
        ? {
            speakingConfig: {
              speakingTitle: `${activeTopic} · Speech Practice`,
              targetSounds: 'Natural Rhythm, Connected Speech & Clear Articulation',
              speakingLength: 'MEDIUM',
            },
          }
        : {}),
    };

    onQuickGenerate(quickParams);
  };

  const topicSuggestions = (DEFAULT_CEFR_TOPICS[cefrLevel] || []).slice(0, 3);

  return (
    <div className="rounded-3xl border border-amber-200/90 bg-gradient-to-br from-amber-50/40 via-white to-orange-50/30 p-6 sm:p-8 space-y-6 shadow-xs">
      {/* Quick Mode Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-amber-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-amber-500 text-white flex items-center justify-center shadow-md shadow-amber-200 flex-shrink-0">
            <Zap className="w-5 h-5 fill-white" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Quick Practice
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800">
                1-Click Start
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Select your level and skill. All fine details are automatically balanced.'
                : 'Instantly generate an exercise without manual parameter configuration.'}
            </p>
          </div>
        </div>

        <div className="text-[11px] font-semibold text-amber-700 bg-amber-50 border border-amber-200/80 px-3 py-1.5 rounded-xl self-start sm:self-auto flex items-center space-x-1.5">
          <Sparkles className="w-3.5 h-3.5 text-amber-500" />
          <span>Fast Token-Saver Generator</span>
        </div>
      </div>

      {/* 1. Target CEFR Level */}
      <div>
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
          1. Target CEFR Level
        </label>
        <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
          {(['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as CefrLevel[]).map((level) => {
            const isSelected = cefrLevel === level;
            return (
              <button
                type="button"
                key={level}
                onClick={() => onCefrLevelChange(level)}
                className={`py-2.5 px-3 rounded-2xl border flex items-center justify-center transition ${
                  isSelected
                    ? 'border-amber-500 bg-amber-50/80 ring-2 ring-amber-400/20 shadow-sm'
                    : 'border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50'
                }`}
              >
                <CefrBadge level={level} size="md" />
              </button>
            );
          })}
        </div>
      </div>

      {/* 2. Format / Skill Pills */}
      <div>
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
          2. Practice Skill / Format
        </label>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
          <button
            type="button"
            onClick={() => handleFormatChange('SURPRISE')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'SURPRISE'
                ? 'border-amber-500 bg-amber-500 text-white shadow-sm'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black">🎲 Surprise Mix</span>
              {format === 'SURPRISE' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[11px] ${format === 'SURPRISE' ? 'text-amber-100' : 'text-slate-400'}`}>
              Random skill & format
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleFormatChange('GRAMMAR')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'GRAMMAR'
                ? 'border-amber-500 bg-amber-500 text-white shadow-sm'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black flex items-center space-x-1.5">
                <Sliders className="w-3.5 h-3.5" />
                <span>Grammar Quiz</span>
              </span>
              {format === 'GRAMMAR' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[11px] ${format === 'GRAMMAR' ? 'text-amber-100' : 'text-slate-400'}`}>
              MCQ, cloze, or rewrite
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleFormatChange('READING')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'READING'
                ? 'border-amber-500 bg-amber-500 text-white shadow-sm'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black flex items-center space-x-1.5">
                <BookOpen className="w-3.5 h-3.5" />
                <span>Reading Article</span>
              </span>
              {format === 'READING' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[11px] ${format === 'READING' ? 'text-amber-100' : 'text-slate-400'}`}>
              Passage & comprehension
            </span>
          </button>

          <button
            type="button"
            onClick={() => handleFormatChange('SPEAKING')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'SPEAKING'
                ? 'border-amber-500 bg-amber-500 text-white shadow-sm'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black flex items-center space-x-1.5">
                <Mic className="w-3.5 h-3.5" />
                <span>Speaking Drill</span>
              </span>
              {format === 'SPEAKING' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[11px] ${format === 'SPEAKING' ? 'text-amber-100' : 'text-slate-400'}`}>
              Read aloud with mic
            </span>
          </button>
        </div>
      </div>

      {/* 3. Topic Selection & Instant Shuffle */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider">
            3. Practice Topic
          </label>
          <button
            type="button"
            onClick={shuffleTopic}
            className="flex items-center space-x-1.5 text-xs font-bold text-amber-700 hover:text-amber-800 bg-amber-100/70 hover:bg-amber-100 px-3 py-1.5 rounded-xl transition shadow-2xs"
            title="Pick another random syllabus topic"
          >
            <Dices className="w-3.5 h-3.5 text-amber-600" />
            <span>Shuffle Topic</span>
          </button>
        </div>

        {/* Selected Topic Display Banner */}
        <div className="p-3.5 rounded-2xl bg-white border border-amber-200 flex flex-col sm:flex-row sm:items-center justify-between gap-2 shadow-xs">
          <div className="flex items-center space-x-2.5 truncate">
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center flex-shrink-0">
              <Shuffle className="w-3.5 h-3.5" />
            </div>
            <div className="truncate">
              <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">
                Active Topic
              </span>
              <span className="text-xs font-bold text-slate-900 truncate">
                {isCustomTopic && customTopicInput.trim() ? customTopicInput : topic}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => setIsCustomTopic(!isCustomTopic)}
            className="text-[11px] font-bold text-amber-700 hover:underline self-end sm:self-auto"
          >
            {isCustomTopic ? 'Use recommended topic' : 'Type custom topic'}
          </button>
        </div>

        {/* Custom Topic Input if toggled */}
        {isCustomTopic && (
          <input
            type="text"
            value={customTopicInput}
            onChange={(e) => setCustomTopicInput(e.target.value)}
            placeholder="e.g. Mixed Conditionals, Job Interview, Climate Solutions..."
            className="w-full px-3.5 py-2.5 rounded-xl border border-amber-300 bg-white text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 transition animate-in fade-in duration-200"
          />
        )}

        {/* Quick Topic Chips */}
        {!isCustomTopic && topicSuggestions.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 pt-1">
            <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
              Suggestions:
            </span>
            {topicSuggestions.map((sug: string) => (
              <button
                key={sug}
                type="button"
                onClick={() => setTopic(sug)}
                className={`text-[11px] font-medium px-2.5 py-1 rounded-xl border transition ${
                  topic === sug
                    ? 'border-amber-400 bg-amber-100 text-amber-900 font-bold'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                }`}
              >
                {sug}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Start Practice Button */}
      <button
        type="button"
        onClick={handleStartPractice}
        disabled={isLoading}
        className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-white font-black text-sm tracking-wide shadow-lg shadow-amber-500/25 transition flex items-center justify-center space-x-2 disabled:opacity-50"
      >
        <Zap className="w-4 h-4 fill-white" />
        <span>{isLoading ? 'Generating Quick Practice Task...' : 'Start Quick Practice'}</span>
        <ArrowRight className="w-4 h-4" />
      </button>
    </div>
  );
};
