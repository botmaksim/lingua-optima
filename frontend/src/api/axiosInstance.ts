import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

/**
 * @file axiosInstance.ts
 * @brief Configured Axios instance with in-memory JWT storage, request interceptor, and silent refresh queue.
 *
 * Implements strict security rule: access tokens are stored strictly in JS memory (never localStorage/sessionStorage)
 * to prevent XSS credential harvesting. Refresh tokens are exchanged securely via HttpOnly cookies.
 */

/**
 * @brief In-memory variable holding the active JWT access token.
 */
let inMemoryAccessToken: string | null = null;

/**
 * @brief Sets the in-memory access token string.
 * @param token The active access token or null to clear.
 */
export const setAccessToken = (token: string | null) => {
  inMemoryAccessToken = token;
};

/**
 * @brief Retrieves the current in-memory access token.
 * @return In-memory token string or null if unauthenticated.
 */
export const getAccessToken = () => inMemoryAccessToken;

/**
 * @brief Resolved API base URL ensuring the /api prefix is present.
 */
const rawBaseUrl = (import.meta.env.VITE_API_URL || '').trim();
const baseURL = !rawBaseUrl
  ? '/api'
  : rawBaseUrl.endsWith('/api')
  ? rawBaseUrl
  : `${rawBaseUrl.replace(/\/+$/, '')}/api`;

/**
 * @brief Pre-configured Axios instance for Lingua Optima REST API communication.
 */
export const axiosInstance = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * @brief Retrieves or initializes a unique client device identifier persisted in localStorage.
 */
export const getDeviceId = (): string => {
  try {
    let id = localStorage.getItem('lingua_device_id');
    if (!id) {
      id = typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : 'dev-' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36);
      localStorage.setItem('lingua_device_id', id);
    }
    return id;
  } catch {
    return 'fallback-browser-device';
  }
};

axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (config.headers) {
      if (inMemoryAccessToken) {
        config.headers.Authorization = `Bearer ${inMemoryAccessToken}`;
      }
      config.headers['X-Device-Id'] = getDeviceId();
      if (!config.headers['X-Request-Id']) {
        config.headers['X-Request-Id'] = typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : 'req-' + Math.random().toString(36).substring(2, 12);
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * @brief Flag indicating whether a silent token refresh is currently in flight.
 */
let isRefreshing = false;

/**
 * @brief Queue of pending requests awaiting completion of the active token refresh.
 */
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

/**
 * @brief Processes queued HTTP requests waiting for an in-flight silent token refresh.
 * @param error Error if refresh failed, null if succeeded.
 * @param token New access token if refreshed, null otherwise.
 */
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else if (token) {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`;
            }
            return axiosInstance(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshResponse = await axios.post(
          `${baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        const newAccessToken = refreshResponse.data.accessToken;
        setAccessToken(newAccessToken);
        processQueue(null, newAccessToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        }
        return axiosInstance(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        setAccessToken(null);
        window.dispatchEvent(new CustomEvent('auth:expired'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
