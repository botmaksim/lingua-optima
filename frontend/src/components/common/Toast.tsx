import React from 'react';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'warning' | 'error' | 'info';

interface ToastProps {
  id: string;
  type: ToastType;
  message: string;
  onClose: (id: string) => void;
}

export const Toast: React.FC<ToastProps> = ({ id, type, message, onClose }) => {
  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-500" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-500" />,
    error: <AlertCircle className="w-5 h-5 text-rose-500" />,
    info: <Info className="w-5 h-5 text-sky-500" />,
  };

  const borderColors = {
    success: 'border-emerald-200 bg-emerald-50/90 text-emerald-900',
    warning: 'border-amber-200 bg-amber-50/90 text-amber-900',
    error: 'border-rose-200 bg-rose-50/90 text-rose-900',
    info: 'border-sky-200 bg-sky-50/90 text-sky-900',
  };

  return (
    <div
      className={`flex items-center space-x-3 p-4 rounded-xl border shadow-lg backdrop-blur-sm transition animate-in slide-in-from-top-2 duration-200 ${borderColors[type]}`}
    >
      <div className="flex-shrink-0">{icons[type]}</div>
      <p className="text-sm font-medium flex-1">{message}</p>
      <button
        onClick={() => onClose(id)}
        className="text-slate-400 hover:text-slate-600 transition"
        aria-label="Dismiss toast"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};
