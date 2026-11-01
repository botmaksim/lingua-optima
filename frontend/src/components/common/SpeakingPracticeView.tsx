/**
 * @file SpeakingPracticeView.tsx
 * @brief Interactive speaking practice and pronunciation evaluation interface.
 *
 * Provides real-time speech recognition via browser Web Speech API, word-by-word
 * phonetic alignment, IPA transcription overlays, click-to-listen TTS audio playback,
 * and actionable pronunciation diagnostics.
 */

import React, { useState, useEffect, useRef, useMemo } from 'react';
import {
  Mic,
  MicOff,
  Volume2,
  Square,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Target,
  Info,
  Award,
  Gauge,
  Timer,
  Lock,
  MessageSquare,
  Sparkles,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import {
  speakWord,
  speakText,
  stopSpeech,
  alignSpokenWords,
  evaluateSituationalSpeech,
  parseSituationContent,
  PronunciationResult,
  AlignedWord
} from '../../utils/phonetics';
import { SpeakingMode } from '../../types/task';

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
  speakingMode?: SpeakingMode;
  sampleResponse?: string;
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
  speakingMode = 'TEXT',
  sampleResponse,
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
  const [showSampleText, setShowSampleText] = useState(false);

  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const startTimeRef = useRef<number>(0);

  const isSituationMode =
    speakingMode === 'SITUATION' ||
    (Boolean(passageText) && (
      passageText.includes('Role:') ||
      passageText.includes('Goal:') ||
      passageText.includes('Talking Points:') ||
      passageText.includes('Communicative Situation')
    ));

  const parsedSituation = useMemo(() => {
    return isSituationMode ? parseSituationContent(passageText) : null;
  }, [isSituationMode, passageText]);

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
      const textToSpeak = isSituationMode
        ? (sampleResponse || (parsedSituation ? `${parsedSituation.goal || ''}. ${parsedSituation.talkingPoints.join('. ')}` : passageText))
        : passageText;
      speakText(textToSpeak, accent, () => {
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
      setMicError('Microphone recording is not supported in this browser.');
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
          setMicError('Microphone access denied. Please allow microphone permissions in your browser settings.');
        } else {
          setMicError(`Microphone error: ${err.message || 'Failed to activate microphone'}`);
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
    if (isSituationMode) {
      const result = evaluateSituationalSpeech(
        parsedSituation || { talkingPoints: [], usefulExpressions: [] },
        fullTranscript,
        duration
      );
      setEvaluationResult(result);
    } else {
      const result = alignSpokenWords(passageText, fullTranscript, 0.92, duration);
      setEvaluationResult(result);
    }
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold tracking-wider uppercase bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              {isSituationMode ? 'Communicative Situation' : 'Speaking Practice'}
            </span>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              {points} Max Points
            </span>
          </div>
          <h2 className="text-xl font-bold text-white">
            {taskTitle || (isSituationMode ? 'Communicative Role-Play Assessment' : 'Oral Reading & Pronunciation Assessment')}
          </h2>
          {targetSounds && (
            <div className="flex items-center gap-1.5 text-xs text-indigo-300 mt-1">
              <Target className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
              <span>{isSituationMode ? 'Target Focus' : 'Phonetic Focus'}: <strong className="text-indigo-200">{targetSounds}</strong></span>
            </div>
          )}
        </div>

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
            title={isModelAudioLocked ? 'Sample audio is locked until response submission (Teacher assignment setting)' : undefined}
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
            <span>{isModelAudioLocked ? 'Sample Hidden' : isPlayingModel ? 'Stop' : 'Listen to Sample'}</span>
          </button>
        </div>
      </div>

      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl p-3.5 text-xs text-slate-300 flex items-start gap-3">
        <Info className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-slate-200">Instructions: </span>
          {isModelAudioLocked ? (
            <span>
              {isSituationMode
                ? 'Review the communicative scenario and record your spoken response. Sample audio will unlock after submission.'
                : 'Read the passage aloud and record your voice. Sample audio and pronunciation guides will unlock after submission.'}
            </span>
          ) : (
            <span>
              {isSituationMode ? (
                <>
                  Review your role, setting, and communicative goal. Click <strong className="text-indigo-300">Start Recording</strong> and respond clearly into your microphone addressing all talking points.
                </>
              ) : (
                <>
                  Click <strong className="text-indigo-300">Start Recording</strong> and read the passage clearly into your microphone. You can click individual words to hear native IPA pronunciation.
                </>
              )}
            </span>
          )}
        </div>
      </div>

      {isSituationMode && parsedSituation ? (
        <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-5 space-y-4">
          <div className="flex flex-wrap items-center gap-2.5">
            {parsedSituation.role && (
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-indigo-500/15 border border-indigo-500/30 text-indigo-200 text-xs font-semibold">
                <span className="text-indigo-400 uppercase text-[10px] tracking-wider font-bold">Role:</span>
                <span>{parsedSituation.role}</span>
              </div>
            )}
            {parsedSituation.setting && (
              <div className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 text-xs font-medium">
                <span className="text-slate-400 uppercase text-[10px] tracking-wider font-bold">Setting:</span>
                <span>{parsedSituation.setting}</span>
              </div>
            )}
          </div>

          {parsedSituation.goal && (
            <div className="bg-indigo-950/30 border border-indigo-500/30 rounded-xl p-3.5 flex items-start gap-3">
              <Target className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
              <div>
                <div className="text-xs font-bold uppercase tracking-wider text-indigo-300 mb-0.5">
                  Communicative Goal
                </div>
                <div className="text-sm font-medium text-slate-100 leading-snug">
                  {parsedSituation.goal}
                </div>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
            {parsedSituation.talkingPoints.length > 0 && (
              <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 space-y-2.5">
                <div className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <MessageSquare className="w-3.5 h-3.5 text-indigo-400" />
                  <span>Talking Points</span>
                </div>
                <ul className="space-y-2">
                  {parsedSituation.talkingPoints.map((pt, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-xs text-slate-200 leading-relaxed">
                      <span className="w-4 h-4 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                        {idx + 1}
                      </span>
                      <span>{pt}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {parsedSituation.usefulExpressions.length > 0 && (
              <div className="bg-slate-900/80 border border-slate-800/80 rounded-xl p-4 space-y-2.5">
                <div className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                  <span>Useful Expressions (click to listen)</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {parsedSituation.usefulExpressions.map((expr, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => speakText(expr, accent)}
                      className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-800/90 hover:bg-indigo-600/20 border border-slate-700/80 hover:border-indigo-500/40 text-xs text-slate-200 hover:text-indigo-200 transition-all text-left group"
                      title="Click to hear pronunciation"
                    >
                      <Volume2 className="w-3 h-3 text-slate-400 group-hover:text-indigo-400 shrink-0" />
                      <span>{expr}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {sampleResponse && !isModelAudioLocked && (
            <div className="border-t border-slate-800/80 pt-3">
              <button
                type="button"
                onClick={() => setShowSampleText(prev => !prev)}
                className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1.5 font-semibold transition-colors"
              >
                <span>{showSampleText ? 'Hide Sample Response' : 'Show Sample Response'}</span>
                {showSampleText ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
              </button>
              {showSampleText && (
                <div className="mt-2 p-3 rounded-lg bg-slate-900/90 border border-slate-800 text-xs text-slate-300 italic leading-relaxed animate-in fade-in duration-200">
                  "{sampleResponse}"
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-6">
          <div className="text-xs uppercase tracking-wider font-semibold text-slate-400 mb-3 flex items-center justify-between">
            <span>Speech Passage</span>
            <span className="text-[11px] text-slate-500 font-normal">Click any word to hear native pronunciation</span>
          </div>

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
                      title={isModelAudioLocked ? 'Audio available after recording' : 'Click to hear pronunciation'}
                    >
                      {w}
                    </button>
                  </div>
                ))}
          </div>
        </div>
      )}

      {micError && (
        <div className="bg-rose-500/10 border border-rose-500/30 rounded-xl p-4 text-xs text-rose-300 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
          <div className="flex-1">
            <div className="font-semibold text-rose-200 mb-0.5">Microphone Notice</div>
            <div>{micError}</div>
          </div>
        </div>
      )}

      {isRecording && (
        <div className="bg-indigo-950/40 border border-indigo-500/50 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2 text-indigo-300 text-xs font-semibold">
              <div className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
              <span>Listening to your voice... Speak clearly into the microphone</span>
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

      {recordedAudioUrl && (
        <div className="bg-slate-950/80 border border-indigo-500/30 rounded-xl p-3.5 flex flex-col sm:flex-row items-center justify-between gap-3 animate-in fade-in duration-200">
          <div className="text-xs text-indigo-300 font-semibold flex items-center gap-2">
            <Mic className="w-4 h-4 text-indigo-400" />
            <span>Your Audio Recording:</span>
          </div>
          <audio controls src={recordedAudioUrl} className="h-8 w-full sm:w-80" />
        </div>
      )}

      {evaluationResult && (
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-700/70 pb-3">
            <div className="flex items-center gap-2">
              <Award className="w-5 h-5 text-indigo-400" />
              <div>
                <h3 className="text-base font-bold text-white">
                  {isSituationMode ? 'Communicative Role-Play Evaluation' : 'Speech & Pronunciation Evaluation'}
                </h3>
                <p className="text-xs text-slate-400">
                  {isSituationMode
                    ? 'Calibrated against communicative fluency, speech volume, and conversational pacing'
                    : 'Calibrated against accuracy, articulation, and pacing rate'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="text-right">
                <div className="text-2xl font-black text-indigo-400">
                  {evaluationResult.accuracyScore}%
                </div>
                <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  {isSituationMode ? 'Communicative Success' : 'Pronunciation Accuracy'}
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

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
            <div className="bg-emerald-500/10 border border-emerald-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-emerald-400">{evaluationResult.accuracyScore}%</div>
              <div className="text-[11px] text-emerald-300/80">
                {isSituationMode ? 'Communicative Fluency' : `Accuracy · ${evaluationResult.correctCount}/${evaluationResult.totalWords}`}
              </div>
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
              <div className="text-lg font-bold text-rose-400">
                {isSituationMode ? evaluationResult.totalWords : evaluationResult.mispronouncedCount}
              </div>
              <div className="text-[11px] text-rose-300/80">
                {isSituationMode ? 'Words Spoken' : 'Mispronounced'}
              </div>
            </div>
            <div className="bg-slate-700/30 border border-slate-700 p-2.5 rounded-lg">
              <div className="flex items-center justify-center gap-1 text-lg font-bold text-slate-200">
                <Timer className="w-4 h-4 text-slate-400" />
                <span>{evaluationResult.durationSeconds}s</span>
              </div>
              <div className="text-[11px] text-slate-400">Recorded Time</div>
            </div>
          </div>

          <div className="space-y-1.5">
            {evaluationResult.generalFeedback.map((tip, i) => (
              <div key={i} className="flex items-start gap-2 text-xs text-slate-300">
                <CheckCircle2 className="w-3.5 h-3.5 text-indigo-400 shrink-0 mt-0.5" />
                <span>{tip}</span>
              </div>
            ))}
          </div>

          {isSituationMode && evaluationResult.words.length > 0 && (
            <div className="pt-2 border-t border-slate-700/60">
              <div className="text-xs font-semibold text-slate-400 mb-2 flex items-center justify-between">
                <span>Spoken Transcript (click any word for IPA)</span>
                <span className="text-[11px] text-slate-500">{evaluationResult.words.length} words</span>
              </div>
              <div className="flex flex-wrap gap-1.5 max-h-48 overflow-y-auto p-2.5 bg-slate-900/60 rounded-lg border border-slate-800">
                {evaluationResult.words.map((item, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      handleWordClick(item.cleanWord);
                      setActiveWordTip(item);
                    }}
                    className="px-2 py-0.5 rounded bg-slate-800 hover:bg-indigo-600/20 text-slate-200 border border-slate-700 hover:border-indigo-500/30 text-xs transition-colors flex items-center gap-1"
                  >
                    <span>{item.word}</span>
                    <span className="text-[10px] text-indigo-400 font-mono">{item.ipa}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

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

      {!readOnly && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-slate-800 pt-5">
          <div className="flex items-center gap-3 w-full sm:w-auto">
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
              <span>{isRecording ? 'Finish & Evaluate' : evaluationResult ? (isSituationMode ? 'Record Again' : 'Read Again') : 'Start Recording'}</span>
            </button>

            {evaluationResult && (
              <button
                type="button"
                onClick={handleReset}
                className="flex items-center gap-1.5 px-4 py-3 rounded-xl text-xs font-semibold text-slate-300 bg-slate-800 hover:bg-slate-700 border border-slate-700 transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset</span>
              </button>
            )}
          </div>

          {evaluationResult && (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-7 py-3 rounded-xl font-bold text-sm bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-600/20 transition-all disabled:opacity-50"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>{isSubmitting ? 'Submitting...' : 'Submit Result'}</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
