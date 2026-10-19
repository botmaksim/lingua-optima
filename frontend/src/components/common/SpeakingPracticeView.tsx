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
  Award
} from 'lucide-react';
import {
  speakWord,
  speakText,
  stopSpeech,
  alignSpokenWords,
  PronunciationResult,
  AlignedWord
} from '../../utils/phonetics';

// Type definitions for browser Web Speech Recognition
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
    words: AlignedWord[];
    generalFeedback: string[];
  }) => void;
  isSubmitting?: boolean;
  readOnly?: boolean;
  initialResult?: PronunciationResult;
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
}) => {
  const [accent, setAccent] = useState<'en-US' | 'en-GB'>('en-US');
  const [isPlayingModel, setIsPlayingModel] = useState(false);
  const [isRecording, setIsRecording] = useState(false);
  const [recordedTranscript, setRecordedTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const [recognitionSupported, setRecognitionSupported] = useState(true);
  const [activeWordTip, setActiveWordTip] = useState<AlignedWord | null>(null);
  const [evaluationResult, setEvaluationResult] = useState<PronunciationResult | null>(initialResult || null);
  const [micError, setMicError] = useState<string | null>(null);

  const recognitionRef = useRef<SpeechRecognitionInstance | null>(null);

  // Initialize Speech Recognition
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
    } catch (e) {
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

  // Model audio playback
  const handlePlayModelAudio = () => {
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

  // Start / Stop speech recording
  const toggleRecording = () => {
    if (isRecording) {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (ignored) {}
      }
      setIsRecording(false);
      evaluateSpokenSpeech();
    } else {
      stopSpeech();
      setIsPlayingModel(false);
      setRecordedTranscript('');
      setInterimTranscript('');
      setEvaluationResult(null);
      setActiveWordTip(null);
      setMicError(null);

      if (recognitionRef.current) {
        try {
          recognitionRef.current.lang = accent;
          recognitionRef.current.start();
        } catch (err) {
          setMicError('Could not start microphone. Please check browser permissions.');
        }
      }
    }
  };

  // Evaluate speech after recording completes
  const evaluateSpokenSpeech = (overrideText?: string) => {
    const fullTranscript = (overrideText || (recordedTranscript + ' ' + interimTranscript)).trim();
    if (!fullTranscript) {
      setMicError('No speech detected. Please speak closer to the microphone and read clearly.');
      return;
    }
    const result = alignSpokenWords(passageText, fullTranscript, 0.92);
    setEvaluationResult(result);
  };

  const handleReset = () => {
    stopSpeech();
    setIsPlayingModel(false);
    setIsRecording(false);
    setRecordedTranscript('');
    setInterimTranscript('');
    setEvaluationResult(null);
    setActiveWordTip(null);
    setMicError(null);
  };

  const handleSubmit = () => {
    if (!evaluationResult) return;
    onSubmit({
      spokenTranscript: recordedTranscript || interimTranscript || 'Speech practice completed.',
      accuracyScore: evaluationResult.accuracyScore,
      overallScore: Math.round((evaluationResult.accuracyScore / 100) * points),
      words: evaluationResult.words,
      generalFeedback: evaluationResult.generalFeedback,
    });
  };

  // Quick word pronunciation click
  const handleWordClick = (word: string) => {
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
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
              isPlayingModel
                ? 'bg-rose-500/20 border-rose-500 text-rose-300 animate-pulse'
                : 'bg-indigo-600/20 border-indigo-500/40 text-indigo-300 hover:bg-indigo-600/30'
            }`}
          >
            {isPlayingModel ? <Square className="w-3.5 h-3.5" /> : <Volume2 className="w-3.5 h-3.5" />}
            <span>{isPlayingModel ? 'Stop Audio' : 'Listen to Model'}</span>
          </button>
        </div>
      </div>

      {/* Instructions Banner */}
      <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl p-3.5 text-xs text-slate-300 flex items-start gap-3">
        <Info className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-semibold text-slate-200">How to practice: </span>
          Click any individual word or icon to hear native audio and inspect IPA transcription. When ready, press{' '}
          <strong className="text-indigo-300">Start Speaking</strong> and read the passage clearly aloud into your microphone.
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
                    className="hover:text-indigo-300 hover:bg-indigo-500/10 px-1.5 py-0.5 rounded transition-colors text-slate-100 cursor-pointer"
                    title="Click to pronounce"
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
        <div className="bg-indigo-950/30 border border-indigo-500/40 rounded-xl p-4 animate-pulse">
          <div className="flex items-center gap-2 text-indigo-300 text-xs font-semibold mb-1">
            <div className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
            <span>Listening to your speech... Speak clearly now</span>
          </div>
          <div className="text-sm text-slate-200 italic font-medium min-h-[1.5rem]">
            {recordedTranscript} <span className="text-indigo-400">{interimTranscript}</span>
          </div>
        </div>
      )}

      {/* Pronunciation Diagnostics Summary Card */}
      {evaluationResult && (
        <div className="bg-slate-800/60 border border-slate-700 rounded-xl p-5 space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-700/70 pb-3">
            <div className="flex items-center gap-2">
              <Award className="w-5 h-5 text-indigo-400" />
              <h3 className="text-base font-bold text-white">Pronunciation Analysis</h3>
            </div>
            <div className="flex items-center gap-3">
              <div className="text-right">
                <div className="text-2xl font-black text-indigo-400">
                  {evaluationResult.accuracyScore}%
                </div>
                <div className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
                  Pronunciation Accuracy
                </div>
              </div>
            </div>
          </div>

          {/* Stats Triad */}
          <div className="grid grid-cols-3 gap-3 text-center">
            <div className="bg-emerald-500/10 border border-emerald-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-emerald-400">{evaluationResult.correctCount}</div>
              <div className="text-[11px] text-emerald-300/80">Accurate Words</div>
            </div>
            <div className="bg-rose-500/10 border border-rose-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-rose-400">{evaluationResult.mispronouncedCount}</div>
              <div className="text-[11px] text-rose-300/80">Mispronounced</div>
            </div>
            <div className="bg-amber-500/10 border border-amber-500/20 p-2.5 rounded-lg">
              <div className="text-lg font-bold text-amber-400">{evaluationResult.omittedCount}</div>
              <div className="text-[11px] text-amber-300/80">Omitted Words</div>
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
              disabled={!recognitionSupported || isSubmitting}
              onClick={toggleRecording}
              className={`flex-1 sm:flex-none flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-sm transition-all shadow-lg ${
                isRecording
                  ? 'bg-rose-600 hover:bg-rose-700 text-white shadow-rose-600/30 animate-pulse'
                  : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-600/30'
              }`}
            >
              {isRecording ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              <span>{isRecording ? 'Stop & Evaluate' : evaluationResult ? 'Read Again' : 'Start Speaking'}</span>
            </button>

            {/* Reset Button */}
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

          {/* Submit Evaluation Button */}
          {evaluationResult && (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="w-full sm:w-auto flex items-center justify-center gap-2 px-7 py-3 rounded-xl font-bold text-sm bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg shadow-emerald-600/20 transition-all disabled:opacity-50"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>{isSubmitting ? 'Submitting...' : 'Submit Speaking Assessment'}</span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};
