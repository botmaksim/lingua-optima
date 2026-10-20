/**
 * @file QuickModePanel.tsx
 * @brief 1-Click Express Practice generator supporting multi-modal recommendation across
 * Grammar, Reading, Speaking, and Essay Writing, with intelligent learner weakness detection.
 */

import React, { useState, useEffect, useMemo } from 'react';
import {
  Zap,
  Sparkles,
  BookOpen,
  Mic,
  Sliders,
  Dices,
  Target,
  PenTool,
  Check,
  ArrowRight,
} from 'lucide-react';
import { DifficultyLevel, TaskType } from '../../types/task';
import { CefrLevel } from '../../types/user';
import { ProgressRecord } from '../../types/progress';
import { SubmissionResult } from '../../types/submission';
import { progressApi } from '../../api/progressApi';
import { submissionApi } from '../../api/submissionApi';
import { CefrBadge } from './CefrBadge';
import {
  DEFAULT_CEFR_TOPICS,
  DEFAULT_ESSAY_TOPICS,
  DEFAULT_MIXED_TOPICS,
  DOMAINS,
} from '../../constants/topics';
import { AutoExpandingTextarea } from './AutoExpandingTextarea';

export type QuickPracticeFormat = 'RECOMMENDED' | 'SURPRISE' | 'GRAMMAR' | 'ESSAY' | 'READING' | 'SPEAKING';

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
  essayConfig?: {
    topic: string;
    domain?: string;
    customRule?: string;
    customVocabulary?: string;
  };
}

interface QuickModePanelProps {
  cefrLevel: CefrLevel;
  onCefrLevelChange: (level: CefrLevel) => void;
  onQuickGenerate: (params: QuickGenerateParams) => void;
  isLoading: boolean;
  isStudent?: boolean;
  activeTopic?: string;
  onActiveTopicChange?: (topic: string) => void;
  activeFormat?: QuickPracticeFormat;
  onActiveFormatChange?: (format: QuickPracticeFormat) => void;
  onSwitchToDetailed?: () => void;
}

interface SkillWeakness {
  modality: 'GRAMMAR' | 'READING' | 'SPEAKING' | 'ESSAY';
  score: number;
  reason: string;
  suggestedTopic: string;
}

export const QuickModePanel: React.FC<QuickModePanelProps> = ({
  cefrLevel,
  onCefrLevelChange,
  onQuickGenerate,
  isLoading,
  isStudent = false,
  activeTopic,
  onActiveTopicChange,
  activeFormat,
  onActiveFormatChange,
  onSwitchToDetailed,
}) => {
  const [internalFormat, setInternalFormat] = useState<QuickPracticeFormat>('RECOMMENDED');
  const format = activeFormat ?? internalFormat;
  const setFormat = (f: QuickPracticeFormat) => {
    setInternalFormat(f);
    onActiveFormatChange?.(f);
  };

  const [internalTopic, setInternalTopic] = useState<string>('');
  const topic = activeTopic !== undefined ? activeTopic : internalTopic;
  const setTopic = (t: string) => {
    setInternalTopic(t);
    onActiveTopicChange?.(t);
  };

  const [isCustomTopic, setIsCustomTopic] = useState<boolean>(false);
  const [customTopicInput, setCustomTopicInput] = useState<string>('');
  const [studentGaps, setStudentGaps] = useState<ProgressRecord[]>([]);
  const [recentSubmissions, setRecentSubmissions] = useState<SubmissionResult[]>([]);

  // Load student learning gaps and recent submissions for comprehensive cross-skill weakness detection
  useEffect(() => {
    progressApi
      .getGaps()
      .then((gaps) => setStudentGaps(gaps || []))
      .catch(() => {});

    submissionApi
      .getMySubmissions()
      .then((subs) => setRecentSubmissions(subs || []))
      .catch(() => {});
  }, []);

  // Multi-modal weakness analysis: compare Grammar, Reading, Speaking, and Essay scores
  const detectedWeakness = useMemo<SkillWeakness>(() => {
    const scoresByModality: Record<'GRAMMAR' | 'READING' | 'SPEAKING' | 'ESSAY', number[]> = {
      GRAMMAR: [],
      READING: [],
      SPEAKING: [],
      ESSAY: [],
    };

    recentSubmissions.forEach((sub) => {
      const type = (sub.taskType || '').toUpperCase();
      const score = sub.effectiveScore ?? sub.score ?? 70;
      if (type === 'READING') {
        scoresByModality.READING.push(score);
      } else if (type === 'SPEAKING' || sub.originalText?.startsWith('[SPEAKING_PRACTICE]')) {
        scoresByModality.SPEAKING.push(score);
      } else if (type === 'ESSAY') {
        scoresByModality.ESSAY.push(score > 10 ? score : score * 10);
      } else {
        scoresByModality.GRAMMAR.push(score);
      }
    });

    const avgScore = (arr: number[], fallback: number) =>
      arr.length > 0 ? arr.reduce((a, b) => a + b, 0) / arr.length : fallback;

    const weakestGrammarGap = studentGaps.length > 0
      ? [...studentGaps].sort((a, b) => a.masteryScore - b.masteryScore)[0]
      : null;

    const grammarAvg = weakestGrammarGap
      ? Math.min(weakestGrammarGap.masteryScore, avgScore(scoresByModality.GRAMMAR, 70))
      : avgScore(scoresByModality.GRAMMAR, 80);

    const readingAvg = avgScore(scoresByModality.READING, 80);
    const speakingAvg = avgScore(scoresByModality.SPEAKING, 80);
    const essayAvg = avgScore(scoresByModality.ESSAY, 80);

    const candidates: SkillWeakness[] = [
      {
        modality: 'GRAMMAR',
        score: grammarAvg,
        reason: weakestGrammarGap
          ? `Grammar gap: ${weakestGrammarGap.grammarTopic} (${Math.round(weakestGrammarGap.masteryScore)}% mastery)`
          : 'Core grammar practice from syllabus',
        suggestedTopic: weakestGrammarGap?.grammarTopic || DEFAULT_CEFR_TOPICS[cefrLevel]?.[0] || 'Grammar Fundamentals',
      },
      {
        modality: 'SPEAKING',
        score: speakingAvg,
        reason: scoresByModality.SPEAKING.length > 0
          ? `Speaking score average (${Math.round(speakingAvg)}%)`
          : 'Speech & oral articulation practice',
        suggestedTopic: 'Natural Speech Flow, Connected Rhythm & Articulation',
      },
      {
        modality: 'READING',
        score: readingAvg,
        reason: scoresByModality.READING.length > 0
          ? `Reading comprehension average (${Math.round(readingAvg)}%)`
          : 'Reading comprehension & inference',
        suggestedTopic: DEFAULT_CEFR_TOPICS[cefrLevel]?.[0] || 'Modern Technology & Society',
      },
      {
        modality: 'ESSAY',
        score: essayAvg,
        reason: scoresByModality.ESSAY.length > 0
          ? `Essay rubric average (${Math.round(essayAvg)}%)`
          : 'Discursive essay composition & argument structure',
        suggestedTopic: DEFAULT_ESSAY_TOPICS[cefrLevel]?.[0] || 'Advantages and Disadvantages of Remote Work',
      },
    ];

    // Pick the skill with the lowest score
    candidates.sort((a, b) => a.score - b.score);
    return candidates[0];
  }, [studentGaps, recentSubmissions, cefrLevel]);

  // Sync recommended topic whenever CEFR level or format changes
  useEffect(() => {
    if (format === 'RECOMMENDED') {
      setTopic(detectedWeakness.suggestedTopic);
      setIsCustomTopic(false);
      setCustomTopicInput('');
    } else if (!topic) {
      shuffleTopic();
    }
  }, [cefrLevel, format, detectedWeakness]);

  const getTopicPoolForFormat = (fmt: QuickPracticeFormat): string[] => {
    if (fmt === 'ESSAY') {
      return DEFAULT_ESSAY_TOPICS[cefrLevel] || [];
    }
    if (fmt === 'SPEAKING') {
      return [
        'Natural Speech Flow, Connected Rhythm & Articulation',
        'Consonant Contrasts & Minimal Pairs (/θ/ vs /ð/, /r/ vs /l/)',
        'Intonation Patterns in Questions & Assertions',
        'Expressive Pacing, Linking Sounds & Sentence Stress',
        'Silent Letters & Consonant Cluster Reductions',
        'Vowel Contrasts: Long vs Short (/iː/ vs /ɪ/)',
      ];
    }
    if (fmt === 'READING') {
      return [
        'Digital Privacy & Ethical Artificial Intelligence',
        'The Evolution of Sustainable Architecture',
        'Sleep Deprivation & Cognitive Function',
        'Global Supply Chains & Economic Interdependence',
        'Urban Green Spaces and Mental Well-being',
        'Language Preservation in the Modern Digital Era',
      ];
    }
    return [
      ...(DEFAULT_CEFR_TOPICS[cefrLevel] || []),
      ...(DEFAULT_MIXED_TOPICS[cefrLevel] || []),
    ];
  };

  const shuffleTopic = () => {
    const pool = getTopicPoolForFormat(format);
    if (pool.length > 0) {
      const randomTopic = pool[Math.floor(Math.random() * pool.length)];
      setTopic(randomTopic);
      setIsCustomTopic(false);
      setCustomTopicInput('');
    }
  };

  const handleFormatChange = (selectedFormat: QuickPracticeFormat) => {
    setFormat(selectedFormat);
    setIsCustomTopic(false);
    setCustomTopicInput('');

    if (selectedFormat === 'RECOMMENDED') {
      setTopic(detectedWeakness.suggestedTopic);
    } else {
      const pool = getTopicPoolForFormat(selectedFormat);
      if (pool.length > 0) {
        setTopic(pool[Math.floor(Math.random() * pool.length)]);
      }
    }
  };

  const handleStartPractice = () => {
    const activeTopic = isCustomTopic && customTopicInput.trim()
      ? customTopicInput.trim()
      : topic || DEFAULT_CEFR_TOPICS[cefrLevel]?.[0] || 'General Practice';

    const randomDomain = DOMAINS[Math.floor(Math.random() * DOMAINS.length)] || 'Daily Life';

    // Determine final task type
    let chosenTaskType: TaskType;
    if (format === 'RECOMMENDED') {
      if (detectedWeakness.modality === 'SPEAKING') {
        chosenTaskType = 'SPEAKING';
      } else if (detectedWeakness.modality === 'READING') {
        chosenTaskType = 'READING';
      } else if (detectedWeakness.modality === 'ESSAY') {
        chosenTaskType = 'ESSAY';
      } else {
        const gapTypes: TaskType[] = ['MCQ', 'GAP_FILL'];
        chosenTaskType = gapTypes[Math.floor(Math.random() * gapTypes.length)];
      }
    } else if (format === 'SURPRISE') {
      // Balanced uniform selection across all 4 major modalities
      const modalities: Array<'GRAMMAR' | 'READING' | 'SPEAKING' | 'ESSAY'> = [
        'GRAMMAR',
        'READING',
        'SPEAKING',
        'ESSAY',
      ];
      const picked = modalities[Math.floor(Math.random() * modalities.length)];
      if (picked === 'READING') {
        chosenTaskType = 'READING';
      } else if (picked === 'SPEAKING') {
        chosenTaskType = 'SPEAKING';
      } else if (picked === 'ESSAY') {
        chosenTaskType = 'ESSAY';
      } else {
        const grammarTypes: TaskType[] = ['MCQ', 'GAP_FILL', 'OPEN_BRACKETS', 'REWRITE'];
        chosenTaskType = grammarTypes[Math.floor(Math.random() * grammarTypes.length)];
      }
    } else if (format === 'GRAMMAR') {
      const grammarTypes: TaskType[] = ['MCQ', 'GAP_FILL', 'OPEN_BRACKETS', 'REWRITE'];
      chosenTaskType = grammarTypes[Math.floor(Math.random() * grammarTypes.length)];
    } else if (format === 'READING') {
      chosenTaskType = 'READING';
    } else if (format === 'SPEAKING') {
      chosenTaskType = 'SPEAKING';
    } else {
      chosenTaskType = 'ESSAY';
    }

    const quickParams: QuickGenerateParams = {
      taskType: chosenTaskType,
      grammarTopic: activeTopic,
      domain: randomDomain,
      difficulty: 'MEDIUM',
      numberOfQuestions: chosenTaskType === 'SPEAKING' || chosenTaskType === 'ESSAY' ? 1 : 5,
      ecoMode: false,
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
              speakingTitle: `${activeTopic} · Speech Drill`,
              targetSounds: 'Natural Rhythm, Connected Speech & Clear Articulation',
              speakingLength: 'MEDIUM',
            },
          }
        : {}),
      ...(chosenTaskType === 'ESSAY'
        ? {
            essayConfig: {
              topic: activeTopic,
              domain: randomDomain,
            },
          }
        : {}),
    };

    onQuickGenerate(quickParams);
  };

  const topicSuggestions = getTopicPoolForFormat(format).slice(0, 4);

  return (
    <div className="rounded-3xl border border-amber-200/90 bg-gradient-to-br from-amber-50/40 via-white to-orange-50/30 p-6 sm:p-8 space-y-6 shadow-xs animate-in fade-in duration-200">
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

        <div className="text-[11px] font-semibold text-amber-800 bg-amber-50 border border-amber-200/80 px-3 py-1.5 rounded-xl self-start sm:self-auto flex items-center space-x-1.5">
          <Sparkles className="w-3.5 h-3.5 text-amber-600" />
          <span>⚡ Express 1-Click Practice</span>
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

      {/* 2. Format / Skill Grid (6 Cards including Essay Writing) */}
      <div>
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
          2. Practice Skill / Format
        </label>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2.5">
          {/* Card 1: Recommended */}
          <button
            type="button"
            onClick={() => handleFormatChange('RECOMMENDED')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'RECOMMENDED'
                ? 'border-amber-600 bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-sm'
                : 'border-amber-200/80 bg-amber-50/50 text-slate-800 hover:border-amber-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black flex items-center space-x-1">
                <Target className="w-3.5 h-3.5" />
                <span>Recommended</span>
              </span>
              {format === 'RECOMMENDED' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'RECOMMENDED' ? 'text-amber-100' : 'text-slate-500'}`}>
              {detectedWeakness.reason}
            </span>
          </button>

          {/* Card 2: Surprise Mix */}
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
              <span className="text-xs font-black flex items-center space-x-1">
                <Dices className="w-3.5 h-3.5" />
                <span>Surprise Mix</span>
              </span>
              {format === 'SURPRISE' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'SURPRISE' ? 'text-amber-100' : 'text-slate-400'}`}>
              Random skill & format
            </span>
          </button>

          {/* Card 3: Grammar Quiz */}
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
              <span className="text-xs font-black flex items-center space-x-1">
                <Sliders className="w-3.5 h-3.5" />
                <span>Grammar Quiz</span>
              </span>
              {format === 'GRAMMAR' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'GRAMMAR' ? 'text-amber-100' : 'text-slate-400'}`}>
              MCQ, cloze, rewrite
            </span>
          </button>

          {/* Card 4: Reading Article */}
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
              <span className="text-xs font-black flex items-center space-x-1">
                <BookOpen className="w-3.5 h-3.5" />
                <span>Reading Article</span>
              </span>
              {format === 'READING' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'READING' ? 'text-amber-100' : 'text-slate-400'}`}>
              Passage & comprehension
            </span>
          </button>

          {/* Card 5: Speaking Drill */}
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
              <span className="text-xs font-black flex items-center space-x-1">
                <Mic className="w-3.5 h-3.5" />
                <span>Speaking Drill</span>
              </span>
              {format === 'SPEAKING' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'SPEAKING' ? 'text-amber-100' : 'text-slate-400'}`}>
              Speech & pronunciation
            </span>
          </button>

          {/* Card 6: Essay Writing */}
          <button
            type="button"
            onClick={() => handleFormatChange('ESSAY')}
            className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
              format === 'ESSAY'
                ? 'border-amber-500 bg-amber-500 text-white shadow-sm'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-black flex items-center space-x-1">
                <PenTool className="w-3.5 h-3.5" />
                <span>Essay Writing</span>
              </span>
              {format === 'ESSAY' && <Check className="w-3.5 h-3.5" />}
            </div>
            <span className={`text-[10px] leading-tight line-clamp-1 ${format === 'ESSAY' ? 'text-amber-100' : 'text-slate-400'}`}>
              Rubric & rule practice
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
            className="flex items-center space-x-1 text-xs font-bold text-amber-700 hover:text-amber-900 bg-amber-100/70 hover:bg-amber-100 px-2.5 py-1 rounded-xl transition"
          >
            <Dices className="w-3.5 h-3.5" />
            <span>Shuffle Topic</span>
          </button>
        </div>

        {/* Active Selected Topic Pill */}
        <div className="p-3.5 rounded-2xl bg-white border border-amber-200/90 shadow-2xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center space-x-2.5 min-w-0">
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center flex-shrink-0">
              <Dices className="w-3.5 h-3.5" />
            </div>
            <div className="min-w-0">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Active Topic
              </span>
              <span className="text-xs font-bold text-slate-800 truncate block">
                {isCustomTopic && customTopicInput ? customTopicInput : topic || 'General Practice'}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setIsCustomTopic(!isCustomTopic);
              if (!isCustomTopic) {
                setCustomTopicInput(topic);
              }
            }}
            className="text-xs font-semibold text-amber-700 hover:underline self-end sm:self-auto whitespace-nowrap"
          >
            {isCustomTopic ? 'Use syllabus topic' : 'Type custom topic'}
          </button>
        </div>

        {/* Custom Topic Input if toggled */}
        {isCustomTopic && (
          <AutoExpandingTextarea
            minRows={1}
            value={customTopicInput}
            onChange={setCustomTopicInput}
            placeholder="e.g. Past Continuous vs Past Simple in crime stories"
            className="w-full text-xs text-slate-800 placeholder:text-slate-400"
          />
        )}

        {/* Quick Topic Suggestions */}
        {!isCustomTopic && topicSuggestions.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 pt-1">
            <span className="text-[11px] font-semibold text-slate-400 mr-1">Suggestions:</span>
            {topicSuggestions.map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setTopic(t)}
                className={`text-xs px-2.5 py-1 rounded-xl border transition ${
                  topic === t
                    ? 'border-amber-500 bg-amber-50 text-amber-900 font-bold'
                    : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Start Button & Mode Switch Callout */}
      <div className="space-y-4 pt-2">
        <button
          type="button"
          onClick={handleStartPractice}
          disabled={isLoading}
          className="w-full py-4 rounded-2xl bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 hover:from-amber-600 hover:to-orange-600 text-white font-black text-sm tracking-wide shadow-md shadow-amber-200/80 hover:shadow-lg transition flex items-center justify-center space-x-2 disabled:opacity-50 cursor-pointer"
        >
          {isLoading ? (
            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <>
              <Zap className="w-4 h-4 fill-white" />
              <span>Start 1-Click Practice Now</span>
            </>
          )}
        </button>

        {/* Switch to Detailed Mode Directional Callout */}
        {onSwitchToDetailed && (
          <div className="p-3 bg-white/80 rounded-2xl border border-amber-200/70 flex items-center justify-between text-xs text-slate-600 gap-2">
            <span>
              Need fine control over passage length, target sounds, scoring rubric, or custom rules?
            </span>
            <button
              type="button"
              onClick={onSwitchToDetailed}
              className="px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary font-bold transition flex items-center space-x-1 whitespace-nowrap text-xs flex-shrink-0"
            >
              <span>Switch to Detailed Studio</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
