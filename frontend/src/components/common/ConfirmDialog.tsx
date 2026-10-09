/**
 * @file ConfirmDialog.tsx
 * @brief Modal confirmation dialog for destructive or critical actions.
 */

import React, { useEffect } from 'react';
import { AlertCircle, AlertTriangle, X } from 'lucide-react';

/**
 * @brief Props for the ConfirmDialog component.
 */
interface ConfirmDialogProps {
  /** @brief Property representing is open in ConfirmDialogProps. */
  isOpen: boolean;
  /** @brief Property representing title in ConfirmDialogProps. */
  title: string;
  /** @brief Property representing message in ConfirmDialogProps. */
  message: string;
  /** @brief Property representing confirm text in ConfirmDialogProps. */
  confirmText?: string;
  /** @brief Property representing cancel text in ConfirmDialogProps. */
  cancelText?: string;
  /** @brief Property representing is destructive in ConfirmDialogProps. */
  isDestructive?: boolean;
  /** @brief Property representing on confirm in ConfirmDialogProps. */
  onConfirm: () => void;
  /** @brief Property representing on cancel in ConfirmDialogProps. */
  onCancel: () => void;
}

/**
 * @brief Renders a modal confirmation dialog requesting user verification before proceeding.
 * @param isOpen Whether the modal is actively visible.
 * @param title Dialog heading.
 * @param message Clarifying description of the action.
 * @param confirmText Label on positive confirmation button.
 * @param cancelText Label on cancellation button.
 * @param isDestructive Whether the action is hazardous (styles button in rose red).
 * @param onConfirm Callback when user clicks confirmation.
 * @param onCancel Callback when user clicks cancel or closes.
 * @return JSX modal element or null.
 */
export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  isDestructive = false,
  onConfirm,
  onCancel,
}) => {
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onCancel]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/55 backdrop-blur-md p-4 animate-in fade-in duration-150"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
    >
      <div
        className="relative overflow-hidden bg-white/95 backdrop-blur-xl rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl shadow-slate-950/25 border border-slate-200/80 ring-1 ring-slate-900/5 animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        <div
          className={`absolute inset-x-0 top-0 h-1.5 ${
            isDestructive
              ? 'bg-gradient-to-r from-rose-500 via-red-500 to-orange-500'
              : 'bg-gradient-to-r from-indigo-500 via-violet-500 to-sky-500'
          }`}
        />

        <button
          type="button"
          onClick={onCancel}
          className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition"
          aria-label="Close dialog"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="flex items-start gap-4">
          <div
            className={`w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 ring-4 ${
              isDestructive
                ? 'bg-gradient-to-br from-rose-50 to-orange-50 text-rose-600 ring-rose-500/10 border border-rose-200/60'
                : 'bg-gradient-to-br from-indigo-50 to-violet-50 text-primary ring-indigo-500/10 border border-indigo-200/60'
            }`}
          >
            {isDestructive ? (
              <AlertTriangle className="w-6 h-6" />
            ) : (
              <AlertCircle className="w-6 h-6" />
            )}
          </div>

          <div className="flex-1 min-w-0 pr-4">
            <span
              className={`inline-block text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded-full mb-1.5 ${
                isDestructive
                  ? 'bg-rose-50 text-rose-700 border border-rose-200/60'
                  : 'bg-indigo-50 text-indigo-700 border border-indigo-200/60'
              }`}
            >
              {isDestructive ? 'Destructive Action' : 'Confirmation Required'}
            </span>
            <h3
              id="confirm-dialog-title"
              className="text-lg font-black text-slate-900 tracking-tight leading-snug"
            >
              {title}
            </h3>
            <p className="text-sm text-slate-600 mt-1.5 leading-relaxed">{message}</p>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2.5 mt-6 pt-4 border-t border-slate-100">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2.5 text-xs sm:text-sm font-semibold text-slate-700 bg-slate-100/90 hover:bg-slate-200/80 rounded-xl transition"
          >
            {cancelText}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`px-5 py-2.5 text-xs sm:text-sm font-bold text-white rounded-xl transition shadow-md ${
              isDestructive
                ? 'bg-gradient-to-r from-rose-600 to-red-600 hover:from-rose-700 hover:to-red-700 shadow-rose-500/20'
                : 'bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 shadow-indigo-500/20'
            }`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};
