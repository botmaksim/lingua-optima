/**
 * @file ConfirmDialog.tsx
 * @brief Modal confirmation dialog for destructive or critical actions.
 */

import React from 'react';
import { AlertCircle } from 'lucide-react';

/**
 * @brief Props for the ConfirmDialog component.
 */
interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDestructive?: boolean;
  onConfirm: () => void;
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
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 animate-in fade-in zoom-in-95 duration-150">
        <div
          className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4 ${
            isDestructive ? 'bg-rose-50 text-rose-600' : 'bg-indigo-50 text-primary'
          }`}
        >
          <AlertCircle className="w-6 h-6" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 mb-2">{title}</h3>
        <p className="text-sm text-slate-600 mb-6">{message}</p>
        <div className="flex space-x-3 justify-end">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 rounded-xl hover:bg-slate-200 transition"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            className={`px-4 py-2 text-sm font-medium text-white rounded-xl transition ${
              isDestructive
                ? 'bg-rose-600 hover:bg-rose-700 shadow-sm shadow-rose-200'
                : 'bg-primary hover:bg-primary-hover shadow-sm shadow-indigo-200'
            }`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};
