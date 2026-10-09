/**
 * @file submissionApi.ts
 * @brief REST client API module for text and image OCR homework submissions, score reviews, and teacher grade overrides.
 */

import { axiosInstance } from './axiosInstance';
import { SubmissionResult, OverrideRequest } from '../types/submission';

export const submissionApi = {
  /**
   * @brief Submits written essay or grammar text for automated AI evaluation.
   * @param data Submission payload containing optional assignment ID, text content, and task type.
   * @return Promise resolving to SubmissionResult with score and rubric feedback.
   */
  submitText: async (data: { assignmentId?: string; text: string; type?: string }): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>('/submissions/text', data);
    return res.data;
  },

  /**
   * @brief Uploads homework photo for zero-retention OCR processing and grading.
   * @param file Image file to process.
   * @param assignmentId Optional assignment identifier.
   * @return Promise resolving to SubmissionResult.
   */
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

  /**
   * @brief Retrieves submission history for the authenticated student.
   * @return Promise resolving to array of SubmissionResult objects.
   */
  getMySubmissions: async (): Promise<SubmissionResult[]> => {
    const res = await axiosInstance.get<SubmissionResult[]>('/submissions/me');
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
