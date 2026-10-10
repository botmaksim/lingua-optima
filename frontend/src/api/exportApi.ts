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
   * @param from Optional start date-time ISO string.
   * @param to Optional end date-time ISO string.
   * @return Promise resolving to Blob containing binary report data.
   */
  downloadGroupReport: async (
    groupId: string,
    format: 'csv' | 'pdf',
    from?: string,
    to?: string
  ): Promise<Blob> => {
    const res = await axiosInstance.get(`/export/group/${groupId}`, {
      params: { format, from: from || undefined, to: to || undefined },
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
   * @param from Optional start date-time ISO string.
   * @param to Optional end date-time ISO string.
   * @return Promise resolving to GroupReportResponse.
   */
  getGroupReportPreview: async (
    groupId: string,
    from?: string,
    to?: string
  ): Promise<GroupReportResponse> => {
    const res = await axiosInstance.get(`/export/group/${groupId}/preview`, {
      params: { from: from || undefined, to: to || undefined },
    });
    return res.data;
  },
};
