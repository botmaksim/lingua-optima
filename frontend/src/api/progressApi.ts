import { axiosInstance } from './axiosInstance';
import { ProgressRecord } from '../types/progress';
import { User } from '../types/user';

export const progressApi = {
  getMyProgress: async (): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>('/progress/me');
    return res.data;
  },

  getGaps: async (): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>('/progress/gaps');
    return res.data;
  },

  getGroupProgress: async (groupId: string): Promise<ProgressRecord[]> => {
    const res = await axiosInstance.get<ProgressRecord[]>(`/progress/group/${groupId}`);
    return res.data;
  },

  confirmLevelUp: async (): Promise<User> => {
    const res = await axiosInstance.post<User>('/progress/level-up/confirm');
    return res.data;
  },
};
