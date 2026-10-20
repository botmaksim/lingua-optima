/**
 * @file SpeakingConfigPanel.tsx
 * @brief Configuration panel for speaking and pronunciation tasks.
 *
 * Enables educators and students to calibrate target phonemes, pre-calibrated JSON word packs,
 * minimal pair contrasts, passage length presets, custom word counts, and point allocations.
 */

import React, { useState, useEffect } from 'react';
import { Mic, Volume2, Sparkles, HelpCircle, Edit3, Leaf, Dices } from 'lucide-react';
import { CefrLevel, PronunciationPack } from '../../types/task';
import { taskApi } from '../../api/taskApi';
import { speakWord } from '../../utils/phonetics';

/**
 * @brief Predefined target phoneme fallback presets if offline or loading.
 */
export const TARGET_SOUND_PRESETS = [
  { label: 'Dental Fricatives: /θ/, /ð/', value: '/θ/ and /ð/ (think, this, thought, breathe)', title: 'Th-Sounds (/θ/ and /ð/)', example: 'think, this, thought, breathe' },
  { label: 'Liquids: /r/ vs /l/', value: '/r/ vs /l/ (right, light, red, led, raw, law)', title: 'R & L Contrast (/r/ vs /l/)', example: 'right, light, red, led, raw, law' },
  { label: 'Labials: /v/ vs /w/', value: '/v/ vs /w/ (very, wary, vine, wine, vest, west)', title: 'V & W Contrast (/v/ vs /w/)', example: 'very, wary, vine, wine, vest, west' },
  { label: 'Vowel Length: /iː/ vs /ɪ/', value: '/iː/ vs /ɪ/ (sheep, ship, seat, sit, beat, bit)', title: 'Long vs Short Vowels (/iː/ vs /ɪ/)', example: 'sheep vs ship, seat vs sit' },
  { label: 'Sibilants & Affricates: /s/, /ʃ/, /tʃ/', value: '/s/, /ʃ/, /tʃ/ (sea, she, chip, cheap, wash, watch)', title: 'S, Sh & Ch (/s/, /ʃ/, /tʃ/)', example: 'sea, she, chip, cheap, watch' },
  { label: 'Silent Letters & Irregular Words', value: 'Silent consonants (comfortable, island, subtle, receipt, doubt, listen)', title: 'Silent Letters & Reductions', example: 'comfortable, island, subtle, receipt' },
  { label: 'General Fluency & Rhythm', value: 'Natural Connected Speech, Linking & Sentence Stress', title: 'Connected Speech & Natural Rhythm', example: 'linking sounds, weak forms, sentence stress' },
] as const;

/**
 * @brief Length presets for speaking exercises.
 */
export const SPEAKING_LENGTH_PRESETS = [
  { label: 'Short Drill', value: 'SHORT', words: '30–50 words', desc: '2–3 focused sentences for sound drills.' },
  { label: 'Medium Passage', value: 'MEDIUM', words: '60–90 words', desc: '1 rich paragraph for fluency and natural rhythm.' },
  { label: 'Long Speech', value: 'LONG', words: '120–180 words', desc: '2–3 paragraphs for sustained discourse.' },
  { label: 'Custom', value: 'CUSTOM', words: 'Custom length', desc: 'Specify custom word count and topic guidelines.' },
] as const;

interface SpeakingConfigPanelProps {
  cefrLevel?: CefrLevel;
  speakingTitle: string;
  onTitleChange: (title: string) => void;
  targetSounds: string;
  onTargetSoundsChange: (sounds: string) => void;
  pronunciationPackId?: string | null;
  onPronunciationPackIdChange?: (packId: string | null) => void;
  speakingLength: string;
  onSpeakingLengthChange: (length: string) => void;
  totalPoints: number;
  onTotalPointsChange: (points: number) => void;
  ecoMode?: boolean;
  onEcoModeChange?: (eco: boolean) => void;
  isStudent?: boolean;
  customWordCount?: number;
  onCustomWordCountChange?: (count: number) => void;
  customInstructions?: string;
  onCustomInstructionsChange?: (instructions: string) => void;
}

export const SpeakingConfigPanel: React.FC<SpeakingConfigPanelProps> = ({
  cefrLevel = 'B1',
  speakingTitle,
  onTitleChange,
  targetSounds,
  onTargetSoundsChange,
  pronunciationPackId,
  onPronunciationPackIdChange,
  speakingLength,
  onSpeakingLengthChange,
  totalPoints,
  onTotalPointsChange,
  ecoMode = false,
  onEcoModeChange,
  isStudent = false,
  customWordCount,
  onCustomWordCountChange,
  customInstructions,
  onCustomInstructionsChange,
}) => {
  const [testWord] = useState('thoughtful');
  const [internalWordCount, setInternalWordCount] = useState<number>(50);
  const [internalInstructions, setInternalInstructions] = useState<string>('');
  const [packs, setPacks] = useState<PronunciationPack[]>([]);
  const [selectedPack, setSelectedPack] = useState<PronunciationPack | null>(null);

  const activeWordCount = customWordCount ?? internalWordCount;
  const updateWordCount = (val: number) => {
    setInternalWordCount(val);
    onCustomWordCountChange?.(val);
  };

  const activeInstructions = customInstructions ?? internalInstructions;
  const updateInstructions = (val: string) => {
    setInternalInstructions(val);
    onCustomInstructionsChange?.(val);
  };

  // Fetch JSON pronunciation packs for the current CEFR level
  useEffect(() => {
    let isMounted = true;
    const fetchPacks = async () => {
      try {
        const loaded = await taskApi.getPronunciationPacks(cefrLevel);
        if (isMounted && loaded && loaded.length > 0) {
          setPacks(loaded);
          if (pronunciationPackId) {
            const found = loaded.find((p) => p.id === pronunciationPackId);
            if (found) setSelectedPack(found);
          }
        }
      } catch (err) {
        console.warn('Failed to load pronunciation packs from API, fallback to presets:', err);
      }
    };
    fetchPacks();
    return () => {
      isMounted = false;
    };
  }, [cefrLevel, pronunciationPackId]);

  const handlePackSelect = (pack: PronunciationPack) => {
    setSelectedPack(pack);
    onPronunciationPackIdChange?.(pack.id);
    const soundDesc = pack.phonemes && pack.phonemes.length > 0
      ? `${pack.phonemes.join(' and ')} · ${pack.title}`
      : pack.title;
    onTargetSoundsChange(soundDesc);
  };

  const handleSoundPresetSelect = (presetVal: string) => {
    setSelectedPack(null);
    onPronunciationPackIdChange?.('');
    onTargetSoundsChange(presetVal);
  };

  const handleRandomPack = () => {
    if (packs.length > 0) {
      const randomIdx = Math.floor(Math.random() * packs.length);
      handlePackSelect(packs[randomIdx]);
    } else {
      const randomIdx = Math.floor(Math.random() * TARGET_SOUND_PRESETS.length);
      handleSoundPresetSelect(TARGET_SOUND_PRESETS[randomIdx].value);
    }
  };

  const handleTestAudio = (word: string) => {
    speakWord(word);
  };

  return (
    <div className="rounded-3xl border border-indigo-200/80 bg-gradient-to-br from-indigo-50/40 via-white to-violet-50/30 p-6 space-y-6 shadow-xs">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-indigo-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-600 text-white flex items-center justify-center shadow-md shadow-indigo-200 flex-shrink-0">
            <Mic className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Speaking Practice
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                CEFR {cefrLevel}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Read the passage aloud into your microphone to practice pronunciation, phonemes, and pace'
                : 'Configure target phonemes, drill word packs, passage length, and grading'}
            </p>
          </div>
        </div>

        {/* Audio Engine Tester */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-2xl bg-white border border-indigo-100 shadow-xs self-start sm:self-auto">
          <span className="text-[11px] font-semibold text-slate-400">TTS Test:</span>
          <button
            type="button"
            onClick={() => handleTestAudio(testWord)}
            className="flex items-center space-x-1.5 text-xs font-bold text-primary hover:text-primary-hover bg-indigo-50/80 hover:bg-indigo-100/80 px-2.5 py-1 rounded-xl transition"
            title="Click to test pronunciation audio playback"
          >
            <Volume2 className="w-3.5 h-3.5" />
            <span>"{testWord}"</span>
          </button>
        </div>
      </div>

      {/* Assignment Title */}
      <div>
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
          Assignment Title
        </label>
        <input
          type="text"
          value={speakingTitle}
          onChange={(e) => onTitleChange(e.target.value)}
          placeholder="e.g. Master the Dental Fricatives: /θ/ and /ð/"
          className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
        />
        <p className="text-[11px] text-slate-400 mt-1">
          Leave blank to use the target sound focus as the title.
        </p>
      </div>

      {/* Target Sounds & Phoneme Packs */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
            <span>Phonetics & Word Packs ({cefrLevel})</span>
            <span title="Select pre-calibrated drill words and minimal pair contrasts from JSON">
              <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
            </span>
          </label>
          <button
            type="button"
            onClick={handleRandomPack}
            className="flex items-center space-x-1 text-[11px] font-bold text-primary hover:text-primary-hover bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-xl transition"
          >
            <Dices className="w-3.5 h-3.5" />
            <span>Surprise Pack</span>
          </button>
        </div>

        {/* Dynamic JSON Packs or Fallback Presets Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {packs.length > 0 ? (
            packs.map((pack) => {
              const isSelected = pronunciationPackId === pack.id || targetSounds.includes(pack.title);
              return (
                <button
                  key={pack.id}
                  type="button"
                  onClick={() => handlePackSelect(pack)}
                  className={`text-left p-3 rounded-2xl border transition flex flex-col justify-between ${
                    isSelected
                      ? 'border-primary bg-indigo-50/80 text-primary ring-2 ring-primary/20 shadow-xs'
                      : 'border-slate-200/90 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50/60'
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between gap-1">
                      <div className="text-xs font-bold leading-tight line-clamp-1">{pack.title}</div>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded-md bg-indigo-100/70 text-indigo-700 flex-shrink-0 font-semibold">
                        {pack.phonemes?.join(' ') || pack.category}
                      </span>
                    </div>
                    {pack.drillWords && pack.drillWords.length > 0 && (
                      <div className="text-[11px] text-slate-400 mt-1 font-mono line-clamp-1">
                        {pack.drillWords.slice(0, 4).join(', ')}...
                      </div>
                    )}
                  </div>
                </button>
              );
            })
          ) : (
            TARGET_SOUND_PRESETS.map((preset) => {
              const isSelected = targetSounds === preset.value;
              return (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => handleSoundPresetSelect(preset.value)}
                  className={`text-left p-3 rounded-2xl border transition flex flex-col justify-between ${
                    isSelected
                      ? 'border-primary bg-indigo-50/80 text-primary ring-2 ring-primary/20 shadow-xs'
                      : 'border-slate-200/90 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50/60'
                  }`}
                >
                  <div>
                    <div className="text-xs font-bold leading-tight">{preset.label}</div>
                    <div className="text-[11px] text-slate-400 mt-1 font-mono line-clamp-1">{preset.example}</div>
                  </div>
                </button>
              );
            })
          )}
        </div>

        {/* Selected Pack Interactive Preview */}
        {selectedPack && !ecoMode && (
          <div className="p-4 rounded-2xl bg-white border border-indigo-100 shadow-xs space-y-3 animate-in fade-in duration-200">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-indigo-50 pb-2">
              <div className="flex items-center space-x-2">
                <Sparkles className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold text-slate-800">
                  Target Words to Weave into Speech:
                </span>
              </div>
              <span className="text-[10px] text-slate-400">Click word to hear pronunciation</span>
            </div>

            {/* Drill Words Pills */}
            {selectedPack.drillWords && selectedPack.drillWords.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {selectedPack.drillWords.map((word) => (
                  <button
                    key={word}
                    type="button"
                    onClick={() => handleTestAudio(word)}
                    className="group px-2.5 py-1 rounded-xl bg-slate-50 hover:bg-indigo-50 border border-slate-200 hover:border-indigo-200 text-xs font-medium text-slate-700 hover:text-primary transition flex items-center space-x-1"
                    title={`Click to pronounce "${word}"`}
                  >
                    <span>{word}</span>
                    <Volume2 className="w-3 h-3 text-slate-400 group-hover:text-primary" />
                  </button>
                ))}
              </div>
            )}

            {/* Minimal Pairs Contrast preview */}
            {selectedPack.minimalPairs && selectedPack.minimalPairs.length > 0 && (
              <div className="pt-1 text-xs text-slate-600 flex flex-wrap items-center gap-2">
                <span className="font-semibold text-slate-500 text-[11px]">Contrast Pairs:</span>
                {selectedPack.minimalPairs.map((pair, idx) => (
                  <span key={idx} className="px-2 py-0.5 rounded-lg bg-indigo-50/60 text-slate-800 font-mono text-[11px]">
                    <strong>{pair.word1}</strong> ({pair.phoneme1}) vs <strong>{pair.word2}</strong> ({pair.phoneme2})
                  </span>
                ))}
              </div>
            )}

            {selectedPack.contrastFocus && (
              <p className="text-[11px] text-slate-500 italic">
                {selectedPack.contrastFocus}
              </p>
            )}
          </div>
        )}

        {/* Selected Sound Focus / Custom Input */}
        <div className="pt-1 space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-slate-600 flex items-center space-x-1">
              <Edit3 className="w-3 h-3 text-primary" />
              <span>Target Sounds, Phonemes & Custom Practice Words:</span>
            </span>
            <span className="text-[10px] text-slate-400">Expandable editor</span>
          </div>
          <textarea
            rows={2}
            value={targetSounds}
            onChange={(e) => onTargetSoundsChange(e.target.value)}
            placeholder="e.g. /θ/, /ð/, rhythm, sentence stress, or custom tongue-twister words (e.g. thought, through, thirty-three, weather)"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-mono text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition resize-y min-h-[64px] leading-relaxed"
          />
          <p className="text-[11px] text-slate-400">
            Selected phoneme pack words are populated here. You can expand this box and freely type additional contrast words or pronunciation goals.
          </p>
        </div>
      </div>

      {/* Eco Mode Toggle */}
      {onEcoModeChange && (
        <div className={`p-4 rounded-2xl border transition ${
          ecoMode
            ? 'bg-emerald-50/70 border-emerald-200'
            : 'bg-white border-slate-200/90'
        } flex items-center justify-between`}>
          <div className="flex items-center space-x-3">
            <div className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 transition ${
              ecoMode ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'
            }`}>
              <Leaf className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Eco Mode
                </h4>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                  ecoMode ? 'bg-emerald-200 text-emerald-900' : 'bg-slate-100 text-slate-600'
                }`}>
                  {ecoMode ? 'ACTIVE' : 'OFF'}
                </span>
              </div>
              <p className="text-[11px] text-slate-500 mt-0.5">
                {ecoMode
                  ? 'Skip heavy phonetic drill word lists to generate a lean passage and save tokens.'
                  : 'Inject level-calibrated drill words and minimal pair contrasts into the passage.'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => onEcoModeChange(!ecoMode)}
            className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
              ecoMode ? 'bg-emerald-600' : 'bg-slate-300'
            }`}
          >
            <span
              className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                ecoMode ? 'translate-x-5' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      )}

      {/* Passage Length Presets */}
      <div className="space-y-3">
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
          Passage Length
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
          {SPEAKING_LENGTH_PRESETS.map((preset) => {
            const isSelected = speakingLength === preset.value;
            return (
              <button
                key={preset.value}
                type="button"
                onClick={() => onSpeakingLengthChange(preset.value)}
                className={`text-left p-3.5 rounded-2xl border transition flex flex-col justify-between ${
                  isSelected
                    ? 'border-primary bg-indigo-50/80 text-primary ring-2 ring-primary/20 shadow-xs'
                    : 'border-slate-200/90 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50/60'
                }`}
              >
                <div>
                  <div className="text-xs font-bold flex items-center justify-between">
                    <span>{preset.label}</span>
                    {isSelected && <Sparkles className="w-3.5 h-3.5 text-primary" />}
                  </div>
                  <div className="text-[11px] text-slate-400 mt-1 leading-relaxed">{preset.desc}</div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Custom Passage Length & Instructions Section */}
        {speakingLength === 'CUSTOM' && (
          <div className="p-4 rounded-2xl border border-indigo-200/90 bg-indigo-50/40 space-y-3 animate-in fade-in duration-200">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-800 uppercase tracking-wider flex items-center space-x-1.5">
                <Sparkles className="w-3.5 h-3.5 text-primary" />
                <span>Custom Length & Instructions</span>
              </label>
              <span className="text-[11px] text-slate-500 font-medium">15–300 words</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Target Word Count:
                </label>
                <input
                  type="number"
                  min={15}
                  max={300}
                  value={activeWordCount}
                  onChange={(e) => updateWordCount(Math.max(15, Math.min(300, parseInt(e.target.value) || 50)))}
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white font-bold text-slate-800 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-center"
                />
              </div>

              <div className="sm:col-span-2">
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Custom Speaking Topic / Monologue Scenario:
                </label>
                <input
                  type="text"
                  value={activeInstructions}
                  onChange={(e) => updateInstructions(e.target.value)}
                  placeholder="e.g. A job interview opening or short tech pitch emphasizing sentence stress..."
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 bg-white text-xs text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Point Weight Allocation */}
      {!isStudent && (
        <div className="flex items-center justify-between border-t border-indigo-100 pt-4">
          <div>
            <label className="text-xs font-bold text-slate-700 uppercase tracking-wider block">
              Total Points for Pronunciation
            </label>
            <p className="text-xs text-slate-500">
              Awarded based on word-level pronunciation accuracy, speech rate (WPM), and completeness
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <input
              type="number"
              min={10}
              max={500}
              step={5}
              value={totalPoints}
              onChange={(e) => onTotalPointsChange(Math.max(1, parseInt(e.target.value) || 100))}
              className="w-24 bg-white border border-slate-200 rounded-xl px-3 py-1.5 text-center text-sm font-bold text-primary focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
            />
            <span className="text-xs font-semibold text-slate-400">pts</span>
          </div>
        </div>
      )}
    </div>
  );
};
