/**
 * @file UpgradeWall.tsx
 * @brief Modal dialog prompting users to upgrade their subscription tier or provide BYOK keys.
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Sparkles, Key, AlertCircle, X } from 'lucide-react';
import { useUIStore } from '../../store/uiStore';

/**
 * @brief Paywall/quota modal rendered when daily AI or OCR limits are reached.
 * @return JSX modal element or null when hidden.
 */
export const UpgradeWall: React.FC = () => {
  const { isUpgradeWallOpen, upgradeWallReason, closeUpgradeWall } = useUIStore();
  const navigate = useNavigate();

  if (!isUpgradeWallOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100 relative animate-in fade-in zoom-in-95 duration-200">
        <button
          onClick={closeUpgradeWall}
          className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 transition"
          aria-label="Close"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="w-12 h-12 rounded-xl bg-indigo-50 text-primary flex items-center justify-center mb-4">
          <AlertCircle className="w-6 h-6" />
        </div>

        <h3 className="text-xl font-bold text-slate-900 mb-2">Upgrade to Continue</h3>
        <p className="text-sm text-slate-600 mb-6">
          {upgradeWallReason || 'You have reached your daily evaluation limit on the Free tier.'}
        </p>

        <div className="space-y-3">
          <button
            onClick={() => {
              closeUpgradeWall();
              navigate('/subscription');
            }}
            className="w-full flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-primary text-white font-medium hover:bg-primary-hover transition shadow-md shadow-indigo-100"
          >
            <Sparkles className="w-4 h-4" />
            <span>Upgrade to Premium</span>
          </button>

          <button
            onClick={() => {
              closeUpgradeWall();
              navigate('/profile');
            }}
            className="w-full flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-slate-50 text-slate-700 font-medium hover:bg-slate-100 border border-slate-200 transition"
          >
            <Key className="w-4 h-4 text-slate-500" />
            <span>Bring Your Own API Key (BYOK)</span>
          </button>
        </div>
      </div>
    </div>
  );
};
