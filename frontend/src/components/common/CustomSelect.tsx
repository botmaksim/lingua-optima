/**
 * @file CustomSelect.tsx
 * @brief Reusable styled dropdown menu component replacing native OS select elements across the platform.
 */

import React, { useState, useRef, useEffect, useMemo } from 'react';
import { ChevronDown, Check } from 'lucide-react';

/**
 * @brief Option item within a CustomSelect menu.
 */
export interface SelectOption {
  /** @brief Unique value of the option. */
  value: string;
  /** @brief Primary display label. */
  label: string;
  /** @brief Optional secondary badge text (e.g. model speed/tier). */
  badge?: string;
  /** @brief Optional flag to disable selection of this option. */
  disabled?: boolean;
}

/**
 * @brief Group of options within a CustomSelect menu (replaces native optgroup).
 */
export interface SelectOptionGroup {
  /** @brief Group header title. */
  label: string;
  /** @brief Options belonging to this group. */
  options: SelectOption[];
}

/**
 * @brief Props for the CustomSelect component.
 */
export interface CustomSelectProps {
  /** @brief Currently selected option value. */
  value: string;
  /** @brief Callback invoked when user selects a new option value. */
  onChange: (value: string) => void;
  /** @brief Flat list of options (if not using grouped options). */
  options?: SelectOption[];
  /** @brief Grouped list of options (replaces native optgroup). */
  groups?: SelectOptionGroup[];
  /** @brief Placeholder text displayed when value is empty or unmatched. */
  placeholder?: string;
  /** @brief Visual size preset ('sm' | 'md' | 'lg'). */
  size?: 'sm' | 'md' | 'lg';
  /** @brief Optional additional CSS classes for the wrapper. */
  className?: string;
  /** @brief Optional disabled state. */
  disabled?: boolean;
  /** @brief Optional aria-label for accessibility. */
  ariaLabel?: string;
}

/**
 * @brief Branded, accessible custom select dropdown with option groups, badges, and keyboard navigation.
 * @param props CustomSelect configuration props.
 * @return JSX element representing the styled dropdown menu.
 */
export const CustomSelect: React.FC<CustomSelectProps> = ({
  value,
  onChange,
  options = [],
  groups = [],
  placeholder = 'Select an option...',
  size = 'md',
  className = '',
  disabled = false,
  ariaLabel,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState<number>(-1);
  const containerRef = useRef<HTMLDivElement>(null);

  const flatOptions = useMemo(() => {
    const list: SelectOption[] = [...options];
    for (const grp of groups) {
      list.push(...grp.options);
    }
    return list;
  }, [options, groups]);

  const selectedOption = useMemo(
    () => flatOptions.find((opt) => opt.value === value),
    [flatOptions, value]
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  /**
   * @brief Handles keyboard navigation for accessibility.
   */
  const handleKeyDown = (e: React.KeyboardEvent<HTMLButtonElement>) => {
    if (disabled) return;
    if (e.key === 'Escape') {
      setIsOpen(false);
      return;
    }
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      if (!isOpen) {
        setIsOpen(true);
        const currentIdx = flatOptions.findIndex((o) => o.value === value);
        setHighlightedIndex(currentIdx >= 0 ? currentIdx : 0);
      } else if (highlightedIndex >= 0 && highlightedIndex < flatOptions.length) {
        const chosen = flatOptions[highlightedIndex];
        if (chosen && !chosen.disabled) {
          onChange(chosen.value);
          setIsOpen(false);
        }
      }
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!isOpen) {
        setIsOpen(true);
        const currentIdx = flatOptions.findIndex((o) => o.value === value);
        setHighlightedIndex(currentIdx >= 0 ? currentIdx : 0);
      } else {
        setHighlightedIndex((prev) => (prev + 1 < flatOptions.length ? prev + 1 : 0));
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (isOpen) {
        setHighlightedIndex((prev) => (prev - 1 >= 0 ? prev - 1 : flatOptions.length - 1));
      }
    }
  };

  const sizeClasses =
    size === 'sm'
      ? 'px-3 py-2 text-xs rounded-xl'
      : size === 'lg'
      ? 'px-4 py-3 text-sm rounded-2xl'
      : 'px-3.5 py-2.5 text-sm rounded-xl';

  const renderOptionButton = (opt: SelectOption) => {
    const isSelected = opt.value === value;
    return (
      <button
        key={opt.value}
        type="button"
        role="option"
        aria-selected={isSelected}
        disabled={opt.disabled}
        onClick={() => {
          if (!opt.disabled) {
            onChange(opt.value);
            setIsOpen(false);
          }
        }}
        className={`w-full text-left px-3.5 py-2.5 text-xs sm:text-sm flex items-center justify-between gap-2 transition rounded-xl ${
          isSelected
            ? 'bg-indigo-50/90 text-primary font-bold'
            : 'text-slate-700 hover:bg-slate-100/80 hover:text-slate-900 font-medium'
        } ${opt.disabled ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}
      >
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <span className="truncate">{opt.label}</span>
          {opt.badge && (
            <span
              className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex-shrink-0 ${
                isSelected
                  ? 'bg-primary/15 text-primary'
                  : 'bg-slate-100 text-slate-500'
              }`}
            >
              {opt.badge}
            </span>
          )}
        </div>
        {isSelected && <Check className="w-4 h-4 text-primary flex-shrink-0" />}
      </button>
    );
  };

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <button
        type="button"
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={ariaLabel}
        onClick={() => !disabled && setIsOpen((prev) => !prev)}
        onKeyDown={handleKeyDown}
        className={`w-full ${sizeClasses} border bg-white text-left font-medium flex items-center justify-between gap-2 transition focus:outline-none ${
          isOpen
            ? 'border-primary ring-2 ring-primary/20 shadow-sm'
            : 'border-slate-200 hover:border-indigo-300 shadow-2xs'
        } ${disabled ? 'opacity-50 cursor-not-allowed bg-slate-50' : 'cursor-pointer'}`}
      >
        <div className="flex items-center gap-2 min-w-0 flex-1">
          {selectedOption ? (
            <>
              <span className="text-slate-900 font-semibold truncate">{selectedOption.label}</span>
              {selectedOption.badge && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-primary flex-shrink-0">
                  {selectedOption.badge}
                </span>
              )}
            </>
          ) : (
            <span className="text-slate-500 truncate">{placeholder}</span>
          )}
        </div>
        <ChevronDown
          className={`w-4 h-4 text-slate-400 flex-shrink-0 transition-transform duration-200 ${
            isOpen ? 'rotate-180 text-primary' : ''
          }`}
        />
      </button>

      {isOpen && (
        <div
          role="listbox"
          className="absolute left-0 right-0 mt-1.5 z-50 bg-white/95 backdrop-blur-md rounded-2xl border border-slate-200/90 shadow-xl shadow-indigo-950/10 ring-1 ring-slate-900/5 max-h-72 overflow-y-auto p-1.5 space-y-1 animate-in fade-in duration-150"
        >
          {options.length > 0 && (
            <div className="space-y-0.5">{options.map((opt) => renderOptionButton(opt))}</div>
          )}

          {groups.map((grp, grpIdx) =>
            grp.options.length > 0 ? (
              <div key={grp.label || grpIdx} className="space-y-0.5">
                {grpIdx > 0 || options.length > 0 ? (
                  <div className="my-1 border-t border-slate-100" />
                ) : null}
                <div className="px-3 py-1.5 text-[10px] font-black uppercase tracking-wider text-indigo-600/80 bg-indigo-50/40 rounded-lg">
                  {grp.label}
                </div>
                <div className="space-y-0.5">
                  {grp.options.map((opt) => renderOptionButton(opt))}
                </div>
              </div>
            ) : null
          )}
        </div>
      )}
    </div>
  );
};
