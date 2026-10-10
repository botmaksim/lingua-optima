/**
 * @file submissionApi.ts
 * @brief REST client API module for text and image OCR homework submissions, score reviews, and teacher grade overrides.
 */

import { axiosInstance } from './axiosInstance';
import { SubmissionResult, OverrideRequest } from '../types/submission';

/**
 * @brief Exported const for submission api.
 */
export const submissionApi = {
  /**
   * @brief Submits written essay or grammar text for automated AI evaluation.
   * @param data Submission payload containing optional assignment ID, text content, and task type.
   * @return Promise resolving to SubmissionResult with score and rubric feedback.
   */
  submitText: async (data: { assignmentId?: string; taskId?: string; text: string; type?: string }): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>('/submissions/text', data);
    return res.data;
  },

  /**
   * @brief Uploads one or multiple homework photos for zero-retention OCR processing and grading.
   * @param files Image file or array of image files to process.
   * @param assignmentId Optional assignment identifier.
   * @return Promise resolving to SubmissionResult.
   */
  submitImage: async (files: File | File[], assignmentId?: string): Promise<SubmissionResult> => {
    const formData = new FormData();
    const fileList = Array.isArray(files) ? files : [files];
    fileList.forEach((file) => {
      formData.append('files', file);
    });
    if (fileList.length > 0) {
      formData.append('file', fileList[0]);
    }
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

  /**
   * @brief Retrieves submission history for the authenticated student.
   * @return Promise resolving to array of SubmissionResult objects.
   */
  getMySubmissions: async (): Promise<SubmissionResult[]> => {
    const res = await axiosInstance.get<SubmissionResult[]>('/submissions/me');
    return res.data;
  },

  /**
   * @brief Retrieves student submissions across all cohorts and assignments managed by the authenticated educator.
   * Strictly excludes the educator's own personal practice submissions.
   * @return Promise resolving to array of student SubmissionResult objects.
   */
  getTeacherSubmissions: async (): Promise<SubmissionResult[]> => {
    const res = await axiosInstance.get<SubmissionResult[]>('/submissions/teacher');
    return res.data;
  },

  /**
   * @brief Retrieves detailed evaluation result for a single submission.
   * @param id Submission identifier.
   * @return Promise resolving to SubmissionResult.
   */
  getSubmissionById: async (id: string): Promise<SubmissionResult> => {
    const res = await axiosInstance.get<SubmissionResult>(`/submissions/${id}`);
    return res.data;
  },

  /**
   * @brief Submits teacher score override and comment for a submission.
   * @param id Submission identifier.
   * @param req OverrideRequest payload containing revised score and feedback comment.
   * @return Promise resolving to updated SubmissionResult.
   */
  overrideScore: async (id: string, req: OverrideRequest): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>(`/submissions/${id}/override`, req);
    return res.data;
  },
};
