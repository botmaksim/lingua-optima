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
  /** @brief Google OAuth2 Client ID for Google Identity Services sign-in. */
  readonly VITE_GOOGLE_CLIENT_ID?: string;
}

/**
 * @brief Augmentation of the standard ImportMeta interface with Vite environment metadata.
 */
interface ImportMeta {
  /** @brief Read-only environment variable map. */
  readonly env: ImportMetaEnv;
}

/**
 * @brief Callback payload returned by Google Identity Services upon successful user authentication.
 */
interface GoogleCredentialResponse {
  /** @brief Signed Google OAuth2 ID token (JWT). */
  credential: string;
}

/**
 * @brief Global Window augmentation for Google Identity Services (GIS) client library.
 */
interface Window {
  /** @brief Optional Google Identity Services namespace injected by accounts.google.com/gsi/client. */
  google?: {
    /** @brief Google accounts API container. */
    accounts?: {
      /** @brief Google Identity Services ID token API. */
      id: {
        /** @brief Initializes the Google ID client with client ID and credential callback. */
        initialize: (config: {
          client_id: string;
          callback: (response: GoogleCredentialResponse) => void;
        }) => void;
        /** @brief Displays the Google One Tap or OAuth2 sign-in prompt. */
        prompt: () => void;
      };
    };
    /** @brief Google Translate element API. */
    translate?: any;
  };
  /** @brief Google translate initialization callback. */
  googleTranslateElementInit?: () => void;
  /** @brief Yandex translate widget namespace. */
  ya?: any;
}

