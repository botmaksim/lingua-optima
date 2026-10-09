/**
 * @file Toast.tsx
 * @brief Toast notification popup component for user feedback messages.
 */

import React from 'react';
import { CheckCircle2, AlertTriangle, AlertCircle, Info, X } from 'lucide-react';

/**
 * @brief Categorization of toast visual severity.
 */
export type ToastType = 'success' | 'warning' | 'error' | 'info';

/**
 * @brief Props for individual Toast message.
 */
interface ToastProps {
  /** @brief Property representing id in ToastProps. */
  id: string;
  /** @brief Property representing type in ToastProps. */
  type: ToastType;
  /** @brief Property representing title in ToastProps. */
  title?: string;
  /** @brief Property representing message in ToastProps. */
  message: string;
  /** @brief Property representing on close in ToastProps. */
  onClose: (id: string) => void;
}

/**
 * @brief Renders a floating notification card with severity styling and dismiss trigger.
 * @param id Unique identifier of the notification.
 * @param type Severity category determining color and icon.
 * @param title Optional bold heading above the message.
 * @param message User message text.
 * @param onClose Callback to dismiss the toast.
 * @return JSX notification element.
 */
export const Toast: React.FC<ToastProps> = ({ id, type, title, message, onClose }) => {
  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-600" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-600" />,
    error: <AlertCircle className="w-5 h-5 text-rose-600" />,
    info: <Info className="w-5 h-5 text-indigo-600" />,
  };

  const styleMap = {
    success: {
      bar: 'bg-emerald-500',
      border: 'border-emerald-200/90',
      badge: 'bg-emerald-50 ring-1 ring-emerald-500/20',
      title: 'text-emerald-900',
    },
    warning: {
      bar: 'bg-amber-500',
      border: 'border-amber-200/90',
      badge: 'bg-amber-50 ring-1 ring-amber-500/20',
      title: 'text-amber-900',
    },
    error: {
      bar: 'bg-rose-500',
      border: 'border-rose-200/90',
      badge: 'bg-rose-50 ring-1 ring-rose-500/20',
      title: 'text-rose-900',
    },
    info: {
      bar: 'bg-indigo-500',
      border: 'border-indigo-200/90',
      badge: 'bg-indigo-50 ring-1 ring-indigo-500/20',
      title: 'text-indigo-900',
    },
  };

  const currentStyle = styleMap[type] || styleMap.info;

  return (
    <div
      role="alert"
      className={`relative overflow-hidden flex items-start gap-3 p-4 pl-4.5 rounded-2xl border bg-white/95 backdrop-blur-xl shadow-xl shadow-slate-900/10 ring-1 ring-slate-900/5 transition animate-in slide-in-from-top-3 duration-200 ${currentStyle.border}`}
    >
      <div className={`absolute left-0 top-2 bottom-2 w-1.5 rounded-r-full ${currentStyle.bar}`} />

      <div className={`p-1.5 rounded-xl shrink-0 mt-0.5 ${currentStyle.badge}`}>
        {icons[type]}
      </div>

      <div className="flex-1 min-w-0 pr-1">
        {title && (
          <h4 className={`text-xs font-black uppercase tracking-wider mb-0.5 ${currentStyle.title}`}>
            {title}
          </h4>
        )}
        <p className="text-xs sm:text-sm font-medium text-slate-700 leading-snug break-words">
          {message}
        </p>
      </div>

      <button
        type="button"
        onClick={() => onClose(id)}
        className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100/80 transition shrink-0 -mr-1 -mt-1"
        aria-label="Dismiss toast"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};
