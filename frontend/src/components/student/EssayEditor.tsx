/**
 * @file EssayEditor.tsx
 * @brief Rich essay editor providing real-time word counting, automatic drafts, and submission for rubric evaluation.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FileText, Save, Send, CheckCircle2, AlertCircle, Clock, RotateCcw } from 'lucide-react';
import { taskApi } from '../../api/taskApi';
import { submissionApi } from '../../api/submissionApi';
import { Task } from '../../types/task';
import { useDraft } from '../../hooks/useDraft';
import { getWordCount } from '../../utils/wordCount';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { sanitizeTaskContent } from '../../utils/textSanitizer';

/**
 * @brief Interactive essay composing workspace with client-side auto-saving and word counting.
 * @return JSX essay editor view.
 */
export const EssayEditor: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();

  const [task, setTask] = useState<Task | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const draftKey = `essay_${taskId || 'custom'}`;
  const { content, isSaved, updateContent, saveNow, clearDraft } = useDraft(draftKey);

  useEffect(() => {
    if (!taskId) {
      setIsLoading(false);
      return;
    }

    taskApi
      .getTaskById(taskId)
      .then((data) => setTask(data))
      .catch((err) => console.error('Failed to load essay assignment:', err))
      .finally(() => setIsLoading(false));
  }, [taskId]);

  const wordCount = getWordCount(content);
  const minWords = 250;
  const isWordCountMet = wordCount >= minWords;

  /**
   * @brief Event handler or helper executing handle submit.
   */
  const handleSubmit = async () => {
    if (!content.trim()) {
      setError('Please write your essay before submitting.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      const result = await submissionApi.submitText({
        taskId: task?.id,
        text: content,
        type: 'ESSAY',
      });
      await clearDraft();
      navigate(`/student/review/${result.id}`);
    } catch (err: any) {
      console.error('Failed to submit essay:', err);
      setError(err.response?.data?.message || 'Failed to submit essay. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading essay assignment..." />;
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Essay Studio</h1>
          <p className="text-sm text-slate-500 mt-1">
            Write your essay. Evaluated on Task Achievement, Coherence, Lexical Resource, and Grammatical Range.
          </p>
        </div>

        {task && <CefrBadge level={task.cefrLevel} size="md" />}
      </div>

      {task && (
        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-2">
          <div className="flex items-center space-x-2 text-xs font-bold text-slate-400 uppercase tracking-wider">
            <FileText className="w-4 h-4 text-primary" />
            <span>Essay Topic · {task.domain}</span>
          </div>
          <h2 className="text-base font-bold text-slate-900">{task.grammarTopic}</h2>
          <p className="text-sm text-slate-600 bg-slate-50 p-4 rounded-2xl border border-slate-100 leading-relaxed whitespace-pre-wrap">
            {sanitizeTaskContent(
              task.content,
              task.grammarTopic
                ? `Write an essay discussing ${task.grammarTopic.toLowerCase()} in relation to ${task.domain.toLowerCase()}. Provide clear arguments and relevant examples.`
                : undefined
            )}
          </p>
          {task.questions && task.questions.length > 0 && (
            <div className="pt-2">
              <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Guiding Questions to Address:
              </h4>
              <ul className="space-y-1.5 text-xs text-slate-600 bg-slate-50/70 p-3 rounded-xl border border-slate-100">
                {task.questions.map((q, idx) => (
                  <li key={q.id || idx} className="flex items-start space-x-2">
                    <span className="font-bold text-primary">{idx + 1}.</span>
                    <span>{q.questionText}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {isSubmitting && (
        <div className="p-5 rounded-3xl bg-indigo-50/90 border border-indigo-200/80 shadow-sm space-y-3 animate-in fade-in duration-300">
          <div className="flex items-center space-x-3">
            <div className="relative flex items-center justify-center flex-shrink-0">
              <div className="w-8 h-8 rounded-full bg-indigo-500/20 animate-ping absolute" />
              <div className="w-8 h-8 rounded-full bg-primary text-white flex items-center justify-center relative">
                <Send className="w-4 h-4 animate-spin" />
              </div>
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                Evaluating Essay with IELTS/Cambridge Rubric...
              </h3>
              <p className="text-xs text-indigo-700 font-medium">
                Scoring Task Achievement, Coherence, Lexical Resource, and Grammatical Range.
              </p>
            </div>
          </div>
          <div className="bg-white/80 backdrop-blur-sm rounded-2xl p-3 border border-indigo-100 text-xs text-slate-600 space-y-1.5 leading-relaxed">
            <p className="flex items-center space-x-1.5">
              <Clock className="w-3.5 h-3.5 text-primary flex-shrink-0" />
              <span>Full linguistic scoring and sentence-level corrections take <strong>10–25 seconds</strong>. Please do not close this page.</span>
            </p>
            <p className="text-[11px] text-slate-500">
              Your essay draft remains safely saved locally in your browser cache.
            </p>
          </div>
        </div>
      )}

      {error && (
        <div className="p-5 rounded-3xl bg-amber-50 border border-amber-200/80 shadow-sm space-y-3 animate-in fade-in duration-300">
          <div className="flex items-start space-x-3">
            <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center flex-shrink-0 mt-0.5">
              <AlertCircle className="w-4 h-4" />
            </div>
            <div className="space-y-1.5 flex-1">
              <h3 className="text-sm font-bold text-slate-900">
                {error.toLowerCase().includes('queue') || error.toLowerCase().includes('temporarily')
                  ? 'AI Service Queue · High Demand'
                  : 'Submission Error'}
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                {error}
              </p>
              <div className="pt-2 flex flex-wrap gap-2 text-xs">
                <button
                  type="button"
                  onClick={handleSubmit}
                  className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 shadow-sm"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Retry Evaluation</span>
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/profile')}
                  className="px-3 py-1.5 rounded-xl bg-white hover:bg-slate-50 text-slate-700 border border-slate-300 font-semibold transition"
                >
                  Configure BYOK API Key
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {task?.canSubmit === false && (
        <div className="p-5 rounded-3xl bg-emerald-50 border border-emerald-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start space-x-3">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="text-sm font-bold text-emerald-950">
                Essay Assignment Completed (1 of 1 Attempt Used)
              </h3>
              <p className="text-xs text-emerald-800 mt-0.5">
                Your teacher configured this essay assignment for a single attempt.
              </p>
            </div>
          </div>
          {task.latestSubmissionId && (
            <button
              type="button"
              onClick={() => navigate(`/student/review/${task.latestSubmissionId}`)}
              className="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition shadow-sm flex-shrink-0"
            >
              View Graded Results
            </button>
          )}
        </div>
      )}

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-4">
        <div className="flex items-center justify-between text-xs font-medium border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <span
              className={`px-2.5 py-1 rounded-full font-bold font-mono ${
                isWordCountMet
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'bg-amber-50 text-amber-700 border border-amber-200'
              }`}
            >
              {wordCount} / {minWords} words
            </span>
            <span className="text-slate-400">
              {isWordCountMet ? 'Word count target reached' : `Write ${minWords - wordCount} more words`}
            </span>
          </div>

          <div className="flex items-center space-x-1.5 text-slate-400">
            {isSaved ? (
              <>
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                <span>Auto-saved to draft</span>
              </>
            ) : (
              <span>Saving draft...</span>
            )}
          </div>
        </div>

        <textarea
          rows={16}
          disabled={task?.canSubmit === false}
          value={content}
          onChange={(e) => updateContent(e.target.value)}
          placeholder="Start writing your essay here... (Auto-saves every 30 seconds)"
          className="w-full p-4 rounded-2xl border border-slate-200 text-slate-800 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-y font-sans disabled:opacity-60"
        />

        <div className="flex items-center justify-between pt-2">
          <button
            type="button"
            onClick={saveNow}
            disabled={task?.canSubmit === false}
            className="flex items-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>Save Draft</span>
          </button>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting || task?.canSubmit === false}
            className="flex items-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
          >
            {isSubmitting ? (
              <LoadingSpinner size="sm" className="p-0 text-white" />
            ) : (
              <>
                <Send className="w-4 h-4" />
                <span>{task?.canSubmit === false ? 'Attempt Already Submitted' : 'Submit for Scoring'}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
