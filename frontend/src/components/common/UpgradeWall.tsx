/**
 * @file UpgradeWall.tsx
 * @brief Modal dialog prompting users to upgrade their subscription tier or provide BYOK keys.
 */

import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Key, X, ArrowRight } from 'lucide-react';
import { useUIStore } from '../../store/uiStore';

/**
 * @brief Paywall/quota modal rendered when daily AI or OCR limits are reached.
 * @return JSX modal element or null when hidden.
 */
export const UpgradeWall: React.FC = () => {
  const { isUpgradeWallOpen, upgradeWallReason, closeUpgradeWall } = useUIStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isUpgradeWallOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        closeUpgradeWall();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isUpgradeWallOpen, closeUpgradeWall]);

  if (!isUpgradeWallOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 backdrop-blur-md p-4 animate-in fade-in duration-150"
      onClick={closeUpgradeWall}
      role="dialog"
      aria-modal="true"
      aria-labelledby="upgrade-wall-title"
    >
      <div
        className="relative overflow-hidden bg-white/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl shadow-slate-950/25 border border-slate-200/80 ring-1 ring-slate-900/5 animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="absolute inset-x-0 top-0 h-1.5 bg-gradient-to-r from-indigo-500 via-purple-500 to-amber-500" />

        <button
          type="button"
          onClick={closeUpgradeWall}
          className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition"
          aria-label="Close"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-indigo-500 to-violet-600 text-white flex items-center justify-center shadow-lg shadow-indigo-500/25 ring-4 ring-indigo-500/10 shrink-0">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <span className="inline-block text-[10px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200/70">
              Daily Limit Reached
            </span>
            <h3 id="upgrade-wall-title" className="text-xl font-black text-slate-900 tracking-tight mt-0.5">
              Upgrade to Continue
            </h3>
          </div>
        </div>

        <p className="text-sm text-slate-600 mb-6 leading-relaxed">
          {upgradeWallReason || 'You have reached your daily evaluation limit on the Free tier. Upgrade for unlimited instant evaluations or add your own custom AI model key.'}
        </p>

        <div className="space-y-3">
          <button
            type="button"
            onClick={() => {
              closeUpgradeWall();
              navigate('/subscription');
            }}
            className="w-full flex items-center justify-between py-3.5 px-5 rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-bold transition shadow-lg shadow-indigo-500/25 group"
          >
            <div className="flex items-center gap-2.5">
              <Sparkles className="w-4 h-4 text-amber-300" />
              <span>Upgrade to Premium</span>
            </div>
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
          </button>

          <button
            type="button"
            onClick={() => {
              closeUpgradeWall();
              navigate('/profile');
            }}
            className="w-full flex items-center justify-between py-3 px-4 rounded-2xl bg-slate-50/90 hover:bg-slate-100 text-slate-700 font-semibold border border-slate-200/80 transition"
          >
            <div className="flex items-center gap-2.5">
              <Key className="w-4 h-4 text-slate-500" />
              <span className="text-xs sm:text-sm">Bring Your Own API Key (BYOK)</span>
            </div>
            <span className="text-[10px] uppercase font-bold text-slate-400 bg-slate-200/70 px-2 py-0.5 rounded-md">Free</span>
          </button>
        </div>
      </div>
    </div>
  );
};
