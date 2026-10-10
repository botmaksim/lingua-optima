/**
 * @file taskApi.ts
 * @brief REST client API module for AI task generation, previewing, template saving, and group assignment.
 */

import { axiosInstance } from './axiosInstance';
import { Task, TaskParams, CreateCustomTaskRequest } from '../types/task';

/**
 * @brief Exported const for task api.
 */
export const taskApi = {
  /**
   * @brief Creates and saves or deploys a customized task with explicit questions and answer keys.
   * @param request Custom task definition payload.
   * @return Promise resolving to created Task object.
   */
  createCustomTask: async (request: CreateCustomTaskRequest): Promise<Task> => {
    const res = await axiosInstance.post<Task>('/tasks/custom', request);
    return res.data;
  },

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
   * @brief Forcefully transfers AI generation control to the caller's device.
   * @return Promise resolving to status message.
   */
  takeoverGeneration: async (): Promise<{ success: boolean; message: string }> => {
    const res = await axiosInstance.post<{ success: boolean; message: string }>('/tasks/takeover');
    return res.data;
  },

  /**
   * @brief Inspects current AI generation lock status across user's devices.
   * @return Promise resolving to lock status payload.
   */
  getGenerationStatus: async (): Promise<{ isGenerating: boolean; activeDeviceId?: string; isCurrentDevice: boolean }> => {
    const res = await axiosInstance.get<{ isGenerating: boolean; activeDeviceId?: string; isCurrentDevice: boolean }>('/tasks/generation-status');
    return res.data;
  },

  /**
   * @brief Assigns a task to student groups with optional due date and attempt limit.
   * @param taskId Unique identifier of the task.
   * @param groupIds Array of target group IDs.
   * @param dueDate Optional ISO deadline timestamp.
   * @param maxAttempts Maximum allowed attempts (1 = single attempt default, 0 = unlimited).
   */
  assignTask: async (
    taskId: string,
    groupIds: string[],
    dueDate?: string,
    maxAttempts: number = 1
  ): Promise<void> => {
    await axiosInstance.post(`/tasks/${taskId}/assign`, { groupIds, dueDate, maxAttempts });
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

  /**
   * @brief Uploads custom curriculum reference file (.txt, .md, .json, .csv) to server.
   * @param file File object from file input.
   * @param type Discriminator indicating RULE or VOCABULARY.
   * @param topic Optional associated grammar topic.
   * @return Promise resolving to CurriculumUploadResponse.
   */
  uploadCurriculumFile: async (
    file: File,
    type: 'RULE' | 'VOCABULARY',
    topic?: string
  ): Promise<import('../types/task').CurriculumUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);
    if (topic) formData.append('topic', topic);

    const res = await axiosInstance.post<import('../types/task').CurriculumUploadResponse>(
      '/tasks/curriculum/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return res.data;
  },

  /**
   * @brief Retrieves canonical or synthesized pedagogical reference material for level and topic.
   * @param level Target CEFR level.
   * @param topic Target grammar topic.
   * @return Promise resolving to CurriculumReferenceResponse.
   */
  getCurriculumReference: async (
    level: string,
    topic: string
  ): Promise<import('../types/task').CurriculumReferenceResponse> => {
    const res = await axiosInstance.get<import('../types/task').CurriculumReferenceResponse>(
      '/tasks/curriculum/reference',
      {
        params: { level, topic },
      }
    );
    return res.data;
  },

  /**
   * @brief Fetches hierarchical catalog of standard, mixed, and cross-level topics.
   * @return Promise resolving to TopicsCatalogResponse.
   */
  getTopicsCatalog: async (): Promise<import('../types/task').TopicsCatalogResponse> => {
    const res = await axiosInstance.get<import('../types/task').TopicsCatalogResponse>('/tasks/topics');
    return res.data;
  },
};
