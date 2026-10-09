/**
 * @file TaskView.tsx
 * @brief Student exercise solving interface supporting offline draft autosaving, countdown timer, and text submission.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Save, Send, Clock, CheckCircle2 } from 'lucide-react';
import { taskApi } from '../../api/taskApi';
import { submissionApi } from '../../api/submissionApi';
import { Task, TaskQuestion } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { saveDraftLocal, getDraftLocal } from '../../utils/offlineSync';

/**
 * @brief Student task interactive view component.
 * @return React component element.
 */
export const TaskView: React.FC = () => {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();

  const [task, setTask] = useState<Task | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    if (!taskId) return;

    const loadTask = async () => {
      try {
        const data = await taskApi.getTaskById(taskId);
        setTask(data);

        const draft = await getDraftLocal(`task_${taskId}`);
        if (draft) {
          try {
            setAnswers(JSON.parse(draft));
          } catch {
          }
        }
      } catch (err) {
        console.error('Failed to load task:', err);
      } finally {
        setIsLoading(false);
      }
    };

    loadTask();

    const timer = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, [taskId]);

  const handleSelectAnswer = (questionId: string, answer: string) => {
    setAnswers((prev) => ({ ...prev, [questionId]: answer }));
  };

  const handleSaveDraft = async () => {
    if (!taskId) return;
    await saveDraftLocal(`task_${taskId}`, JSON.stringify(answers));
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2000);
  };

  const handleSubmit = async () => {
    if (!task) return;
    setIsSubmitting(true);

    try {
      const formattedAnswers = (task.questions || []).map((q, idx) => {
        return `Q${idx + 1}: ${answers[q.id] || 'No answer'}`;
      }).join('\n');

      const result = await submissionApi.submitText({
        text: formattedAnswers,
        type: 'GRAMMAR',
      });

      navigate(`/student/review/${result.id}`);
    } catch (err) {
      console.error('Submission failed:', err);
      alert('Submission failed. Your draft has been saved locally.');
      await handleSaveDraft();
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner size="lg" message="Loading practice task..." />;
  }

  if (!task) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-600 mb-4">Task not found.</p>
        <button
          onClick={() => navigate('/student')}
          className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-semibold"
        >
          Return to Dashboard
        </button>
      </div>
    );
  }

  const formatTimer = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const questions: TaskQuestion[] = task.questions || [];

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Top Action Bar */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center space-x-1.5 text-xs font-bold text-slate-500 hover:text-slate-800 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back</span>
        </button>

        <div className="flex items-center space-x-2 text-xs font-mono bg-slate-100 text-slate-700 px-3 py-1.5 rounded-xl">
          <Clock className="w-3.5 h-3.5 text-slate-500" />
          <span>{formatTimer(elapsedSeconds)}</span>
        </div>
      </div>

      {/* Task Header Card */}
      <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-3">
        <div className="flex items-center space-x-2">
          <CefrBadge level={task.cefrLevel} size="md" />
          <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-indigo-50 text-primary">
            {task.type}
          </span>
          <span className="text-xs text-slate-400">· {task.domain}</span>
        </div>
        <h1 className="text-xl font-black text-slate-900 tracking-tight">
          {task.grammarTopic || 'Grammar Practice'}
        </h1>
        {task.content && (
          <p className="text-sm text-slate-600 bg-slate-50 p-4 rounded-2xl border border-slate-100 leading-relaxed">
            {task.content}
          </p>
        )}
      </div>

      {/* Questions List */}
      <div className="space-y-4">
        {questions.length === 0 ? (
          <div className="bg-white p-6 rounded-2xl text-center text-slate-500 text-sm">
            No specific multiple-choice questions loaded.
          </div>
        ) : (
          questions.map((q, idx) => (
            <div
              key={q.id}
              className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4"
            >
              <div className="flex items-start justify-between gap-4">
                <span className="text-sm font-bold text-slate-800">
                  {idx + 1}. {q.questionText}
                </span>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-500">
                  Diff: {q.difficulty}
                </span>
              </div>

              {/* Options */}
              {Array.isArray(q.options) && q.options.length > 0 ? (
                <div className="space-y-2">
                  {q.options.map((opt, optIdx) => {
                    const isSelected = answers[q.id] === opt;
                    return (
                      <button
                        type="button"
                        key={optIdx}
                        onClick={() => handleSelectAnswer(q.id, opt)}
                        className={`w-full text-left p-3.5 rounded-2xl border text-sm font-medium transition flex items-center justify-between ${
                          isSelected
                            ? 'border-primary bg-indigo-50/60 text-primary font-semibold'
                            : 'border-slate-200 hover:border-slate-300 text-slate-700'
                        }`}
                      >
                        <span>{opt}</span>
                        {isSelected && <CheckCircle2 className="w-4 h-4 text-primary" />}
                      </button>
                    );
                  })}
                </div>
              ) : (
                <input
                  type="text"
                  placeholder="Type your answer here..."
                  value={answers[q.id] || ''}
                  onChange={(e) => handleSelectAnswer(q.id, e.target.value)}
                  className="w-full p-3.5 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                />
              )}
            </div>
          ))
        )}
      </div>

      {/* Footer Buttons: [Save Draft] [Submit Answers] */}
      <div className="flex items-center space-x-3 pt-4">
        <button
          type="button"
          onClick={handleSaveDraft}
          className="flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition"
        >
          <Save className="w-4 h-4" />
          <span>{saveSuccess ? 'Draft Saved!' : 'Save Draft'}</span>
        </button>

        <button
          type="button"
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="flex-1 flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
        >
          {isSubmitting ? (
            <LoadingSpinner size="sm" className="p-0 text-white" />
          ) : (
            <>
              <Send className="w-4 h-4" />
              <span>Submit Answers</span>
            </>
          )}
        </button>
      </div>
    </div>
  );
};
