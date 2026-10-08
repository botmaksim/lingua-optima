import { axiosInstance } from './axiosInstance';

export interface ApiKeyItem {
  id: string;
  provider: 'GROQ' | 'GEMINI' | 'OPENAI' | 'ANTHROPIC';
  createdAt: string;
}

export const apiKeyApi = {
  getKeys: async (): Promise<ApiKeyItem[]> => {
    const res = await axiosInstance.get<ApiKeyItem[]>('/api-keys');
    return res.data;
  },

  saveKey: async (provider: string, rawKey: string): Promise<ApiKeyItem> => {
    const res = await axiosInstance.post<ApiKeyItem>('/api-keys', { provider, rawKey });
    return res.data;
  },

  deleteKey: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/api-keys/${id}`);
  },
};
