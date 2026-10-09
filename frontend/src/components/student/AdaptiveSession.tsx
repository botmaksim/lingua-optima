/**
 * @file AdaptiveSession.tsx
 * @brief Interactive Computerized Adaptive Testing (CAT) testing interface with dynamic difficulty adjustments.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, ArrowRight, Zap, Sparkles, FileText } from 'lucide-react';
import { sessionApi } from '../../api/sessionApi';
import { taskApi } from '../../api/taskApi';
import { Task } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { SessionState, QuestionResponse, AnswerFeedback } from '../../types/session';
import { LoadingSpinner } from '../common/LoadingSpinner';

/**
 * @brief Student adaptive testing session component.
 * @return React component element.
 */
export const AdaptiveSession: React.FC = () => {
  const { assignmentId } = useParams<{ assignmentId: string }>();
  const navigate = useNavigate();

  const [session, setSession] = useState<SessionState | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<QuestionResponse | null>(null);
  const [selectedAnswer, setSelectedAnswer] = useState<string>('');
  const [feedback, setFeedback] = useState<AnswerFeedback | null>(null);
  const [availableTasks, setAvailableTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const initSession = async () => {
      try {
        let active = await sessionApi.getActiveSession();
        if (!active && assignmentId) {
          active = await sessionApi.startSession(assignmentId);
        }
        if (active) {
          setSession(active);
          const q = await sessionApi.getNextQuestion(active.id);
          setCurrentQuestion(q);
        } else {
          const tasks = await taskApi.getTasks().catch(() => []);
          setAvailableTasks(tasks);
        }
      } catch (err) {
        console.error('Failed to init adaptive session:', err);
      } finally {
        setIsLoading(false);
      }
    };
    initSession();
  }, [assignmentId]);

  /**
   * @brief Event handler or helper executing handle submit answer.
   */
  const handleSubmitAnswer = async () => {
    if (!session || !currentQuestion || !selectedAnswer) return;
    setIsSubmitting(true);

    try {
      const fb = await sessionApi.submitAnswer(session.id, {
        questionId: currentQuestion.id,
        answer: selectedAnswer,
      });
      setFeedback(fb);
    } catch (err) {
      console.error('Failed to submit answer:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * @brief Event handler or helper executing handle next question.
   */
  const handleNextQuestion = async () => {
    if (!session) return;

    if (feedback?.completed) {
      setIsLoading(true);
      try {
        const result = await sessionApi.completeSession(session.id);
        navigate(`/student/review/${result.id}`);
      } catch (err) {
        console.error('Failed to complete session:', err);
      }
      return;
    }

    setIsLoading(true);
    setFeedback(null);
    setSelectedAnswer('');

    try {
      const q = await sessionApi.getNextQuestion(session.id);
      setCurrentQuestion(q);
    } catch (err) {
      console.error('Failed to get next question:', err);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Calibrating adaptive CAT question..." />;
  }

  if (!session || !currentQuestion) {
    return (
      <div className="max-w-3xl mx-auto space-y-6 animate-in fade-in duration-200">
        <div className="bg-gradient-to-r from-amber-500 to-indigo-600 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-amber-100 space-y-3">
          <div className="flex items-center space-x-2 text-xs font-bold uppercase tracking-wider text-amber-200">
            <Zap className="w-4 h-4 fill-amber-300 text-amber-300" />
            <span>Computerized Adaptive Testing (CAT)</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
            Dynamic CEFR Diagnostic Testing
          </h1>
          <p className="text-amber-100 text-sm leading-relaxed max-w-xl">
            Our adaptive algorithm calibrates question difficulty in real time based on Item Response Theory (IRT). Each correct or incorrect answer immediately adjusts the subsequent item between Level 1 and Level 4.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm space-y-1">
            <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Calibration</div>
            <div className="text-lg font-black text-slate-900">10 Questions</div>
            <p className="text-xs text-slate-500">Rapidly identifies true CEFR proficiency ceiling.</p>
          </div>
          <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm space-y-1">
            <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Difficulty Scale</div>
            <div className="text-lg font-black text-slate-900">Levels 1 – 4</div>
            <p className="text-xs text-slate-500">Calibrates across beginner to native mastery.</p>
          </div>
          <div className="bg-white rounded-2xl p-5 border border-slate-100 shadow-sm space-y-1">
            <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Pass Benchmark</div>
            <div className="text-lg font-black text-slate-900">85%+ Mastery</div>
            <p className="text-xs text-slate-500">Unlocks dynamic level-up recommendations.</p>
          </div>
        </div>

        {availableTasks.length > 0 ? (
          <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
            <h2 className="text-base font-bold text-slate-900 flex items-center space-x-2">
              <Sparkles className="w-4 h-4 text-primary" />
              <span>Available Tasks for Practice</span>
            </h2>
            <div className="space-y-3">
              {availableTasks.slice(0, 4).map((t) => (
                <div
                  key={t.id}
                  onClick={() => {
                    if (t.type === 'ESSAY') {
                      navigate(`/student/essay/${t.id}`);
                    } else {
                      navigate(`/student/task/${t.id}`);
                    }
                  }}
                  className="p-4 rounded-2xl border border-slate-100 hover:border-indigo-200 hover:shadow-sm transition cursor-pointer flex items-center justify-between group"
                >
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <CefrBadge level={t.cefrLevel} size="sm" />
                      <span className="text-xs font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
                        {t.type}
                      </span>
                    </div>
                    <p className="text-sm font-bold text-slate-800 group-hover:text-primary transition">
                      {t.grammarTopic || 'General Practice'}
                    </p>
                  </div>
                  <button className="py-2 px-4 rounded-xl bg-indigo-50 text-primary font-bold text-xs group-hover:bg-primary group-hover:text-white transition">
                    Start Task
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : null}

        <div className="flex flex-wrap items-center gap-3 pt-2">
          <button
            onClick={() => navigate('/student/generate')}
            className="flex-1 sm:flex-initial py-3 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold text-sm shadow-md shadow-indigo-100 transition flex items-center justify-center space-x-2"
          >
            <Sparkles className="w-4 h-4" />
            <span>Generate Adaptive Practice Task</span>
          </button>
          <button
            onClick={() => navigate('/student/essay')}
            className="flex-1 sm:flex-initial py-3 px-5 rounded-2xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-semibold text-sm transition flex items-center justify-center space-x-2"
          >
            <FileText className="w-4 h-4" />
            <span>Essay Studio</span>
          </button>
          <button
            onClick={() => navigate('/student')}
            className="py-3 px-5 rounded-2xl text-slate-500 hover:text-slate-800 text-sm font-semibold transition"
          >
            Return to Dashboard
          </button>
        </div>
      </div>
    );
  }

  const currentIdx = feedback ? feedback.currentQuestionIndex : session.currentQuestionIndex;
  const progressPercent = Math.min(100, Math.round(((currentIdx + 1) / 10) * 100));

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Zap className="w-5 h-5 text-amber-500 fill-amber-500" />
            <span className="text-sm font-bold text-slate-800">Adaptive CAT Session</span>
          </div>
          <div className="flex items-center space-x-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
            <span>Difficulty Level:</span>
            <span className="text-primary font-mono text-sm">{feedback ? feedback.newDifficulty : currentQuestion.difficulty}/4</span>
          </div>
        </div>

        <div className="space-y-1.5">
          <div className="flex justify-between text-xs text-slate-500 font-medium">
            <span>Question {currentIdx + 1} of 10</span>
            <span>{progressPercent}% Complete</span>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-2">
            <div
              className="bg-primary h-2 rounded-full transition-all duration-300"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div className="space-y-2">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            Rule: {currentQuestion.grammarRule || 'Grammar'}
          </span>
          <h2 className="text-lg font-bold text-slate-900 leading-snug">
            {currentQuestion.questionText}
          </h2>
        </div>

        {Array.isArray(currentQuestion.options) && currentQuestion.options.length > 0 ? (
          <div className="space-y-3">
            {currentQuestion.options.map((opt, idx) => {
              const isSelected = selectedAnswer === opt;
              return (
                <button
                  type="button"
                  key={idx}
                  disabled={!!feedback}
                  onClick={() => setSelectedAnswer(opt)}
                  className={`w-full text-left p-4 rounded-2xl border text-sm font-medium transition flex items-center justify-between ${
                    isSelected
                      ? 'border-primary bg-indigo-50/60 text-primary font-semibold'
                      : 'border-slate-200 hover:border-slate-300 text-slate-700'
                  } ${feedback ? 'cursor-default' : ''}`}
                >
                  <span>{opt}</span>
                </button>
              );
            })}
          </div>
        ) : (
          <input
            type="text"
            disabled={!!feedback}
            placeholder="Type your answer..."
            value={selectedAnswer}
            onChange={(e) => setSelectedAnswer(e.target.value)}
            className="w-full p-4 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          />
        )}

        {feedback && (
          <div
            className={`p-5 rounded-2xl border flex items-start space-x-3 animate-in fade-in duration-200 ${
              feedback.correct
                ? 'bg-emerald-50/80 border-emerald-200 text-emerald-900'
                : 'bg-rose-50/80 border-rose-200 text-rose-900'
            }`}
          >
            {feedback.correct ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            ) : (
              <XCircle className="w-5 h-5 text-rose-600 flex-shrink-0 mt-0.5" />
            )}
            <div className="space-y-1">
              <p className="text-sm font-bold">
                {feedback.correct ? 'Spot on! Correct answer.' : 'Incorrect.'}
              </p>
              <p className="text-xs text-slate-700">{feedback.explanation}</p>
            </div>
          </div>
        )}

        {!feedback ? (
          <button
            type="button"
            onClick={handleSubmitAnswer}
            disabled={!selectedAnswer || isSubmitting}
            className="w-full py-4 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-40"
          >
            {isSubmitting ? 'Checking...' : 'Submit Answer'}
          </button>
        ) : (
          <button
            type="button"
            onClick={handleNextQuestion}
            className="w-full py-4 rounded-2xl bg-slate-900 hover:bg-black text-white font-bold transition flex items-center justify-center space-x-2"
          >
            <span>{feedback.completed ? 'Complete Session & View Report' : 'Next Question'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
};
