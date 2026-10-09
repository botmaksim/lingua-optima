/**
 * @file vite.config.ts
 * @brief Vite build, development server proxy, root .env loader, and Vitest test runner configuration.
 */

import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

/**
 * @brief Exports the Vite and Vitest configuration object for the Lingua Optima frontend.
 */
export default defineConfig(({ mode }) => {
  const rootDir = path.resolve(__dirname, '..');
  const env = loadEnv(mode, rootDir, '');
  const backendPort = env.SERVER_PORT || '8080';
  const proxyTarget = env.VITE_BACKEND_PROXY_TARGET || `http://localhost:${backendPort}`;

  return {
    plugins: [react()],
    envDir: rootDir,
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: Number(env.VITE_DEV_PORT || 3000),
      allowedHosts: true,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
    },
  };
});
