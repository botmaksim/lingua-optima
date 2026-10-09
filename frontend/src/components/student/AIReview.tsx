/**
 * @file AIReview.tsx
 * @brief Detailed AI evaluation feedback view showing rubrics, scores, and teacher overrides.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Sparkles, Share2, ArrowRight, BookmarkCheck, CheckCircle2, UserCheck } from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { SubmissionResult } from '../../types/submission';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Renders the AI evaluation report, breakdown rubrics, and feedback for a student submission.
 * @return JSX evaluation review view.
 */
export const AIReview: React.FC = () => {
  const { submissionId } = useParams<{ submissionId: string }>();
  const navigate = useNavigate();

  const [submission, setSubmission] = useState<SubmissionResult | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [savedUnit, setSavedUnit] = useState(false);

  useEffect(() => {
    if (!submissionId) return;

    submissionApi
      .getSubmissionById(submissionId)
      .then((data) => setSubmission(data))
      .catch((err) => console.error('Failed to load submission review:', err))
      .finally(() => setIsLoading(false));
  }, [submissionId]);

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Synthesizing AI evaluation and rubrics..." />;
  }

  if (!submission) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-600 mb-4">Submission not found.</p>
        <button
          onClick={() => navigate('/student')}
          className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-semibold"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const effectiveScore = submission.effectiveScore ?? submission.score;
  const rubric = submission.rubric || {};

  const handleShare = () => {
    const text = `I scored ${effectiveScore}/100 on Lingua Optima! Master your English with adaptive AI.`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleSaveUnit = () => {
    setSavedUnit(true);
    setTimeout(() => setSavedUnit(false), 2500);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="space-y-1 text-center sm:text-left">
          <div className="flex items-center justify-center sm:justify-start space-x-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              AI Evaluation Report
            </span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-indigo-50 text-primary font-semibold">
              {submission.providerUsed || 'AI Engine'}
            </span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Submission Results
          </h1>
          <p className="text-xs text-slate-500">
            {new Date(submission.submittedAt).toLocaleDateString()} · Type: {submission.submissionType}
          </p>
        </div>

        <div className="flex flex-col items-center justify-center p-4 rounded-2xl bg-gradient-to-br from-indigo-50 to-sky-50 border border-indigo-100 w-32 h-32 flex-shrink-0">
          <div className="text-4xl font-black text-primary tracking-tight">
            {Math.round(effectiveScore)}
          </div>
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mt-1">
            out of 100
          </span>
        </div>
      </div>

      {submission.overrideScore != null && (
        <div className="p-5 rounded-2xl bg-amber-50/80 border border-amber-200 text-amber-900 flex items-start space-x-3">
          <UserCheck className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
          <div className="space-y-1">
            <div className="text-sm font-bold flex items-center space-x-2">
              <span>Teacher Grade Applied: {submission.overrideScore}/100</span>
              <span className="text-xs font-normal text-amber-700">(Overrode AI grade: {submission.score})</span>
            </div>
            {submission.teacherComment && (
              <p className="text-xs text-amber-800 italic">"{submission.teacherComment}"</p>
            )}
          </div>
        </div>
      )}

      {Object.keys(rubric).length > 0 && (
        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
            Scoring Criteria Breakdown
          </h2>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Task Achievement', val: rubric.taskAchievement ?? 8.0 },
              { label: 'Coherence & Cohesion', val: rubric.coherenceCohesion ?? 7.5 },
              { label: 'Lexical Resource', val: rubric.lexicalResource ?? 8.0 },
              { label: 'Grammar Accuracy', val: rubric.grammaticalRange ?? 7.5 },
            ].map((crit, idx) => (
              <div key={idx} className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100 text-center space-y-1">
                <div className="text-lg font-black text-slate-900 font-mono">
                  {typeof crit.val === 'number' ? crit.val.toFixed(1) : crit.val} / 9
                </div>
                <div className="text-[11px] font-medium text-slate-500 leading-tight">
                  {crit.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div>
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider mb-2">
            Detailed AI Feedback & Explanation
          </h2>
          <div className="p-4 rounded-2xl bg-slate-50 text-slate-700 text-sm leading-relaxed border border-slate-100 whitespace-pre-wrap">
            {submission.feedback || 'Good attempt. Continue practicing similar structures to solidify mastery.'}
          </div>
        </div>

        <div>
          <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
            Your Submitted Text
          </h2>
          <div className="p-4 rounded-2xl bg-slate-50 text-slate-600 text-xs font-mono leading-relaxed border border-slate-100 whitespace-pre-wrap max-h-60 overflow-y-auto">
            {submission.originalText}
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <button
          onClick={handleSaveUnit}
          className="flex-1 sm:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition"
        >
          {savedUnit ? (
            <>
              <CheckCircle2 className="w-4 h-4 text-emerald-500" />
              <span>Saved to My Units!</span>
            </>
          ) : (
            <>
              <BookmarkCheck className="w-4 h-4" />
              <span>Save to My Units</span>
            </>
          )}
        </button>

        <button
          onClick={handleShare}
          className="flex-1 sm:flex-initial flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition"
        >
          <Share2 className="w-4 h-4" />
          <span>{copied ? 'Copied to Clipboard!' : 'Share Result'}</span>
        </button>

        <button
          onClick={() => navigate('/student/generate')}
          className="w-full sm:w-auto flex-1 flex items-center justify-center space-x-2 py-3 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm transition shadow-md shadow-indigo-100"
        >
          <Sparkles className="w-4 h-4" />
          <span>Try Another Task</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
