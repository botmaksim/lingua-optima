/**
 * @file SpeakingConfigPanel.tsx
 * @brief Configuration panel for speaking and pronunciation tasks.
 *
 * Enables educators and students to calibrate target phonemes, sound contrasts,
 * passage length presets, tongue twister modes, and point allocations.
 */

import React, { useState } from 'react';
import { Mic, Volume2, Sparkles, HelpCircle } from 'lucide-react';
import { speakWord } from '../../utils/phonetics';

/**
 * @brief Predefined target phoneme focus options for speaking exercises.
 */
export const TARGET_SOUND_PRESETS = [
  { label: 'Dental Fricatives (/θ/, /ð/)', value: '/θ/ and /ð/ (think, this, thought, breathe)' },
  { label: 'Liquids (/r/ vs /l/)', value: '/r/ vs /l/ (right, light, red, led, raw, law)' },
  { label: 'Labials (/v/ vs /w/)', value: '/v/ vs /w/ (very, wary, vine, wine, vest, west)' },
  { label: 'Vowel Length (/iː/ vs /ɪ/)', value: '/iː/ vs /ɪ/ (sheep, ship, seat, sit, beat, bit)' },
  { label: 'Sibilants & Affricates (/s/, /ʃ/, /tʃ/)', value: '/s/, /ʃ/, /tʃ/ (sea, she, chip, cheap, wash, watch)' },
  { label: 'Silent Letters & Irregular Words', value: 'Silent consonants (comfortable, island, subtle, receipt, doubt, listen)' },
  { label: 'General Fluency & Rhythm', value: 'Natural Connected Speech, Linking & Sentence Stress' },
] as const;

/**
 * @brief Length presets for speaking exercises.
 */
export const SPEAKING_LENGTH_PRESETS = [
  { label: 'Short Drill (30–50 words)', value: 'SHORT', desc: '2–3 sentences. Ideal for focused sound drills and quick testing.' },
  { label: 'Medium Passage (60–90 words)', value: 'MEDIUM', desc: '1 rich paragraph. Balanced fluency, rhythm, and natural discourse.' },
  { label: 'Long Speech (120–180 words)', value: 'LONG', desc: '2–3 paragraphs. Continuous discourse and complex prosody.' },
  { label: 'Custom', value: 'CUSTOM', desc: 'Custom passage word count and instructions.' },
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
}) => {
  const [testWord] = useState('thoughtful');

  const handleSoundPresetSelect = (presetVal: string) => {
    onTargetSoundsChange(presetVal);
  };

  const handleTestAudio = () => {
    speakWord(testWord);
  };

  return (
    <div className="bg-slate-900/60 border border-indigo-500/30 rounded-xl p-5 mb-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-700/60 pb-3">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-indigo-500/20 text-indigo-400 rounded-lg">
            <Mic className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-semibold text-white">
              {isStudent ? 'Speaking & Pronunciation Studio' : 'Speaking Practice Studio'}
            </h3>
            <p className="text-xs text-slate-400">
              {isStudent
                ? 'Generate an authentic speech passage to read aloud into your microphone'
                : 'Configure AI speech generation parameters, sound contrasts, and grading weights'}
            </p>
          </div>
        </div>

        {/* Audio Engine Tester */}
        <div className="flex items-center gap-2 bg-slate-800/80 border border-slate-700 rounded-lg px-2.5 py-1">
          <span className="text-[11px] text-slate-400">TTS Test:</span>
          <button
            type="button"
            onClick={handleTestAudio}
            className="flex items-center gap-1 text-xs text-indigo-300 hover:text-indigo-200 bg-indigo-900/40 px-2 py-0.5 rounded transition-colors"
            title="Click to test pronunciation audio playback"
          >
            <Volume2 className="w-3.5 h-3.5" />
            <span>"{testWord}"</span>
          </button>
        </div>
      </div>

      {/* Assignment Title */}
      <div>
        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-1.5">
          Assignment Title (Optional)
        </label>
        <input
          type="text"
          value={speakingTitle}
          onChange={(e) => onTitleChange(e.target.value)}
          placeholder="e.g. Master the Dental Fricatives: /θ/ and /ð/"
          className="w-full bg-slate-800/80 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      {/* Target Sounds & Phonemes */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="text-xs font-semibold uppercase tracking-wider text-slate-300 flex items-center gap-1.5">
            <span>Target Sounds & Phonetic Focus</span>
            <span title="Select specific sound contrasts or enter custom phonemes">
              <HelpCircle className="w-3.5 h-3.5 text-slate-400" />
            </span>
          </label>
          <span className="text-[11px] text-indigo-400">Click preset to populate</span>
        </div>

        {/* Presets Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 mb-3">
          {TARGET_SOUND_PRESETS.map((preset) => {
            const isSelected = targetSounds === preset.value;
            return (
              <button
                key={preset.label}
                type="button"
                onClick={() => handleSoundPresetSelect(preset.value)}
                className={`text-left p-2.5 rounded-lg border text-xs transition-all ${
                  isSelected
                    ? 'bg-indigo-600/30 border-indigo-500 text-white font-medium shadow-sm shadow-indigo-500/20'
                    : 'bg-slate-800/50 border-slate-700/70 text-slate-300 hover:bg-slate-800 hover:border-slate-600'
                }`}
              >
                <div className="font-semibold text-slate-200">{preset.label}</div>
                <div className="text-[11px] text-slate-400 truncate mt-0.5">{preset.value}</div>
              </button>
            );
          })}
        </div>

        {/* Custom Target Sounds Input */}
        <input
          type="text"
          value={targetSounds}
          onChange={(e) => onTargetSoundsChange(e.target.value)}
          placeholder="Or enter custom phonemes: e.g. /θ/, /ð/, rhythm and natural pauses"
          className="w-full bg-slate-800/80 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
        />
      </div>

      {/* Passage Length Presets */}
      <div>
        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
          Passage Length Preset
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5">
          {SPEAKING_LENGTH_PRESETS.map((preset) => {
            const isSelected = speakingLength === preset.value;
            return (
              <button
                key={preset.value}
                type="button"
                onClick={() => onSpeakingLengthChange(preset.value)}
                className={`text-left p-3 rounded-lg border transition-all ${
                  isSelected
                    ? 'bg-indigo-600/30 border-indigo-500 text-white shadow-sm shadow-indigo-500/20'
                    : 'bg-slate-800/50 border-slate-700/70 text-slate-300 hover:bg-slate-800 hover:border-slate-600'
                }`}
              >
                <div className="text-xs font-semibold text-slate-100 flex items-center justify-between">
                  <span>{preset.label}</span>
                  {isSelected && <Sparkles className="w-3.5 h-3.5 text-indigo-400" />}
                </div>
                <div className="text-[11px] text-slate-400 mt-1 leading-relaxed">{preset.desc}</div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Point Weight Allocation */}
      {!isStudent && (
        <div className="flex items-center justify-between border-t border-slate-700/60 pt-4">
          <div>
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-300 block">
              Total Points for Pronunciation
            </label>
            <p className="text-xs text-slate-400">Awarded based on word-level pronunciation accuracy and completeness</p>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="number"
              min={10}
              max={500}
              step={5}
              value={totalPoints}
              onChange={(e) => onTotalPointsChange(Math.max(1, parseInt(e.target.value) || 100))}
              className="w-24 bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-center text-sm font-bold text-indigo-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <span className="text-xs font-medium text-slate-400">pts</span>
          </div>
        </div>
      )}
    </div>
  );
};
