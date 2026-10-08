import { axiosInstance } from './axiosInstance';
import { Task, TaskParams } from '../types/task';

export const taskApi = {
  generateTask: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/generate', params);
    return res.data;
  },

  previewTask: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/preview', params);
    return res.data;
  },

  saveTemplate: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/template', params);
    return res.data;
  },

  assignTask: async (taskId: string, groupIds: string[], dueDate?: string): Promise<void> => {
    await axiosInstance.post(`/tasks/${taskId}/assign`, { groupIds, dueDate });
  },

  getTasks: async (): Promise<Task[]> => {
    const res = await axiosInstance.get<Task[]>('/tasks');
    return res.data;
  },

  getTaskById: async (taskId: string): Promise<Task> => {
    const res = await axiosInstance.get<Task>(`/tasks/${taskId}`);
    return res.data;
  },
};
