/**
 * @file customCurriculumApi.ts
 * @brief REST client for educator custom grammar rules and vocabulary sets.
 */

import { axiosInstance } from './axiosInstance';
import { CustomCurriculumEntry, CreateCustomCurriculumRequest } from '../types/curriculum';

export const customCurriculumApi = {
  getCustomCurriculum: async (type?: 'RULE' | 'VOCABULARY'): Promise<CustomCurriculumEntry[]> => {
    const params = type ? { type } : undefined;
    const res = await axiosInstance.get<CustomCurriculumEntry[]>('/curriculum/custom', { params });
    return res.data;
  },

  createCustomCurriculum: async (req: CreateCustomCurriculumRequest): Promise<CustomCurriculumEntry> => {
    const res = await axiosInstance.post<CustomCurriculumEntry>('/curriculum/custom', req);
    return res.data;
  },

  deleteCustomCurriculum: async (id: string): Promise<void> => {
    await axiosInstance.delete(`/curriculum/custom/${id}`);
  },
};
