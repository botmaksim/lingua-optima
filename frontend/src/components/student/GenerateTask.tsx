/**
 * @file GenerateTask.tsx
 * @brief Dynamic task generator interface allowing students to generate CEFR-aligned exercises.
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, RotateCcw, AlertTriangle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useUsage } from '../../hooks/useUsage';
import { useUIStore } from '../../store/uiStore';
import { taskApi } from '../../api/taskApi';
import { CefrLevel } from '../../types/user';
import { TaskType, DifficultyLevel } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';

const CEFR_TOPICS: Record<CefrLevel, string[]> = {
  B1: [
    'Present Perfect vs Past Simple',
    'Conditionals (First & Second)',
    'Modal Verbs of Obligation',
    'Passive Voice (Basic)',
    'Relative Clauses (Defining)',
    'Used to & Would',
  ],
  B2: [
    'Third & Mixed Conditionals',
    'Passive Voice (Advanced & Causative)',
    'Inversion for Emphasis',
    'Participle Clauses',
    'Subjunctive & Wish Structures',
    'Advanced Modal Verbs',
  ],
  C1: [
    'Cleft Sentences',
    'Inversion with Negative Adverbials',
    'Complex Gerunds & Infinitives',
    'Advanced Subjunctive',
    'Discourse Markers & Nuance',
    'Ellipsis & Substitution',
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

  const [cefrLevel, setCefrLevel] = useState<CefrLevel>(user?.cefrLevel || 'B1');
  const [grammarTopic, setGrammarTopic] = useState<string>(CEFR_TOPICS[cefrLevel][0]);
  const [domain, setDomain] = useState<string>(DOMAINS[0]);
  const [taskType, setTaskType] = useState<TaskType>('MCQ');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
  const [numberOfQuestions, setNumberOfQuestions] = useState<number>(5);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleCefrChange = (newLevel: CefrLevel) => {
    setCefrLevel(newLevel);
    setGrammarTopic(CEFR_TOPICS[newLevel][0]);
  };

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
          Customize your task parameters. Our AI fallback chain (Groq & Gemini) will generate curriculum-aligned exercises.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center space-x-2">
          <AlertTriangle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleGenerate} className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div>
          <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">
            Target CEFR Level
          </label>
          <div className="grid grid-cols-3 gap-3">
            {(['B1', 'B2', 'C1'] as CefrLevel[]).map((level) => (
              <button
                type="button"
                key={level}
                onClick={() => handleCefrChange(level)}
                className={`py-3 px-4 rounded-2xl border text-sm font-bold flex items-center justify-center space-x-2 transition ${
                  cefrLevel === level
                    ? 'border-primary bg-indigo-50/50 text-primary shadow-sm'
                    : 'border-slate-200 hover:border-slate-300 text-slate-600'
                }`}
              >
                <span>Level</span>
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
