import { useState, useEffect } from 'react';
import { flushOfflineSubmissions } from '../utils/offlineSync';
import { submissionApi } from '../api/submissionApi';

export const useOnline = () => {
  const [isOnline, setIsOnline] = useState<boolean>(
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      // Auto flush pending submissions
      flushOfflineSubmissions(async (sub) => {
        await submissionApi.submitText({
          assignmentId: sub.assignmentId,
          text: sub.text,
          type: sub.type,
        });
      });
    };

    const handleOffline = () => {
      setIsOnline(false);
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Also listen to Service Worker background sync trigger message
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
