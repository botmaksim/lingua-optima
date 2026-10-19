/**
 * @file SpeakingConfigPanel.tsx
 * @brief Configuration panel for speaking and pronunciation tasks.
 *
 * Enables educators and students to calibrate target phonemes, sound contrasts,
 * passage length presets, tongue twister modes, custom word counts, and point allocations.
 */

import React, { useState } from 'react';
import { Mic, Volume2, Sparkles, HelpCircle, Edit3 } from 'lucide-react';
import { speakWord } from '../../utils/phonetics';

/**
 * @brief Predefined target phoneme focus options for speaking exercises.
 */
export const TARGET_SOUND_PRESETS = [
  { label: 'Dental Fricatives (/θ/, /ð/)', value: '/θ/ and /ð/ (think, this, thought, breathe)', title: 'Th-Sounds (/θ/ and /ð/)', example: 'think, this, thought, breathe' },
  { label: 'Liquids (/r/ vs /l/)', value: '/r/ vs /l/ (right, light, red, led, raw, law)', title: 'R & L Contrast (/r/ vs /l/)', example: 'right, light, red, led, raw, law' },
  { label: 'Labials (/v/ vs /w/)', value: '/v/ vs /w/ (very, wary, vine, wine, vest, west)', title: 'V & W Contrast (/v/ vs /w/)', example: 'very, wary, vine, wine, vest, west' },
  { label: 'Vowel Length (/iː/ vs /ɪ/)', value: '/iː/ vs /ɪ/ (sheep, ship, seat, sit, beat, bit)', title: 'Long vs Short Vowels (/iː/ vs /ɪ/)', example: 'sheep vs ship, seat vs sit' },
  { label: 'Sibilants & Affricates (/s/, /ʃ/, /tʃ/)', value: '/s/, /ʃ/, /tʃ/ (sea, she, chip, cheap, wash, watch)', title: 'S, Sh & Ch (/s/, /ʃ/, /tʃ/)', example: 'sea, she, chip, cheap, watch' },
  { label: 'Silent Letters & Irregular Words', value: 'Silent consonants (comfortable, island, subtle, receipt, doubt, listen)', title: 'Silent Letters & Reductions', example: 'comfortable, island, subtle, receipt' },
  { label: 'General Fluency & Rhythm', value: 'Natural Connected Speech, Linking & Sentence Stress', title: 'Connected Speech & Natural Rhythm', example: 'linking sounds, weak forms, sentence stress' },
] as const;

/**
 * @brief Length presets for speaking exercises.
 */
export const SPEAKING_LENGTH_PRESETS = [
  { label: 'Short Drill (30–50 words)', value: 'SHORT', words: '30–50 words', desc: '2–3 focused sentences. Ideal for sound drills and quick testing.' },
  { label: 'Medium Passage (60–90 words)', value: 'MEDIUM', words: '60–90 words', desc: '1 rich paragraph. Balanced fluency, rhythm, and natural discourse.' },
  { label: 'Long Speech (120–180 words)', value: 'LONG', words: '120–180 words', desc: '2–3 paragraphs. Continuous discourse and complex prosody.' },
  { label: 'Custom', value: 'CUSTOM', words: 'Custom length', desc: 'Specify custom word count, scenario, or exact sentences to drill.' },
] as const;

interface SpeakingConfigPanelProps {
  speakingTitle: string;
  onTitleChange: (title: string) => void;
  targetSounds: string;
  onTargetSoundsChange: (sounds: string) => void;
  speakingLength: string;
  onSpeakingLengthChange: (length: string) => void;
  totalPoints: number;
  onTotalPointsChange: (points: number) => void;
  isStudent?: boolean;
  customWordCount?: number;
  onCustomWordCountChange?: (count: number) => void;
  customInstructions?: string;
  onCustomInstructionsChange?: (instructions: string) => void;
}

export const SpeakingConfigPanel: React.FC<SpeakingConfigPanelProps> = ({
  speakingTitle,
  onTitleChange,
  targetSounds,
  onTargetSoundsChange,
  speakingLength,
  onSpeakingLengthChange,
  totalPoints,
  onTotalPointsChange,
  isStudent = false,
  customWordCount,
  onCustomWordCountChange,
  customInstructions,
  onCustomInstructionsChange,
}) => {
  const [testWord] = useState('thoughtful');
  const [internalWordCount, setInternalWordCount] = useState<number>(50);
  const [internalInstructions, setInternalInstructions] = useState<string>('');

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

  const handleSoundPresetSelect = (presetVal: string) => {
    onTargetSoundsChange(presetVal);
  };

  const handleTestAudio = () => {
    speakWord(testWord);
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
                {isStudent ? 'Speaking & Pronunciation Studio' : 'Speaking Practice Studio'}
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                Acoustic WPM & Fluency
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Generate an authentic speech passage to read aloud into your microphone'
                : 'Configure AI speech generation parameters, sound contrasts, and grading weights'}
            </p>
          </div>
        </div>

        {/* Audio Engine Tester */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-2xl bg-white border border-indigo-100 shadow-xs self-start sm:self-auto">
          <span className="text-[11px] font-semibold text-slate-400">TTS Test:</span>
          <button
            type="button"
            onClick={handleTestAudio}
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
          Assignment Title (Optional)
        </label>
        <input
          type="text"
          value={speakingTitle}
          onChange={(e) => onTitleChange(e.target.value)}
          placeholder="e.g. Master the Dental Fricatives: /θ/ and /ð/"
          className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition bg-white"
        />
        <p className="text-[11px] text-slate-400 mt-1">
          Leave blank to automatically derive from the selected phonetic focus.
        </p>
      </div>

      {/* Target Sounds & Phonemes */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center space-x-1.5">
            <span>Target Sounds & Phonetic Focus</span>
            <span title="Select specific sound contrasts or enter custom phonemes">
              <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
            </span>
          </label>
          <span className="text-[11px] font-semibold text-primary">Click preset to populate</span>
        </div>

        {/* Presets Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
          {TARGET_SOUND_PRESETS.map((preset) => {
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
          })}
        </div>

        {/* Selected Sound Focus / Custom Input */}
        <div className="pt-1 space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-slate-600 flex items-center space-x-1">
              <Edit3 className="w-3 h-3 text-primary" />
              <span>Selected Sound Focus & Drill Words (Freely Editable):</span>
            </span>
          </div>
          <input
            type="text"
            value={targetSounds}
            onChange={(e) => onTargetSoundsChange(e.target.value)}
            placeholder="e.g. /θ/, /ð/, rhythm, sentence stress, or custom tongue-twister words"
            className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-mono text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
          />
        </div>
      </div>

      {/* Passage Length Presets */}
      <div className="space-y-3">
        <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
          Passage Length & Drill Scope
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
                <span>Custom Passage Length & Specific Instructions</span>
              </label>
              <span className="text-[11px] text-slate-500 font-medium">15 to 300 words</span>
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
