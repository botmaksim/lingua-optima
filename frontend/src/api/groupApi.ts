import { axiosInstance } from './axiosInstance';
import { Group, CreateGroupRequest } from '../types/group';

export const groupApi = {
  getGroups: async (): Promise<Group[]> => {
    const res = await axiosInstance.get<Group[]>('/groups');
    return res.data;
  },

  createGroup: async (data: CreateGroupRequest): Promise<Group> => {
    const res = await axiosInstance.post<Group>('/groups', data);
    return res.data;
  },

  getGroupDetails: async (groupId: string): Promise<Group> => {
    const res = await axiosInstance.get<Group>(`/groups/${groupId}`);
    return res.data;
  },

  addStudent: async (groupId: string, email: string): Promise<void> => {
    await axiosInstance.post(`/groups/${groupId}/students`, { email });
  },

  removeStudent: async (groupId: string, studentId: string): Promise<void> => {
    await axiosInstance.delete(`/groups/${groupId}/students/${studentId}`);
  },

  deleteGroup: async (groupId: string): Promise<void> => {
    await axiosInstance.delete(`/groups/${groupId}`);
  },
};
