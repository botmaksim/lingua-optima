import { axiosInstance } from './axiosInstance';
import { GroupReportResponse } from '../types/export';

/**
 * @file exportApi.ts
 * @brief REST client API module for downloading class and individual student performance reports in CSV/PDF.
 */

export const exportApi = {
  /**
   * @brief Downloads a class cohort report file blob in CSV or PDF format.
   * @param groupId Group identifier.
   * @param format Export format ('csv' | 'pdf').
   * @return Promise resolving to Blob containing binary report data.
   */
  downloadGroupReport: async (groupId: string, format: 'csv' | 'pdf'): Promise<Blob> => {
    const res = await axiosInstance.get(`/export/group/${groupId}`, {
      params: { format },
      responseType: 'blob',
    });
    return res.data;
  },

  /**
   * @brief Downloads an individual student progress report file blob in CSV or PDF format.
   * @param studentId Student user identifier.
   * @param format Export format ('csv' | 'pdf').
   * @return Promise resolving to Blob containing binary report data.
   */
  downloadStudentReport: async (studentId: string, format: 'csv' | 'pdf'): Promise<Blob> => {
    const res = await axiosInstance.get(`/export/student/${studentId}`, {
      params: { format },
      responseType: 'blob',
    });
    return res.data;
  },

  /**
   * @brief Fetches structured cohort report preview data including student roster and homework breakdowns.
   * @param groupId Group identifier.
   * @return Promise resolving to GroupReportResponse.
   */
  getGroupReportPreview: async (groupId: string): Promise<GroupReportResponse> => {
    const res = await axiosInstance.get(`/export/group/${groupId}/preview`);
    return res.data;
  },
};
