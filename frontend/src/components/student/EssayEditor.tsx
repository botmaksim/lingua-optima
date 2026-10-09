/**
 * @file EssayEditor.tsx
 * @brief Rich essay editor providing real-time word counting, automatic drafts, and submission for rubric evaluation.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FileText, Save, Send, CheckCircle2, AlertCircle } from 'lucide-react';
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
        </div>
      )}

      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-sm flex items-center space-x-2">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{error}</span>
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
          value={content}
          onChange={(e) => updateContent(e.target.value)}
          placeholder="Start writing your essay here... (Auto-saves every 30 seconds)"
          className="w-full p-4 rounded-2xl border border-slate-200 text-slate-800 text-sm leading-relaxed focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-y font-sans"
        />

        <div className="flex items-center justify-between pt-2">
          <button
            type="button"
            onClick={saveNow}
            className="flex items-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition"
          >
            <Save className="w-4 h-4" />
            <span>Save Draft</span>
          </button>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="flex items-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
          >
            {isSubmitting ? (
              <LoadingSpinner size="sm" className="p-0 text-white" />
            ) : (
              <>
                <Send className="w-4 h-4" />
                <span>Submit for Scoring</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
