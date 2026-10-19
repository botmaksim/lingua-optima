/**
 * @file AnnotatedTextReview.tsx
 * @brief Interactive text review component with inline red mistake highlighting,
 * contextual explanations popover, and educator mistake editing/removal/highlighting capabilities.
 */

import React, { useState, useRef, useMemo } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  XCircle,
  Lightbulb,
  Trash2,
  Edit3,
  Plus,
  X,
  Sparkles,
  BookOpen,
} from 'lucide-react';
import { SentenceCorrection } from '../../types/submission';

export interface AnnotatedTextReviewProps {
  /** @brief Original student-submitted text or essay. */
  text: string;
  /** @brief Grammatical and stylistic corrections list. */
  corrections: SentenceCorrection[];
  /** @brief Whether educator editing controls (add/edit/remove highlights) are enabled. */
  editable?: boolean;
  /** @brief Callback triggered when teacher modifies corrections. */
  onCorrectionsChange?: (newCorrections: SentenceCorrection[]) => void;
  /** @brief Optional additional CSS container classes. */
  className?: string;
  /** @brief Optional section title. */
  title?: string;
}

interface TextSpan {
  text: string;
  correction?: SentenceCorrection;
  correctionIndex?: number;
}

export const AnnotatedTextReview: React.FC<AnnotatedTextReviewProps> = ({
  text,
  corrections,
  editable = false,
  onCorrectionsChange,
  className = '',
  title = 'Student Text & Mistake Analysis',
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [selectedCorrectionIndex, setSelectedCorrectionIndex] = useState<number | null>(null);
  const [hoveredCorrectionIndex, setHoveredCorrectionIndex] = useState<number | null>(null);

  // New highlight selection state (for teacher mode)
  const [newSelection, setNewSelection] = useState<{
    text: string;
    x: number;
    y: number;
  } | null>(null);

  // Editing existing correction state
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editExplanation, setEditExplanation] = useState<string>('');
  const [editCorrected, setEditCorrected] = useState<string>('');
  const [editRule, setEditRule] = useState<string>('');

  // Add new highlight modal state
  const [isAddingCorrection, setIsAddingCorrection] = useState(false);
  const [newOriginal, setNewOriginal] = useState<string>('');
  const [newCorrected, setNewCorrected] = useState<string>('');
  const [newExplanation, setNewExplanation] = useState<string>('');
  const [newRule, setNewRule] = useState<string>('Grammar & Usage');

  /**
   * @brief Splits original text into plain text chunks and highlighted correction spans.
   */
  const parsedSpans: TextSpan[] = useMemo(() => {
    if (!text) return [];
    if (!corrections || corrections.length === 0) {
      return [{ text }];
    }

    // Filter valid corrections with non-empty original
    const validCorrections = corrections
      .map((corr, idx) => ({ corr, idx }))
      .filter(({ corr }) => corr.original && corr.original.trim().length > 0);

    if (validCorrections.length === 0) {
      return [{ text }];
    }

    // Find all matches in text
    interface MatchPos {
      start: number;
      end: number;
      corr: SentenceCorrection;
      idx: number;
    }
    const matches: MatchPos[] = [];

    const lowerText = text.toLowerCase();
    validCorrections.forEach(({ corr, idx }) => {
      const searchTarget = corr.original.trim().toLowerCase();
      let pos = 0;
      while ((pos = lowerText.indexOf(searchTarget, pos)) !== -1) {
        matches.push({
          start: pos,
          end: pos + searchTarget.length,
          corr,
          idx,
        });
        pos += searchTarget.length;
      }
    });

    if (matches.length === 0) {
      return [{ text }];
    }

    // Sort by start position, resolving overlaps by choosing first occurrence
    matches.sort((a, b) => a.start - b.start);
    const nonOverlapping: MatchPos[] = [];
    let lastEnd = 0;
    for (const m of matches) {
      if (m.start >= lastEnd) {
        nonOverlapping.push(m);
        lastEnd = m.end;
      }
    }

    // Build chunks
    const result: TextSpan[] = [];
    let currentIndex = 0;
    for (const m of nonOverlapping) {
      if (m.start > currentIndex) {
        result.push({ text: text.substring(currentIndex, m.start) });
      }
      result.push({
        text: text.substring(m.start, m.end),
        correction: m.corr,
        correctionIndex: m.idx,
      });
      currentIndex = m.end;
    }
    if (currentIndex < text.length) {
      result.push({ text: text.substring(currentIndex) });
    }

    return result;
  }, [text, corrections]);

  /**
   * @brief Handles user mouseup selection in teacher mode to allow highlighting a new mistake.
   */
  const handleMouseUp = () => {
    if (!editable) return;
    const selection = window.getSelection();
    if (!selection || selection.isCollapsed) {
      setNewSelection(null);
      return;
    }

    const selectedStr = selection.toString().trim();
    if (selectedStr.length < 2) {
      setNewSelection(null);
      return;
    }

    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();
    if (containerRef.current) {
      const containerRect = containerRef.current.getBoundingClientRect();
      setNewSelection({
        text: selectedStr,
        x: rect.left - containerRect.left + rect.width / 2,
        y: rect.top - containerRect.top - 8,
      });
    }
  };

  /**
   * @brief Opens the modal to create a new error annotation from selection.
   */
  const handleOpenAddModal = (initialText?: string) => {
    setNewOriginal(initialText || newSelection?.text || '');
    setNewCorrected('');
    setNewExplanation('');
    setNewRule('Grammar & Usage');
    setIsAddingCorrection(true);
    setNewSelection(null);
  };

  /**
   * @brief Saves a newly created correction.
   */
  const handleSaveNewCorrection = () => {
    if (!newOriginal.trim() || !onCorrectionsChange) return;
    const added: SentenceCorrection = {
      original: newOriginal.trim(),
      corrected: newCorrected.trim() || newOriginal.trim(),
      explanation: newExplanation.trim() || 'Highlighted for review by educator',
      grammarRule: newRule.trim() || 'Grammar & Usage',
    };
    const updated = [...corrections, added];
    onCorrectionsChange(updated);
    setIsAddingCorrection(false);
  };

  /**
   * @brief Starts editing an existing correction.
   */
  const handleStartEdit = (idx: number) => {
    const c = corrections[idx];
    if (!c) return;
    setEditingIndex(idx);
    setEditExplanation(c.explanation || '');
    setEditCorrected(c.corrected || '');
    setEditRule(c.grammarRule || '');
  };

  /**
   * @brief Saves changes made to an existing correction.
   */
  const handleSaveEdit = (idx: number) => {
    if (!onCorrectionsChange) return;
    const updated = [...corrections];
    updated[idx] = {
      ...updated[idx],
      explanation: editExplanation.trim(),
      corrected: editCorrected.trim(),
      grammarRule: editRule.trim() || 'Grammar & Usage',
    };
    onCorrectionsChange(updated);
    setEditingIndex(null);
  };

  /**
   * @brief Deletes a correction and removes its red highlight.
   */
  const handleDeleteCorrection = (idx: number) => {
    if (!onCorrectionsChange) return;
    const updated = corrections.filter((_, i) => i !== idx);
    onCorrectionsChange(updated);
    setSelectedCorrectionIndex(null);
    setEditingIndex(null);
  };

  const activeIndex = selectedCorrectionIndex ?? hoveredCorrectionIndex;
  const activeCorrection =
    activeIndex !== null && corrections[activeIndex] ? corrections[activeIndex] : null;

  return (
    <div className={`space-y-4 ${className}`} ref={containerRef}>
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-200">
        <div className="flex items-center space-x-2">
          <BookOpen className="w-4 h-4 text-primary" />
          <h3 className="text-sm font-bold text-slate-900 tracking-tight">{title}</h3>
          <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800">
            {corrections.length} {corrections.length === 1 ? 'error detected' : 'errors detected'}
          </span>
        </div>

        {editable && (
          <div className="flex items-center space-x-2">
            <button
              type="button"
              onClick={() => handleOpenAddModal()}
              className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-rose-50 text-rose-700 hover:bg-rose-100 text-xs font-bold transition border border-rose-200"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Highlight New Mistake</span>
            </button>
          </div>
        )}
      </div>

      {editable && (
        <div className="p-3 rounded-2xl bg-indigo-50/60 border border-indigo-100 text-[11px] text-indigo-900 flex items-start space-x-2">
          <Sparkles className="w-4 h-4 text-primary shrink-0 mt-0.5" />
          <div>
            <span className="font-bold">Educator Diagnostic Mode:</span> Click any red highlight to
            modify explanations or remove the error. Select text with your cursor to highlight a new mistake.
          </div>
        </div>
      )}

      {/* Main Annotated Text Display Container */}
      <div
        onMouseUp={handleMouseUp}
        className="relative p-5 sm:p-6 rounded-2xl bg-white border border-slate-200 text-slate-800 text-sm leading-relaxed whitespace-pre-wrap select-text font-serif shadow-xs"
      >
        {parsedSpans.map((span, sIdx) => {
          if (span.correctionIndex !== undefined && span.correction) {
            const isSelected = selectedCorrectionIndex === span.correctionIndex;
            const isHovered = hoveredCorrectionIndex === span.correctionIndex;
            const isActive = isSelected || isHovered;

            return (
              <mark
                key={sIdx}
                onClick={() =>
                  setSelectedCorrectionIndex(
                    isSelected ? null : (span.correctionIndex as number)
                  )
                }
                onMouseEnter={() => setHoveredCorrectionIndex(span.correctionIndex as number)}
                onMouseLeave={() => setHoveredCorrectionIndex(null)}
                className={`relative inline-block px-1.5 py-0.5 mx-0.5 rounded-md font-sans font-medium text-xs cursor-pointer transition-all ${
                  isActive
                    ? 'bg-rose-500 text-white shadow-md shadow-rose-300 ring-2 ring-rose-300'
                    : 'bg-rose-100 text-rose-950 border-b-2 border-rose-500 hover:bg-rose-200'
                }`}
                title={span.correction.explanation || 'Grammar error detected'}
              >
                <span>{span.text}</span>
              </mark>
            );
          }
          return <span key={sIdx}>{span.text}</span>;
        })}

        {/* Floating "Highlight Selection" Button in Teacher Mode */}
        {editable && newSelection && (
          <div
            style={{
              position: 'absolute',
              left: `${newSelection.x}px`,
              top: `${newSelection.y}px`,
              transform: 'translate(-50%, -100%)',
            }}
            className="z-30 animate-in fade-in zoom-in-95 duration-100"
          >
            <button
              type="button"
              onClick={() => handleOpenAddModal(newSelection.text)}
              className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-lg shadow-rose-600/30 flex items-center space-x-1.5 transition whitespace-nowrap"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Mark as Mistake</span>
            </button>
          </div>
        )}
      </div>

      {/* Active Correction Inspector Card (shows when a highlight is clicked or hovered) */}
      {activeCorrection && activeIndex !== null && (
        <div className="p-4 rounded-2xl bg-gradient-to-br from-rose-50/70 via-white to-amber-50/30 border border-rose-200/90 shadow-sm space-y-3 animate-in fade-in duration-150">
          <div className="flex items-center justify-between pb-2 border-b border-rose-100">
            <div className="flex items-center space-x-2">
              <span className="w-6 h-6 rounded-lg bg-rose-100 text-rose-700 flex items-center justify-center text-xs font-bold">
                {activeIndex + 1}
              </span>
              <span className="text-xs font-bold text-slate-800">
                Diagnostic Analysis #{activeIndex + 1}
              </span>
              {activeCorrection.grammarRule && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-primary border border-indigo-100">
                  {activeCorrection.grammarRule}
                </span>
              )}
            </div>

            <div className="flex items-center space-x-1.5">
              {editable && editingIndex !== activeIndex && (
                <>
                  <button
                    type="button"
                    onClick={() => handleStartEdit(activeIndex)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-primary hover:bg-white transition"
                    title="Edit explanation and correction"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDeleteCorrection(activeIndex)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-rose-600 hover:bg-white transition"
                    title="Remove error highlight"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </>
              )}
              <button
                type="button"
                onClick={() => setSelectedCorrectionIndex(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 transition"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>

          {editingIndex === activeIndex ? (
            /* Educator Inline Edit Form */
            <div className="space-y-3 pt-1">
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Suggested Rewrite / Correct Form:
                </label>
                <input
                  type="text"
                  value={editCorrected}
                  onChange={(e) => setEditCorrected(e.target.value)}
                  className="w-full px-3 py-1.5 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Grammar Rule:
                </label>
                <input
                  type="text"
                  value={editRule}
                  onChange={(e) => setEditRule(e.target.value)}
                  placeholder="e.g. Past Simple vs Present Perfect"
                  className="w-full px-3 py-1.5 rounded-xl border border-slate-300 text-xs font-semibold text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Diagnostic Explanation (What is wrong):
                </label>
                <textarea
                  rows={2}
                  value={editExplanation}
                  onChange={(e) => setEditExplanation(e.target.value)}
                  className="w-full px-3 py-1.5 rounded-xl border border-slate-300 text-xs font-medium text-slate-800 focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              <div className="flex items-center justify-end space-x-2 pt-1">
                <button
                  type="button"
                  onClick={() => setEditingIndex(null)}
                  className="px-3 py-1.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => handleSaveEdit(activeIndex)}
                  className="px-4 py-1.5 rounded-xl bg-primary text-white text-xs font-bold hover:bg-primary-hover shadow-sm"
                >
                  Save Changes
                </button>
              </div>
            </div>
          ) : (
            /* Read-only view of active correction */
            <div className="space-y-2.5 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-950 flex items-start space-x-2">
                  <XCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-rose-800">Incorrect: </span>
                    <span className="line-through font-semibold text-rose-900">
                      {activeCorrection.original}
                    </span>
                  </div>
                </div>

                <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-950 flex items-start space-x-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold text-emerald-800">Correct form: </span>
                    <span className="font-semibold text-emerald-900">
                      {activeCorrection.corrected}
                    </span>
                  </div>
                </div>
              </div>

              <div className="p-2.5 rounded-xl bg-white border border-slate-200 space-y-1">
                <div className="flex items-center space-x-1 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
                  <Lightbulb className="w-3.5 h-3.5 text-amber-500" />
                  <span>Why this is wrong & Grammar advice:</span>
                </div>
                <p className="text-xs text-slate-700 leading-relaxed font-medium">
                  {activeCorrection.explanation}
                </p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Add New Mistake Highlight Modal in Teacher Mode */}
      {editable && isAddingCorrection && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600" />
                <span>Highlight Grammatical Mistake</span>
              </h3>
              <button
                type="button"
                onClick={() => setIsAddingCorrection(false)}
                className="p-1 rounded-xl text-slate-400 hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Selected Student Text:
                </label>
                <input
                  type="text"
                  value={newOriginal}
                  onChange={(e) => setNewOriginal(e.target.value)}
                  placeholder="Text snippet containing error..."
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 font-semibold text-rose-700 bg-rose-50/50"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Suggested Correction / Rewrite:
                </label>
                <input
                  type="text"
                  value={newCorrected}
                  onChange={(e) => setNewCorrected(e.target.value)}
                  placeholder="Correct grammatically polished phrasing..."
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 font-semibold text-emerald-700 bg-emerald-50/50"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  Grammar Rule:
                </label>
                <input
                  type="text"
                  value={newRule}
                  onChange={(e) => setNewRule(e.target.value)}
                  placeholder="e.g. Subject-Verb Agreement, Article Usage"
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 font-medium text-slate-800"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                  What is wrong & Pedagogical Explanation:
                </label>
                <textarea
                  rows={3}
                  value={newExplanation}
                  onChange={(e) => setNewExplanation(e.target.value)}
                  placeholder="Explain why this phrasing is incorrect and guide the student..."
                  className="w-full px-3 py-2 rounded-xl border border-slate-200 font-medium text-slate-800"
                />
              </div>
            </div>

            <div className="flex items-center justify-end space-x-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setIsAddingCorrection(false)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveNewCorrection}
                disabled={!newOriginal.trim()}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition shadow-sm disabled:opacity-50"
              >
                Apply Highlight
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
