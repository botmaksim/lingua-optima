/**
 * @file offlineSync.ts
 * @brief Offline synchronization and IndexedDB draft storage engine.
 *
 * Provides resilient local storage for in-progress essay drafts and queues offline submissions
 * for automatic replay when network connectivity is re-established.
 */

const DB_NAME = 'LinguaOptimaOfflineDB';
const DB_VERSION = 1;

/**
 * @interface OfflineSubmission
 * @brief Queued homework submission record awaiting network connection.
 */
interface OfflineSubmission {
  id: string;
  assignmentId?: string;
  text: string;
  type: string;
  timestamp: number;
}

/**
 * @interface Draft
 * @brief Draft document record stored locally.
 */
interface Draft {
  key: string;
  content: string;
  updatedAt: number;
}

/**
 * @brief Opens and initializes the IndexedDB storage instance.
 * @return Promise resolving to IDBDatabase instance.
 */
const openDB = (): Promise<IDBDatabase> => {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      return reject(new Error('IndexedDB not supported'));
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains('drafts')) {
        db.createObjectStore('drafts', { keyPath: 'key' });
      }
      if (!db.objectStoreNames.contains('offline_submissions')) {
        db.createObjectStore('offline_submissions', { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
};

/**
 * @brief Saves an in-progress draft locally to IndexedDB with localStorage fallback.
 * @param key Unique draft key identifier.
 * @param content Draft text content.
 */
export const saveDraftLocal = async (key: string, content: string): Promise<void> => {
  try {
    const db = await openDB();
    const tx = db.transaction('drafts', 'readwrite');
    const store = tx.objectStore('drafts');
    const draft: Draft = { key, content, updatedAt: Date.now() };
    store.put(draft);
  } catch (err) {
    localStorage.setItem(`draft_${key}`, content);
  }
};

/**
 * @brief Retrieves a previously saved draft from IndexedDB or localStorage fallback.
 * @param key Unique draft key identifier.
 * @return Promise resolving to draft string or null if absent.
 */
export const getDraftLocal = async (key: string): Promise<string | null> => {
  try {
    const db = await openDB();
    return new Promise((resolve) => {
      const tx = db.transaction('drafts', 'readonly');
      const store = tx.objectStore('drafts');
      const req = store.get(key);
      req.onsuccess = () => {
        if (req.result) {
          resolve(req.result.content);
        } else {
          resolve(localStorage.getItem(`draft_${key}`));
        }
      };
      req.onerror = () => resolve(localStorage.getItem(`draft_${key}`));
    });
  } catch {
    return localStorage.getItem(`draft_${key}`);
  }
};

/**
 * @brief Deletes a saved draft from local storage upon submission or dismissal.
 * @param key Unique draft key identifier.
 */
export const clearDraftLocal = async (key: string): Promise<void> => {
  try {
    const db = await openDB();
    const tx = db.transaction('drafts', 'readwrite');
    tx.objectStore('drafts').delete(key);
  } catch {
  }
  localStorage.removeItem(`draft_${key}`);
};

/**
 * @brief Queues a submission into IndexedDB when client is offline.
 * @param data Submission payload containing optional assignmentId, text, and type.
 */
export const queueOfflineSubmission = async (data: { assignmentId?: string; text: string; type?: string }): Promise<void> => {
  try {
    const db = await openDB();
    const tx = db.transaction('offline_submissions', 'readwrite');
    const item: OfflineSubmission = {
      id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
      assignmentId: data.assignmentId,
      text: data.text,
      type: data.type || 'TEXT',
      timestamp: Date.now(),
    };
    tx.objectStore('offline_submissions').put(item);

    if ('serviceWorker' in navigator && 'SyncManager' in window) {
      const reg = await navigator.serviceWorker.ready;
      /* Background sync registration */
      await (reg as any).sync?.register('sync-submissions');
    }
  } catch (err) {
    console.error('Failed to queue offline submission:', err);
  }
};

/**
 * @brief Replays and transmits all queued offline submissions once network is restored.
 * @param onSubmit Callback handler executing remote API submission.
 */
export const flushOfflineSubmissions = async (
  onSubmit: (sub: OfflineSubmission) => Promise<void>
): Promise<void> => {
  try {
    const db = await openDB();
    const tx = db.transaction('offline_submissions', 'readonly');
    const store = tx.objectStore('offline_submissions');
    const req = store.getAll();

    req.onsuccess = async () => {
      const items: OfflineSubmission[] = req.result || [];
      for (const item of items) {
        try {
          await onSubmit(item);
          const deleteTx = db.transaction('offline_submissions', 'readwrite');
          deleteTx.objectStore('offline_submissions').delete(item.id);
        } catch (err) {
          console.error('Failed to sync item:', item.id, err);
        }
      }
    };
  } catch (err) {
    console.error('Error flushing offline submissions:', err);
  }
};
