/**
 * @file GrammarConfigPanel.tsx
 * @brief Unified configuration module for grammar exercises (MCQ, Gap Fill, Open Brackets, Sentence Rewrite).
 *
 * Encapsulates topic selection, domain context, difficulty tier, question counts,
 * Eco Mode optimization, and pedagogical curriculum rules/vocabulary references without UI duplication.
 */

import React, { useState, useRef } from 'react';
import {
  Sliders,
  BookOpen,
  Leaf,
  ChevronDown,
  ChevronUp,
  UploadCloud,
  FileText,
  Trash2,
  CheckCircle2,
  Check,
  Bookmark,
} from 'lucide-react';
import { CefrLevel, DifficultyLevel, TopicsCatalogResponse, TaskType } from '../../types/task';
import { CustomCurriculumEntry } from '../../types/curriculum';
import { TopicSelector } from '../common/TopicSelector';
import { taskApi } from '../../api/taskApi';

export interface UploadedCurriculumFile {
  fileName: string;
  fileSize: number;
  serverPath: string;
}

interface GrammarConfigPanelProps {
  taskType?: TaskType;
  onTaskTypeChange?: (type: TaskType) => void;
  cefrLevel: CefrLevel;
  grammarTopic: string;
  onGrammarTopicChange: (topic: string) => void;
  domain: string;
  onDomainChange: (domain: string) => void;
  difficulty: DifficultyLevel;
  onDifficultyChange: (diff: DifficultyLevel) => void;
  numberOfQuestions: number;
  onNumberOfQuestionsChange: (count: number) => void;
  totalPoints?: number;
  onTotalPointsChange?: (points: number) => void;
  ecoMode: boolean;
  onEcoModeChange: (eco: boolean) => void;
  customRule: string;
  onCustomRuleChange: (rule: string) => void;
  customVocabulary: string;
  onCustomVocabularyChange: (vocab: string) => void;
  uploadedRuleFile: UploadedCurriculumFile | null;
  onUploadedRuleFileChange: (file: UploadedCurriculumFile | null) => void;
  uploadedVocabFile: UploadedCurriculumFile | null;
  onUploadedVocabFileChange: (file: UploadedCurriculumFile | null) => void;
  catalog: TopicsCatalogResponse | null;
  isStudent?: boolean;
  savedRules?: CustomCurriculumEntry[];
  savedVocabSets?: CustomCurriculumEntry[];
  onSaveRuleToLibrary?: () => void;
  onSaveVocabToLibrary?: () => void;
  isSavingRule?: boolean;
  isSavingVocab?: boolean;
}

export const GrammarConfigPanel: React.FC<GrammarConfigPanelProps> = ({
  taskType = 'MCQ',
  onTaskTypeChange,
  cefrLevel,
  grammarTopic,
  onGrammarTopicChange,
  domain,
  onDomainChange,
  difficulty,
  onDifficultyChange,
  numberOfQuestions,
  onNumberOfQuestionsChange,
  totalPoints,
  onTotalPointsChange,
  ecoMode,
  onEcoModeChange,
  customRule,
  onCustomRuleChange,
  customVocabulary,
  onCustomVocabularyChange,
  uploadedRuleFile,
  onUploadedRuleFileChange,
  uploadedVocabFile,
  onUploadedVocabFileChange,
  catalog,
  isStudent = false,
  savedRules = [],
  savedVocabSets = [],
  onSaveRuleToLibrary,
  onSaveVocabToLibrary,
  isSavingRule = false,
  isSavingVocab = false,
}) => {
  const [showCurriculumPanel, setShowCurriculumPanel] = useState<boolean>(false);
  const [isLoadingRef, setIsLoadingRef] = useState<boolean>(false);
  const [refNotice, setRefNotice] = useState<string | null>(null);
  const [isUploadingRule, setIsUploadingRule] = useState<boolean>(false);
  const [isUploadingVocab, setIsUploadingVocab] = useState<boolean>(false);

  const ruleFileInputRef = useRef<HTMLInputElement>(null);
  const vocabFileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, type: 'RULE' | 'VOCABULARY') => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (type === 'RULE') setIsUploadingRule(true);
    else setIsUploadingVocab(true);

    try {
      const res = await taskApi.uploadCurriculumFile(file, type, grammarTopic);
      if (type === 'RULE') {
        onUploadedRuleFileChange({
          fileName: res.fileName,
          fileSize: res.fileSize,
          serverPath: res.serverPath,
        });
        if (res.contentSnippet && !customRule.trim()) {
          onCustomRuleChange(res.contentSnippet);
        }
      } else {
        onUploadedVocabFileChange({
          fileName: res.fileName,
          fileSize: res.fileSize,
          serverPath: res.serverPath,
        });
        if (res.contentSnippet && !customVocabulary.trim()) {
          onCustomVocabularyChange(res.contentSnippet);
        }
      }
    } catch (err) {
      console.error('File upload failed:', err);
    } finally {
      if (type === 'RULE') setIsUploadingRule(false);
      else setIsUploadingVocab(false);
      e.target.value = '';
    }
  };

  const handleLoadReference = async () => {
    setIsLoadingRef(true);
    setRefNotice(null);
    try {
      const ref = await taskApi.getCurriculumReference(cefrLevel, grammarTopic || 'General');
      if (ref) {
        if (ref.referenceRule) {
          onCustomRuleChange(ref.referenceRule);
        }
        if (ref.referenceVocabulary && ref.referenceVocabulary.length > 0) {
          onCustomVocabularyChange(ref.referenceVocabulary.join(', '));
        }
        setRefNotice(`Loaded standard curriculum reference for ${cefrLevel} · ${ref.grammarTopic}`);
        setTimeout(() => setRefNotice(null), 4000);
      }
    } catch (err) {
      console.error('Failed to load curriculum reference:', err);
    } finally {
      setIsLoadingRef(false);
    }
  };

  return (
    <div className="rounded-3xl border border-slate-200/90 bg-white p-6 space-y-6 shadow-xs">
      {/* Module Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-2xl bg-primary text-white flex items-center justify-center shadow-xs flex-shrink-0">
            <Sliders className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-black text-slate-900 tracking-tight">
                Grammar Practice
              </h3>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                CEFR {cefrLevel}
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isStudent
                ? 'Select syllabus topics, difficulty, and optional rule guidelines'
                : 'Configure grammar topics, question difficulty, and reference materials'}
            </p>
          </div>
        </div>
      </div>

      {/* Exercise Sub-type Selection */}
      {onTaskTypeChange && (
        <div className="space-y-2">
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
            Exercise Format
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {[
              { type: 'MCQ', label: 'Multiple Choice', desc: '4 choices quiz' },
              { type: 'GAP_FILL', label: 'Fill in Blanks', desc: 'Missing cloze items' },
              { type: 'OPEN_BRACKETS', label: 'Open Brackets', desc: 'Conjugate / transform' },
              { type: 'REWRITE', label: 'Sentence Rewrite', desc: 'Sentence rephrasing' },
            ].map((fmt) => {
              const isSelected = taskType === fmt.type;
              return (
                <button
                  type="button"
                  key={fmt.type}
                  onClick={() => onTaskTypeChange(fmt.type as TaskType)}
                  className={`p-3 rounded-2xl border text-left transition flex flex-col justify-between ${
                    isSelected
                      ? 'border-indigo-600 bg-indigo-50/90 text-indigo-950 ring-2 ring-indigo-200 shadow-xs'
                      : 'border-slate-200 bg-white hover:border-slate-300 text-slate-700'
                  }`}
                >
                  <span className={`text-xs font-bold ${isSelected ? 'text-indigo-950 font-black' : 'text-slate-800'}`}>
                    {fmt.label}
                  </span>
                  <span className="text-[10px] text-slate-400 mt-1 line-clamp-1">
                    {fmt.desc}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* Core Syllabus: Topic & Vocabulary Domain Selection */}
      <TopicSelector
        cefrLevel={cefrLevel}
        topic={grammarTopic}
        onTopicChange={onGrammarTopicChange}
        domain={domain}
        onDomainChange={onDomainChange}
        catalog={catalog}
        showTopic={true}
        showDomain={true}
        size="lg"
      />

      {/* Difficulty & Number of Questions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
            Difficulty Tier
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['EASY', 'MEDIUM', 'HARD'] as DifficultyLevel[]).map((level) => {
              const isSelected = difficulty === level;
              return (
                <button
                  type="button"
                  key={level}
                  onClick={() => onDifficultyChange(level)}
                  className={`py-2 px-2 rounded-xl border text-xs font-bold text-center transition truncate ${
                    isSelected
                      ? 'border-primary bg-indigo-50/80 text-primary ring-2 ring-primary/20 shadow-xs'
                      : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                  }`}
                >
                  {level.charAt(0) + level.slice(1).toLowerCase()}
                </button>
              );
            })}
          </div>
        </div>

        <div className={`grid ${!isStudent && totalPoints !== undefined && onTotalPointsChange ? 'grid-cols-2' : 'grid-cols-1'} gap-3`}>
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              Number of Questions
            </label>
            <div className="flex items-center space-x-2">
              <input
                type="number"
                min={1}
                max={25}
                value={numberOfQuestions}
                onChange={(e) => onNumberOfQuestionsChange(Math.max(1, Math.min(25, parseInt(e.target.value) || 5)))}
                className="w-full px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-center"
              />
              <span className="text-xs font-semibold text-slate-400">items</span>
            </div>
          </div>

          {!isStudent && totalPoints !== undefined && onTotalPointsChange && (
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                Total Points
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="number"
                  min={5}
                  max={500}
                  step={5}
                  value={totalPoints}
                  onChange={(e) => onTotalPointsChange(Math.max(1, parseInt(e.target.value) || 100))}
                  className="w-full px-3.5 py-2 rounded-xl border border-slate-200 bg-white text-xs font-bold text-primary focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-center"
                />
                <span className="text-xs font-semibold text-slate-400">pts</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Eco Mode Toggle */}
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
                ? 'Skip curriculum notes for faster generation and lower token usage.'
                : 'Include curriculum rules and target vocabulary in generation.'}
            </p>
          </div>
        </div>
        <button
          type="button"
          aria-label="Toggle Eco Mode"
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

      {/* Curriculum Rules & Target Vocabulary Collapsible Panel */}
      <div className={`rounded-2xl border transition overflow-hidden ${
        ecoMode ? 'border-slate-200 bg-slate-50/30 opacity-75' : 'border-slate-200/90 bg-white'
      }`}>
        <button
          type="button"
          onClick={() => setShowCurriculumPanel(!showCurriculumPanel)}
          className="w-full p-4 flex items-center justify-between text-left hover:bg-slate-50/60 transition"
        >
          <div className="flex items-center space-x-2.5">
            <div className="w-7 h-7 rounded-lg bg-indigo-100 text-primary flex items-center justify-center">
              <BookOpen className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                Target Rules & Vocabulary Reference
              </h3>
              <p className="text-[11px] text-slate-500">
                {ecoMode ? (
                  <span className="flex items-center gap-1 text-emerald-600 font-semibold">
                    <Leaf className="w-3.5 h-3.5" />
                    <span>Eco Mode active: reference materials will be skipped during generation</span>
                  </span>
                ) : uploadedRuleFile || uploadedVocabFile || customRule || customVocabulary ? (
                  <span className="flex items-center gap-1 text-indigo-600 font-semibold">
                    <Check className="w-3.5 h-3.5" />
                    <span>Custom rules or vocabulary active</span>
                  </span>
                ) : (
                  <span>Attach lesson rules, target vocabulary, or upload reference files</span>
                )}
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2 text-slate-400">
            {showCurriculumPanel ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </div>
        </button>

        {showCurriculumPanel && (
          <div className="p-4 sm:p-5 border-t border-slate-100 bg-slate-50/30 space-y-4 animate-in fade-in duration-200">
            <div className="flex flex-wrap items-center justify-between gap-2 pb-2 border-b border-slate-200/60">
              <span className="text-xs text-slate-600 font-medium">
                Enforce exact target rules and vocabulary in the assignment:
              </span>
              <button
                type="button"
                onClick={handleLoadReference}
                disabled={isLoadingRef}
                className="px-3 py-1.5 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-primary text-xs font-bold transition flex items-center space-x-1.5 disabled:opacity-50"
              >
                <BookOpen className="w-3.5 h-3.5" />
                <span>{isLoadingRef ? 'Loading...' : 'Auto-Fill Standard Reference'}</span>
              </button>
            </div>

            {refNotice && (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                <span>{refNotice}</span>
              </div>
            )}

            {/* Target Grammar Rule Section */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-700">
                  Target Grammar Rule / Formula
                </label>
                <div className="flex items-center space-x-2">
                  <input
                    ref={ruleFileInputRef}
                    type="file"
                    accept=".txt,.md,.json"
                    className="hidden"
                    onChange={(e) => handleFileUpload(e, 'RULE')}
                  />
                  <button
                    type="button"
                    onClick={() => ruleFileInputRef.current?.click()}
                    disabled={isUploadingRule}
                    className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                  >
                    <UploadCloud className="w-3.5 h-3.5" />
                    <span>{isUploadingRule ? 'Uploading...' : 'Upload Rule File (.md, .txt)'}</span>
                  </button>
                </div>
              </div>

              {uploadedRuleFile && (
                <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2 truncate">
                    <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                    <span className="font-semibold text-slate-800 truncate">{uploadedRuleFile.fileName}</span>
                    <span className="text-slate-500 font-mono text-[10px]">
                      ({(uploadedRuleFile.fileSize / 1024).toFixed(1)} KB)
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => onUploadedRuleFileChange(null)}
                    className="text-slate-400 hover:text-rose-500 transition ml-2"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {!isStudent && (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1">
                  {savedRules.length > 0 ? (
                    <select
                      onChange={(e) => {
                        const found = savedRules.find((r) => r.id === e.target.value);
                        if (found) onCustomRuleChange(found.content);
                      }}
                      defaultValue=""
                      className="text-xs px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 w-full sm:w-auto"
                    >
                      <option value="" disabled>Load saved rule from library ({savedRules.length})...</option>
                      {savedRules.map((r) => (
                        <option key={r.id} value={r.id}>{r.title}</option>
                      ))}
                    </select>
                  ) : <div />}
                  {customRule.trim() && onSaveRuleToLibrary && (
                    <button
                      type="button"
                      onClick={onSaveRuleToLibrary}
                      disabled={isSavingRule}
                      className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1"
                    >
                      <Bookmark className="w-3.5 h-3.5" />
                      <span>{isSavingRule ? 'Saving...' : 'Save Rule to Library'}</span>
                    </button>
                  )}
                </div>
              )}

              <textarea
                value={customRule}
                onChange={(e) => onCustomRuleChange(e.target.value)}
                rows={3}
                placeholder="e.g. Focus on Inversion after negative adverbials (Seldom, Rarely, Never before)..."
                className="w-full p-3 rounded-xl border border-slate-200 bg-white text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
              />
            </div>

            {/* Target Vocabulary Section */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-700">
                  Target Vocabulary / Lexicon List
                </label>
                <div className="flex items-center space-x-2">
                  <input
                    ref={vocabFileInputRef}
                    type="file"
                    accept=".txt,.json,.csv"
                    className="hidden"
                    onChange={(e) => handleFileUpload(e, 'VOCABULARY')}
                  />
                  <button
                    type="button"
                    onClick={() => vocabFileInputRef.current?.click()}
                    disabled={isUploadingVocab}
                    className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1 disabled:opacity-50"
                  >
                    <UploadCloud className="w-3.5 h-3.5" />
                    <span>{isUploadingVocab ? 'Uploading...' : 'Upload Vocab File (.json, .csv)'}</span>
                  </button>
                </div>
              </div>

              {uploadedVocabFile && (
                <div className="p-2.5 rounded-xl bg-indigo-50/80 border border-indigo-200 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2 truncate">
                    <FileText className="w-4 h-4 text-primary flex-shrink-0" />
                    <span className="font-semibold text-slate-800 truncate">{uploadedVocabFile.fileName}</span>
                    <span className="text-slate-500 font-mono text-[10px]">
                      ({(uploadedVocabFile.fileSize / 1024).toFixed(1)} KB)
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => onUploadedVocabFileChange(null)}
                    className="text-slate-400 hover:text-rose-500 transition ml-2"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}

              {!isStudent && (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1">
                  {savedVocabSets.length > 0 ? (
                    <select
                      onChange={(e) => {
                        const found = savedVocabSets.find((v) => v.id === e.target.value);
                        if (found) onCustomVocabularyChange(found.content);
                      }}
                      defaultValue=""
                      className="text-xs px-2.5 py-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 w-full sm:w-auto"
                    >
                      <option value="" disabled>Load saved vocabulary ({savedVocabSets.length})...</option>
                      {savedVocabSets.map((v) => (
                        <option key={v.id} value={v.id}>{v.title}</option>
                      ))}
                    </select>
                  ) : <div />}
                  {customVocabulary.trim() && onSaveVocabToLibrary && (
                    <button
                      type="button"
                      onClick={onSaveVocabToLibrary}
                      disabled={isSavingVocab}
                      className="text-xs font-semibold text-primary hover:text-primary-hover flex items-center space-x-1"
                    >
                      <Bookmark className="w-3.5 h-3.5" />
                      <span>{isSavingVocab ? 'Saving...' : 'Save Vocabulary to Library'}</span>
                    </button>
                  )}
                </div>
              )}

              <textarea
                value={customVocabulary}
                onChange={(e) => onCustomVocabularyChange(e.target.value)}
                rows={2}
                placeholder="e.g. substantiate, empirical, paradigm, ubiquitous, resilient, compromise..."
                className="w-full p-3 rounded-xl border border-slate-200 bg-white text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition font-mono leading-relaxed"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
