/**
 * @file vite-env.d.ts
 * @brief TypeScript ambient declarations for Vite client environment variables.
 */

/**
 * @brief Typed environment variables injected by Vite at build and runtime.
 */
interface ImportMetaEnv {
  /** @brief Base URL for the Lingua Optima backend API. */
  readonly VITE_API_URL: string;
}

/**
 * @brief Augmentation of the standard ImportMeta interface with Vite environment metadata.
 */
interface ImportMeta {
  /** @brief Read-only environment variable map. */
  readonly env: ImportMetaEnv;
}
