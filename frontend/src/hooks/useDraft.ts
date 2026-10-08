import { useState, useEffect, useRef } from 'react';
import { saveDraftLocal, getDraftLocal, clearDraftLocal } from '../utils/offlineSync';

export const useDraft = (draftKey: string, initialContent = '') => {
  const [content, setContent] = useState<string>(initialContent);
  const [isSaved, setIsSaved] = useState<boolean>(true);
  const contentRef = useRef<string>(content);
  contentRef.current = content;

  // Load draft on mount
  useEffect(() => {
    let isMounted = true;
    getDraftLocal(draftKey).then((saved) => {
      if (isMounted && saved) {
        setContent(saved);
      }
    });
    return () => {
      isMounted = false;
    };
  }, [draftKey]);

  // Auto-save every 30 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      if (contentRef.current) {
        saveDraftLocal(draftKey, contentRef.current);
        setIsSaved(true);
      }
    }, 30000);

    return () => clearInterval(timer);
  }, [draftKey]);

  const updateContent = (newText: string) => {
    setContent(newText);
    setIsSaved(false);
  };

  const saveNow = async () => {
    await saveDraftLocal(draftKey, content);
    setIsSaved(true);
  };

  const clearDraft = async () => {
    await clearDraftLocal(draftKey);
    setContent('');
    setIsSaved(true);
  };

  return {
    content,
    isSaved,
    updateContent,
    saveNow,
    clearDraft,
  };
};
