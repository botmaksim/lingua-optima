/**
 * @file taskApi.ts
 * @brief REST client API module for AI task generation, previewing, template saving, and group assignment.
 */

import { axiosInstance } from './axiosInstance';
import { Task, TaskParams } from '../types/task';

export const taskApi = {
  /**
   * @brief Dispatches request to generate an AI exercise and saves it.
   * @param params Task generation parameters.
   * @return Promise resolving to generated Task object.
   */
  generateTask: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/generate', params);
    return res.data;
  },

  /**
   * @brief Generates transient preview of task without persistence.
   * @param params Task generation parameters.
   * @return Promise resolving to preview Task object.
   */
  previewTask: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/preview', params);
    return res.data;
  },

  /**
   * @brief Generates and saves task as an educator template.
   * @param params Task generation parameters.
   * @return Promise resolving to saved template Task.
   */
  saveTemplate: async (params: TaskParams): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/template', params);
    return res.data;
  },

  /**
   * @brief Assigns a task to student groups with optional due date.
   * @param taskId Unique identifier of the task.
   * @param groupIds Array of target group IDs.
   * @param dueDate Optional ISO deadline timestamp.
   */
  assignTask: async (taskId: string, groupIds: string[], dueDate?: string): Promise<void> => {
    await axiosInstance.post(`/tasks/${taskId}/assign`, { groupIds, dueDate });
  },

  /**
   * @brief Retrieves all accessible tasks for the current user.
   * @return Promise resolving to array of tasks.
   */
  getTasks: async (): Promise<Task[]> => {
    const res = await axiosInstance.get<Task[]>('/tasks');
    return res.data;
  },

  /**
   * @brief Fetches full task details by task ID.
   * @param taskId Unique identifier of the task.
   * @return Promise resolving to Task object.
   */
  getTaskById: async (taskId: string): Promise<Task> => {
    const res = await axiosInstance.get<Task>(`/tasks/${taskId}`);
    return res.data;
  },
};
