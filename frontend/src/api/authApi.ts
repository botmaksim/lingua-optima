import { axiosInstance, setAccessToken } from './axiosInstance';
import { AuthResponse } from '../types/user';

export const authApi = {
  login: async (email: string, password: string):Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/login', { email, password });
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  register: async (email: string, password: string, fullName: string, role: string): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/register', { email, password, fullName, role });
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  refreshToken: async (): Promise<AuthResponse> => {
    const res = await axiosInstance.post<AuthResponse>('/auth/refresh');
    setAccessToken(res.data.accessToken);
    return res.data;
  },

  logout: async (): Promise<void> => {
    try {
      await axiosInstance.post('/auth/logout');
    } finally {
      setAccessToken(null);
    }
  },

  forgotPassword: async (email: string): Promise<void> => {
    await axiosInstance.post('/auth/forgot-password', { email });
  },

  getMe: async () => {
    const res = await axiosInstance.get('/users/me');
    return res.data;
  },
};
