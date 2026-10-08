import { axiosInstance } from './axiosInstance';
import { SubmissionResult, OverrideRequest } from '../types/submission';

export const submissionApi = {
  submitText: async (data: { assignmentId?: string; text: string; type?: string }): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>('/submissions/text', data);
    return res.data;
  },

  submitImage: async (file: File, assignmentId?: string): Promise<SubmissionResult> => {
    const formData = new FormData();
    formData.append('file', file);
    if (assignmentId) {
      formData.append('assignmentId', assignmentId);
    }
    const res = await axiosInstance.post<SubmissionResult>('/submissions/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  getMySubmissions: async (): Promise<SubmissionResult[]> => {
    const res = await axiosInstance.get<SubmissionResult[]>('/submissions/me');
    return res.data;
  },

  getSubmissionById: async (id: string): Promise<SubmissionResult> => {
    const res = await axiosInstance.get<SubmissionResult>(`/submissions/${id}`);
    return res.data;
  },

  overrideScore: async (id: string, req: OverrideRequest): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>(`/submissions/${id}/override`, req);
    return res.data;
  },
};
