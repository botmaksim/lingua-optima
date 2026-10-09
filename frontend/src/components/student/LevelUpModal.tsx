/**
 * @file LevelUpModal.tsx
 * @brief Modal dialogue celebrating student progression and offering voluntary promotion to the next CEFR level.
 */

import React, { useState } from 'react';
import { Award, Sparkles } from 'lucide-react';
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

  if (!isOpen || !user) return null;

  const currentLevel = user.cefrLevel;
  const nextLevel = (currentLevel === 'B1' ? 'B2' : 'C1') as CefrLevel;

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl border border-slate-100 text-center animate-in zoom-in-95 duration-200">
        <div className="w-16 h-16 rounded-2xl bg-amber-50 text-amber-500 mx-auto flex items-center justify-center mb-4 shadow-inner">
          <Award className="w-10 h-10" />
        </div>

        <h3 className="text-2xl font-black text-slate-900 mb-2">
          Congratulations! 🎉
        </h3>
        <p className="text-sm text-slate-600 mb-6">
          You have achieved an average mastery above 85% across all topics in level{' '}
          <span className="font-bold text-slate-800">{currentLevel}</span>. You are ready to advance to{' '}
          <span className="font-bold text-primary">{nextLevel}</span>!
        </p>

        <div className="flex items-center justify-center space-x-4 mb-8">
          <CefrBadge level={currentLevel} size="lg" />
          <span className="text-slate-400 font-bold">➔</span>
          <CefrBadge level={nextLevel} size="lg" />
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <button
            onClick={() => setIsOpen(false)}
            className="flex-1 py-3 px-4 rounded-xl border border-slate-200 text-slate-700 font-semibold hover:bg-slate-50 transition"
          >
            Stay on {currentLevel}
          </button>
          <button
            onClick={handleConfirm}
            disabled={isSubmitting}
            className="flex-1 flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-primary hover:bg-primary-hover text-white font-bold transition shadow-md shadow-indigo-100 disabled:opacity-50"
          >
            <Sparkles className="w-4 h-4" />
            <span>{isSubmitting ? 'Promoting...' : `Yes, Level Up!`}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
