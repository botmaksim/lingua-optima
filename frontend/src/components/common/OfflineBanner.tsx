/**
 * @file OfflineBanner.tsx
 * @brief Floating banner alerting the user to offline connectivity status.
 */

import React from 'react';
import { WifiOff } from 'lucide-react';
import { useOnline } from '../../hooks/useOnline';

/**
 * @brief Displays an informational banner when the browser loses network connection.
 * @return JSX banner element or null if online.
 */
export const OfflineBanner: React.FC = () => {
  const isOnline = useOnline();

  if (isOnline) return null;

  return (
    <div className="bg-amber-500 text-white text-xs font-medium py-1.5 px-4 flex items-center justify-center space-x-2 shadow-inner">
      <WifiOff className="w-3.5 h-3.5 animate-pulse" />
      <span>You are currently offline. Answers & drafts are saved locally and will auto-sync when back online.</span>
    </div>
  );
};
