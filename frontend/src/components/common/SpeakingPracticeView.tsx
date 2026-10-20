/**
 * @file SpeakingPracticeView.tsx
 * @brief Interactive speaking practice and pronunciation evaluation interface.
 *
 * Provides real-time speech recognition via browser Web Speech API, word-by-word
 * phonetic alignment, IPA transcription overlays, click-to-listen TTS audio playback,
 * and actionable pronunciation diagnostics.
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  Mic,
  MicOff,
  Volume2,
  Square,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
  Info,
  Award,
  Gauge,
  Timer,
  Lock
} from 'lucide-react';
import {
  speakWord,
  speakText,
  stopSpeech,
  alignSpokenWords,
  PronunciationResult,
  AlignedWord
} from '../../utils/phonetics';

interface SpeechRecognitionEvent {
  results: {
    [index: number]: {
      [index: number]: {
        transcript: string;
        confidence: number;
      };
      isFinal?: boolean;
      length: number;
    };
    length: number;
  };
}

interface SpeechRecognitionInstance {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onstart: () => void;
  onresult: (event: SpeechRecognitionEvent) => void;
  onerror: (event: { error: string }) => void;
  onend: () => void;
}

interface SpeakingPracticeViewProps {
  passageText: string;
  taskTitle?: string;
  targetSounds?: string;
  points?: number;
  onSubmit: (result: {
    spokenTranscript: string;
    accuracyScore: number;
    overallScore: number;
    wpm: number;
    durationSeconds: number;
    pacingScore: number;
    pacingFeedback: string;
    words: AlignedWord[];
    generalFeedback: string[];
  }) => void;
  isSubmitting?: boolean;
  readOnly?: boolean;
  initialResult?: PronunciationResult;
  isTeacherTask?: boolean;
}

export const SpeakingPracticeView: React.FC<SpeakingPracticeViewProps> = ({
  passageText,
  taskTitle,
  targetSounds,
  points = 100,
  onSubmit,
  isSubmitting = false,
  readOnly = false,
  initialResult,
  isTeacherTask = false,
}) => {
  const [accent, setAccent] = useState<'en-US' | 'en-GB'>('en-US');
  const [isPlayingModel, setIsPlayingModel] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [recordedTranscript, setRecordedTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const [recognitionSupported, setRecognitionSupported] = useState(true);
  const [recordedAudioUrl, setRecordedAudioUrl] = useState<string | null>(null);
  const [activeWordTip, setActiveWordTip] = useState<AlignedWord | null>(null);
  const [evaluationResult, setEvaluationResult] = useState<PronunciationResult | null>(initialResult || null);
  const [micError, setMicError] = useState<string | null>(null);

  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const startTimeRef = useRef<number>(0);

  const isModelAudioLocked = Boolean(isTeacherTask && !evaluationResult && !readOnly);

  useEffect(() => {
    let timer: NodeJS.Timeout | null = null;
    if (isRecording) {
      timer = setInterval(() => {
        setRecordingSeconds(Math.max(1, Math.round((Date.now() - startTimeRef.current) / 1000)));
      }, 500);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [isRecording]);

  useEffect(() => {
    const SpeechRecognitionAPI =
      (window as unknown as { SpeechRecognition?: new () => SpeechRecognitionInstance }).SpeechRecognition ||
      (window as unknown as { webkitSpeechRecognition?: new () => SpeechRecognitionInstance }).webkitSpeechRecognition;

    if (!SpeechRecognitionAPI) {
      setRecognitionSupported(false);
      return;
    }

    try {
      const recognition = new SpeechRecognitionAPI();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = accent;

      recognition.onstart = () => {
        setIsRecording(true);
        setMicError(null);
      };

      recognition.onresult = (event: SpeechRecognitionEvent) => {
        let finalStr = '';
        let interimStr = '';
        for (let i = 0; i < event.results.length; i++) {
          const res = event.results[i];
          if (res.isFinal) {
            finalStr += res[0].transcript + ' ';
          } else {
            interimStr += res[0].transcript;
          }
        }
        if (finalStr.trim()) {
          setRecordedTranscript(prev => (prev + ' ' + finalStr).trim());
        }
        setInterimTranscript(interimStr);
      };

      recognition.onerror = (event: { error: string }) => {
        if (event.error === 'not-allowed') {
          setMicError('Microphone permission was denied. Please allow microphone access in your browser settings.');
        } else if (event.error !== 'no-speech') {
          setMicError(`Speech recognition notice: ${event.error}`);
        }
        setIsRecording(false);
      };

      recognition.onend = () => {
        setIsRecording(false);
      };

      recognitionRef.current = recognition;
    } catch {
      setRecognitionSupported(false);
    }

    return () => {
      stopSpeech();
      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch (ignored) {}
      }
    };
  }, [accent]);

  const handlePlayModelAudio = () => {
    if (isModelAudioLocked) return;
    if (isPlayingModel) {
      stopSpeech();
      setIsPlayingModel(false);
    } else {
      setIsPlayingModel(true);
      speakText(passageText, accent, () => {
        setIsPlayingModel(false);
      });
    }
  };

  const stopAllRecording = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop();
      } catch (ignored) {}
    }
    if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
      try {
        mediaRecorderRef.current.stop();
      } catch (ignored) {}
    }
    if (mediaStreamRef.current) {
      try {
        mediaStreamRef.current.getTracks().forEach(track => track.stop());
      } catch (ignored) {}
      mediaStreamRef.current = null;
    }
  };

  const startMediaRecorderFallback = () => {
    if (typeof navigator === 'undefined' || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setMicError('Запись микрофона не поддерживается в данном браузере.');
      return;
    }

    navigator.mediaDevices.getUserMedia({ audio: true })
      .then((stream) => {
        mediaStreamRef.current = stream;
        const mr = new MediaRecorder(stream);
        mr.ondataavailable = (e) => {
          if (e.data && e.data.size > 0) {
            audioChunksRef.current.push(e.data);
          }
        };
        mr.onstop = () => {
          if (audioChunksRef.current.length > 0) {
            const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
            setRecordedAudioUrl(URL.createObjectURL(blob));
          }
        };
        mediaRecorderRef.current = mr;
        mr.start(250);
        setIsRecording(true);
        setMicError(null);
      })
      .catch((err) => {
        if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
          setMicError('Доступ к микрофону отклонен. Пожалуйста, разрешите доступ к микрофону в настройках браузера.');
        } else {
          setMicError(`Ошибка микрофона: ${err.message || 'Не удалось активировать микрофон'}`);
        }
        setIsRecording(false);
      });
  };

  const toggleRecording = () => {
    if (isRecording) {
      const elapsed = Math.max(1, Math.round((Date.now() - startTimeRef.current) / 1000));
      stopAllRecording();
      setIsRecording(false);

      setTimeout(() => {
        const fullTranscript = (recordedTranscript + ' ' + interimTranscript).trim();
        if (fullTranscript) {
          evaluateSpokenSpeech(undefined, elapsed);
        } else if (audioChunksRef.current.length > 0) {
          const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
          setRecordedAudioUrl(URL.createObjectURL(blob));
          evaluateSpokenSpeech('[Recorded Audio Submission]', elapsed);
        } else {
          evaluateSpokenSpeech(undefined, elapsed);
        }
      }, 150);
    } else {
      stopSpeech();
      setIsPlayingModel(false);
      setRecordedTranscript('');
      setInterimTranscript('');
      setEvaluationResult(null);
      setActiveWordTip(null);
      setMicError(null);
      setRecordingSeconds(0);
      setRecordedAudioUrl(null);
      audioChunksRef.current = [];
      startTimeRef.current = Date.now();

      if (recognitionSupported && recognitionRef.current) {
        try {
          recognitionRef.current.lang = accent;
          recognitionRef.current.start();
        } catch {
          startMediaRecorderFallback();
        }
      } else {
        startMediaRecorderFallback();
      }
    }
  };

  const evaluateSpokenSpeech = (overrideText?: string, elapsedSeconds?: number) => {
    const fullTranscript = (overrideText || (recordedTranscript + ' ' + interimTranscript)).trim();
    if (!fullTranscript) {
      setMicError('No speech detected. Please speak closer to the microphone and read clearly.');
      return;
    }
    const duration = elapsedSeconds || recordingSeconds || 5;
    const result = alignSpokenWords(passageText, fullTranscript, 0.92, duration);
    setEvaluationResult(result);
  };

  const handleReset = () => {
    stopSpeech();
    stopAllRecording();
    setIsPlayingModel(false);
    setIsRecording(false);
    setRecordedTranscript('');
    setInterimTranscript('');
    setEvaluationResult(null);
    setActiveWordTip(null);
    setMicError(null);
    setRecordingSeconds(0);
    setRecordedAudioUrl(null);
    audioChunksRef.current = [];
  };

  const handleSubmit = () => {
    if (!evaluationResult) return;
    onSubmit({
      spokenTranscript: recordedTranscript || interimTranscript || '[Recorded Audio Submission]',
      accuracyScore: evaluationResult.accuracyScore,
      overallScore: Math.round((evaluationResult.overallScore / 100) * points),
      wpm: evaluationResult.wpm,
      durationSeconds: evaluationResult.durationSeconds,
      pacingScore: evaluationResult.pacingScore,
      pacingFeedback: evaluationResult.pacingFeedback,
      words: evaluationResult.words,
      generalFeedback: evaluationResult.generalFeedback,
    });
  };

  const formatDuration = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Quick word pronunciation click
  const handleWordClick = (word: string) => {
    if (isModelAudioLocked) return;
    speakWord(word, accent);
  };

  const wordsList = passageText.trim().split(/\s+/);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl max-w-4xl mx-auto space-y-6">
      {/* Title & Target Sounds Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-wider uppercase bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              Speaking Practice
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              {points} Max Points
            </span>
          </div>
          <h2 className="text-xl font-bold text-white">
            {taskTitle || 'Oral Reading & Pronunciation Assessment'}
          </h2>
          {targetSounds && (
            <div className="flex items-center gap-1.5 text-xs text-indigo-300 mt-1">
              <Sparkles className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
              <span>Phonetic Focus: <strong className="text-indigo-200">{targetSounds}</strong></span>
            </div>
          )}
        </div>

        {/* Accent Selector & Audio Model Button */}
        <div className="flex items-center gap-2 shrink-0">
          <div className="flex bg-slate-800 p-0.5 rounded-lg border border-slate-700 text-xs">
            <button
              type="button"
              onClick={() => setAccent('en-US')}
              className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                accent === 'en-US' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              US Accent
            </button>
            <button
              type="button"
              onClick={() => setAccent('en-GB')}
              className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                accent === 'en-GB' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-white'
              }`}
            >
              UK Accent
            </button>
          </div>

          <button
            type="button"
            onClick={handlePlayModelAudio}
            disabled={isModelAudioLocked}
            title={isModelAudioLocked ? 'Прослушивание образца заблокировано до сдачи ответа (Задание преподавателя)' : undefined}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
              isModelAudioLocked
                ? 'bg-slate-800 text-slate-500 border-slate-700 cursor-not-allowed opacity-60'
                : isPlayingModel
                ? 'bg-rose-500/20 border-rose-500 text-rose-300 animate-pulse'
                : 'bg-indigo-600/20 border-indigo-500/40 text-indigo-300 hover:bg-indigo-600/30'
            }`}
          >
            {isModelAudioLocked ? (
              <Lock className="w-3.5 h-3.5 text-slate-400" />
            ) : isPlayingModel ? (
              <Square className="w-3.5 h-3.5" />
            ) : (
              <Volume2 className="w-3.5 h-3.5" />
            )}
            <span>{isModelAudioLocked ? 'Образец скрыт' : isPlayingModel ? 'Остановить' : 'Слушать образец'}</span>
          </button>
        </div>
      </div>

      {/* Instructions Banner */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl p-3.5 text-xs text-slate-300 flex items-start gap-3">
        <Info className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-slate-200">Инструкция: </span>
          {isModelAudioLocked ? (
            <span>
              Прочитайте отрывок вслух и запишите ваш голос. Прослушивание образца и аудиоподсказки станут доступны после записи.
            </span>
          ) : (
            <span>
              Нажмите <strong className="text-indigo-300">Начать запись</strong> и четко прочитайте текст в микрофон. Вы можете нажимать на слова для прослушивания IPA произношения.
            </span>
          )}
        </div>
      </div>

      {/* The Text Passage Display */}
      <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-6">
        <div className="text-xs uppercase tracking-wider font-semibold text-slate-400 mb-3 flex items-center justify-between">
          <span>Speech Passage</span>
          <span className="text-[11px] text-slate-500 font-normal">Click any word to hear native pronunciation</span>
        </div>

        {/* Word-by-Word Interactive Display */}
        <div className="leading-loose text-lg font-medium text-slate-100 flex flex-wrap gap-x-2 gap-y-3">
          {evaluationResult
            ? evaluationResult.words.map((item, idx) => {
                const isCorrect = item.status === 'CORRECT';
                const isMispronounced = item.status === 'MISPRONOUNCED';

                return (
                  <div key={idx} className="relative group inline-flex flex-col items-center">
                    <button
                      type="button"
                      onClick={() => {
                        handleWordClick(item.cleanWord);
                        setActiveWordTip(item);
                      }}
                      className={`px-2 py-0.5 rounded-md border text-base font-medium transition-all ${
                        isCorrect
                          ? 'bg-emerald-500/20 text-emerald-200 border-emerald-500/40 hover:bg-emerald-500/30'
                          : isMispronounced
                          ? 'bg-rose-500/20 text-rose-200 border-rose-500/50 hover:bg-rose-500/30 ring-1 ring-rose-500/30 font-semibold'
                          : 'bg-amber-500/10 text-amber-300/70 border-amber-500/30 line-through'
                      }`}
                      title={`Click to pronounce: ${item.ipa}`}
                    >
                      {item.word}
                    </button>
                    {/* IPA Subtitle */}
                    <span className="text-[10px] text-slate-400 font-mono tracking-tight select-none mt-0.5">
                      {item.ipa}
                    </span>
                  </div>
                );
              })
            : wordsList.map((w, idx) => (
                <div key={idx} className="inline-flex flex-col items-center">
                  <button
                    type="button"
                    onClick={() => handleWordClick(w)}
                    disabled={isModelAudioLocked}
                    className={`px-1.5 py-0.5 rounded transition-colors text-slate-100 ${
                      isModelAudioLocked ? 'cursor-default opacity-80' : 'hover:text-indigo-300 hover:bg-indigo-500/10 cursor-pointer'
                    }`}
                    title={isModelAudioLocked ? 'Озвучивание доступно после записи' : 'Нажмите для прослушивания'}
                  >
                    {w}
                  </button>
                </div>
              ))}
        </div>
      </div>

      {/* Microphone Error Banner */}
      {micError && (
        <div className="bg-rose-500/10 border border-rose-500/30 rounded-xl p-4 text-xs text-rose-300 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
          <div className="flex-1">
            <div className="font-semibold text-rose-200 mb-0.5">Microphone Notice</div>
            <div>{micError}</div>
          </div>
        </div>
      )}

      {/* Recording Status / Live Transcription */}
      {isRecording && (
        <div className="bg-indigo-950/40 border border-indigo-500/50 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2 text-indigo-300 text-xs font-semibold">
              <div className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
              <span>Слушаем вашу речь... Читайте текст четко и внятно</span>
            </div>
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-indigo-900/60 border border-indigo-500/30 text-indigo-200 text-xs font-mono font-bold">
              <Timer className="w-3.5 h-3.5 text-indigo-400" />
              <span>{formatDuration(recordingSeconds)}</span>
            </div>
          </div>
          <div className="text-sm text-slate-200 italic font-medium min-h-[1.5rem] bg-slate-900/60 p-3 rounded-lg border border-slate-800">
            {recordedTranscript} <span className="text-indigo-400">{interimTranscript}</span>
          </div>
        </div>
      )}

      {/* Recorded Audio Playback */}
      {recordedAudioUrl && (
        <div className="bg-slate-950/80 border border-indigo-500/30 rounded-xl p-3.5 flex flex-col sm:flex-row items-center justify-between gap-3 animate-in fade-in duration-200">
          <div className="text-xs text-indigo-300 font-semibold flex items-center gap-2">
            <Mic className="w-4 h-4 text-indigo-400" />
            <span>Ваша аудиозапись:</span>
          </div>
          <audio controls src={recordedAudioUrl} className="h-8 w-full sm:w-80" />
        </div>
      )}

      {/* Pronunciation Diagnostics Summary Card */}
      {evaluationResult && (
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-700/70 pb-3">
            <div className="flex items-center gap-2">
              <Award className="w-5 h-5 text-indigo-400" />
              <div>
                <h3 className="text-base font-bold text-white">Speech & Pronunciation Evaluation</h3>
                <p className="text-xs text-slate-400">Calibrated against accuracy, articulation, and pacing rate</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right">
                <div className="text-2xl font-black text-indigo-400">
                  {evaluationResult.accuracyScore}%
                </div>
                <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  Pronunciation Accuracy
                </div>
              </div>
              <div className="h-8 w-px bg-slate-700" />
              <div className="text-right">
                <div className="text-xl font-bold text-emerald-400">
                  {Math.round((evaluationResult.overallScore / 100) * points)} / {points}
                </div>
                <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  Earned Points ({evaluationResult.overallScore}%)
                </div>
              </div>
            </div>
          </div>

          {/* Stats Quad */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
            <div className="bg-emerald-500/10 border border-emerald-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-emerald-400">{evaluationResult.accuracyScore}%</div>
              <div className="text-[11px] text-emerald-300/80">Accuracy · {evaluationResult.correctCount}/{evaluationResult.totalWords}</div>
            </div>
            <div className="bg-indigo-500/10 border border-indigo-500/20 p-2.5 rounded-lg">
              <div className="flex items-center justify-center gap-1 text-lg font-bold text-indigo-400">
                <Gauge className="w-4 h-4 text-indigo-400" />
                <span>{evaluationResult.wpm}</span>
                <span className="text-xs font-normal text-indigo-300">WPM</span>
              </div>
              <div className="text-[11px] text-indigo-300/80">Pacing Score · {evaluationResult.pacingScore}%</div>
            </div>
            <div className="bg-rose-500/10 border border-rose-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-rose-400">{evaluationResult.mispronouncedCount}</div>
              <div className="text-[11px] text-rose-300/80">Mispronounced</div>
            </div>
            <div className="bg-slate-700/30 border border-slate-700 p-2.5 rounded-lg">
              <div className="flex items-center justify-center gap-1 text-lg font-bold text-slate-200">
                <Timer className="w-4 h-4 text-slate-400" />
                <span>{evaluationResult.durationSeconds}s</span>
              </div>
              <div className="text-[11px] text-slate-400">Recorded Time</div>
            </div>
          </div>

          {/* Feedback Notes */}
          <div className="space-y-1.5">
            {evaluationResult.generalFeedback.map((tip, i) => (
              <div key={i} className="flex items-start gap-2 text-xs text-slate-300">
                <CheckCircle2 className="w-3.5 h-3.5 text-indigo-400 shrink-0 mt-0.5" />
                <span>{tip}</span>
              </div>
            ))}
          </div>

          {/* Selected Word Diagnostic Details */}
          {activeWordTip && (
            <div className="bg-slate-900 border border-indigo-500/40 rounded-lg p-3.5 text-xs space-y-2 mt-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-sm text-white">{activeWordTip.word}</span>
                  <span className="font-mono text-indigo-300 bg-indigo-950/60 px-2 py-0.5 rounded border border-indigo-800">
                    {activeWordTip.ipa}
                  </span>
                  {activeWordTip.heard && (
                    <span className="text-slate-400">Heard as: <em className="text-rose-300 font-semibold">"{activeWordTip.heard}"</em></span>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => speakWord(activeWordTip.cleanWord, accent)}
                  className="flex items-center gap-1 text-xs text-indigo-300 bg-indigo-600/30 hover:bg-indigo-600/50 px-2.5 py-1 rounded transition-colors"
                >
                  <Volume2 className="w-3.5 h-3.5" />
                  <span>Listen</span>
                </button>
              </div>
              {activeWordTip.tip && (
                <div className="text-slate-300 bg-slate-950/50 p-2 rounded border border-slate-800 leading-relaxed">
                  {activeWordTip.tip}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Control Actions Bottom Bar */}
      {!readOnly && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-slate-800 pt-5">
          <div className="flex items-center gap-3 w-full sm:w-auto">
            {/* Main Record Button */}
            <button
              type="button"
              disabled={isSubmitting}
              onClick={toggleRecording}
              className={`flex-1 sm:flex-none flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-sm transition-all shadow-lg ${
                isRecording
                  ? 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-600/30 animate-pulse'
                  : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-600/30'
              }`}
            >
              {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              <span>{isRecording ? 'Завершить и оценить' : evaluationResult ? 'Прочитать снова' : 'Начать запись'}</span>
            </button>

            {/* Reset Button */}
            {evaluationResult && (
              <button
                type="button"
                onClick={handleReset}
                className="flex items-center gap-1.5 px-4 py-3 rounded-xl text-xs font-semibold text-slate-300 bg-slate-800 hover:bg-slate-700 border border-slate-700 transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Сбросить</span>
              </button>
            )}
          </div>

          {/* Submit Evaluation Button */}
          {evaluationResult && (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-7 py-3 rounded-xl font-bold text-sm bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-600/20 transition-all disabled:opacity-50"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>{isSubmitting ? 'Отправка...' : 'Отправить результат'}</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
