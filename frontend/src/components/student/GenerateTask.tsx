/**
 * @file GenerateTask.tsx
 * @brief Dynamic task generator interface allowing students to generate CEFR-aligned exercises.
 *
 * Supports both 1-Click Quick Practice mode (randomized parameters) and Detailed Studio mode
 * with modular panels for Grammar, Reading, Speaking, and Essay practice.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles,
  RotateCcw,
  AlertTriangle,
  Cpu,
  Clock,
  Key,
  FileCheck,
  Zap,
  SlidersHorizontal,
  ArrowRightLeft,
  Smartphone,
  BookOpen,
  FileText,
  Mic,
  PenTool,
  Target,
  Shuffle,
  ArrowRight,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { taskApi } from '../../api/taskApi';
import { progressApi } from '../../api/progressApi';
import { customCurriculumApi } from '../../api/customCurriculumApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { CefrLevel } from '../../types/user';
import { TaskType, DifficultyLevel, TopicsCatalogResponse } from '../../types/task';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNotificationStore } from '../../store/notificationStore';
import {
  AIProviderType,
  getDefaultModelForProvider,
} from '../../constants/aiModels';
import { DEFAULT_CEFR_TOPICS, DEFAULT_ESSAY_TOPICS, DOMAINS } from '../../constants/topics';
import { GrammarConfigPanel, UploadedCurriculumFile } from '../teacher/GrammarConfigPanel';
import { SpeakingConfigPanel, TARGET_SOUND_PRESETS } from '../teacher/SpeakingConfigPanel';
import { ReadingConfigPanel, ReadingConfig } from '../teacher/ReadingConfigPanel';
import { EssayConfigPanel } from '../teacher/EssayConfigPanel';
import { QuickModePanel, QuickGenerateParams } from '../common/QuickModePanel';

export const GenerateTask: React.FC = () => {
  const { user } = useAuth();
  const { isQuotaExceeded } = useUsage();
  const { openUpgradeWall } = useUIStore();
  const { addToast } = useNotificationStore();
  const navigate = useNavigate();

  // Mode switcher: Quick Practice (1-click) vs Detailed Studio
  const [generationMode, setGenerationMode] = useState<'QUICK' | 'DETAILED'>('QUICK');

  // AI Engine mode: Platform default (Gemini) vs User custom BYOK key
  const [aiEngineMode, setAiEngineMode] = useState<'PLATFORM' | 'BYOK'>('PLATFORM');

  const [catalog, setCatalog] = useState<TopicsCatalogResponse | null>(null);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>(user?.cefrLevel || 'B1');
  const [grammarTopic, setGrammarTopic] = useState<string>(
    DEFAULT_CEFR_TOPICS[user?.cefrLevel || 'B1']?.[0] || DEFAULT_CEFR_TOPICS.B1[0]
  );
  const [domain, setDomain] = useState<string>(DOMAINS[0]);
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
  const [numberOfQuestions, setNumberOfQuestions] = useState<number>(5);
  const [provider, setProvider] = useState<AIProviderType>('GEMINI');
  const [modelName, setModelName] = useState<string>(getDefaultModelForProvider('GEMINI'));
  const [savedKeys, setSavedKeys] = useState<ApiKeyItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [ecoMode, setEcoMode] = useState<boolean>(false);

  // Saved library rules and vocabularies (loaded from user and their teachers)
  const [savedRules, setSavedRules] = useState<CustomCurriculumEntry[]>([]);
  const [savedVocabSets, setSavedVocabSets] = useState<CustomCurriculumEntry[]>([]);

  // Grammar curriculum context state
  const [customRule, setCustomRule] = useState<string>('');
  const [customVocabulary, setCustomVocabulary] = useState<string>('');
  const [uploadedRuleFile, setUploadedRuleFile] = useState<UploadedCurriculumFile | null>(null);
  const [uploadedVocabFile, setUploadedVocabFile] = useState<UploadedCurriculumFile | null>(null);

  // Dedicated Speaking task configuration state
  const [speakingTitle, setSpeakingTitle] = useState<string>('');
  const [targetSounds, setTargetSounds] = useState<string>(TARGET_SOUND_PRESETS[0].value);
  const [pronunciationPackId, setPronunciationPackId] = useState<string | null>(null);
  const [speakingLength, setSpeakingLength] = useState<string>('MEDIUM');
  const [customWordCount, setCustomWordCount] = useState<number>(60);
  const [customInstructions, setCustomInstructions] = useState<string>('');

  // Dedicated Reading task configuration state
  const [readingConfig, setReadingConfig] = useState<ReadingConfig>({
    readingTitle: '',
    readingLength: 'Medium (250-400 words)',
    readingParagraphs: '3 to 4 distinct paragraphs with clear narrative progression',
    mcqQuestionsCount: 2,
    mcqPoints: 10,
    trueFalseQuestionsCount: 2,
    trueFalsePoints: 10,
    openQuestionsCount: 1,
    openPoints: 10,
    isRandomMode: true,
  });

  const [concurrentConflict, setConcurrentConflict] = useState<boolean>(false);
  const [isTakingOver, setIsTakingOver] = useState<boolean>(false);

  useEffect(() => {
    // Load topics catalog from backend
    taskApi
      .getTopicsCatalog()
      .then((data) => setCatalog(data))
      .catch((err) => console.warn('Could not load topics catalog from API, using defaults:', err));

    // Load saved curriculum rules & vocabularies (including shared teacher rules)
    customCurriculumApi
      .getCustomCurriculum('RULE')
      .then((data) => setSavedRules(data || []))
      .catch((err) => console.warn('Could not load saved rules:', err));

    customCurriculumApi
      .getCustomCurriculum('VOCABULARY')
      .then((data) => setSavedVocabSets(data || []))
      .catch((err) => console.warn('Could not load saved vocabulary:', err));

    // Load user API keys
    apiKeyApi
      .getKeys()
      .then((keys) => {
        setSavedKeys(keys);
        if (keys.length > 0) {
          const first = keys[0];
          setProvider(first.provider);
          setModelName(first.modelName || getDefaultModelForProvider(first.provider));
        }
      })
      .catch(() => {});
  }, []);

  const handleCefrChange = (newLevel: CefrLevel) => {
    setCefrLevel(newLevel);
    const available = catalog?.topicsByLevel?.[newLevel] || DEFAULT_CEFR_TOPICS[newLevel];
    if (available && available.length > 0) {
      setGrammarTopic(available[0]);
    }
  };

  /**
   * @brief Fast generation triggered from QuickModePanel.
   */
  const handleQuickGenerate = async (params: QuickGenerateParams) => {
    if (isQuotaExceeded) {
      openUpgradeWall('You have used all your free AI evaluations for today.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const readingQuestionsCount = params.readingConfig
        ? (params.readingConfig.mcqQuestionsCount || 0) +
          (params.readingConfig.trueFalseQuestionsCount || 0) +
          (params.readingConfig.openQuestionsCount || 0)
        : undefined;

      const readingTotalPoints = params.readingConfig
        ? (params.readingConfig.mcqQuestionsCount || 0) * (params.readingConfig.mcqPoints || 0) +
          (params.readingConfig.trueFalseQuestionsCount || 0) * (params.readingConfig.trueFalsePoints || 0) +
          (params.readingConfig.openQuestionsCount || 0) * (params.readingConfig.openPoints || 0)
        : undefined;

      const task = await taskApi.generateTask({
        cefrLevel,
        grammarTopic: params.grammarTopic,
        domain: params.domain,
        taskType: params.taskType,
        difficulty: params.difficulty,
        numberOfQuestions: params.taskType === 'READING' ? readingQuestionsCount : params.numberOfQuestions,
        totalPoints: readingTotalPoints,
        provider,
        modelName,
        ecoMode: params.ecoMode,
        ...(params.readingConfig
          ? {
              readingTitle: params.readingConfig.readingTitle,
              readingLength: params.readingConfig.readingLength,
              readingParagraphs: params.readingConfig.readingParagraphs,
              mcqQuestionsCount: params.readingConfig.mcqQuestionsCount,
              mcqPoints: params.readingConfig.mcqPoints,
              trueFalseQuestionsCount: params.readingConfig.trueFalseQuestionsCount,
              trueFalsePoints: params.readingConfig.trueFalsePoints,
              openQuestionsCount: params.readingConfig.openQuestionsCount,
              openPoints: params.readingConfig.openPoints,
            }
          : {}),
        ...(params.speakingConfig
          ? {
              speakingTitle: params.speakingConfig.speakingTitle,
              targetSounds: params.speakingConfig.targetSounds,
              speakingLength: params.speakingConfig.speakingLength,
              pronunciationPackId: params.speakingConfig.pronunciationPackId,
            }
          : {}),
      });

      if (params.taskType === 'ESSAY') {
        navigate(`/student/essay/${task.id}`);
      } else {
        navigate(`/student/task/${task.id}`);
      }
    } catch (err: any) {
      console.error('Failed to quick-generate task:', err);
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setConcurrentConflict(true);
      } else if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(err.response?.data?.message);
      } else {
        setError(err.response?.data?.message || 'Failed to generate task. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Detailed execution handler for custom configured tasks.
   */
  const executeGenerateTask = async () => {
    if (isQuotaExceeded) {
      openUpgradeWall('You have used all your free AI evaluations for today.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const readingQuestionsCount =
        (readingConfig.mcqQuestionsCount || 0) +
        (readingConfig.trueFalseQuestionsCount || 0) +
        (readingConfig.openQuestionsCount || 0);

      const readingTotalPoints =
        (readingConfig.mcqQuestionsCount || 0) * (readingConfig.mcqPoints || 0) +
        (readingConfig.trueFalseQuestionsCount || 0) * (readingConfig.trueFalsePoints || 0) +
        (readingConfig.openQuestionsCount || 0) * (readingConfig.openPoints || 0);

      const task = await taskApi.generateTask({
        cefrLevel,
        grammarTopic: grammarTopic.trim() || 'General Practice',
        domain,
        taskType,
        difficulty,
        numberOfQuestions:
          taskType === 'SPEAKING'
            ? 1
            : taskType === 'READING'
            ? readingQuestionsCount
            : numberOfQuestions,
        totalPoints: taskType === 'READING' ? readingTotalPoints : undefined,
        provider,
        modelName,
        customRule: customRule.trim() || undefined,
        customVocabulary: customVocabulary.trim() || undefined,
        ruleFilePath: uploadedRuleFile?.serverPath || undefined,
        vocabularyFilePath: uploadedVocabFile?.serverPath || undefined,
        ecoMode,
        ...(taskType === 'READING'
          ? {
              readingTitle: readingConfig.readingTitle.trim() || undefined,
              readingLength: readingConfig.readingLength.trim() || undefined,
              readingParagraphs: readingConfig.readingParagraphs.trim() || undefined,
              mcqQuestionsCount: readingConfig.mcqQuestionsCount,
              mcqPoints: readingConfig.mcqPoints,
              trueFalseQuestionsCount: readingConfig.trueFalseQuestionsCount,
              trueFalsePoints: readingConfig.trueFalsePoints,
              openQuestionsCount: readingConfig.openQuestionsCount,
              openPoints: readingConfig.openPoints,
            }
          : {}),
        ...(taskType === 'SPEAKING'
          ? {
              speakingTitle: speakingTitle.trim() || undefined,
              targetSounds: targetSounds.trim() || undefined,
              pronunciationPackId: pronunciationPackId || undefined,
              speakingLength:
                speakingLength === 'CUSTOM'
                  ? `Custom (${customWordCount} words${customInstructions ? `: ${customInstructions}` : ''})`
                  : speakingLength.trim() || undefined,
            }
          : {}),
      });

      if (taskType === 'ESSAY') {
        navigate(`/student/essay/${task.id}`);
      } else {
        navigate(`/student/task/${task.id}`);
      }
    } catch (err: any) {
      console.error('Failed to generate task:', err);
      if (err.response?.status === 409 || err.response?.data?.errorCode === 'CONCURRENT_GENERATION') {
        setConcurrentConflict(true);
      } else if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(err.response?.data?.message);
      } else {
        setError(err.response?.data?.message || 'Failed to generate task. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    await executeGenerateTask();
  };

  const handleTakeoverAndGenerate = async () => {
    setIsTakingOver(true);
    try {
      await taskApi.takeoverGeneration();
      setConcurrentConflict(false);
      await executeGenerateTask();
    } catch (err) {
      console.error('Failed to takeover generation lock:', err);
      setError('Could not switch active generation to this device. Please retry.');
    } finally {
      setIsTakingOver(false);
    }
  };

  /**
   * @brief Fills the weakest student gap or foundational topic into the active topic field.
   */
  const handleFillRecommended = async () => {
    try {
      const gaps = await progressApi.getGaps();
      if (gaps && gaps.length > 0) {
        const weakest = [...gaps].sort((a, b) => a.masteryScore - b.masteryScore)[0];
        setGrammarTopic(weakest.grammarTopic);
        addToast({
          type: 'info',
          title: 'Topic Auto-Filled',
          message: `Recommended study gap: ${weakest.grammarTopic} (${Math.round(weakest.masteryScore)}% mastery). You can edit it freely.`,
        });
        return;
      }
    } catch (_) {}
    const defaultList = catalog?.topicsByLevel?.[cefrLevel] || DEFAULT_CEFR_TOPICS[cefrLevel];
    if (defaultList && defaultList.length > 0) {
      setGrammarTopic(defaultList[0]);
      addToast({
        type: 'info',
        title: 'Topic Auto-Filled',
        message: `Syllabus foundation: ${defaultList[0]}`,
      });
    }
  };

  /**
   * @brief Fills a random topic from the syllabus or essay pool into the active topic field.
   */
  const handleFillRandom = () => {
    let pool: string[] = [];
    if (taskType === 'ESSAY') {
      pool = DEFAULT_ESSAY_TOPICS[cefrLevel] || [];
    } else {
      pool = [
        ...(catalog?.topicsByLevel?.[cefrLevel] || DEFAULT_CEFR_TOPICS[cefrLevel] || []),
        ...(catalog?.mixedTopicsByLevel?.[cefrLevel] || []),
      ];
    }
    if (pool.length > 0) {
      const rolled = pool[Math.floor(Math.random() * pool.length)];
      setGrammarTopic(rolled);
      addToast({
        type: 'info',
        title: 'Random Topic Rolled',
        message: `Rolled: ${rolled}. You can edit it freely.`,
      });
    }
  };

  const handleClear = () => {
    setGrammarTopic(DEFAULT_CEFR_TOPICS[cefrLevel]?.[0] || DEFAULT_CEFR_TOPICS.A1[0]);
    setDomain(DOMAINS[0]);
    setTaskType('MCQ');
    setDifficulty('MEDIUM');
    setNumberOfQuestions(5);
    setCustomRule('');
    setCustomVocabulary('');
    setUploadedRuleFile(null);
    setUploadedVocabFile(null);
    setSpeakingTitle('');
    setTargetSounds(TARGET_SOUND_PRESETS[0].value);
    setPronunciationPackId('');
    setSpeakingLength('MEDIUM');
    setEcoMode(false);
  };

  const isGrammarTask =
    taskType === 'MCQ' ||
    taskType === 'GAP_FILL' ||
    taskType === 'OPEN_BRACKETS' ||
    taskType === 'REWRITE';

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-in fade-in duration-300">
      {/* Page Title & Mode Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">
            Practice Generator
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Create tailored CEFR exercises, reading comprehension passages, or speech drills.
          </p>
        </div>

        {/* Prominent Mode Switcher Tabs */}
        <div className="flex items-center p-1.5 rounded-2xl bg-slate-100 border border-slate-200/90 shadow-2xs self-start sm:self-auto gap-1">
          <button
            type="button"
            onClick={() => setGenerationMode('QUICK')}
            className={`py-2 px-3.5 rounded-xl text-xs font-black transition flex items-center space-x-2 cursor-pointer ${
              generationMode === 'QUICK'
                ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-sm ring-1 ring-amber-400/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
            }`}
          >
            <Zap className={`w-3.5 h-3.5 ${generationMode === 'QUICK' ? 'fill-white' : 'text-amber-500'}`} />
            <div className="text-left">
              <span className="block leading-tight">Quick Practice</span>
              <span className={`text-[10px] block font-medium ${generationMode === 'QUICK' ? 'text-amber-100' : 'text-slate-400'}`}>
                1-Click Express
              </span>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setGenerationMode('DETAILED')}
            className={`py-2 px-3.5 rounded-xl text-xs font-black transition flex items-center space-x-2 cursor-pointer ${
              generationMode === 'DETAILED'
                ? 'bg-indigo-600 text-white shadow-sm ring-1 ring-indigo-500/30'
                : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
            }`}
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <div className="text-left">
              <span className="block leading-tight">Detailed Studio</span>
              <span className={`text-[10px] block font-medium ${generationMode === 'DETAILED' ? 'text-indigo-200' : 'text-slate-400'}`}>
                Full Customization
              </span>
            </div>
          </button>
        </div>
      </div>

      {isLoading && (
        <div className="p-5 rounded-3xl bg-indigo-50/90 border border-indigo-200/80 shadow-xs space-y-3 animate-in fade-in duration-300">
          <div className="flex items-center space-x-3">
            <div className="relative flex items-center justify-center flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-indigo-500/20 animate-ping absolute" />
              <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center relative">
                <Sparkles className="w-4 h-4 animate-spin" />
              </div>
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Creating your {cefrLevel} practice task...
              </h3>
              <p className="text-xs text-indigo-700 font-medium">
                Model: <span className="font-mono font-semibold">{provider} · {modelName}</span>
              </p>
            </div>
          </div>
          <div className="bg-white/80 backdrop-blur-xs rounded-2xl p-3 border border-indigo-100 text-xs text-slate-600 space-y-1.5 leading-relaxed">
            <p className="flex items-center space-x-1.5">
              <Clock className="w-3.5 h-3.5 text-primary flex-shrink-0" />
              <span>Calibrating exercises for level {cefrLevel}...</span>
            </p>
            {(customRule || customVocabulary) && (
              <p className="flex items-center space-x-1.5 text-emerald-700 font-medium">
                <FileCheck className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                <span>Applying custom rules and vocabulary...</span>
              </p>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="p-5 rounded-3xl bg-amber-50 border border-amber-200/80 shadow-xs space-y-3 animate-in fade-in duration-300">
          <div className="flex items-start space-x-3">
            <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center flex-shrink-0 mt-0.5">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div className="space-y-1.5 flex-1">
              <h3 className="text-sm font-bold text-slate-900">
                {error.toLowerCase().includes('queue') || error.toLowerCase().includes('temporarily')
                  ? 'AI Service Queue · High Demand'
                  : 'Generation Error'}
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">{error}</p>
              <div className="pt-2 flex flex-wrap gap-2 text-xs">
                <button
                  type="button"
                  onClick={(e) => handleGenerate(e as any)}
                  className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 shadow-xs"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Retry Generation</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* QUICK MODE PANEL */}
      {generationMode === 'QUICK' && (
        <QuickModePanel
          cefrLevel={cefrLevel}
          onCefrLevelChange={handleCefrChange}
          activeTopic={grammarTopic}
          onActiveTopicChange={setGrammarTopic}
          onQuickGenerate={handleQuickGenerate}
          isLoading={isLoading}
          isStudent={true}
          onSwitchToDetailed={() => setGenerationMode('DETAILED')}
        />
      )}

      {/* DETAILED STUDIO FORM */}
      {generationMode === 'DETAILED' && (
        <form onSubmit={handleGenerate} className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-xs space-y-6">
          {/* Detailed Studio Header Callout & Quick Fills */}
          <div className="p-4 rounded-2xl bg-gradient-to-r from-indigo-50/70 via-white to-amber-50/50 border border-indigo-100/90 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="space-y-0.5">
              <div className="flex items-center space-x-2">
                <span className="text-xs font-black text-slate-800">Quick Fill Topic & Goal</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-indigo-100/80 text-primary font-bold">
                  Freely editable below
                </span>
              </div>
              <span className="text-[11px] text-slate-500 block">
                Auto-fill your recommended study gaps or standard syllabus topics with one click.
              </span>
            </div>

            <div className="flex items-center space-x-2 self-start sm:self-auto">
              <button
                type="button"
                onClick={handleFillRecommended}
                className="px-3 py-1.5 rounded-xl border border-amber-300 bg-amber-50/90 hover:bg-amber-100 text-amber-900 text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shadow-2xs"
              >
                <Target className="w-3.5 h-3.5 text-amber-600" />
                <span>🎯 Recommended</span>
              </button>
              <button
                type="button"
                onClick={handleFillRandom}
                className="px-3 py-1.5 rounded-xl border border-indigo-200 bg-white hover:bg-indigo-50 text-indigo-900 text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shadow-2xs"
              >
                <Shuffle className="w-3.5 h-3.5 text-indigo-600" />
                <span>🎲 Surprise Topic</span>
              </button>
            </div>
          </div>

          {/* AI Engine Selection */}
          <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center space-x-2">
                <Cpu className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  AI Engine
                </span>
              </div>
              <div className="flex items-center p-1 rounded-xl bg-slate-200/80 border border-slate-300/60 text-xs font-bold self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => {
                    setAiEngineMode('PLATFORM');
                    setProvider('GEMINI');
                    setModelName('gemini-3.8-flash');
                  }}
                  className={`px-3 py-1.5 rounded-lg transition ${
                    aiEngineMode === 'PLATFORM'
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  ⚡ Platform AI (Gemini)
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setAiEngineMode('BYOK');
                    if (savedKeys.length > 0) {
                      setProvider(savedKeys[0].provider);
                      setModelName(savedKeys[0].modelName || getDefaultModelForProvider(savedKeys[0].provider));
                    }
                  }}
                  className={`px-3 py-1.5 rounded-lg transition ${
                    aiEngineMode === 'BYOK'
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  🔑 Custom Key (BYOK)
                </button>
              </div>
            </div>

            {aiEngineMode === 'PLATFORM' ? (
              <div className="flex items-center justify-between p-3.5 bg-white rounded-2xl border border-slate-200/90 shadow-2xs">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 rounded-xl bg-indigo-50 text-primary flex items-center justify-center flex-shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="text-xs font-bold text-slate-800">Google Gemini 3.8 Flash</span>
                    <span className="text-[11px] text-slate-500 block">Fast, balanced platform model included in your tier</span>
                  </div>
                </div>
                <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-2.5 py-1 rounded-full">
                  Active
                </span>
              </div>
            ) : savedKeys.length > 0 ? (
              <div className="flex items-center justify-between p-3.5 bg-white rounded-2xl border border-slate-200/90 shadow-2xs">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
                    <Key className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="text-xs font-bold text-slate-800">
                      {savedKeys[0].provider} · {savedKeys[0].modelName || 'Default Model'}
                    </span>
                    <span className="text-[11px] text-slate-500 block">Using your personal API key</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="text-xs font-bold text-primary hover:underline whitespace-nowrap"
                >
                  Change in Profile →
                </button>
              </div>
            ) : (
              <div className="p-4 bg-amber-50 border border-amber-200/90 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between text-xs text-amber-900 gap-3">
                <div className="flex items-center space-x-2.5">
                  <Key className="w-5 h-5 text-amber-600 flex-shrink-0" />
                  <span>
                    No custom API keys configured. You can add your OpenAI, Anthropic, DeepSeek, or Groq key in Profile.
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="self-end sm:self-auto px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl font-bold whitespace-nowrap text-xs shadow-xs transition"
                >
                  Configure Key
                </button>
              </div>
            )}
          </div>

          {/* CEFR Level Selection */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Target CEFR Level
            </label>
            <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
              {(['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as CefrLevel[]).map((level) => {
                const isSelected = cefrLevel === level;
                return (
                  <button
                    type="button"
                    key={level}
                    onClick={() => handleCefrChange(level)}
                    className={`py-2.5 px-3 rounded-2xl border flex items-center justify-center transition ${
                      isSelected
                        ? 'border-primary bg-indigo-50/70 ring-2 ring-primary/20 shadow-xs'
                        : 'border-slate-200 hover:border-slate-300 bg-white hover:bg-slate-50'
                    }`}
                  >
                    <CefrBadge level={level} size="md" />
                  </button>
                );
              })}
            </div>
          </div>

          {/* Task Format / 4 Primary Modules */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
              Task Format
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {[
                { id: 'GRAMMAR', label: 'Grammar', icon: BookOpen, desc: 'MCQ, Cloze, Brackets, Rewrite' },
                { id: 'READING', label: 'Reading', icon: FileText, desc: 'Comprehension passage & questions' },
                { id: 'SPEAKING', label: 'Speaking', icon: Mic, desc: 'Phonetics & speech drill' },
                { id: 'ESSAY', label: 'Essay', icon: PenTool, desc: 'Discursive writing & rubric scoring' },
              ].map((mod) => {
                const isSelected =
                  (mod.id === 'GRAMMAR' && isGrammarTask) ||
                  (mod.id === 'READING' && taskType === 'READING') ||
                  (mod.id === 'SPEAKING' && taskType === 'SPEAKING') ||
                  (mod.id === 'ESSAY' && taskType === 'ESSAY');

                return (
                  <button
                    type="button"
                    key={mod.id}
                    onClick={() => {
                      if (mod.id === 'GRAMMAR') {
                        if (!isGrammarTask) setTaskType('MCQ');
                      } else if (mod.id === 'READING') {
                        setTaskType('READING');
                      } else if (mod.id === 'SPEAKING') {
                        setTaskType('SPEAKING');
                      } else {
                        setTaskType('ESSAY');
                      }
                    }}
                    className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between cursor-pointer ${
                      isSelected
                        ? 'border-primary bg-indigo-50/90 text-primary ring-2 ring-primary/20 shadow-xs'
                        : 'border-slate-200 bg-white hover:border-slate-300 text-slate-700'
                    }`}
                  >
                    <div className="flex items-center space-x-2 mb-1">
                      <mod.icon className={`w-4 h-4 ${isSelected ? 'text-primary' : 'text-slate-500'}`} />
                      <span className={`text-xs font-bold ${isSelected ? 'text-primary' : 'text-slate-900'}`}>
                        {mod.label}
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 leading-tight">
                      {mod.desc}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Dynamic Module 1: Grammar Exercises */}
          {isGrammarTask && (
            <GrammarConfigPanel
              taskType={taskType}
              onTaskTypeChange={setTaskType}
              cefrLevel={cefrLevel}
              grammarTopic={grammarTopic}
              onGrammarTopicChange={setGrammarTopic}
              domain={domain}
              onDomainChange={setDomain}
              difficulty={difficulty}
              onDifficultyChange={setDifficulty}
              numberOfQuestions={numberOfQuestions}
              onNumberOfQuestionsChange={setNumberOfQuestions}
              ecoMode={ecoMode}
              onEcoModeChange={setEcoMode}
              customRule={customRule}
              onCustomRuleChange={setCustomRule}
              customVocabulary={customVocabulary}
              onCustomVocabularyChange={setCustomVocabulary}
              uploadedRuleFile={uploadedRuleFile}
              onUploadedRuleFileChange={setUploadedRuleFile}
              uploadedVocabFile={uploadedVocabFile}
              onUploadedVocabFileChange={setUploadedVocabFile}
              catalog={catalog}
              isStudent={true}
              savedRules={savedRules}
              savedVocabSets={savedVocabSets}
            />
          )}

          {/* Dynamic Module 2: Reading Comprehension */}
          {taskType === 'READING' && (
            <ReadingConfigPanel
              config={readingConfig}
              onChange={(updated) => setReadingConfig((prev) => ({ ...prev, ...updated }))}
              cefrLevel={cefrLevel}
              customVocabulary={customVocabulary}
              onCustomVocabularyChange={setCustomVocabulary}
              savedVocabSets={savedVocabSets}
              isStudent={true}
            />
          )}

          {/* Dynamic Module 3: Speaking Practice */}
          {taskType === 'SPEAKING' && (
            <SpeakingConfigPanel
              cefrLevel={cefrLevel}
              speakingTitle={speakingTitle}
              onTitleChange={setSpeakingTitle}
              targetSounds={targetSounds}
              onTargetSoundsChange={setTargetSounds}
              pronunciationPackId={pronunciationPackId}
              onPronunciationPackIdChange={setPronunciationPackId}
              speakingLength={speakingLength}
              onSpeakingLengthChange={setSpeakingLength}
              totalPoints={100}
              onTotalPointsChange={() => {}}
              ecoMode={ecoMode}
              onEcoModeChange={setEcoMode}
              isStudent={true}
              customWordCount={customWordCount}
              onCustomWordCountChange={setCustomWordCount}
              customInstructions={customInstructions}
              onCustomInstructionsChange={setCustomInstructions}
            />
          )}

          {/* Dynamic Module 4: Essay Studio */}
          {taskType === 'ESSAY' && (
            <div className="animate-in fade-in duration-200">
              <EssayConfigPanel
                cefrLevel={cefrLevel}
                topic={grammarTopic}
                onTopicChange={setGrammarTopic}
                domain={domain}
                onDomainChange={setDomain}
                customRule={customRule}
                onCustomRuleChange={setCustomRule}
                customVocabulary={customVocabulary}
                onCustomVocabularyChange={setCustomVocabulary}
                totalPoints={100}
                onTotalPointsChange={() => {}}
                savedRules={savedRules}
                savedVocabSets={savedVocabSets}
                isStudent={true}
              />
            </div>
          )}

          {/* Switch to Quick Mode Callout */}
          <div className="p-3.5 bg-amber-50/70 rounded-2xl border border-amber-200/80 flex flex-col sm:flex-row sm:items-center justify-between text-xs text-amber-900 gap-2">
            <span>⚡ Prefer 1-Click Practice without adjusting manual parameters?</span>
            <button
              type="button"
              onClick={() => setGenerationMode('QUICK')}
              className="px-3.5 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 whitespace-nowrap text-xs self-end sm:self-auto cursor-pointer shadow-2xs"
            >
              <span>Switch to Quick Practice</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center space-x-3 pt-4 border-t border-slate-100">
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50 cursor-pointer"
            >
              {isLoading ? (
                <LoadingSpinner size="sm" className="p-0 text-white" />
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  <span>Generate Task</span>
                </>
              )}
            </button>
            <button
              type="button"
              onClick={handleClear}
              disabled={isLoading}
              className="flex items-center justify-center space-x-1 py-3.5 px-4 rounded-2xl border border-slate-200 text-slate-600 hover:bg-slate-50 font-semibold text-sm transition cursor-pointer"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Clear</span>
            </button>
          </div>
        </form>
      )}

      {/* Concurrent Generation Transfer Modal */}
      {concurrentConflict && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
          <div className="bg-white rounded-2xl shadow-2xl border border-amber-200/80 max-w-md w-full p-6 space-y-4 animate-in zoom-in-95 duration-150">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 border border-amber-200 text-amber-600 flex items-center justify-center mx-auto shadow-xs">
              <Smartphone className="w-6 h-6" />
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-base font-bold text-slate-900">
                Active Generation on Another Device
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                An AI generation session is currently active on another device associated with your account.
                You can transfer control to this device to proceed immediately.
              </p>
            </div>
            <div className="pt-2 flex flex-col sm:flex-row gap-2">
              <button
                type="button"
                onClick={handleTakeoverAndGenerate}
                disabled={isTakingOver}
                className="flex-1 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-xs disabled:opacity-50"
              >
                <ArrowRightLeft className="w-4 h-4" />
                <span>{isTakingOver ? 'Switching...' : 'Switch to This Device'}</span>
              </button>
              <button
                type="button"
                onClick={() => setConcurrentConflict(false)}
                className="py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
