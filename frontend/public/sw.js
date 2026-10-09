/**
 * @file sw.js
 * @brief Progressive Web App (PWA) Service Worker for offline static asset caching and background submission sync.
 */

/**
 * @brief Versioned cache storage key for static shell assets.
 */
const CACHE_NAME = 'lingua-optima-v1';

/**
 * @brief Core application shell assets pre-cached during Service Worker installation.
 */
const STATIC_ASSETS = [
  '/',
  '/index.html',
  '/favicon.svg',
];

/**
 * @brief Service Worker install event listener pre-caching core static shell assets.
 */
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS);
    })
  );
  self.skipWaiting();
});

/**
 * @brief Service Worker activate event listener purging obsolete cache versions.
 */
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      );
    })
  );
  self.clients.claim();
});

/**
 * @brief Service Worker fetch interceptor applying Network-First for API routes and Cache-First for static assets.
 */
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request).catch(() => {
        return new Response(
          JSON.stringify({ error: 'You are offline. Submission queued for background sync.' }),
          {
            status: 503,
            headers: { 'Content-Type': 'application/json' },
          }
        );
      })
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) {
        return cached;
      }
      return fetch(event.request).then((response) => {
        if (response && response.status === 200 && response.type === 'basic') {
          const responseToCache = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return response;
      });
    })
  );
});

/**
 * @brief Background Sync event listener notifying active clients to flush offline submission queues.
 */
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-submissions') {
    event.waitUntil(
      self.clients.matchAll().then((clients) => {
        clients.forEach((client) => {
          client.postMessage({ type: 'TRIGGER_BACKGROUND_SYNC' });
        });
      })
    );
  }
});
