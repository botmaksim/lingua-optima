/**
 * @file groupApi.ts
 * @brief REST client API module for educator student cohort management, enrollment, and soft deletion.
 */

import { axiosInstance } from './axiosInstance';
import { Group, CreateGroupRequest } from '../types/group';

export const groupApi = {
  /**
   * @brief Retrieves all groups owned by the educator.
   * @return Promise resolving to array of Group objects.
   */
  getGroups: async (): Promise<Group[]> => {
    const res = await axiosInstance.get<Group[]>('/groups');
    return res.data;
  },

  /**
   * @brief Creates a new student group cohort.
   * @param data Payload containing group name.
   * @return Promise resolving to created Group.
   */
  createGroup: async (data: CreateGroupRequest): Promise<Group> => {
    const res = await axiosInstance.post<Group>('/groups', data);
    return res.data;
  },

  /**
   * @brief Retrieves detailed group roster and performance metrics.
   * @param groupId Group identifier.
   * @return Promise resolving to Group object.
   */
  getGroupDetails: async (groupId: string): Promise<Group> => {
    const res = await axiosInstance.get<Group>(`/groups/${groupId}`);
    return res.data;
  },

  /**
   * @brief Enrolls a student into a group by email address.
   * @param groupId Target group identifier.
   * @param email Student email.
   */
  addStudent: async (groupId: string, email: string): Promise<void> => {
    await axiosInstance.post(`/groups/${groupId}/students`, { email });
  },

  /**
   * @brief Soft-deletes a student membership from a group cohort.
   * @param groupId Group identifier.
   * @param studentId Student user identifier.
   */
  removeStudent: async (groupId: string, studentId: string): Promise<void> => {
    await axiosInstance.delete(`/groups/${groupId}/students/${studentId}`);
  },

  /**
   * @brief Deletes an entire group cohort.
   * @param groupId Group identifier.
   */
  deleteGroup: async (groupId: string): Promise<void> => {
    await axiosInstance.delete(`/groups/${groupId}`);
  },
};
