/**
 * @file SubmissionsReview.tsx
 * @brief Educator submission grading and AI evaluation override interface.
 */

import React, { useState, useEffect } from 'react';
import { CheckCircle, Edit3, ChevronDown, ChevronUp, X } from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { SubmissionResult } from '../../types/submission';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNotificationStore } from '../../store/notificationStore';
import { formatDate } from '../../utils/formatDate';

/**
 * @brief Educator component reviewing student homework and adjusting scores.
 * @return React component element.
 */
export const SubmissionsReview: React.FC = () => {
  const { addToast } = useNotificationStore();
  const [submissions, setSubmissions] = useState<SubmissionResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const [editingSub, setEditingSub] = useState<SubmissionResult | null>(null);
  const [newScore, setNewScore] = useState<number>(85);
  const [teacherComment, setTeacherComment] = useState<string>('');
  const [isSubmittingOverride, setIsSubmittingOverride] = useState(false);

  useEffect(() => {
    if (!editingSub) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setEditingSub(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [editingSub]);

  /**
   * @brief Event handler or helper executing load submissions.
   */
  const loadSubmissions = async () => {
    try {
      const data = await submissionApi.getTeacherSubmissions();
      setSubmissions(data);
    } catch (err) {
      console.error('Failed to load educator submissions queue:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadSubmissions();
  }, []);

  /**
   * @brief Event handler or helper executing handle open override.
   */
  const handleOpenOverride = (sub: SubmissionResult) => {
    setEditingSub(sub);
    setNewScore(sub.overrideScore ?? sub.score);
    setTeacherComment(sub.teacherComment || 'Great improvement. Well reasoned argument.');
  };

  /**
   * @brief Event handler or helper executing handle save override.
   */
  const handleSaveOverride = async () => {
    if (!editingSub) return;
    setIsSubmittingOverride(true);
    try {
      await submissionApi.overrideScore(editingSub.id, {
        overrideScore: Number(newScore),
        teacherComment,
      });
      setEditingSub(null);
      addToast({
        type: 'success',
        title: 'Grade Updated',
        message: 'Student score and teacher feedback have been saved.',
      });
      await loadSubmissions();
    } catch (err) {
      console.error('Failed to save score override:', err);
      addToast({
        type: 'error',
        title: 'Update Failed',
        message: 'Could not save the grade override. Please try again.',
      });
    } finally {
      setIsSubmittingOverride(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle approve ai grade.
   */
  const handleApproveAiGrade = async (sub: SubmissionResult) => {
    try {
      await submissionApi.overrideScore(sub.id, {
        overrideScore: sub.score,
        teacherComment: 'AI grade approved by teacher.',
      });
      addToast({
        type: 'success',
        title: 'Grade Approved',
        message: 'AI evaluation score confirmed for this submission.',
      });
      await loadSubmissions();
    } catch (err) {
      console.error('Failed to approve AI grade:', err);
      addToast({
        type: 'error',
        title: 'Approval Failed',
        message: 'Could not approve the AI grade. Please try again.',
      });
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading submissions awaiting review..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Submissions Review</h1>
        <p className="text-sm text-slate-500 mt-1">
          Review student exercises and essays. Override scores, approve grades, and provide customized feedback.
        </p>
      </div>

      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {submissions.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-sm">
            No student submissions to review.
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {submissions.map((sub) => {
              const isExpanded = expandedId === sub.id;
              const effectiveScore = sub.overrideScore ?? sub.score;

              return (
                <div key={sub.id} className="p-5 hover:bg-slate-50/50 transition space-y-3">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center space-x-2">
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-50 text-primary">
                          {sub.submissionType}
                        </span>
                        <span className="text-xs text-slate-400">
                          {formatDate(sub.submittedAt)}
                        </span>
                        {sub.overrideScore != null ? (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700">
                            Teacher Graded
                          </span>
                        ) : (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-amber-50 text-amber-700">
                            AI Graded
                          </span>
                        )}
                      </div>
                      <p className="text-sm font-bold text-slate-800 line-clamp-1">
                        {sub.originalText}
                      </p>
                    </div>

                    <div className="flex items-center space-x-4 self-end sm:self-center">
                      <div className="text-right">
                        <div className="text-lg font-black text-primary font-mono">
                          {Math.round(effectiveScore)}
                        </div>
                        <span className="text-[10px] text-slate-400 font-bold uppercase">
                          Score / 100
                        </span>
                      </div>

                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => handleOpenOverride(sub)}
                          className="flex items-center space-x-1 py-1.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                          <span>Override</span>
                        </button>
                        <button
                          onClick={() => handleApproveAiGrade(sub)}
                          className="flex items-center space-x-1 py-1.5 px-3 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-semibold transition"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>Approve</span>
                        </button>
                        <button
                          onClick={() => setExpandedId(isExpanded ? null : sub.id)}
                          className="p-1.5 rounded-xl hover:bg-slate-100 text-slate-400 transition"
                        >
                          {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="pt-3 border-t border-slate-100 space-y-3 text-xs animate-in fade-in duration-150">
                      <div>
                        <span className="font-bold text-slate-500 uppercase">Student Answer Text:</span>
                        <div className="p-3 mt-1 bg-slate-50 rounded-xl font-mono text-slate-700 whitespace-pre-wrap">
                          {sub.originalText}
                        </div>
                      </div>

                      <div>
                        <span className="font-bold text-slate-500 uppercase">AI Diagnostic Feedback:</span>
                        <div className="p-3 mt-1 bg-indigo-50/50 rounded-xl text-slate-700 leading-relaxed whitespace-pre-wrap">
                          {sub.feedback}
                        </div>
                      </div>

                      {sub.teacherComment && (
                        <div>
                          <span className="font-bold text-amber-700 uppercase">Teacher Comment:</span>
                          <div className="p-3 mt-1 bg-amber-50 rounded-xl text-amber-900 italic">
                            "{sub.teacherComment}"
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {editingSub && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-md p-4 animate-in fade-in duration-150"
          onClick={() => setEditingSub(null)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="override-modal-title"
        >
          <div
            className="relative overflow-hidden bg-white/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl shadow-slate-950/25 border border-slate-200/80 ring-1 ring-slate-900/5 space-y-5 animate-in zoom-in-95 duration-150"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="absolute inset-x-0 top-0 h-1.5 bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-500" />

            <button
              type="button"
              onClick={() => setEditingSub(null)}
              className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition"
              aria-label="Close"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-2xl bg-indigo-50 text-primary flex items-center justify-center ring-4 ring-indigo-500/10 border border-indigo-200/60 shrink-0">
                <Edit3 className="w-5 h-5" />
              </div>
              <div className="min-w-0 pr-6">
                <span className="inline-block text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200/60 mb-0.5">
                  Teacher Assessment
                </span>
                <h3 id="override-modal-title" className="text-lg font-black text-slate-900 tracking-tight leading-snug">
                  Override Grade & Feedback
                </h3>
              </div>
            </div>

            <div className="bg-slate-50/90 rounded-2xl p-3 border border-slate-200/70 flex items-center justify-between text-xs">
              <span className="text-slate-600 truncate max-w-[220px]">
                {editingSub.grammarTopic || 'Grammar Practice'}
              </span>
              <span className="font-mono font-bold text-slate-500">
                AI Score: <span className="text-primary font-black">{editingSub.score}%</span>
              </span>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5 tracking-wider">
                New Score (0 - 100)
              </label>
              <input
                type="number"
                min="0"
                max="100"
                value={newScore}
                onChange={(e) => setNewScore(Number(e.target.value))}
                className="w-full px-4 py-2.5 rounded-xl border border-slate-200/90 bg-white text-sm font-mono font-bold focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5 tracking-wider">
                Teacher Comment for Student
              </label>
              <textarea
                rows={4}
                value={teacherComment}
                onChange={(e) => setTeacherComment(e.target.value)}
                placeholder="Write specific feedback to help the student improve..."
                className="w-full p-3.5 rounded-xl border border-slate-200/90 bg-white text-xs text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
              />
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setEditingSub(null)}
                className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveOverride}
                disabled={isSubmittingOverride}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white transition shadow-md shadow-indigo-500/20 disabled:opacity-50"
              >
                {isSubmittingOverride ? 'Saving...' : 'Apply Grade'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
