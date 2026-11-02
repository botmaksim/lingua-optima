import '@testing-library/jest-dom';
import { vi } from 'vitest';
import { axiosInstance } from '../api/axiosInstance';

/**
 * @file setup.ts
 * @brief Vitest testing environment initialization, matchMedia mock, and safe API fallbacks.
 */
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
});

// Provide safe in-memory fallbacks for background lifecycle data fetches in components
const originalGet = axiosInstance.get.bind(axiosInstance);
axiosInstance.get = vi.fn().mockImplementation((url: string, config?: any) => {
  if (typeof url === 'string') {
    if (url.includes('pronunciation-packs')) {
      return Promise.resolve({ data: [] });
    }
    if (url.includes('gaps')) {
      return Promise.resolve({ data: { gaps: [], strengths: [], weaknesses: [] } });
    }
    if (url.includes('/submissions/me') || url.includes('/submissions/my')) {
      return Promise.resolve({ data: [] });
    }
  }
  return originalGet(url, config).catch(() => Promise.resolve({ data: [] }));
});

