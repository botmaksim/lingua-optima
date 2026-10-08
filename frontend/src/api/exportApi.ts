import { axiosInstance } from './axiosInstance';

export const exportApi = {
  downloadGroupReport: async (groupId: string, format: 'csv' | 'pdf'): Promise<Blob> => {
    const res = await axiosInstance.get(`/export/group/${groupId}`, {
      params: { format },
      responseType: 'blob',
    });
    return res.data;
  },

  downloadStudentReport: async (studentId: string, format: 'csv' | 'pdf'): Promise<Blob> => {
    const res = await axiosInstance.get(`/export/student/${studentId}`, {
      params: { format },
      responseType: 'blob',
    });
    return res.data;
  },
};
