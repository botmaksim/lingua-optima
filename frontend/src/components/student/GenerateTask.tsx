/**
 * @file GenerateTask.tsx
 * @brief Dynamic task generator interface allowing students to generate CEFR-aligned exercises.
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, RotateCcw, AlertTriangle, Cpu, Clock, Key } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { taskApi } from '../../api/taskApi';
import { apiKeyApi, ApiKeyItem } from '../../api/apiKeyApi';
import { CefrLevel } from '../../types/user';
import { TaskType, DifficultyLevel } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import {
  AI_PROVIDER_CATALOG,
  AIProviderType,
  getDefaultModelForProvider,
  useProviderModels,
} from '../../constants/aiModels';

const CEFR_TOPICS: Record<CefrLevel, string[]> = {
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

  const [cefrLevel, setCefrLevel] = useState<CefrLevel>(user?.cefrLevel || 'A1');
  const [grammarTopic, setGrammarTopic] = useState<string>(
    CEFR_TOPICS[user?.cefrLevel || 'A1']?.[0] || CEFR_TOPICS.A1[0]
  );
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

  useEffect(() => {
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
   * @brief Event handler or helper executing handle cefr change.
   */
  const handleCefrChange = (newLevel: CefrLevel) => {
    setCefrLevel(newLevel);
    setGrammarTopic(CEFR_TOPICS[newLevel][0]);
  };

  /**
   * @brief Event handler or helper executing handle generate.
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
        grammarTopic,
        domain,
        taskType,
        difficulty,
        numberOfQuestions,
        provider,
        modelName,
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
   * @brief Event handler or helper executing handle clear.
   */
  const handleClear = () => {
    setCefrLevel(user?.cefrLevel || 'B1');
    setGrammarTopic(CEFR_TOPICS[user?.cefrLevel || 'B1'][0]);
    setDomain(DOMAINS[0]);
    setTaskType('MCQ');
    setDifficulty('MEDIUM');
    setNumberOfQuestions(5);
    setError(null);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Generate Practice Task</h1>
        <p className="text-sm text-slate-500 mt-1">
          Customize your task parameters and choose your preferred AI provider and model.
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
                Engine: <span className="font-mono font-semibold">{provider} ({modelName})</span> · Topic: <span className="font-semibold">{grammarTopic}</span>
              </p>
            </div>
          </div>
          <div className="bg-white/80 backdrop-blur-sm rounded-2xl p-3 border border-indigo-100 text-xs text-slate-600 space-y-1.5 leading-relaxed">
            <p className="flex items-center space-x-1.5">
              <Clock className="w-3.5 h-3.5 text-primary flex-shrink-0" />
              <span>Pedagogical task generation typically takes <strong>5–20 seconds</strong> depending on provider traffic.</span>
            </p>
            <p className="text-[11px] text-slate-500">
              Please keep this page open. If public AI traffic is elevated, automated failover seamlessly attempts backup high-availability models.
            </p>
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
              <p className="text-xs text-slate-600 leading-relaxed">
                {error}
              </p>
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

        <div>
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
            Grammar Topic
          </label>
          <select
            value={grammarTopic}
            onChange={(e) => setGrammarTopic(e.target.value)}
            className="w-full px-4 py-3 rounded-2xl border border-slate-200 bg-white text-slate-900 font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          >
            {CEFR_TOPICS[cefrLevel].map((topic) => (
              <option key={topic} value={topic}>
                {topic}
              </option>
            ))}
          </select>
        </div>

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
