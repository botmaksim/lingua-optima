/**
 * @file AutoExpandingTextarea.tsx
 * @brief Seamless auto-expanding textarea supporting modern CSS field-sizing: content
 * with a zero-scrollbars height sync fallback.
 */

import React, { useRef, useEffect, useLayoutEffect, TextareaHTMLAttributes } from 'react';

export interface AutoExpandingTextareaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange'> {
  value: string;
  onChange: (value: string) => void;
  minRows?: number;
  maxRows?: number;
  className?: string;
  containerClassName?: string;
}

export const AutoExpandingTextarea: React.FC<AutoExpandingTextareaProps> = ({
  value,
  onChange,
  minRows = 1,
  maxRows = 8,
  className = '',
  containerClassName = '',
  placeholder,
  disabled = false,
  ...rest
}) => {
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const adjustHeight = () => {
    const el = textareaRef.current;
    if (!el) return;
    // Reset to calculate natural scrollHeight
    el.style.height = 'auto';
    const computed = window.getComputedStyle(el);
    const lineHeight = parseFloat(computed.lineHeight) || 20;
    const paddingTop = parseFloat(computed.paddingTop) || 8;
    const paddingBottom = parseFloat(computed.paddingBottom) || 8;
    const minHeight = lineHeight * minRows + paddingTop + paddingBottom;
    const maxHeight = lineHeight * maxRows + paddingTop + paddingBottom;

    const newHeight = Math.max(minHeight, Math.min(el.scrollHeight, maxHeight));
    el.style.height = `${newHeight}px`;
    // Suppress scrollbars unless maxHeight is strictly exceeded
    el.style.overflowY = el.scrollHeight > maxHeight ? 'auto' : 'hidden';
  };

  useLayoutEffect(() => {
    adjustHeight();
  }, [value, minRows, maxRows]);

  useEffect(() => {
    // Re-adjust upon resize or font load
    window.addEventListener('resize', adjustHeight);
    return () => window.removeEventListener('resize', adjustHeight);
  }, []);

  return (
    <div className={`relative w-full ${containerClassName}`}>
      <textarea
        ref={textareaRef}
        rows={minRows}
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(e) => {
          onChange(e.target.value);
          adjustHeight();
        }}
        style={{
          // Modern standard CSS field-sizing
          fieldSizing: 'content' as any,
          resize: 'none',
          overflowY: 'hidden',
        }}
        className={`w-full px-3.5 py-2.5 rounded-xl border border-slate-200 bg-white text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition leading-relaxed block ${className}`}
        {...rest}
      />
    </div>
  );
};
