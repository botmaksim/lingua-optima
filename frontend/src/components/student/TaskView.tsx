/**
 * @file TaskView.tsx
 * @brief Student exercise solving interface supporting offline draft autosaving, countdown timer, and text submission.
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Save,
  Send,
  Clock,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  BookOpen,
  Maximize2,
  Minimize2,
  Layers,
  List,
  Lightbulb,
  ChevronLeft,
  ChevronRight,
  Zap,
  X,
  XCircle,
  Check,
  Keyboard,
  Volume2,
  VolumeX,
} from 'lucide-react';
import { taskApi } from '../../api/taskApi';
import { submissionApi } from '../../api/submissionApi';
import { Task, TaskQuestion, ExplainSentenceResponse } from '../../types/task';
import { CefrBadge } from '../common/CefrBadge';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ConfirmDialog } from '../common/ConfirmDialog';
import { saveDraftLocal, getDraftLocal } from '../../utils/offlineSync';
import { sanitizeTaskContent } from '../../utils/textSanitizer';
import { SpeakingPracticeView } from '../common/SpeakingPracticeView';
import { triggerConfetti } from '../../utils/confetti';
import { speakText, stopSpeech } from '../../utils/phonetics';
import { HotkeysModal } from '../common/HotkeysModal';

export type TaskViewMode = 'LIST' | 'CARDS';

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
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [showUnansweredConfirm, setShowUnansweredConfirm] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  // View modes: Step-by-Step Card Focus (default) or List
  const [viewMode, setViewMode] = useState<TaskViewMode>('CARDS');
  const [currentCardIndex, setCurrentCardIndex] = useState<number>(0);
  const [instantFeedback, setInstantFeedback] = useState<boolean>(false);
  const [lockedInstantQuestions, setLockedInstantQuestions] = useState<Set<string>>(new Set());
  const [showReadingInCard, setShowReadingInCard] = useState<boolean>(false);

  const isTeacherTask = Boolean(task?.isTeacherTask || task?.assignedByName);
  const isExplanationLocked = Boolean(isTeacherTask && task?.canSubmit !== false);

  useEffect(() => {
    if (isTeacherTask && instantFeedback) {
      setInstantFeedback(false);
    }
  }, [isTeacherTask, instantFeedback]);

  // Sentence explanation drawer & text selection state
  const [explainingSentence, setExplainingSentence] = useState<string | null>(null);
  const [isExplaining, setIsExplaining] = useState<boolean>(false);
  const [explanationData, setExplanationData] = useState<ExplainSentenceResponse | null>(null);
  const [explanationError, setExplanationError] = useState<string | null>(null);
  const [floatingSelection, setFloatingSelection] = useState<string | null>(null);
  const [isSpeaking, setIsSpeaking] = useState<boolean>(false);
  const [showHotkeysModal, setShowHotkeysModal] = useState<boolean>(false);

  // Reading Comprehension view scaling preferences
  const [readingWidthPreset, setReadingWidthPreset] = useState<'compact' | 'balanced' | 'wide' | 'expanded'>('balanced');
  const [readingFontSize, setReadingFontSize] = useState<'sm' | 'base' | 'lg' | 'xl'>('base');
  const [isFullHeight, setIsFullHeight] = useState<boolean>(false);

  useEffect(() => {
    if (!taskId) return;

    const loadTask = async () => {
      try {
        const data = await taskApi.getTaskById(taskId);
        if (data.type === 'ESSAY') {
          navigate(`/student/essay/${taskId}`, { replace: true });
          return;
        }
        setTask(data);
        if (data.type === 'READING') {
          setViewMode('LIST');
        } else {
          setViewMode('CARDS');
        }

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

  // Stop speech playback when switching questions or view modes
  useEffect(() => {
    stopSpeech();
    setIsSpeaking(false);
  }, [currentCardIndex, viewMode]);

  useEffect(() => {
    return () => {
      stopSpeech();
    };
  }, []);

  // Detect user text selection across passage and questions for quick AI explanation
  useEffect(() => {
    const handleSelectionChange = () => {
      const sel = window.getSelection();
      const text = sel ? sel.toString().trim() : '';
      if (text.length >= 8 && text.length <= 300) {
        setFloatingSelection(text);
      } else {
        setFloatingSelection(null);
      }
    };

    const handleDocumentClick = (e: MouseEvent) => {
      const target = e.target as HTMLElement | null;
      if (target?.closest('button') || target?.closest('input') || target?.closest('textarea') || target?.closest('[role="button"]') || target?.closest('[data-dismiss-selection="true"]')) {
        setFloatingSelection(null);
      }
    };

    const handleDocKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setFloatingSelection(null);
        window.getSelection()?.removeAllRanges();
      }
    };

    document.addEventListener('selectionchange', handleSelectionChange);
    document.addEventListener('mouseup', handleSelectionChange);
    document.addEventListener('touchend', handleSelectionChange);
    document.addEventListener('click', handleDocumentClick);
    document.addEventListener('keydown', handleDocKeyDown);
    return () => {
      document.removeEventListener('selectionchange', handleSelectionChange);
      document.removeEventListener('mouseup', handleSelectionChange);
      document.removeEventListener('touchend', handleSelectionChange);
      document.removeEventListener('click', handleDocumentClick);
      document.removeEventListener('keydown', handleDocKeyDown);
    };
  }, []);

  // Dismiss floating selection when changing view modes or question cards
  useEffect(() => {
    setFloatingSelection(null);
  }, [viewMode, currentCardIndex]);

  const handleExplainSentence = async (sentenceText: string) => {
    if (isExplanationLocked) {
      return;
    }
    if (!sentenceText.trim()) return;
    setExplainingSentence(sentenceText.trim());
    setIsExplaining(true);
    setExplanationError(null);
    setExplanationData(null);

    try {
      const res = await taskApi.explainSentence({
        sentence: sentenceText.trim(),
        context: task?.content || '',
        cefrLevel: task?.cefrLevel,
      });
      setExplanationData(res);
    } catch (err: any) {
      console.error('Sentence explanation failed:', err);
      setExplanationError(err?.response?.data?.message || 'Не удалось получить разбор предложения.');
    } finally {
      setIsExplaining(false);
    }
  };

  const handleSelectAnswer = (questionId: string, answer: string) => {
    if (task?.canSubmit === false) return;
    if (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(questionId)) {
      return;
    }
    setFloatingSelection(null);
    setAnswers((prev) => ({ ...prev, [questionId]: answer }));
    if (instantFeedback && !isTeacherTask) {
      setLockedInstantQuestions((prev) => new Set(prev).add(questionId));
    }
  };

  const handleSaveDraft = async () => {
    if (!taskId) return;
    await saveDraftLocal(`task_${taskId}`, JSON.stringify(answers));
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2000);
  };

  const questions: TaskQuestion[] = task?.questions || [];
  const answeredCount = questions.filter((q) => {
    const val = answers[q.id];
    return val !== undefined && val !== null && val.trim() !== '';
  }).length;
  const unansweredCount = questions.length - answeredCount;

  /**
   * @brief Handles submit button click, warning the student if questions remain unanswered.
   */
  const handleSubmitClick = () => {
    if (!task) return;
    if (unansweredCount > 0) {
      setShowUnansweredConfirm(true);
    } else {
      executeSubmit();
    }
  };

  /**
   * @brief Executes final submission payload transmission to the backend.
   */
  const executeSubmit = async () => {
    if (!task) return;
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const formattedAnswers = (task.questions || []).map((q, idx) => {
        return `Q${idx + 1}: ${answers[q.id] || 'No answer'}`;
      }).join('\n');

      const result = await submissionApi.submitText({
        taskId: task.id,
        text: formattedAnswers,
        type: 'GRAMMAR',
      });

      if ((result.effectiveScore ?? result.score ?? 0) >= 90) {
        triggerConfetti(90);
      }

      navigate(`/student/review/${result.id}`);
    } catch (err: any) {
      console.error('Submission failed:', err);
      setSubmitError(err?.response?.data?.message || 'Submission failed. Your draft has been saved locally.');
      await handleSaveDraft();
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * @brief Handles speaking practice submission with pronunciation metrics and transcript.
   */
  const handleSpeakingSubmit = async (result: {
    spokenTranscript: string;
    accuracyScore: number;
    overallScore: number;
    wpm?: number;
    durationSeconds?: number;
    pacingScore?: number;
    pacingFeedback?: string;
    words: any[];
    generalFeedback: string[];
  }) => {
    if (!task) return;
    setIsSubmitting(true);
    setSubmitError(null);
    try {
      const formattedSubmission = [
        `Pronunciation Accuracy: ${result.accuracyScore}%`,
        `Overall Speaking Score: ${result.overallScore}%`,
        `Speech Rate: ${result.wpm ?? 120} WPM (Pacing Score: ${result.pacingScore ?? 100}%)`,
        `Speaking Duration: ${result.durationSeconds ?? 0}s`,
        `Pacing Diagnostics: ${result.pacingFeedback || 'Normal pacing'}`,
        `Spoken Transcript: ${result.spokenTranscript}`,
        `Reference Passage: ${task.content || task.questions?.[0]?.questionText || ''}`,
      ].join('\n\n');

      const res = await submissionApi.submitText({
        taskId: task.id,
        text: formattedSubmission,
        type: 'SPEAKING',
      });
      navigate(`/student/review/${res.id}`);
    } catch (err: any) {
      console.error('Speaking submission failed:', err);
      setSubmitError(err?.response?.data?.message || 'Submission failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  /**
   * @brief Toggles audio speech playback for a sentence or question text.
   */
  const handleToggleSpeech = (text: string) => {
    if (!text || !text.trim()) return;
    if (isSpeaking) {
      stopSpeech();
      setIsSpeaking(false);
    } else {
      setIsSpeaking(true);
      speakText(text.trim(), 'en-US', () => setIsSpeaking(false));
    }
  };

  // Comprehensive Keyboard Shortcuts (Hotkeys) for Exercise Navigation, Answering & Actions
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 1. Always allow Escape to close active modals, dialogs, cheatsheet, or text selection
      if (e.key === 'Escape') {
        if (showHotkeysModal) {
          e.preventDefault();
          setShowHotkeysModal(false);
          return;
        }
        if (explainingSentence != null) {
          e.preventDefault();
          setExplainingSentence(null);
          return;
        }
        if (showUnansweredConfirm) {
          e.preventDefault();
          setShowUnansweredConfirm(false);
          return;
        }
        if (floatingSelection != null) {
          e.preventDefault();
          setFloatingSelection(null);
          window.getSelection()?.removeAllRanges();
          return;
        }
        return;
      }

      // Check if user is currently typing in an input, textarea, or contentEditable
      const activeEl = document.activeElement as HTMLElement | null;
      const activeTag = (activeEl?.tagName || '').toLowerCase();
      const isInput = activeTag === 'input' || activeTag === 'textarea' || !!activeEl?.isContentEditable;

      // 2. Ctrl+Enter or Cmd+Enter: Instant submit anytime
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        handleSubmitClick();
        return;
      }

      // If user is actively typing in an input/textarea:
      if (isInput) {
        // In single-line text input: pressing Enter advances to next question or submits if on last
        if (activeTag === 'input' && e.key === 'Enter') {
          e.preventDefault();
          if (viewMode === 'CARDS') {
            if (currentCardIndex < questions.length - 1) {
              setCurrentCardIndex((prev) => prev + 1);
            } else {
              handleSubmitClick();
            }
          }
        }
        // Do not intercept other keystrokes while user is typing answers!
        return;
      }

      // 3. Open Hotkeys cheatsheet modal: '?' or Shift+'/'
      if (e.key === '?' || (e.shiftKey && (e.key === '/' || e.key === ','))) {
        e.preventDefault();
        setShowHotkeysModal((prev) => !prev);
        return;
      }

      // For exercise navigation & answering, we need an active question
      const currentQ = questions[currentCardIndex];
      if (!currentQ) return;

      const keyLower = e.key.toLowerCase();

      // 4. Space: Pronounce question / target sentence (or stop playback)
      if (e.code === 'Space' || e.key === ' ') {
        e.preventDefault();
        handleToggleSpeech(currentQ.questionText);
        return;
      }

      // 5. Navigation: Previous card / slide (ArrowLeft, 'h', 'k')
      if (e.key === 'ArrowLeft' || keyLower === 'h' || keyLower === 'k') {
        e.preventDefault();
        stopSpeech();
        setIsSpeaking(false);
        if (currentCardIndex > 0) {
          setCurrentCardIndex((prev) => prev - 1);
        }
        return;
      }

      // 6. Navigation: Next card / slide (ArrowRight, 'l', 'j')
      if (e.key === 'ArrowRight' || keyLower === 'l' || keyLower === 'j') {
        e.preventDefault();
        stopSpeech();
        setIsSpeaking(false);
        if (currentCardIndex < questions.length - 1) {
          setCurrentCardIndex((prev) => prev + 1);
        }
        return;
      }

      // 7. Enter: Next question, or submit on last question
      if (e.key === 'Enter') {
        e.preventDefault();
        if (currentCardIndex < questions.length - 1) {
          setCurrentCardIndex((prev) => prev + 1);
        } else {
          handleSubmitClick();
        }
        return;
      }

      // 8. AI Grammar & Vocabulary Sentence Explainer: 'e' / 'E' (or 'у' / 'У' in Russian layout)
      if (keyLower === 'e' || keyLower === 'у') {
        e.preventDefault();
        if (isExplanationLocked) return;
        handleExplainSentence(currentQ.questionText);
        return;
      }

      // 9. Instant Feedback toggle: 'f' / 'F' (or 'а' / 'А' in Russian layout)
      if (viewMode === 'CARDS' && (keyLower === 'f' || keyLower === 'а')) {
        e.preventDefault();
        if (isTeacherTask) return;
        setInstantFeedback((prev) => !prev);
        return;
      }

      // 10. View mode toggle: 'v' / 'V' (or 'м' / 'М' in Russian layout)
      if (task?.type !== 'SPEAKING' && (keyLower === 'v' || keyLower === 'м')) {
        e.preventDefault();
        setViewMode((prev) => (prev === 'CARDS' ? 'LIST' : 'CARDS'));
        return;
      }

      // 12. Multiple Choice & True/False answer selection
      if (Array.isArray(currentQ.options) && currentQ.options.length > 0 && task?.canSubmit !== false) {
        const isTF =
          currentQ.options.length === 2 &&
          (currentQ.options.some((o) => o.toLowerCase() === 'true') ||
            currentQ.options.some((o) => o.toLowerCase() === 'false'));

        if (isTF) {
          // True: '1', 't', 'е'
          if (e.key === '1' || keyLower === 't' || keyLower === 'е') {
            e.preventDefault();
            const trueOpt = currentQ.options.find((o) => o.toLowerCase() === 'true') || currentQ.options[0];
            handleSelectAnswer(currentQ.id, trueOpt);
            return;
          }
          // False: '2', 'f', 'а'
          if (e.key === '2' || keyLower === 'f' || keyLower === 'а') {
            e.preventDefault();
            const falseOpt = currentQ.options.find((o) => o.toLowerCase() === 'false') || currentQ.options[1];
            handleSelectAnswer(currentQ.id, falseOpt);
            return;
          }
        }

        // Number keys 1..9
        const num = parseInt(e.key, 10);
        if (!isNaN(num) && num >= 1 && num <= currentQ.options.length) {
          e.preventDefault();
          handleSelectAnswer(currentQ.id, currentQ.options[num - 1]);
          return;
        }

        // Letter keys A, B, C, D, E and Russian keyboard equivalents
        const letterMap: Record<string, number> = {
          a: 0,
          ф: 0,
          b: 1,
          и: 1,
          c: 2,
          с: 2,
          d: 3,
          в: 3,
          e: 4,
          у: 4,
        };
        if (keyLower in letterMap) {
          const optIdx = letterMap[keyLower];
          if (optIdx < currentQ.options.length) {
            e.preventDefault();
            handleSelectAnswer(currentQ.id, currentQ.options[optIdx]);
            return;
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [
    viewMode,
    currentCardIndex,
    questions,
    task,
    showHotkeysModal,
    explainingSentence,
    showUnansweredConfirm,
    floatingSelection,
    isSpeaking,
    instantFeedback,
    answers,
  ]);

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

  const fontSizes: ('sm' | 'base' | 'lg' | 'xl')[] = ['sm', 'base', 'lg', 'xl'];
  const decreaseFontSize = () => {
    const idx = fontSizes.indexOf(readingFontSize);
    if (idx > 0) setReadingFontSize(fontSizes[idx - 1]);
  };
  const increaseFontSize = () => {
    const idx = fontSizes.indexOf(readingFontSize);
    if (idx < fontSizes.length - 1) setReadingFontSize(fontSizes[idx + 1]);
  };

  const readingColSpans = {
    compact: { passage: 'lg:col-span-4', questions: 'lg:col-span-8' },
    balanced: { passage: 'lg:col-span-6', questions: 'lg:col-span-6' },
    wide: { passage: 'lg:col-span-7', questions: 'lg:col-span-5' },
    expanded: { passage: 'lg:col-span-8', questions: 'lg:col-span-4' },
  }[readingWidthPreset];

  const fontSizeClass = {
    sm: 'text-xs leading-relaxed',
    base: 'text-sm leading-relaxed',
    lg: 'text-base leading-relaxed',
    xl: 'text-lg leading-relaxed',
  }[readingFontSize];

  const containerMaxWidth =
    task?.type === 'READING'
      ? readingWidthPreset === 'expanded' || readingWidthPreset === 'wide'
        ? 'max-w-7xl'
        : 'max-w-6xl'
      : 'max-w-3xl';

  return (
    <div className={`${containerMaxWidth} mx-auto space-y-6 transition-all duration-300`}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center space-x-1.5 text-xs font-bold text-slate-500 hover:text-slate-800 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Назад</span>
        </button>

        {task?.type !== 'SPEAKING' && (
          <div className="flex items-center bg-slate-100/90 p-1 rounded-2xl border border-slate-200 text-xs">
            <button
              type="button"
              onClick={() => setViewMode('LIST')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl font-bold transition ${
                viewMode === 'LIST'
                  ? 'bg-white text-primary shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <List className="w-3.5 h-3.5" />
              <span>Список</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode('CARDS')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-xl font-bold transition ${
                viewMode === 'CARDS'
                  ? 'bg-white text-primary shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Карточки</span>
            </button>
          </div>
        )}

        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={() => setShowHotkeysModal(true)}
            className="p-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 hover:text-slate-900 transition flex items-center space-x-1"
            title="Горячие клавиши (?)"
            aria-label="Горячие клавиши"
          >
            <Keyboard className="w-4 h-4" />
            <span className="hidden sm:inline text-[11px] font-mono font-bold text-slate-500">?</span>
          </button>
          <div className="flex items-center space-x-2 text-xs font-mono bg-slate-100 text-slate-700 px-3 py-1.5 rounded-xl">
            <Clock className="w-3.5 h-3.5 text-slate-500" />
            <span>{formatTimer(elapsedSeconds)}</span>
          </div>
        </div>
      </div>

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
                Checking your answers...
              </h3>
              <p className="text-xs text-indigo-700 font-medium">
                Reviewing grammatical accuracy and preparing detailed explanations.
              </p>
            </div>
          </div>
          <div className="bg-white/80 backdrop-blur-sm rounded-2xl p-3 border border-indigo-100 text-xs text-slate-600 space-y-1.5 leading-relaxed">
            <p className="flex items-center space-x-1.5">
              <Clock className="w-3.5 h-3.5 text-primary flex-shrink-0" />
              <span>Reviewing typically takes <strong>5–15 seconds</strong>. Please keep this tab open.</span>
            </p>
          </div>
        </div>
      )}

      {submitError && (
        <div className="p-5 rounded-3xl bg-amber-50 border border-amber-200/80 shadow-sm space-y-3 animate-in fade-in duration-300">
          <div className="flex items-start space-x-3">
            <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center flex-shrink-0 mt-0.5">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div className="space-y-1.5 flex-1">
              <h3 className="text-sm font-bold text-slate-900">
                {submitError.toLowerCase().includes('queue') || submitError.toLowerCase().includes('temporarily')
                  ? 'Service Temporarily Busy'
                  : 'Submission Error'}
              </h3>
              <p className="text-xs text-slate-600 leading-relaxed">
                {submitError}
              </p>
              <div className="pt-2 flex flex-wrap gap-2 text-xs">
                <button
                  type="button"
                  onClick={executeSubmit}
                  className="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition flex items-center space-x-1.5 shadow-sm"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Retry</span>
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

      <div className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <CefrBadge level={task.cefrLevel} size="md" />
          <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-indigo-50 text-primary">
            {task.type}
          </span>
          <span className="text-xs text-slate-400">· {task.domain}</span>
          {task.assignedByName && (
            <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700">
              Assigned by {task.assignedByName}
            </span>
          )}
          {task.maxAttempts === 1 && (
            <span
              className={`text-xs font-bold px-2.5 py-1 rounded-lg ${
                task.canSubmit === false
                  ? 'bg-emerald-50 text-emerald-700'
                  : 'bg-amber-50 text-amber-700'
              }`}
            >
              {task.canSubmit === false ? '1/1 Attempt Used' : '1 Attempt Allowed'}
            </span>
          )}
          {task.maxAttempts === 0 && (
            <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-sky-50 text-sky-700">
              ∞ Unlimited Attempts
            </span>
          )}
        </div>
        <h1 className="text-xl font-black text-slate-900 tracking-tight">
          {task.grammarTopic || 'Grammar Practice'}
        </h1>
        {task.content && task.type !== 'READING' && task.type !== 'SPEAKING' && (
          <p className="text-sm text-slate-600 bg-slate-50 p-4 rounded-2xl border border-slate-100 leading-relaxed">
            {sanitizeTaskContent(task.content, 'Read the questions carefully and select the best answer.')}
          </p>
        )}
      </div>

      {task.canSubmit === false && (
        <div className="p-5 rounded-3xl bg-emerald-50 border border-emerald-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start space-x-3">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="text-sm font-bold text-emerald-950">
                Assignment Completed (1 of 1 Attempt Used)
              </h3>
              <p className="text-xs text-emerald-800 mt-0.5">
                Your teacher configured this assignment for a single attempt. You can review your graded answers and AI analysis at any time.
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

      {task.type === 'SPEAKING' ? (
        <SpeakingPracticeView
          passageText={task.content || task.questions?.[0]?.questionText || ''}
          taskTitle={
            task.speakingTitle ||
            (task.grammarTopic && !task.grammarTopic.includes('Voice') && !task.grammarTopic.includes('Passive') && !task.grammarTopic.includes('Tense')
              ? task.grammarTopic
              : 'Oral Reading & Pronunciation Assessment')
          }
          targetSounds={task.targetSounds}
          points={task.totalPoints || 100}
          onSubmit={handleSpeakingSubmit}
          isSubmitting={isSubmitting}
          readOnly={task.canSubmit === false}
          isTeacherTask={isTeacherTask}
        />
      ) : viewMode === 'CARDS' ? (
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-5 border border-slate-100 shadow-sm flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
            <div className="flex items-center space-x-2 overflow-x-auto py-1">
              {questions.map((q, idx) => {
                const isAnswered = !!answers[q.id]?.trim();
                const isCurrent = idx === currentCardIndex;
                return (
                  <button
                    key={q.id}
                    type="button"
                    onClick={() => setCurrentCardIndex(idx)}
                    className={`w-8 h-8 rounded-xl text-xs font-bold transition flex items-center justify-center flex-shrink-0 ${
                      isCurrent
                        ? 'bg-primary text-white shadow-sm ring-2 ring-primary/20'
                        : isAnswered
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                    }`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center justify-between sm:justify-end space-x-3">
              {!isTeacherTask && (
                <label className="flex items-center space-x-2 text-xs font-semibold text-slate-700 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={instantFeedback}
                    onChange={(e) => setInstantFeedback(e.target.checked)}
                    className="rounded text-primary focus:ring-primary h-4 w-4"
                  />
                  <span className="flex items-center space-x-1">
                    <Zap className="w-3.5 h-3.5 text-amber-500" />
                    <span>Мгновенная проверка</span>
                  </span>
                </label>
              )}

              {task.type === 'READING' && task.content && (
                <button
                  type="button"
                  onClick={() => setShowReadingInCard(!showReadingInCard)}
                  className={`px-3 py-1.5 rounded-xl border text-xs font-semibold flex items-center space-x-1.5 transition ${
                    showReadingInCard
                      ? 'bg-indigo-50 border-indigo-200 text-primary'
                      : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <BookOpen className="w-3.5 h-3.5" />
                  <span>{showReadingInCard ? 'Скрыть текст' : 'Текст статьи'}</span>
                </button>
              )}
            </div>
          </div>

          {task.type === 'READING' && showReadingInCard && task.content && (
            <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-sm space-y-3 animate-in fade-in duration-200">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center space-x-2">
                  <BookOpen className="w-4 h-4 text-primary" />
                  <span>Материал для чтения</span>
                </h4>
                {!isExplanationLocked && (
                  <button
                    type="button"
                    onClick={() => handleExplainSentence(task.content || '')}
                    className="text-xs font-semibold text-primary hover:underline flex items-center space-x-1"
                  >
                    <Lightbulb className="w-3.5 h-3.5" />
                    <span>Пояснить весь текст</span>
                  </button>
                )}
              </div>
              <div className="text-slate-800 text-sm leading-relaxed whitespace-pre-wrap font-serif max-h-60 overflow-y-auto pr-2">
                {task.content}
              </div>
            </div>
          )}

          {(() => {
            const currentQ = questions[currentCardIndex];
            if (!currentQ) {
              return (
                <div className="bg-white p-12 rounded-3xl text-center text-slate-500">
                  Вопросы не найдены.
                </div>
              );
            }

            const isTF =
              Array.isArray(currentQ.options) &&
              currentQ.options.length === 2 &&
              (currentQ.options.includes('True') || currentQ.options.includes('False'));
            const isMCQ = Array.isArray(currentQ.options) && currentQ.options.length > 0 && !isTF;
            const hasOptions =
              Array.isArray(currentQ.options) &&
              currentQ.options.length > 0 &&
              task.type !== 'REWRITE' &&
              task.type !== 'OPEN_BRACKETS' &&
              task.type !== 'SHORT_ANSWER';
            const selectedAnswer = answers[currentQ.id] || '';
            const isAnswered = selectedAnswer.trim() !== '';

            const isCorrect = isAnswered && currentQ.correctAnswer
              ? selectedAnswer.trim().toLowerCase() === currentQ.correctAnswer.trim().toLowerCase()
              : null;

            return (
              <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-100 shadow-md space-y-6">
                <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-slate-100">
                  <div className="flex items-center space-x-2.5">
                    <span className="w-8 h-8 rounded-xl bg-primary/10 text-primary font-black text-sm flex items-center justify-center">
                      {currentCardIndex + 1}
                    </span>
                    <div>
                      <span className="text-[11px] font-bold text-slate-400 block uppercase tracking-wider">
                        Вопрос {currentCardIndex + 1} из {questions.length}
                      </span>
                      <span className="text-xs font-semibold text-slate-600">
                        {isTF ? 'True / False' : isMCQ ? 'Выбор ответа' : currentQ.grammarRule || task.grammarTopic}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2">
                    {currentQ.points && (
                      <span className="text-xs font-bold px-2.5 py-1 rounded-xl bg-indigo-50 text-primary">
                        {currentQ.points} баллов
                      </span>
                    )}
                    <button
                      type="button"
                      onClick={() => handleToggleSpeech(currentQ.questionText)}
                      className={`px-3 py-1.5 rounded-xl border text-xs font-bold transition flex items-center space-x-1.5 ${
                        isSpeaking
                          ? 'bg-indigo-600 border-indigo-600 text-white shadow-sm ring-2 ring-indigo-400/30 animate-pulse'
                          : 'bg-indigo-50 hover:bg-indigo-100 border-indigo-200/80 text-primary'
                      }`}
                      title="Прослушать произношение (Пробел)"
                    >
                      {isSpeaking ? <VolumeX className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
                      <span>{isSpeaking ? 'Пауза' : 'Озвучить'}</span>
                      <kbd className="kbd-shortcut hidden md:inline-block px-1 py-0.5 rounded bg-white/40 text-[10px] font-mono font-bold text-current">
                        Space
                      </kbd>
                    </button>
                    {!isExplanationLocked && (
                      <button
                        type="button"
                        onClick={() => handleExplainSentence(currentQ.questionText)}
                        className="px-3 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-800 text-xs font-bold transition flex items-center space-x-1.5"
                        title="Пояснить предложение и грамматику с помощью AI (E)"
                      >
                        <Lightbulb className="w-3.5 h-3.5 text-amber-600" />
                        <span>Пояснить</span>
                        <kbd className="kbd-shortcut hidden md:inline-block px-1 py-0.5 rounded bg-amber-100/70 text-[10px] font-mono font-bold text-amber-900">
                          E
                        </kbd>
                      </button>
                    )}
                  </div>
                </div>

                <div className="space-y-2">
                  <h2 className="text-lg sm:text-xl font-bold text-slate-900 leading-snug">
                    {currentQ.questionText}
                  </h2>
                </div>

                {hasOptions ? (
                  <div className="space-y-3">
                    <div className="hidden md:block text-[11px] font-semibold text-slate-400">
                      Используйте клавиши <kbd className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-slate-700 font-bold border border-slate-200">1</kbd>–<kbd className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-slate-700 font-bold border border-slate-200">{currentQ.options.length}</kbd> или <kbd className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-slate-700 font-bold border border-slate-200">A</kbd>–<kbd className="px-1.5 py-0.5 rounded bg-slate-100 font-mono text-slate-700 font-bold border border-slate-200">{['A', 'B', 'C', 'D', 'E'][Math.min(currentQ.options.length - 1, 4)]}</kbd> для быстрого выбора:
                    </div>
                    <div className="grid grid-cols-1 gap-2.5">
                      {currentQ.options.map((opt, optIdx) => {
                        const isSelected = selectedAnswer === opt;
                        const isThisCorrect = currentQ.correctAnswer
                          ? opt.trim().toLowerCase() === currentQ.correctAnswer.trim().toLowerCase()
                          : false;

                        const letter = ['A', 'B', 'C', 'D', 'E'][optIdx] || `${optIdx + 1}`;
                        const keyBadge = isTF
                          ? (opt.toLowerCase() === 'true' ? '1 · T' : '2 · F')
                          : `${optIdx + 1} · ${letter}`;

                        let styleClass = 'border-slate-200 hover:border-slate-300 text-slate-800 bg-white';
                        if (instantFeedback && isAnswered) {
                          if (isSelected) {
                            styleClass = isCorrect
                              ? 'border-emerald-500 bg-emerald-50 text-emerald-950 font-bold ring-2 ring-emerald-400/20'
                              : 'border-rose-400 bg-rose-50 text-rose-950 font-bold ring-2 ring-rose-400/20';
                          } else if (isThisCorrect) {
                            styleClass = 'border-emerald-400 bg-emerald-50/50 text-emerald-900 font-semibold';
                          }
                        } else if (isSelected) {
                          styleClass = 'border-primary bg-indigo-50/70 text-primary font-bold shadow-xs';
                        }

                        return (
                          <button
                            type="button"
                            key={optIdx}
                            disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(currentQ.id))}
                            onClick={() => handleSelectAnswer(currentQ.id, opt)}
                            className={`w-full text-left p-4 rounded-2xl border text-sm transition flex items-center justify-between ${styleClass}`}
                          >
                            <div className="flex items-center space-x-3">
                              <span
                                className={`px-2 py-1 rounded-lg text-xs font-mono font-bold flex items-center justify-center flex-shrink-0 transition ${
                                  isSelected
                                    ? 'bg-primary text-white shadow-2xs'
                                    : 'bg-slate-100 text-slate-700 border border-slate-200/80'
                                }`}
                              >
                                <span className="md:hidden">{isTF ? (opt.toLowerCase() === 'true' ? 'T' : 'F') : letter}</span>
                                <span className="hidden md:inline">{keyBadge}</span>
                              </span>
                              <span>{opt}</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              {instantFeedback && isAnswered && isSelected && (
                                isCorrect ? (
                                  <span className="flex items-center space-x-1 text-xs text-emerald-700 font-bold">
                                    <Check className="w-4 h-4 text-emerald-600" />
                                    <span>Верно</span>
                                  </span>
                                ) : (
                                  <span className="flex items-center space-x-1 text-xs text-rose-700 font-bold">
                                    <XCircle className="w-4 h-4 text-rose-600" />
                                    <span>Неверно</span>
                                  </span>
                                )
                              )}
                              {!instantFeedback && isSelected && (
                                <CheckCircle2 className="w-4 h-4 text-primary" />
                              )}
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ) : task.type === 'REWRITE' ? (
                  <div className="space-y-2">
                    <textarea
                      rows={3}
                      disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(currentQ.id))}
                      placeholder="Напишите полный перефразированный вариант предложения..."
                      value={selectedAnswer}
                      onChange={(e) => handleSelectAnswer(currentQ.id, e.target.value)}
                      className="w-full p-4 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-y"
                    />
                  </div>
                ) : (
                  <div className="space-y-2">
                    <input
                      type="text"
                      disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(currentQ.id))}
                      placeholder={
                        task.type === 'OPEN_BRACKETS'
                          ? 'Введите правильную форму слова из скобок...'
                          : 'Введите ваш ответ...'
                      }
                      value={selectedAnswer}
                      onChange={(e) => handleSelectAnswer(currentQ.id, e.target.value)}
                      className="w-full p-4 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                    />
                  </div>
                )}

                {instantFeedback && isAnswered && currentQ.grammarRule && (
                  <div className={`p-4 rounded-2xl border text-xs leading-relaxed space-y-1 animate-in fade-in duration-200 ${
                    isCorrect ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900' : 'bg-amber-50/70 border-amber-200 text-amber-950'
                  }`}>
                    <div className="font-bold flex items-center space-x-1.5">
                      <BookOpen className="w-3.5 h-3.5 text-amber-600" />
                      <span>Грамматическое правило:</span>
                    </div>
                    <p>{currentQ.grammarRule}</p>
                  </div>
                )}

                <div className="kbd-shortcut hidden md:flex flex-wrap items-center justify-between gap-2 px-3.5 py-2.5 bg-slate-50 border border-slate-200/70 rounded-2xl text-[11px] text-slate-500">
                  <div className="flex flex-wrap items-center gap-1.5">
                    <span className="font-semibold text-slate-400">Горячие клавиши:</span>
                    <span className="inline-flex items-center space-x-1">
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">1–{currentQ.options?.length || 4}</kbd>
                      <span>/</span>
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">A–D</kbd>
                      <span className="text-slate-600 font-medium">Выбор</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center space-x-1">
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">Пробел</kbd>
                      <span className="text-slate-600 font-medium">Озвучить</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center space-x-1">
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">← / →</kbd>
                      <span className="text-slate-600 font-medium">Карточка</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center space-x-1">
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">Enter</kbd>
                      <span className="text-slate-600 font-medium">Далее</span>
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="inline-flex items-center space-x-1">
                      <kbd className="px-1.5 py-0.5 rounded bg-white border border-slate-200 font-mono font-bold text-slate-700 text-[10px]">E</kbd>
                      <span className="text-slate-600 font-medium">Разбор</span>
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowHotkeysModal(true)}
                    className="inline-flex items-center space-x-1 font-bold text-primary hover:text-primary-hover transition"
                  >
                    <Keyboard className="w-3.5 h-3.5" />
                    <span>Все клавиши (?)</span>
                  </button>
                </div>

                <div className="flex items-center justify-between pt-4 border-t border-slate-100 gap-3">
                  <button
                    type="button"
                    onClick={() => setCurrentCardIndex((prev) => Math.max(0, prev - 1))}
                    disabled={currentCardIndex === 0}
                    className="flex items-center space-x-1.5 px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-xs font-bold hover:bg-slate-50 transition disabled:opacity-30"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    <span>Назад</span>
                  </button>

                  <span className="text-xs font-semibold text-slate-400">
                    {currentCardIndex + 1} / {questions.length}
                  </span>

                  {currentCardIndex < questions.length - 1 ? (
                    <button
                      type="button"
                      onClick={() => setCurrentCardIndex((prev) => Math.min(questions.length - 1, prev + 1))}
                      className="flex items-center space-x-1.5 px-5 py-2.5 rounded-xl bg-primary hover:bg-primary-hover text-white text-xs font-bold transition shadow-sm"
                    >
                      <span>Далее</span>
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={handleSubmitClick}
                      disabled={isSubmitting || task.canSubmit === false}
                      className="flex items-center space-x-1.5 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition shadow-sm"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>Завершить задание</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })()}
        </div>
      ) : task?.type === 'READING' ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          <div className={`${readingColSpans.passage} lg:sticky lg:top-6 rounded-3xl bg-white p-6 border border-slate-200/90 shadow-sm space-y-4 transition-all duration-200`}>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 pb-3 border-b border-slate-100">
              <div className="flex items-center space-x-2">
                <BookOpen className="w-4 h-4 text-primary" />
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Reading Passage
                </h3>
                <span className="text-[11px] font-semibold text-slate-400 bg-slate-50 px-2 py-0.5 rounded-full border border-slate-100">
                  {task.content?.split(/\s+/).filter(Boolean).length || 0} words
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-1.5 self-start sm:self-auto">
                <div className="flex items-center bg-slate-100/90 rounded-xl p-0.5 border border-slate-200">
                  <button
                    type="button"
                    onClick={decreaseFontSize}
                    disabled={readingFontSize === 'sm'}
                    className="px-2 py-1 text-xs font-bold text-slate-600 hover:text-slate-900 disabled:opacity-30 transition rounded-lg hover:bg-white"
                    title="Decrease font size"
                    aria-label="Decrease font size"
                  >
                    A-
                  </button>
                  <span className="px-1 text-[10px] font-mono font-semibold text-slate-400 uppercase">
                    {readingFontSize}
                  </span>
                  <button
                    type="button"
                    onClick={increaseFontSize}
                    disabled={readingFontSize === 'xl'}
                    className="px-2 py-1 text-xs font-bold text-slate-600 hover:text-slate-900 disabled:opacity-30 transition rounded-lg hover:bg-white"
                    title="Increase font size"
                    aria-label="Increase font size"
                  >
                    A+
                  </button>
                </div>

                <div className="flex items-center bg-slate-100/90 rounded-xl p-0.5 border border-slate-200 text-[11px]">
                  {(
                    [
                      { id: 'compact', label: '35%' },
                      { id: 'balanced', label: '50%' },
                      { id: 'wide', label: '58%' },
                      { id: 'expanded', label: '67%' },
                    ] as const
                  ).map((preset) => {
                    const isSelected = readingWidthPreset === preset.id;
                    return (
                      <button
                        key={preset.id}
                        type="button"
                        onClick={() => setReadingWidthPreset(preset.id)}
                        className={`px-2 py-1 rounded-lg font-bold transition ${
                          isSelected
                            ? 'bg-white text-primary shadow-xs'
                            : 'text-slate-500 hover:text-slate-900'
                        }`}
                        title={`Resize reading passage column to ${preset.label}`}
                      >
                        {preset.label}
                      </button>
                    );
                  })}
                </div>

                <button
                  type="button"
                  onClick={() => setIsFullHeight(!isFullHeight)}
                  className={`p-1.5 rounded-xl border text-xs font-semibold transition ${
                    isFullHeight
                      ? 'bg-indigo-50 border-indigo-200 text-primary'
                      : 'bg-slate-100/90 border-slate-200 text-slate-500 hover:text-slate-800'
                  }`}
                  title={isFullHeight ? 'Switch to scrollable window' : 'Expand full reading text height'}
                  aria-label="Toggle full height reading window"
                >
                  {isFullHeight ? <Minimize2 className="w-3.5 h-3.5" /> : <Maximize2 className="w-3.5 h-3.5" />}
                </button>
              </div>
            </div>

            <div
              className={`text-slate-800 ${fontSizeClass} whitespace-pre-wrap font-serif select-text ${
                isFullHeight ? 'max-h-none overflow-visible' : 'max-h-[72vh] overflow-y-auto pr-2'
              } space-y-3 transition-all duration-200`}
            >
              {task.content}
            </div>
          </div>

          <div className={`${readingColSpans.questions} space-y-4 transition-all duration-200`}>
            {questions.length === 0 ? (
              <div className="bg-white p-6 rounded-2xl text-center text-slate-500 text-sm">
                No comprehension questions loaded for this reading task.
              </div>
            ) : (
              questions.map((q, idx) => {
                const isTF =
                  Array.isArray(q.options) &&
                  q.options.length === 2 &&
                  (q.options.includes('True') || q.options.includes('False'));
                const isMCQ = Array.isArray(q.options) && q.options.length > 0 && !isTF;

                return (
                  <div
                    key={q.id}
                    className="bg-white rounded-3xl p-6 border border-slate-100 shadow-sm space-y-4"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="space-y-1">
                        <div className="flex items-center space-x-2">
                          <span className="w-6 h-6 rounded-lg bg-primary/10 text-primary text-xs font-bold flex items-center justify-center">
                            {idx + 1}
                          </span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                            {isTF ? 'True / False' : isMCQ ? 'Multiple Choice' : 'Sentence Completion'}
                          </span>
                          {q.points && (
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-primary">
                              {q.points} pts
                            </span>
                          )}
                        </div>
                        <span className="text-sm font-bold text-slate-800 block pt-1">
                          {q.questionText}
                        </span>
                      </div>

                      {!isExplanationLocked && (
                        <button
                          type="button"
                          onClick={() => handleExplainSentence(q.questionText)}
                          className="text-[11px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200/80 px-2 py-0.5 rounded-lg transition flex items-center space-x-1 flex-shrink-0"
                          title="Пояснить предложение"
                        >
                          <Lightbulb className="w-3 h-3 text-amber-600" />
                          <span>Пояснить</span>
                        </button>
                      )}
                    </div>

                    {isMCQ || isTF ? (
                      <div className="space-y-2">
                        {q.options?.map((opt, optIdx) => {
                          const isSelected = answers[q.id] === opt;
                          return (
                            <button
                              type="button"
                              key={optIdx}
                              disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(q.id))}
                              onClick={() => handleSelectAnswer(q.id, opt)}
                              className={`w-full text-left p-3.5 rounded-2xl border text-sm font-medium transition flex items-center justify-between ${
                                isSelected
                                  ? 'border-primary bg-indigo-50/60 text-primary font-semibold'
                                  : 'border-slate-200 hover:border-slate-300 text-slate-700'
                              } disabled:opacity-60`}
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
                        disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(q.id))}
                        placeholder="Complete the sentence or answer based on the reading text..."
                        value={answers[q.id] || ''}
                        onChange={(e) => handleSelectAnswer(q.id, e.target.value)}
                        className="w-full p-3.5 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition disabled:opacity-60"
                      />
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      ) : (
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
                  <div className="flex items-center space-x-2 flex-shrink-0">
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-500">
                      Diff: {q.difficulty}
                    </span>
                    {!isExplanationLocked && (
                      <button
                        type="button"
                        onClick={() => handleExplainSentence(q.questionText)}
                        className="text-[11px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200/80 px-2 py-0.5 rounded-lg transition flex items-center space-x-1"
                        title="Пояснить предложение"
                      >
                        <Lightbulb className="w-3 h-3 text-amber-600" />
                        <span>Пояснить</span>
                      </button>
                    )}
                  </div>
                </div>

                {task.type !== 'REWRITE' && task.type !== 'OPEN_BRACKETS' && task.type !== 'SHORT_ANSWER' && Array.isArray(q.options) && q.options.length > 0 ? (
                  <div className="space-y-2">
                    {q.options.map((opt, optIdx) => {
                      const isSelected = answers[q.id] === opt;
                      return (
                        <button
                          type="button"
                          key={optIdx}
                          disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(q.id))}
                          onClick={() => handleSelectAnswer(q.id, opt)}
                          className={`w-full text-left p-3.5 rounded-2xl border text-sm font-medium transition flex items-center justify-between ${
                            isSelected
                              ? 'border-primary bg-indigo-50/60 text-primary font-semibold'
                              : 'border-slate-200 hover:border-slate-300 text-slate-700'
                          } disabled:opacity-60`}
                        >
                          <span>{opt}</span>
                          {isSelected && <CheckCircle2 className="w-4 h-4 text-primary" />}
                        </button>
                      );
                    })}
                  </div>
                ) : task.type === 'REWRITE' ? (
                  <textarea
                    rows={2}
                    disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(q.id))}
                    placeholder="Rewrite the complete sentence here..."
                    value={answers[q.id] || ''}
                    onChange={(e) => handleSelectAnswer(q.id, e.target.value)}
                    className="w-full p-3.5 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition disabled:opacity-60 resize-y"
                  />
                ) : (
                  <input
                    type="text"
                    disabled={task.canSubmit === false || (instantFeedback && !isTeacherTask && lockedInstantQuestions.has(q.id))}
                    placeholder={
                      task.type === 'OPEN_BRACKETS'
                        ? 'Type the correct form of the word in brackets...'
                        : 'Type your answer here...'
                    }
                    value={answers[q.id] || ''}
                    onChange={(e) => handleSelectAnswer(q.id, e.target.value)}
                    className="w-full p-3.5 rounded-2xl border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition disabled:opacity-60"
                  />
                )}
              </div>
            ))
          )}
        </div>
      )}

      {task.type !== 'SPEAKING' && (
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 pt-4 border-t border-slate-100">
          <div className="flex items-center space-x-3 text-xs font-semibold text-slate-500">
            <span>{answeredCount} of {questions.length} answered</span>
            <div className="w-24 h-2 bg-slate-100 rounded-full overflow-hidden border border-slate-200">
              <div
                className="h-full bg-primary transition-all duration-300"
                style={{ width: `${questions.length > 0 ? (answeredCount / questions.length) * 100 : 0}%` }}
              />
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={handleSaveDraft}
              disabled={task.canSubmit === false}
              className="flex items-center justify-center space-x-2 py-3 px-5 rounded-2xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm transition disabled:opacity-50"
            >
              <Save className="w-4 h-4" />
              <span>{saveSuccess ? 'Draft Saved!' : 'Save Draft'}</span>
            </button>

            <button
              type="button"
              onClick={handleSubmitClick}
              disabled={isSubmitting || task.canSubmit === false}
              className="flex-1 sm:flex-initial flex items-center justify-center space-x-2 py-3.5 px-6 rounded-2xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
            >
              {isSubmitting ? (
                <LoadingSpinner size="sm" className="p-0 text-white" />
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>{task.canSubmit === false ? 'Attempt Already Submitted' : 'Submit Answers'}</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {explainingSentence != null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6 sm:p-7 shadow-2xl border border-slate-100 space-y-5 animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                  <Lightbulb className="w-4 h-4 text-amber-600" />
                </div>
                <div>
                  <h3 className="text-sm font-black text-slate-900">
                    Пояснение предложения
                  </h3>
                  <span className="text-[11px] text-slate-400 font-semibold">
                    Изучение грамматики и контекста
                  </span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setExplainingSentence(null)}
                className="p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 space-y-1">
              <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">
                Предложение
              </span>
              <p className="text-sm font-semibold text-slate-900 leading-relaxed font-serif">
                "{explainingSentence}"
              </p>
            </div>

            {isExplaining && (
              <div className="py-8 text-center space-y-3">
                <LoadingSpinner size="md" message="Анализируем грамматические структуры и лексику..." />
              </div>
            )}

            {explanationError && (
              <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
                <span>{explanationError}</span>
                <button
                  type="button"
                  onClick={() => handleExplainSentence(explainingSentence)}
                  className="px-3 py-1 bg-rose-600 text-white rounded-lg font-bold hover:bg-rose-700 transition"
                >
                  Повторить
                </button>
              </div>
            )}

            {explanationData && (
              <div className="space-y-4 animate-in fade-in duration-200">
                <div className="p-4 rounded-2xl bg-indigo-50/70 border border-indigo-100 space-y-1">
                  <span className="text-[10px] uppercase font-bold text-primary block tracking-wider">
                    Перевод на русский
                  </span>
                  <p className="text-sm font-semibold text-slate-900 leading-relaxed">
                    {explanationData.translation}
                  </p>
                </div>

                {explanationData.grammarBreakdown && (
                  <div className="space-y-1.5">
                    <span className="text-xs font-bold text-slate-700 block">Грамматический разбор:</span>
                    <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-xs text-slate-800 leading-relaxed font-mono">
                      {explanationData.grammarBreakdown}
                    </div>
                  </div>
                )}

                {explanationData.explanation && (
                  <div className="space-y-1.5">
                    <span className="text-xs font-bold text-slate-700 block">Пояснение:</span>
                    <p className="text-xs text-slate-700 leading-relaxed bg-white p-4 rounded-2xl border border-slate-200">
                      {explanationData.explanation}
                    </p>
                  </div>
                )}

                {explanationData.keyVocabulary && explanationData.keyVocabulary.length > 0 && (
                  <div className="space-y-2">
                    <span className="text-xs font-bold text-slate-700 block">Ключевая лексика:</span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                      {explanationData.keyVocabulary.map((v, i) => (
                        <div key={i} className="p-2.5 rounded-xl border border-slate-200 bg-slate-50/60 text-xs text-slate-800 font-medium">
                          {v}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}

      {floatingSelection != null && !explainingSentence && (
        <div className="fixed bottom-6 right-6 z-40 animate-in slide-in-from-bottom-3 duration-200">
          <div className="flex items-center space-x-1 p-1 bg-slate-900 rounded-2xl shadow-xl shadow-slate-900/40 ring-2 ring-primary/40 text-white">
            <button
              type="button"
              onClick={() => handleExplainSentence(floatingSelection)}
              className="flex items-center space-x-2 py-2 px-3.5 hover:text-primary-light transition font-bold text-xs"
            >
              <Lightbulb className="w-4 h-4 text-amber-400 flex-shrink-0" />
              <span className="truncate max-w-[200px] sm:max-w-[280px]">
                Пояснить: &laquo;{floatingSelection.slice(0, 24)}...&raquo;
              </span>
            </button>
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                setFloatingSelection(null);
                window.getSelection()?.removeAllRanges();
              }}
              className="p-1.5 text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition"
              title="Закрыть"
              aria-label="Dismiss selection"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      <ConfirmDialog
        isOpen={showUnansweredConfirm}
        title="Unanswered Questions Detected"
        message={`You have ${unansweredCount} unanswered questions out of ${questions.length}. Any unanswered questions will receive 0 points. Are you sure you want to submit your attempt now?`}
        confirmText="Submit Anyway"
        cancelText="Keep Answering"
        onConfirm={() => {
          setShowUnansweredConfirm(false);
          executeSubmit();
        }}
        onCancel={() => setShowUnansweredConfirm(false)}
      />

      <HotkeysModal
        isOpen={showHotkeysModal}
        onClose={() => setShowHotkeysModal(false)}
      />
    </div>
  );
};
