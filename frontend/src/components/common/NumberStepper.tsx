/**
 * @file NumberStepper.tsx
 * @brief High-precision, accessible numerical stepper component featuring custom +/- triggers,
 * suppressed native spinners, and smooth boundary clamping.
 */

import React from 'react';
import { Minus, Plus } from 'lucide-react';

export interface NumberStepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  unit?: string;
  size?: 'sm' | 'md';
  disabled?: boolean;
  className?: string;
  'aria-label'?: string;
  ariaLabel?: string;
}

export const NumberStepper: React.FC<NumberStepperProps> = ({
  value,
  onChange,
  min = 0,
  max = 1000,
  step = 1,
  unit,
  size = 'md',
  disabled = false,
  className = '',
  'aria-label': ariaLabelProp,
  ariaLabel,
}) => {
  const resolvedAriaLabel = ariaLabel || ariaLabelProp || 'Number value';
  const handleDecrement = () => {
    if (disabled || value <= min) return;
    onChange(Math.max(min, value - step));
  };

  const handleIncrement = () => {
    if (disabled || value >= max) return;
    onChange(Math.min(max, value + step));
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.trim();
    if (raw === '') {
      onChange(min);
      return;
    }
    const parsed = parseInt(raw, 10);
    if (!isNaN(parsed)) {
      onChange(Math.max(min, Math.min(max, parsed)));
    }
  };

  const isSm = size === 'sm';
  const btnSize = isSm ? 'w-6 h-6' : 'w-7 h-7';
  const iconSize = isSm ? 'w-3 h-3' : 'w-3.5 h-3.5';
  const inputWidth = isSm ? 'w-10' : 'w-12';
  const textSize = isSm ? 'text-[11px]' : 'text-xs';

  return (
    <div
      className={`inline-flex items-center rounded-xl border border-slate-200 bg-slate-50/70 p-1 focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/20 transition shadow-2xs select-none ${
        disabled ? 'opacity-50 pointer-events-none' : ''
      } ${className}`}
    >
      <button
        type="button"
        onClick={handleDecrement}
        disabled={disabled || value <= min}
        aria-label="Decrement value"
        className={`${btnSize} flex items-center justify-center rounded-lg bg-white border border-slate-200/90 text-slate-600 hover:text-slate-900 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition shadow-2xs cursor-pointer active:scale-95`}
      >
        <Minus className={iconSize} />
      </button>

      <div className="flex items-center px-1">
        <input
          type="number"
          min={min}
          max={max}
          step={step}
          value={isNaN(value) ? '' : value}
          onChange={handleInputChange}
          aria-label={resolvedAriaLabel}
          disabled={disabled}
          className={`${inputWidth} ${textSize} text-center font-black text-slate-800 bg-transparent focus:outline-none [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none`}
        />
        {unit && (
          <span className="text-[10px] font-bold text-slate-400 pl-0.5 pointer-events-none">
            {unit}
          </span>
        )}
      </div>

      <button
        type="button"
        onClick={handleIncrement}
        disabled={disabled || value >= max}
        aria-label="Increment value"
        className={`${btnSize} flex items-center justify-center rounded-lg bg-white border border-slate-200/90 text-slate-600 hover:text-slate-900 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed transition shadow-2xs cursor-pointer active:scale-95`}
      >
        <Plus className={iconSize} />
      </button>
    </div>
  );
};
