import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  className = '',
  message,
}) => {
  const sizeClass = {
    sm: 'w-4 h-4 border-2',
    md: 'w-8 h-8 border-3',
    lg: 'w-12 h-12 border-4',
  }[size];

  return (
    <div className={`flex flex-col items-center justify-center p-4 ${className}`}>
      <div
        className={`${sizeClass} border-indigo-200 border-t-primary rounded-full animate-spin`}
        role="status"
        aria-label="Loading"
      />
      {message && <p className="mt-3 text-sm text-slate-600 font-medium">{message}</p>}
    </div>
  );
};
