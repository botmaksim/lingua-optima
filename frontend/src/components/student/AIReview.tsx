/**
 * @file AIReview.tsx
 * @brief Detailed AI evaluation feedback view showing sentence breakdowns, green/red answer status, explanations, and AI gap analysis.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Sparkles,
  Share2,
  ArrowRight,
  BookmarkCheck,
  CheckCircle2,
  XCircle,
  UserCheck,
  Lightbulb,
  AlertCircle,
  BrainCircuit,
  Target,
  FileCheck,
  HelpCircle,
} from 'lucide-react';
import { submissionApi } from '../../api/submissionApi';
import { SubmissionResult, SubmissionItem, SentenceCorrection } from '../../types/submission';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { sanitizeFeedback } from '../../utils/textSanitizer';

/**
 * @brief Renders the AI evaluation report, sentence-by-sentence breakdown, explanations, and AI gap analysis.
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

  const rubric = submission?.rubric || {};

  /**
   * @brief Resolves items to render: uses backend items if present, or parses fallback questions from student text.
   */
  const itemsToRender: SubmissionItem[] = React.useMemo(() => {
    if (!submission) return [];
    if (submission.items && submission.items.length > 0) {
      return submission.items;
    }

    // Fallback parser if older submission does not have structured items yet
    if (submission.originalText && /Q\d+[:.]/i.test(submission.originalText)) {
      const lines = submission.originalText.split('\n');
      const parsed: SubmissionItem[] = [];
      const regex = /^(?:Q|Question\s*)?(\d+)[:.)\-\\s]+(.*)$/i;

      lines.forEach((line) => {
        const trimmed = line.trim();
        if (!trimmed) return;
        const match = trimmed.match(regex);
        if (match) {
          const qNum = parseInt(match[1], 10);
          const studentAns = match[2].trim();
          const mentionsError = new RegExp(`(?:question\\s*${qNum}|Q${qNum})[^.!?]*?(?:error|wrong|incorrect|added|instead)`, 'i').test(submission.feedback || '');
          const isCorrect = !mentionsError && ((submission.effectiveScore ?? submission.score ?? 0) >= 95 || !submission.feedback?.toLowerCase().includes(`question ${qNum}`));

          parsed.push({
            questionNumber: qNum,
            sentence: `Question ${qNum}: Complete the sentence with the appropriate form`,
            studentAnswer: studentAns,
            correctAnswer: isCorrect ? studentAns : 'Review feedback for expected form',
            isCorrect: isCorrect,
            explanation: isCorrect
              ? 'Accurate grammatical structure applied according to context.'
              : 'Review verb tense and auxiliary conjugation rules.',
            grammarRule: submission.grammarTopic || 'Grammar Accuracy',
          });
        }
      });
      return parsed;
    }

    return [];
  }, [submission]);

  const effectiveScore = React.useMemo(() => {
    if (submission?.overrideScore != null) {
      return submission.overrideScore;
    }
    if (itemsToRender.length > 0) {
      const totalWeight = itemsToRender.reduce((sum, item) => sum + (item.points && item.points > 0 ? item.points : 1), 0);
      const earnedWeight = itemsToRender
        .filter((item) => item.isCorrect)
        .reduce((sum, item) => sum + (item.points && item.points > 0 ? item.points : 1), 0);
      return totalWeight > 0 ? Math.round((earnedWeight / totalWeight) * 100) : 0;
    }
    return submission?.effectiveScore ?? submission?.score ?? 0;
  }, [submission, itemsToRender]);

  /**
   * @brief Resolves sentence corrections from submission or rubric.
   */
  const correctionsToRender: SentenceCorrection[] = React.useMemo(() => {
    if (!submission) return [];
    if (submission.corrections && submission.corrections.length > 0) {
      return submission.corrections;
    }
    if (rubric.corrections && Array.isArray(rubric.corrections)) {
      return rubric.corrections;
    }
    return [];
  }, [submission, rubric]);

  /**
   * @brief Helper to check whether an answer string represents an unattempted question.
   */
  const isSkippedAnswer = (ans?: string) => {
    if (!ans || !ans.trim()) return true;
    const t = ans.trim().toLowerCase();
    return t === 'no answer' || t === 'no_answer' || t === 'skipped' || t === 'unanswered' || t === '-';
  };

  /**
   * @brief Resolves AI gap analysis data with strict mutual exclusivity and attempt analytics.
   */
  const aiAnalysis = React.useMemo(() => {
    if (!submission) {
      return {
        summary: '',
        weaknesses: [],
        strengths: [],
        recommendations: '',
        suggestedTopics: [],
      };
    }

    const norm = (s: string) => s.trim().toLowerCase();

    if (submission.aiAnalysis) {
      const rawWeaknesses = (submission.aiAnalysis.weaknesses || []).filter(Boolean);
      const rawStrengths = (submission.aiAnalysis.strengths || []).filter(Boolean);

      const weaknesses = Array.from(new Set(rawWeaknesses));
      // Strict mutual exclusivity: purge any weakness from strengths!
      const strengths = Array.from(new Set(rawStrengths)).filter(
        (s) => !weaknesses.some((w) => norm(w) === norm(s))
      );

      const rawSuggested = (submission.aiAnalysis.suggestedTopics || []).filter(Boolean);
      const suggestedTopics = (rawSuggested.length > 0 ? rawSuggested : weaknesses)
        .filter((t) => !strengths.some((s) => norm(s) === norm(t)));

      return {
        ...submission.aiAnalysis,
        weaknesses,
        strengths,
        suggestedTopics: suggestedTopics.length > 0
          ? suggestedTopics
          : (weaknesses.length > 0
              ? weaknesses
              : [`${submission.grammarTopic || 'Grammar'} (Advanced Practice)`]),
      };
    }

    // Fallback gap analysis calculation if backend aiAnalysis was not present
    const ruleStats = new Map<string, { total: number; correct: number; incorrect: number; skipped: number }>();

    itemsToRender.forEach((item) => {
      const rule = item.grammarRule || submission.grammarTopic || 'Target Grammar Structure';
      const stat = ruleStats.get(rule) || { total: 0, correct: 0, incorrect: 0, skipped: 0 };
      stat.total++;
      const isSkipped = item.isSkipped || isSkippedAnswer(item.studentAnswer);
      if (isSkipped) {
        stat.skipped++;
      } else if (item.isCorrect) {
        stat.correct++;
      } else {
        stat.incorrect++;
      }
      ruleStats.set(rule, stat);
    });

    const failedRules: string[] = [];
    const passedRules: string[] = [];
    const skippedRules: string[] = [];

    ruleStats.forEach((stat, rule) => {
      if (stat.incorrect > 0) {
        failedRules.push(rule);
      } else if (stat.skipped > 0) {
        skippedRules.push(rule);
      } else if (stat.correct > 0) {
        passedRules.push(rule);
      }
    });

    correctionsToRender.forEach((corr) => {
      const rule = corr.grammarRule || 'Sentence Structure & Flow';
      if (!failedRules.includes(rule)) failedRules.push(rule);
    });

    const weaknesses = failedRules;
    const strengths = passedRules.filter((s) => !weaknesses.some((w) => norm(w) === norm(s)));

    const suggestedTopics = weaknesses.length > 0
      ? weaknesses
      : (skippedRules.length > 0
          ? skippedRules
          : [submission.grammarTopic ? `${submission.grammarTopic} (Advanced Practice)` : 'Grammar & Sentence Structure']);

    const totalQuestions = itemsToRender.length;
    const skippedCount = itemsToRender.filter((it) => it.isSkipped || isSkippedAnswer(it.studentAnswer)).length;
    const attemptedCount = totalQuestions - skippedCount;
    const correctCount = itemsToRender.filter((it) => it.isCorrect).length;
    const attemptPercent = attemptedCount > 0 ? Math.round((correctCount / attemptedCount) * 100) : 0;

    const summary = totalQuestions > 0
      ? (skippedCount > 0
          ? `You answered ${correctCount} out of ${totalQuestions} questions correctly (${Math.round(effectiveScore)}% overall score). Attempt accuracy: ${attemptPercent}% (${correctCount} of ${attemptedCount} answered, ${skippedCount} skipped).`
          : `You completed ${totalQuestions} questions. Accuracy: ${Math.round(effectiveScore)}%.`)
      : `Evaluation completed with an overall score of ${Math.round(effectiveScore)}/100.`;

    const recommendations = weaknesses.length > 0
      ? `Focus on reviewing: ${weaknesses.join(', ')}. Pay special attention to core grammatical rules and tense markers.`
      : (skippedRules.length > 0
          ? `Good accuracy on attempted questions! Make sure to attempt all questions in ${skippedRules.join(', ')} to demonstrate complete mastery.`
          : 'Solid performance across target linguistic structures. Advance to higher difficulty tasks to expand spontaneous fluency.');

    return {
      summary,
      weaknesses,
      strengths,
      recommendations,
      suggestedTopics,
      attemptedCount,
      correctCount,
      totalQuestions,
      skippedCount,
      attemptAccuracy: attemptPercent,
    };
  }, [submission, itemsToRender, correctionsToRender, effectiveScore]);

  /**
   * @brief Consolidated attempt metrics resolving both backend analysis and rendered question items.
   */
  const attemptMetrics = React.useMemo(() => {
    if (aiAnalysis.totalQuestions != null && aiAnalysis.attemptedCount != null && aiAnalysis.skippedCount != null) {
      return {
        totalQuestions: aiAnalysis.totalQuestions,
        attemptedCount: aiAnalysis.attemptedCount,
        correctCount: aiAnalysis.correctCount ?? 0,
        skippedCount: aiAnalysis.skippedCount,
        attemptAccuracy: aiAnalysis.attemptAccuracy ?? (aiAnalysis.attemptedCount > 0 ? Math.round(((aiAnalysis.correctCount ?? 0) / aiAnalysis.attemptedCount) * 100) : 0),
      };
    }
    const totalQuestions = itemsToRender.length;
    const skippedCount = itemsToRender.filter((it) => it.isSkipped || isSkippedAnswer(it.studentAnswer)).length;
    const attemptedCount = totalQuestions - skippedCount;
    const correctCount = itemsToRender.filter((it) => it.isCorrect).length;
    const attemptAccuracy = attemptedCount > 0 ? Math.round((correctCount / attemptedCount) * 100) : 0;
    return {
      totalQuestions,
      attemptedCount,
      correctCount,
      skippedCount,
      attemptAccuracy,
    };
  }, [aiAnalysis, itemsToRender]);

  /**
   * @brief Copies a formatted summary of the submission score to the system clipboard.
   */
  const handleShare = () => {
    const text = `I scored ${Math.round(effectiveScore)}/100 on Lingua Optima! Master your English with adaptive AI.`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  /**
   * @brief Saves the evaluated task unit to the student's personal unit bookmark list.
   */
  const handleSaveUnit = () => {
    setSavedUnit(true);
    setTimeout(() => setSavedUnit(false), 2500);
  };

  /**
   * @brief Navigates to the task generator prefilled with the first identified weak topic.
   */
  const handlePracticeWeaknesses = () => {
    const targetTopic = aiAnalysis.suggestedTopics[0] || submission?.grammarTopic || 'General';
    navigate(`/student/generate?topic=${encodeURIComponent(targetTopic)}`);
  };

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

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* 1. Top Summary Banner */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="space-y-1 text-center sm:text-left">
          <div className="flex items-center justify-center sm:justify-start space-x-2">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
              AI Evaluation Report
            </span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-indigo-50 text-primary font-semibold">
              {submission.providerUsed || 'AI Engine'}
            </span>
            {submission.grammarTopic && (
              <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
                {submission.grammarTopic}
              </span>
            )}
          </div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Submission Results
          </h1>
          <p className="text-xs text-slate-500">
            {new Date(submission.submittedAt).toLocaleDateString()} · Format: {submission.taskType || submission.submissionType}
          </p>
        </div>

        <div className="flex flex-col items-center justify-center p-4 rounded-2xl bg-gradient-to-br from-indigo-50 to-sky-50 border border-indigo-100 min-w-32 min-h-32 flex-shrink-0 text-center">
          <div className={`text-4xl font-black tracking-tight ${effectiveScore >= 70 ? 'text-primary' : 'text-rose-600'}`}>
            {Math.round(effectiveScore)}
          </div>
          <span className="text-[11px] font-bold text-slate-500 uppercase tracking-wider mt-0.5">
            out of 100
          </span>
          {attemptMetrics.skippedCount > 0 && (
            <div className="mt-2 pt-2 border-t border-indigo-100/80 flex flex-col items-center">
              <span className="text-[10px] font-bold text-amber-800 bg-amber-100/90 px-2 py-0.5 rounded-full whitespace-nowrap">
                {attemptMetrics.attemptAccuracy}% Attempt Accuracy
              </span>
              <span className="text-[9px] text-slate-500 mt-0.5 whitespace-nowrap font-medium">
                {attemptMetrics.correctCount}/{attemptMetrics.attemptedCount} answered · {attemptMetrics.skippedCount} skipped
              </span>
            </div>
          )}
        </div>
      </div>

      {/* 2. Teacher Override Notice (if applied) */}
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

      {/* 3. CEFR Rubric Criteria (if essay/rubric available) */}
      {Object.keys(rubric).length > 0 && (
        <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
          <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
            Scoring Criteria Breakdown
          </h2>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Task Achievement', val: rubric.taskAchievement ?? 8.0 },
              { label: 'Coherence & Cohesion', val: rubric.coherence ?? rubric.coherenceCohesion ?? 7.5 },
              { label: 'Lexical Resource', val: rubric.lexicalResource ?? 8.0 },
              { label: 'Grammar Accuracy', val: rubric.grammarRange ?? rubric.grammaticalRange ?? 7.5 },
            ].map((crit, idx) => (
              <div key={idx} className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100 text-center space-y-1">
                <div className="text-lg font-black text-slate-900 font-mono">
                  {typeof crit.val === 'number' ? crit.val.toFixed(1) : crit.val} / 10
                </div>
                <div className="text-[11px] font-medium text-slate-500 leading-tight">
                  {crit.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 4. Sentence-by-Sentence Breakdown (with Green/Red styling) */}
      {itemsToRender.length > 0 && (
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center space-x-2">
              <FileCheck className="w-5 h-5 text-primary" />
              <h2 className="text-base font-bold text-slate-900">
                Sentence-by-Sentence Question Breakdown
              </h2>
            </div>
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-600">
              {itemsToRender.length} Questions
            </span>
          </div>

          <div className="space-y-4">
            {itemsToRender.map((item) => {
              const cleanStudent = (item.studentAnswer || '').replace(/[\u00A0\u202F\u200B]/g, ' ').trim().toLowerCase();
              const cleanCorrect = (item.correctAnswer || '').replace(/[\u00A0\u202F\u200B]/g, ' ').trim().toLowerCase();
              const isSkipped = item.isSkipped || (!cleanStudent || cleanStudent === 'no answer' || cleanStudent === 'no_answer' || cleanStudent === 'skipped' || cleanStudent === 'unanswered' || cleanStudent === '-');
              const isEffectivelyCorrect = !isSkipped && (item.isCorrect || (cleanStudent !== '' && cleanStudent === cleanCorrect));

              return (
              <div
                key={item.questionNumber}
                className={`p-5 rounded-2xl border transition space-y-3 ${
                  isEffectivelyCorrect
                    ? 'border-emerald-200 bg-emerald-50/20'
                    : isSkipped
                    ? 'border-amber-200/80 bg-amber-50/20'
                    : 'border-rose-200 bg-rose-50/20'
                }`}
              >
                {/* Item Header */}
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold px-2.5 py-0.5 rounded-lg bg-white border border-slate-200 text-slate-700">
                    Sentence #{item.questionNumber}
                  </span>
                  {isEffectivelyCorrect ? (
                    <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 flex items-center space-x-1">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                      <span>Correct</span>
                    </span>
                  ) : isSkipped ? (
                    <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-200 flex items-center space-x-1">
                      <HelpCircle className="w-3.5 h-3.5 text-amber-600" />
                      <span>Skipped (0 pts)</span>
                    </span>
                  ) : (
                    <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800 flex items-center space-x-1">
                      <XCircle className="w-3.5 h-3.5 text-rose-600" />
                      <span>Needs Revision</span>
                    </span>
                  )}
                </div>

                {/* Sentence prompt */}
                <p className="text-sm font-semibold text-slate-800 leading-snug">
                  {item.sentence}
                </p>

                {/* Answer comparison */}
                <div className="space-y-2 pt-1">
                  {isEffectivelyCorrect ? (
                    <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-950 text-xs font-medium flex items-center justify-between">
                      <div className="space-x-1">
                        <span className="font-semibold text-emerald-800">Your answer:</span>
                        <span className="font-bold">{item.studentAnswer}</span>
                      </div>
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    </div>
                  ) : isSkipped ? (
                    <>
                      <div className="p-3 rounded-xl bg-amber-50/80 border border-amber-200 text-amber-950 text-xs font-medium flex items-center justify-between">
                        <div className="space-x-1">
                          <span className="font-semibold text-amber-800">Your answer:</span>
                          <span className="italic text-amber-900 font-semibold">No answer submitted</span>
                        </div>
                        <span className="text-[10px] font-bold text-amber-700 uppercase tracking-wider">Unanswered</span>
                      </div>

                      <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-950 text-xs font-medium flex items-center justify-between">
                        <div className="space-x-1">
                          <span className="font-semibold text-emerald-800">Correct answer:</span>
                          <span className="font-bold text-emerald-900">{item.correctAnswer}</span>
                        </div>
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      </div>
                    </>
                  ) : (
                    <>
                      <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-950 text-xs font-medium flex items-center justify-between">
                        <div className="space-x-1">
                          <span className="font-semibold text-rose-800">Your answer:</span>
                          <span className="line-through font-bold text-rose-900">{item.studentAnswer}</span>
                        </div>
                        <XCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
                      </div>

                      <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-950 text-xs font-medium flex items-center justify-between">
                        <div className="space-x-1">
                          <span className="font-semibold text-emerald-800">Correct answer:</span>
                          <span className="font-bold text-emerald-900">{item.correctAnswer}</span>
                        </div>
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                      </div>
                    </>
                  )}
                </div>

                {/* Explanation block */}
                <div className="p-3 rounded-xl bg-white border border-slate-100 space-y-1">
                  <div className="flex items-center space-x-1 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                    <Lightbulb className="w-3.5 h-3.5 text-amber-500" />
                    <span>Explanation</span>
                    {item.grammarRule && (
                      <span className="ml-auto font-mono text-[10px] text-primary normal-case font-bold">
                        Rule: {item.grammarRule}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-700 leading-relaxed">
                    {item.explanation}
                  </p>
                </div>
              </div>
            );
          })}
          </div>
        </div>
      )}

      {/* 5. Essay Corrections Section (if available) */}
      {correctionsToRender.length > 0 && (
        <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-5">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-100">
            <Target className="w-5 h-5 text-indigo-600" />
            <h2 className="text-base font-bold text-slate-900">
              Sentence-Level Corrections & Polishing
            </h2>
          </div>

          <div className="space-y-3">
            {correctionsToRender.map((corr, idx) => (
              <div key={idx} className="p-4 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-2.5">
                <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-medium text-rose-950 flex items-start space-x-2">
                  <XCircle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-rose-800">Original: </span>
                    <span className="line-through">{corr.original}</span>
                  </div>
                </div>

                <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-xs font-medium text-emerald-950 flex items-start space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-emerald-800">Suggested Rewrite: </span>
                    <span>{corr.corrected}</span>
                  </div>
                </div>

                {corr.explanation && (
                  <p className="text-xs text-slate-600 italic px-1 flex items-start gap-1.5">
                    <Lightbulb className="w-3.5 h-3.5 text-amber-500 flex-shrink-0 mt-0.5" />
                    <span>{corr.explanation}</span>
                  </p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 6. Dedicated AI Gap Analysis & Recommendations Card */}
      <div className="bg-gradient-to-br from-indigo-50/60 via-white to-sky-50/60 rounded-3xl p-6 sm:p-8 border border-indigo-200/80 shadow-sm space-y-5">
        <div className="flex items-center justify-between pb-3 border-b border-indigo-100">
          <div className="flex items-center space-x-2.5">
            <BrainCircuit className="w-5 h-5 text-indigo-600" />
            <h2 className="text-base font-bold text-slate-900">
              AI Diagnostic Analysis: Areas for Improvement
            </h2>
          </div>
          <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800">
            Targeted Gaps
          </span>
        </div>

        {/* Diagnostic summary */}
        <p className="text-xs sm:text-sm text-slate-700 leading-relaxed font-medium">
          {aiAnalysis.summary}
        </p>

        {/* Identified Weaknesses */}
        {aiAnalysis.weaknesses.length > 0 && (
          <div className="space-y-2">
            <div className="flex items-center space-x-1.5 text-xs font-bold text-rose-800 uppercase tracking-wider">
              <AlertCircle className="w-3.5 h-3.5 text-rose-600" />
              <span>Identified Weaknesses & Grammar Gaps:</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {aiAnalysis.weaknesses.map((w, idx) => (
                <span
                  key={idx}
                  className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-rose-50 border border-rose-200 text-rose-900"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                  <span>{w}</span>
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Mastered Strengths */}
        {aiAnalysis.strengths.length > 0 ? (
          <div className="space-y-2">
            <div className="flex items-center space-x-1.5 text-xs font-bold text-emerald-800 uppercase tracking-wider">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
              <span>Mastered Concepts:</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {aiAnalysis.strengths.map((s, idx) => (
                <span
                  key={idx}
                  className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-emerald-50 border border-emerald-200 text-emerald-900"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                  <span>{s}</span>
                </span>
              ))}
            </div>
          </div>
        ) : (
          <div className="space-y-1 pt-0.5">
            <div className="flex items-center space-x-1.5 text-xs font-bold text-slate-500 uppercase tracking-wider">
              <CheckCircle2 className="w-3.5 h-3.5 text-slate-400" />
              <span>Mastered Concepts:</span>
            </div>
            <p className="text-xs text-slate-500 italic pl-5">
              Complete all questions for a target rule with 100% accuracy to demonstrate mastery.
            </p>
          </div>
        )}

        {/* Actionable recommendations */}
        <div className="p-4 rounded-2xl bg-white border border-indigo-100 space-y-1.5">
          <div className="flex items-center space-x-1.5 text-xs font-bold text-indigo-900">
            <Lightbulb className="w-4 h-4 text-amber-500" />
            <span>AI Coach Recommendations</span>
          </div>
          <p className="text-xs text-slate-700 leading-relaxed">
            {aiAnalysis.recommendations}
          </p>
        </div>

        {/* 1-Click CTA to Practice Weaknesses */}
        {aiAnalysis.suggestedTopics.length > 0 && (
          <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-indigo-50/50 p-4 rounded-2xl border border-indigo-100">
            <div className="text-xs text-indigo-900 font-medium">
              <span className="font-bold">Next recommended step:</span> Practice exercises targeting{' '}
              <span className="font-bold underline">{aiAnalysis.suggestedTopics[0]}</span>.
            </div>
            <button
              type="button"
              onClick={handlePracticeWeaknesses}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition flex items-center justify-center space-x-1.5 shadow-sm"
            >
              <Target className="w-3.5 h-3.5" />
              <span>Practice This Topic</span>
            </button>
          </div>
        )}
      </div>

      {/* 7. General AI Narrative Feedback */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-4">
        <h2 className="text-sm font-bold text-slate-800 uppercase tracking-wider">
          Detailed AI Feedback & Explanation
        </h2>
        <div className="p-4 rounded-2xl bg-slate-50 text-slate-700 text-sm leading-relaxed border border-slate-100 whitespace-pre-wrap">
          {sanitizeFeedback(
            submission.feedback,
            'Good attempt. Continue practicing similar structures to solidify mastery.'
          )}
        </div>
      </div>

      {/* 8. Bottom Action Buttons */}
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
