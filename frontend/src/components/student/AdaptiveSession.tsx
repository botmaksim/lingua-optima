import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, ArrowRight, Zap } from 'lucide-react';
import { sessionApi } from '../../api/sessionApi';
import { SessionState, QuestionResponse, AnswerFeedback } from '../../types/session';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const AdaptiveSession: React.FC = () => {
  const { assignmentId } = useParams<{ assignmentId: string }>();
  const navigate = useNavigate();

  const [session, setSession] = useState<SessionState | null>(null);
  const [currentQuestion, setCurrentQuestion] = useState<QuestionResponse | null>(null);
  const [selectedAnswer, setSelectedAnswer] = useState<string>('');
  const [feedback, setFeedback] = useState<AnswerFeedback | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    const initSession = async () => {
      try {
        // Try resuming existing active session
        let active = await sessionApi.getActiveSession();
        if (!active && assignmentId) {
          active = await sessionApi.startSession(assignmentId);
        }
        if (active) {
          setSession(active);
          const q = await sessionApi.getNextQuestion(active.id);
          setCurrentQuestion(q);
        }
      } catch (err) {
        console.error('Failed to init adaptive session:', err);
      } finally {
        setIsLoading(false);
      }
    };
    initSession();
  }, [assignmentId]);

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
      <div className="text-center py-12">
        <p className="text-slate-600 mb-4">No active adaptive session found.</p>
        <button
          onClick={() => navigate('/student')}
          className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-semibold"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const currentIdx = feedback ? feedback.currentQuestionIndex : session.currentQuestionIndex;
  const progressPercent = Math.min(100, Math.round(((currentIdx + 1) / 10) * 100));

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Session Progress Header */}
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

        {/* Progress Bar */}
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

      {/* Question Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-sm space-y-6">
        <div className="space-y-2">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            Rule: {currentQuestion.grammarRule || 'Grammar'}
          </span>
          <h2 className="text-lg font-bold text-slate-900 leading-snug">
            {currentQuestion.questionText}
          </h2>
        </div>

        {/* Multiple Choice Options or Input */}
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

        {/* Feedback Section (appears after submitting answer) */}
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

        {/* Submit or Next Button */}
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
