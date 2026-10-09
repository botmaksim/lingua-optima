/**
 * @file useOnline.ts
 * @brief Custom React hook monitoring client online/offline network connectivity.
 *
 * Automatically triggers flushing of queued offline homework submissions when connectivity resumes.
 */

import { useState, useEffect } from 'react';
import { flushOfflineSubmissions } from '../utils/offlineSync';
import { submissionApi } from '../api/submissionApi';

/**
 * @brief React hook tracking network availability.
 * @return True if browser reports online connectivity, false if offline.
 */
export const useOnline = () => {
  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );

  useEffect(() => {
    /**
     * @brief Event handler or helper executing handle online.
     */
    const handleOnline = () => {
      setIsOnline(true);
      flushOfflineSubmissions(async (sub) => {
        await submissionApi.submitText({
          assignmentId: sub.assignmentId,
          text: sub.text,
          type: sub.type,
        });
      });
    };

    /**
     * @brief Event handler or helper executing handle offline.
     */
    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', (event) => {
        if (event.data?.type === 'TRIGGER_BACKGROUND_SYNC') {
          handleOnline();
        }
      });
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  return isOnline;
};
