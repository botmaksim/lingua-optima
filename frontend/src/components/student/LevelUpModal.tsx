/**
 * @file LevelUpModal.tsx
 * @brief Modal dialogue celebrating student progression and offering voluntary promotion to the next CEFR level.
 */

import React, { useState, useEffect } from 'react';
import { Award, Sparkles, X, ArrowRight } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { progressApi } from '../../api/progressApi';
import { CefrBadge } from '../common/CefrBadge';
import { CefrLevel } from '../../types/user';

/**
 * @brief Prompts the student when eligible to advance their account's CEFR proficiency level.
 * @return JSX modal element or null.
 */
export const LevelUpModal: React.FC = () => {
  const { user, setUser } = useAuthStore();
  const [isOpen, setIsOpen] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen]);

  if (!isOpen || !user) return null;

  const NEXT_LEVEL_MAP: Record<CefrLevel, CefrLevel> = {
    A1: 'A2',
    A2: 'B1',
    B1: 'B2',
    B2: 'C1',
    C1: 'C2',
    C2: 'C2',
  };

  const currentLevel: CefrLevel = user?.cefrLevel || 'A1';
  if (currentLevel === 'C2') return null;
  const nextLevel = NEXT_LEVEL_MAP[currentLevel] || 'A2';

  /**
   * @brief Event handler or helper executing handle confirm.
   */
  const handleConfirm = async () => {
    setIsSubmitting(true);
    try {
      const updatedUser = await progressApi.confirmLevelUp();
      setUser(updatedUser);
      setIsOpen(false);
    } catch (err) {
      console.error('Failed to confirm level up:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-md p-4 animate-in fade-in duration-150"
      onClick={() => setIsOpen(false)}
      role="dialog"
      aria-modal="true"
      aria-labelledby="levelup-modal-title"
    >
      <div
        className="relative overflow-hidden bg-white/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl shadow-slate-950/25 border border-slate-200/80 ring-1 ring-slate-900/5 text-center animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="absolute inset-x-0 top-0 h-1.5 bg-gradient-to-r from-amber-400 via-orange-500 to-indigo-600" />

        <button
          type="button"
          onClick={() => setIsOpen(false)}
          className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition"
          aria-label="Close"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-amber-400 to-orange-500 text-white mx-auto flex items-center justify-center mb-4 shadow-xl shadow-amber-500/25 ring-4 ring-amber-500/15">
          <Award className="w-9 h-9" />
        </div>

        <span className="inline-block text-[10px] font-black uppercase tracking-wider px-3 py-1 rounded-full bg-amber-50 text-amber-800 border border-amber-200/80 mb-2">
          Milestone Unlocked
        </span>

        <h3 id="levelup-modal-title" className="text-2xl font-black text-slate-900 tracking-tight mb-2 flex items-center justify-center gap-2">
          <span>Congratulations!</span>
          <Sparkles className="w-6 h-6 text-amber-500 animate-pulse" />
        </h3>
        <p className="text-sm text-slate-600 mb-6 leading-relaxed">
          You have achieved an average mastery above 85% across all topics in level{' '}
          <span className="font-bold text-slate-900">{currentLevel}</span>. You are ready to advance to{' '}
          <span className="font-bold text-primary">{nextLevel}</span>!
        </p>

        <div className="bg-slate-50/80 border border-slate-200/70 rounded-2xl p-4 flex items-center justify-center gap-4 mb-6">
          <CefrBadge level={currentLevel} size="lg" />
          <ArrowRight className="w-5 h-5 text-slate-400" />
          <CefrBadge level={nextLevel} size="lg" />
        </div>

        <div className="flex flex-col sm:flex-row gap-2.5 justify-center">
          <button
            type="button"
            onClick={() => setIsOpen(false)}
            className="flex-1 py-3 px-4 rounded-xl border border-slate-200/90 text-slate-700 font-semibold hover:bg-slate-100/80 transition text-sm"
          >
            Stay on {currentLevel}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={isSubmitting}
            className="flex-1 flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-bold transition shadow-md shadow-indigo-500/20 disabled:opacity-50 text-sm"
          >
            <Sparkles className="w-4 h-4 text-amber-300" />
            <span>{isSubmitting ? 'Promoting...' : `Yes, Level Up!`}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
