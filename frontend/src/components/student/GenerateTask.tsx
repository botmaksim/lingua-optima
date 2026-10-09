/**
 * @file GenerateTask.tsx
 * @brief Dynamic task generator interface allowing students to generate CEFR-aligned exercises with custom topics and curriculum context.
 */

import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles,
  RotateCcw,
  AlertTriangle,
  Cpu,
  Clock,
  Key,
  UploadCloud,
  FileText,
  Trash2,
  BookOpen,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  FileCheck,
  Leaf,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { CefrLevel } from '../../types/user';
import { TaskType, DifficultyLevel, TopicsCatalogResponse } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  useProviderModels,
} from '../../constants/aiModels';

const DEFAULT_CEFR_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple (to be & common verbs)',
    'Articles (a, an, the) & Demonstratives',
    'Basic Prepositions of Place & Time (in, at, on)',
    "Can / Can't for Ability & Permission",
    "Possessive Adjectives & Possessive 's",
    'Imperatives & Basic Question Formation',
  ],
  A2: [
    'Past Simple (Regular & Irregular Verbs)',
    "Future with 'Going to' vs 'Will'",
    'Comparative and Superlative Adjectives',
    'Countable vs Uncountable Nouns (some, any, much, many)',
    'Have to & Must (Basic Rules)',
    'Present Continuous for Future Arrangements',
  ],
  B1: [
    'Present Perfect vs Past Simple',
    'Past Continuous',
    'Conditionals (First & Second)',
    'Modal Verbs of Obligation',
    'Passive Voice (Basic)',
    'Relative Clauses (Defining)',
    'Used to & Would',
  ],
  B2: [
    'Third & Mixed Conditionals',
    'Passive Voice (Advanced & Causative)',
    'Reported Speech',
    'Wish & If Only Structures',
    'Modal Verbs of Deduction',
    'Inversion for Emphasis',
    'Participle Clauses',
  ],
  C1: [
    'Advanced Inversion & Fronting',
    'Subjunctive Mood',
    'Cleft Sentences',
    'Complex Gerunds & Infinitives',
    'Discourse Markers & Nuance',
    'Ellipsis & Substitution',
  ],
  C2: [
    'Stylistic Inversion & Rhetorical Fronting',
    'Subtle Modal Nuances & Speculative Stance',
    'Complex Cleft Constructions & Focalization',
    'Idiomatic Phrasal Collocations & Register Shifts',
    'Advanced Ellipsis, Substitution & Cohesive Ties',
    'Figurative Language & Lexical Precision',
  ],
};

const DEFAULT_MIXED_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple vs Present Continuous in Daily Routines',
    'Articles, Plurals, and Demonstrative Pronouns',
    'Question Formation with To Be, Do/Does, and Can',
  ],
  A2: [
    'Past Simple vs Past Continuous Narrative Interruption',
    'Future Plans: Going to vs Present Continuous vs Will',
    'Comparatives, Superlatives, and As...As Equality',
  ],
  B1: [
    'Narrative Tenses: Past Simple, Continuous, and Perfect',
    'Mixed Modal Verbs: Obligation, Permission, and Advice',
    'Zero, First, and Second Conditionals with Unless',
  ],
  B2: [
    'Mixed Conditionals (Past Cause with Present Result)',
    'Advanced Passive and Causative Structures (Have/Get something done)',
    'Reported Speech Shifts with Reporting Verbs & Modals',
  ],
  C1: [
    'Negative Inversion and Cleft Sentences Combined',
    'Participle Clauses with Reduced Relatives & Adverbials',
    'Subjunctive Mood and Formulaic Mandative Expressions',
  ],
  C2: [
    'Stylistic Inversion, Clefting, and Focal Fronting',
    'Epistemic Stance, Subtle Modal Nuances, and Hedging',
    'Advanced Ellipsis, Substitution, and Cohesive Chaining',
  ],
};

const DOMAINS = ['Daily Life', 'Business', 'Academic', 'Technology', 'Travel & Culture'];

/**
 * @brief Form component to configure and generate AI-driven practice tasks.
 * @return JSX form element for task generation.
 */
export const GenerateTask: React.FC = () => {
  const { user } = useAuth();
  const { isQuotaExceeded } = useUsage();
  const { openUpgradeWall } = useUIStore();
  const navigate = useNavigate();

  const [catalog, setCatalog] = useState<TopicsCatalogResponse | null>(null);
  const [cefrLevel, setCefrLevel] = useState<CefrLevel>(user?.cefrLevel || 'A1');
  const [selectedTopicOption, setSelectedTopicOption] = useState<string>(
    DEFAULT_CEFR_TOPICS[user?.cefrLevel || 'A1']?.[0] || DEFAULT_CEFR_TOPICS.A1[0]
  );
  const [customTopicText, setCustomTopicText] = useState<string>('');
  const [domain, setDomain] = useState<string>(DOMAINS[0]);
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
  const [numberOfQuestions, setNumberOfQuestions] = useState<number>(5);
  const [provider, setProvider] = useState<AIProviderType>('GEMINI');
  const [modelName, setModelName] = useState<string>(getDefaultModelForProvider('GEMINI'));
  const { models: providerModels, isLiveSynced } = useProviderModels(provider);
  const [savedKeys, setSavedKeys] = useState<ApiKeyItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [ecoMode, setEcoMode] = useState<boolean>(false);

  // Curriculum context state
  const [showCurriculumPanel, setShowCurriculumPanel] = useState<boolean>(false);
  const [customRule, setCustomRule] = useState<string>('');
  const [customVocabulary, setCustomVocabulary] = useState<string>('');
  const [uploadedRuleFile, setUploadedRuleFile] = useState<{
    fileName: string;
    fileSize: number;
    contentSnippet: string;
    serverPath: string;
  } | null>(null);
  const [uploadedVocabFile, setUploadedVocabFile] = useState<{
    fileName: string;
    fileSize: number;
    contentSnippet: string;
    serverPath: string;
  } | null>(null);
  const [isUploadingRule, setIsUploadingRule] = useState<boolean>(false);
  const [isUploadingVocab, setIsUploadingVocab] = useState<boolean>(false);
  const [isLoadingRef, setIsLoadingRef] = useState<boolean>(false);
  const [refNotice, setRefNotice] = useState<string | null>(null);

  const ruleFileInputRef = useRef<HTMLInputElement>(null);
  const vocabFileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Load topics catalog from backend
    taskApi
      .getTopicsCatalog()
      .then((data) => setCatalog(data))
      .catch((err) => console.warn('Could not load topics catalog from API, using defaults:', err));

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

  const isCustomTopic = selectedTopicOption === '__CUSTOM__';
  const effectiveGrammarTopic = isCustomTopic
    ? customTopicText.trim() || 'Custom Practice Topic'
    : selectedTopicOption;

  const currentLevelTopics =
    catalog?.topicsByLevel?.[cefrLevel] || DEFAULT_CEFR_TOPICS[cefrLevel] || [];
  const currentLevelMixedTopics =
    catalog?.mixedTopicsByLevel?.[cefrLevel] || DEFAULT_MIXED_TOPICS[cefrLevel] || [];
  const crossLevelTopics = catalog?.crossLevelTopics || [];

  /**
   * @brief Event handler updating selected AI provider and its associated model.
   * @param nextProvider New AI provider identifier.
   */
  const handleProviderChange = (nextProvider: AIProviderType) => {
    setProvider(nextProvider);
    const matchingKey = savedKeys.find((k) => k.provider === nextProvider);
    setModelName(matchingKey?.modelName || getDefaultModelForProvider(nextProvider));
  };

  /**
   * @brief Event handler updating CEFR level and resetting topic to default.
   */
  const handleCefrChange = (newLevel: CefrLevel) => {
    setCefrLevel(newLevel);
    const available = catalog?.topicsByLevel?.[newLevel] || DEFAULT_CEFR_TOPICS[newLevel];
    if (!isCustomTopic && available && available.length > 0) {
      setSelectedTopicOption(available[0]);
    }
  };

  /**
   * @brief Loads canonical or synthesized reference curriculum from server.
   */
  const handleLoadReference = async () => {
    setIsLoadingRef(true);
    setRefNotice(null);
    try {
      const ref = await taskApi.getCurriculumReference(cefrLevel, effectiveGrammarTopic);
      if (ref.referenceRule) {
        setCustomRule(ref.referenceRule);
      }
      if (ref.referenceVocabulary && ref.referenceVocabulary.length > 0) {
        setCustomVocabulary(ref.referenceVocabulary.join(', '));
      }
      setRefNotice(
        ref.source === 'CANONICAL'
          ? 'Loaded canonical curriculum rules & vocabulary from server repository!'
          : 'Synthesized reference curriculum aligned with CEFR standards.'
      );
      setShowCurriculumPanel(true);
    } catch (err: any) {
      console.error('Failed to load reference curriculum:', err);
      setRefNotice('Could not fetch reference curriculum. You can write your own below.');
    } finally {
      setIsLoadingRef(false);
    }
  };

  /**
   * @brief Handles file upload for rule or vocabulary context.
   */
  const handleFileUpload = async (
    e: React.ChangeEvent<HTMLInputElement>,
    type: 'RULE' | 'VOCABULARY'
  ) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (type === 'RULE') {
      setIsUploadingRule(true);
    } else {
      setIsUploadingVocab(true);
    }

    try {
      const res = await taskApi.uploadCurriculumFile(file, type, effectiveGrammarTopic);
      if (type === 'RULE') {
        setUploadedRuleFile({
          fileName: res.fileName,
          fileSize: res.fileSize,
          contentSnippet: res.contentSnippet,
          serverPath: res.serverPath,
        });
        if (res.fullContent) {
          setCustomRule(res.fullContent);
        }
      } else {
        setUploadedVocabFile({
          fileName: res.fileName,
          fileSize: res.fileSize,
          contentSnippet: res.contentSnippet,
          serverPath: res.serverPath,
        });
        if (res.fullContent) {
          setCustomVocabulary(res.fullContent);
        }
      }
      setShowCurriculumPanel(true);
    } catch (err: any) {
      console.error('Failed to upload curriculum file:', err);
      setError(err.response?.data?.message || 'Failed to upload file. Allowed: .txt, .md, .json, .csv');
    } finally {
      if (type === 'RULE') {
        setIsUploadingRule(false);
        if (ruleFileInputRef.current) ruleFileInputRef.current.value = '';
      } else {
        setIsUploadingVocab(false);
        if (vocabFileInputRef.current) vocabFileInputRef.current.value = '';
      }
    }
  };

  /**
   * @brief Event handler executing task generation.
   */
  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isQuotaExceeded) {
      openUpgradeWall('You have used all your free AI evaluations for today.');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const task = await taskApi.generateTask({
        cefrLevel,
        grammarTopic: effectiveGrammarTopic,
        domain,
        taskType,
        difficulty,
        numberOfQuestions,
        provider,
        modelName,
        customRule: customRule.trim() || undefined,
        customVocabulary: customVocabulary.trim() || undefined,
        ruleFilePath: uploadedRuleFile?.serverPath || undefined,
        vocabularyFilePath: uploadedVocabFile?.serverPath || undefined,
        ecoMode,
      });

      if (taskType === 'ESSAY') {
        navigate(`/student/essay/${task.id}`);
      } else {
        navigate(`/student/task/${task.id}`);
      }
    } catch (err: any) {
      console.error('Failed to generate task:', err);
      if (err.response?.status === 402 || err.response?.status === 429) {
        openUpgradeWall(err.response?.data?.message);
      } else {
        setError(err.response?.data?.message || 'Failed to generate task. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * @brief Resets all form fields to default values.
   */
  const handleClear = () => {
    setCefrLevel(user?.cefrLevel || 'B1');
    setSelectedTopicOption(
      DEFAULT_CEFR_TOPICS[user?.cefrLevel || 'B1']?.[0] || DEFAULT_CEFR_TOPICS.B1[0]
    );
    setCustomTopicText('');
    setDomain(DOMAINS[0]);
    setTaskType('MCQ');
    setDifficulty('MEDIUM');
    setNumberOfQuestions(5);
    setCustomRule('');
    setCustomVocabulary('');
    setUploadedRuleFile(null);
    setUploadedVocabFile(null);
    setRefNotice(null);
    setEcoMode(false);
    setError(null);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Generate Practice Task</h1>
        <p className="text-sm text-slate-500 mt-1">
          Customize your task parameters, enter custom topics, inject grammar rules and vocabulary context, and choose your preferred AI engine.
        </p>
      </div>

      {isLoading && (
        <div className="p-5 rounded-3xl bg-indigo-50/90 border border-indigo-200/80 shadow-sm space-y-3 animate-in fade-in duration-300">
          <div className="flex items-center space-x-3">
            <div className="relative flex items-center justify-center flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-indigo-500/20 animate-ping absolute" />
              <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center relative">
                <Sparkles className="w-4 h-4 animate-spin" />
              </div>
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Generating Custom CEFR {cefrLevel} Practice...
              </h3>
              <p className="text-xs text-indigo-700 font-medium">
                Engine: <span className="font-mono font-semibold">{provider} ({modelName})</span> · Topic: <span className="font-semibold">{effectiveGrammarTopic}</span>
              </p>
            </div>
          </div>
          <div className="bg-white/80 backdrop-blur-sm rounded-2xl p-3 border border-indigo-100 text-xs text-slate-600 space-y-1.5 leading-relaxed">
            <p className="flex items-center space-x-1.5">
              <Clock className="w-3.5 h-3.5 text-primary flex-shrink-0" />
              <span>
                Applying CEFR {cefrLevel} pedagogical constraints and domain vocabulary...
              </span>
            </p>
            {(customRule || customVocabulary) && (
              <p className="flex items-center space-x-1.5 text-emerald-700 font-medium">
                <FileCheck className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                <span>Injecting custom pedagogical rule and target vocabulary context...</span>
              </p>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="p-5 rounded-3xl bg-amber-50 border border-amber-200/80 shadow-sm space-y-3 animate-in fade-in duration-300">
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
                  className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 shadow-sm"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Retry Generation</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setProvider('GEMINI');
                    setModelName('gemini-3.5-flash-lite');
                  }}
                  className="px-3 py-1.5 rounded-xl bg-white hover:bg-amber-100/50 text-amber-800 border border-amber-300 font-semibold transition"
                >
                  Switch to Gemini 3.5 Flash Lite
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="px-3 py-1.5 rounded-xl bg-white hover:bg-slate-50 text-slate-700 border border-slate-300 font-semibold transition flex items-center space-x-1.5"
                >
                  <Key className="w-3.5 h-3.5 text-primary" />
                  <span>Configure BYOK Key</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <form onSubmit={handleGenerate} className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        {/* AI Engine Selection */}
        <div className="p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Cpu className="w-4 h-4 text-primary" />
              <span className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                AI Engine & Model Selection
              </span>
            </div>
            <span className="text-[11px] font-mono font-semibold text-primary bg-indigo-50 px-2.5 py-0.5 rounded-full">
              {provider} · {modelName}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-bold text-slate-500 uppercase mb-1">
                AI Provider
              </label>
              <select
                value={provider}
                onChange={(e) => handleProviderChange(e.target.value as AIProviderType)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-900 font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              >
                {AI_PROVIDER_CATALOG.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="flex items-center justify-between text-[11px] font-bold text-slate-500 uppercase mb-1">
                <span>AI Model</span>
                {isLiveSynced && (
                  <span className="text-[10px] font-semibold text-emerald-600 lowercase">
                    ● live vendor sync
                  </span>
                )}
              </label>
              <select
                value={modelName}
                onChange={(e) => setModelName(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-900 font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              >
                {providerModels.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.label} ({m.badge})
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {/* CEFR Level Selection */}
        <div>
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
            Target CEFR Level
          </label>
          <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
            {(['A1', 'A2', 'B1', 'B2', 'C1', 'C2'] as CefrLevel[]).map((level) => (
              <button
                type="button"
                key={level}
                onClick={() => handleCefrChange(level)}
                className={`py-2.5 px-3 rounded-2xl border text-xs sm:text-sm font-bold flex items-center justify-center space-x-1.5 transition ${
                  cefrLevel === level
                    ? 'border-primary bg-indigo-50/50 text-primary shadow-sm'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600'
                }`}
              >
                <span>{level}</span>
                <CefrBadge level={level} size="sm" />
              </button>
            ))}
          </div>
        </div>

        {/* Topic Selection with Standard, Mixed Challenges, and Custom Topic */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Practice Topic & Mixed Challenges
            </label>
            <button
              type="button"
              onClick={() => {
                if (isCustomTopic) {
                  setSelectedTopicOption(currentLevelTopics[0] || 'Present Simple');
                } else {
                  setSelectedTopicOption('__CUSTOM__');
                }
              }}
              className="text-xs font-semibold text-primary hover:text-primary-hover transition flex items-center space-x-1"
            >
              <span>{isCustomTopic ? '← Switch to Syllabus List' : '✏️ Enter Custom Topic'}</span>
            </button>
          </div>

          <select
            value={selectedTopicOption}
            onChange={(e) => setSelectedTopicOption(e.target.value)}
            className="w-full px-4 py-3 rounded-2xl border border-slate-200 bg-white text-slate-900 font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          >
            <optgroup label={`Core Syllabus Topics (CEFR ${cefrLevel})`}>
              {currentLevelTopics.map((topic) => (
                <option key={topic} value={topic}>
                  {topic}
                </option>
              ))}
            </optgroup>

            {currentLevelMixedTopics.length > 0 && (
              <optgroup label={`🔀 Mixed Challenges (CEFR ${cefrLevel})`}>
                {currentLevelMixedTopics.map((topic) => (
                  <option key={topic} value={topic}>
                    {topic}
                  </option>
                ))}
              </optgroup>
            )}

            {crossLevelTopics.length > 0 && (
              <optgroup label="🌐 Thematic & Cross-Level Challenges">
                {crossLevelTopics.map((topic) => (
                  <option key={topic} value={topic}>
                    {topic}
                  </option>
                ))}
              </optgroup>
            )}

            <optgroup label="Custom Practice">
              <option value="__CUSTOM__">✏️ Custom Topic (Enter your own)...</option>
            </optgroup>
          </select>

          {isCustomTopic && (
            <div className="mt-3 p-4 rounded-2xl bg-indigo-50/50 border border-indigo-100 space-y-2 animate-in fade-in duration-200">
              <label className="block text-xs font-bold text-indigo-900">
                Custom Practice Subject or Specialized Rule:
              </label>
              <input
                type="text"
                value={customTopicText}
                onChange={(e) => setCustomTopicText(e.target.value)}
                placeholder="e.g. Mixed Conditionals in Contract Law, Medical Passive Voice, Tech Startup Pitch Collocations..."
                className="w-full px-3.5 py-2.5 rounded-xl border border-indigo-200 bg-white text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
              <p className="text-[11px] text-indigo-700">
                💡 Tip: You can also attach your own reference grammar rules and vocabulary lists in the Curriculum Context section below.
              </p>
            </div>
          )}
        </div>

        {/* Eco Mode (Token Saver) Toggle */}
        <div className={`p-4 rounded-2xl border transition ${
          ecoMode
            ? 'bg-emerald-50/70 border-emerald-200'
            : 'bg-slate-50/80 border-slate-200/80'
        } flex items-center justify-between`}>
          <div className="flex items-center space-x-3">
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 transition ${
              ecoMode ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-500'
            }`}>
              <Leaf className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Eco Mode (Token Saver)
                </h4>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                  ecoMode ? 'bg-emerald-200 text-emerald-900' : 'bg-slate-200 text-slate-600'
                }`}>
                  {ecoMode ? 'ACTIVE' : 'OFF'}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 mt-0.5">
                {ecoMode
                  ? 'Omit verbose grammar rules & vocabulary lists from the AI prompt to accelerate generation and save quota.'
                  : 'Inject detailed curriculum rules and target vocabulary into the AI generation prompt.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setEcoMode(!ecoMode)}
            className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              ecoMode ? 'bg-emerald-600' : 'bg-slate-300'
            }`}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                ecoMode ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>

        {/* Curriculum Context (Rules & Vocabulary) Expandable Panel */}
        <div className={`rounded-2xl border transition overflow-hidden ${
          ecoMode ? 'border-slate-200 bg-slate-50/30 opacity-75' : 'border-slate-200/90 bg-slate-50/50'
        }`}>
          <button
            type="button"
            onClick={() => setShowCurriculumPanel(!showCurriculumPanel)}
            className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-100/60 transition"
          >
            <div className="flex items-center space-x-2.5">
              <div className="w-7 h-7 rounded-lg bg-indigo-100 text-primary flex items-center justify-center">
                <BookOpen className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Curriculum Rules & Vocabulary Context (Optional)
                </h3>
                <p className="text-[11px] text-slate-500">
                  {ecoMode
                    ? '🌱 Eco Mode is active: curriculum context will be skipped during generation'
                    : uploadedRuleFile || uploadedVocabFile || customRule || customVocabulary
                    ? '✓ Custom rules or vocabulary active'
                    : 'Inject specific grammar rules, word lists, or upload files (.txt, .md, .json)'}
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2 text-slate-400">
              {showCurriculumPanel ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </div>
          </button>

          {showCurriculumPanel && (
            <div className="p-4 sm:p-5 border-t border-slate-200/80 bg-white space-y-4 animate-in fade-in duration-200">
              <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-slate-100">
                <span className="text-xs text-slate-600 font-medium">
                  Provide exact target rules and vocabulary for the AI engine:
                </span>
                <button
                  type="button"
                  onClick={handleLoadReference}
                  disabled={isLoadingRef}
                  className="px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition flex items-center space-x-1.5 disabled:opacity-50"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>{isLoadingRef ? 'Loading Reference...' : '💡 Auto-Fill Canonical Reference'}</span>
                </button>
              </div>

              {refNotice && (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                  <span>{refNotice}</span>
                </div>
              )}

              {/* Target Grammar Rule Section */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-700 flex items-center space-x-1.5">
                    <span>1. Target Grammar Rule / Structural Guide</span>
                  </label>
                  <div className="flex items-center space-x-2">
                    <input
                      ref={ruleFileInputRef}
                      type="file"
                      accept=".txt,.md,.json"
                      className="hidden"
                      onChange={(e) => handleFileUpload(e, 'RULE')}
                    />
                    <button
                      type="button"
                      onClick={() => ruleFileInputRef.current?.click()}
                      disabled={isUploadingRule}
                      className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                    >
                      <UploadCloud className="w-3.5 h-3.5" />
                      <span>{isUploadingRule ? 'Uploading...' : 'Upload Rule File (.md, .txt)'}</span>
                    </button>
                  </div>
                </div>

                {uploadedRuleFile && (
                  <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                      <span className="font-semibold text-slate-800 truncate">{uploadedRuleFile.fileName}</span>
                      <span className="text-slate-500 font-mono text-[10px]">
                        ({(uploadedRuleFile.fileSize / 1024).toFixed(1)} KB)
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setUploadedRuleFile(null)}
                      className="text-slate-400 hover:text-rose-500 transition ml-2"
                      title="Remove uploaded rule file"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}

                <textarea
                  value={customRule}
                  onChange={(e) => setCustomRule(e.target.value)}
                  rows={3}
                  placeholder="e.g. Focus on Inversion after negative adverbials (Seldom, Rarely, Never before). The auxiliary verb must precede the subject: Seldom had they witnessed..."
                  className="w-full p-3 rounded-xl border border-slate-200 bg-slate-50/40 text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
                />
              </div>

              {/* Target Vocabulary Section */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-slate-700 flex items-center space-x-1.5">
                    <span>2. Target Vocabulary / Lexicon List</span>
                  </label>
                  <div className="flex items-center space-x-2">
                    <input
                      ref={vocabFileInputRef}
                      type="file"
                      accept=".txt,.json,.csv"
                      className="hidden"
                      onChange={(e) => handleFileUpload(e, 'VOCABULARY')}
                    />
                    <button
                      type="button"
                      onClick={() => vocabFileInputRef.current?.click()}
                      disabled={isUploadingVocab}
                      className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                    >
                      <UploadCloud className="w-3.5 h-3.5" />
                      <span>{isUploadingVocab ? 'Uploading...' : 'Upload Vocab File (.json, .csv)'}</span>
                    </button>
                  </div>
                </div>

                {uploadedVocabFile && (
                  <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                    <div className="flex items-center space-x-2 truncate">
                      <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                      <span className="font-semibold text-slate-800 truncate">{uploadedVocabFile.fileName}</span>
                      <span className="text-slate-500 font-mono text-[10px]">
                        ({(uploadedVocabFile.fileSize / 1024).toFixed(1)} KB)
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setUploadedVocabFile(null)}
                      className="text-slate-400 hover:text-rose-500 transition ml-2"
                      title="Remove uploaded vocab file"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                )}

                <textarea
                  value={customVocabulary}
                  onChange={(e) => setCustomVocabulary(e.target.value)}
                  rows={2}
                  placeholder="e.g. substantiate, empirical, paradigm, ubiquitous, resilient, compromise, consequence"
                  className="w-full p-3 rounded-xl border border-slate-200 bg-slate-50/40 text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
                />
              </div>
            </div>
          )}
        </div>

        {/* Vocabulary Domain */}
        <div>
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
            Vocabulary Domain
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {DOMAINS.map((d) => (
              <button
                type="button"
                key={d}
                onClick={() => setDomain(d)}
                className={`py-2.5 px-3 rounded-xl border text-xs font-semibold transition ${
                  domain === d
                    ? 'border-sky-500 bg-sky-50 text-sky-700'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600'
                }`}
              >
                {d}
              </button>
            ))}
          </div>
        </div>

        {/* Task Type */}
        <div>
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
            Task Type
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {(
              [
                { type: 'MCQ', label: 'Multiple Choice' },
                { type: 'GAP_FILL', label: 'Fill in Blanks' },
                { type: 'REWRITE', label: 'Sentence Rewrite' },
                { type: 'ESSAY', label: 'Essay Writing' },
              ] as { type: TaskType; label: string }[]
            ).map((item) => (
              <button
                type="button"
                key={item.type}
                onClick={() => setTaskType(item.type)}
                className={`py-2.5 px-3 rounded-xl border text-xs font-semibold text-center transition ${
                  taskType === item.type
                    ? 'border-primary bg-indigo-50 text-primary'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>

        {/* Difficulty and Question Count */}
        {taskType !== 'ESSAY' && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-2">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
                Difficulty
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['EASY', 'MEDIUM', 'HARD'] as DifficultyLevel[]).map((diff) => (
                  <button
                    type="button"
                    key={diff}
                    onClick={() => setDifficulty(diff)}
                    className={`py-2 rounded-xl text-xs font-bold border transition ${
                      difficulty === diff
                        ? 'border-indigo-600 bg-indigo-600 text-white'
                        : 'border-slate-200 text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    {diff}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                  Number of Questions
                </label>
                <span className="text-xs font-bold text-primary">{numberOfQuestions}</span>
              </div>
              <input
                type="range"
                min="3"
                max="10"
                value={numberOfQuestions}
                onChange={(e) => setNumberOfQuestions(Number(e.target.value))}
                className="w-full accent-primary h-2 bg-slate-100 rounded-lg cursor-pointer"
              />
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex items-center space-x-3 pt-4 border-t border-slate-100">
          <button
            type="submit"
            disabled={isLoading}
            className="flex-1 flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
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
            className="flex items-center justify-center space-x-1 py-3.5 px-4 rounded-2xl border border-slate-200 text-slate-600 hover:bg-slate-50 font-semibold text-sm transition"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Clear</span>
          </button>
        </div>
      </form>
    </div>
  );
};
