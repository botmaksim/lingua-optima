/**
 * @file vite.config.ts
 * @brief Vite build, development server proxy, root .env loader, and Vitest test runner configuration.
 */

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

/**
 * @brief Exports the Vite and Vitest configuration object for the Lingua Optima frontend.
 */
export default defineConfig({
  plugins: [react()],
  envDir: path.resolve(__dirname, '..'),
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
});
