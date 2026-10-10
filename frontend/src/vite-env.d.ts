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
          auto_select?: boolean;
          cancel_on_tap_outside?: boolean;
          context?: string;
        }) => void;
        /** @brief Renders the official Sign In with Google button into the target container. */
        renderButton: (
          parent: HTMLElement,
          options: {
            type?: 'standard' | 'icon';
            theme?: 'outline' | 'filled_blue' | 'filled_black';
            size?: 'large' | 'medium' | 'small';
            text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin';
            shape?: 'rectangular' | 'pill' | 'circle' | 'square';
            logo_alignment?: 'left' | 'center';
            width?: number | string;
            locale?: string;
          }
        ) => void;
        /** @brief Displays the Google One Tap prompt. */
        prompt: (notification?: (notification: any) => void) => void;
      };
    };
    /** @brief Google Translate element API. */
    translate?: any;
  };
  /** @brief Google translate initialization callback. */
  googleTranslateElementInit?: () => void;
}

