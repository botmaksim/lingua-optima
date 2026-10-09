import { axiosInstance } from './axiosInstance';

/**
 * @file apiKeyApi.ts
 * @brief REST client API module for Bring-Your-Own-Key (BYOK) AI provider credential management.
 */

/**
 * @interface ApiKeyItem
 * @brief Metadata representation of a registered user API key.
 */
export interface ApiKeyItem {
  id: string;
  provider: 'GROQ' | 'GEMINI' | 'OPENAI' | 'ANTHROPIC' | 'DEEPSEEK' | 'QWEN' | 'KIMI';
  createdAt: string;
}

/**
 * @brief Exported const for api key api.
 */
export const apiKeyApi = {
  /**
   * @brief Retrieves all registered custom API keys for current user.
   * @return Promise resolving to array of ApiKeyItem objects.
   */
  getKeys: async (): Promise<ApiKeyItem[]> => {
    const res = await axiosInstance.get<ApiKeyItem[]>('/api-keys');
    return res.data;
  },

  /**
   * @brief Encrypts and saves a new custom API key on the backend.
   * @param provider Provider identifier ('GROQ', 'GEMINI', 'OPENAI', 'ANTHROPIC').
   * @param rawKey Plaintext API key string.
   * @return Promise resolving to saved ApiKeyItem.
   */
  saveKey: async (provider: string, rawKey: string): Promise<ApiKeyItem> => {
    const res = await axiosInstance.post<ApiKeyItem>('/api-keys', { provider, rawKey });
    return res.data;
  },

  /**
   * @brief Deletes a registered API key.
   * @param id API key identifier.
   */
  deleteKey: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/api-keys/${id}`);
  },
};
