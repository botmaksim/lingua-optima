import React from 'react';
import { CefrLevel } from '../../types/user';
import { getCefrBadgeClasses } from '../../utils/cefrColors';

interface CefrBadgeProps {
  level: CefrLevel;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const CefrBadge: React.FC<CefrBadgeProps> = ({ level, size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-sm px-2.5 py-1',
    lg: 'text-base px-3.5 py-1.5 font-semibold',
  }[size];

  return (
    <span
      className={`inline-flex items-center justify-center font-mono rounded-full border shadow-sm ${getCefrBadgeClasses(
        level
      )} ${sizeClasses} ${className}`}
    >
      {level}
    </span>
  );
};
