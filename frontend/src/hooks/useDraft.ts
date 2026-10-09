/**
 * @file useDraft.ts
 * @brief Custom React hook managing local draft persistence with automatic 30-second background save timer.
 */

import { useState, useEffect, useRef } from 'react';
import { saveDraftLocal, getDraftLocal, clearDraftLocal } from '../utils/offlineSync';

/**
 * @brief React hook managing student work-in-progress draft text state.
 * @param draftKey Unique storage key for the document.
 * @param initialContent Initial content if no draft is found.
 * @return State object containing content, save state, and persistence handlers.
 */
export const useDraft = (draftKey: string, initialContent = '') => {
  const [content, setContent] = useState<string>(initialContent);
  const [isSaved, setIsSaved] = useState<boolean>(true);
  const contentRef = useRef<string>(content);
  contentRef.current = content;

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
