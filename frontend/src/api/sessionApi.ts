/**
 * @file sessionApi.ts
 * @brief REST client API module for Computerized Adaptive Testing (CAT) session execution and answer feedback.
 */

import { axiosInstance } from './axiosInstance';
import { SessionState, QuestionResponse, AnswerRequest, AnswerFeedback } from '../types/session';
import { SubmissionResult } from '../types/submission';

export const sessionApi = {
  /**
   * @brief Starts a new adaptive testing session for the given assignment.
   * @param assignmentId Unique identifier of the task assignment.
   * @return Promise resolving to initialized SessionState.
   */
  startSession: async (assignmentId: string): Promise<SessionState> => {
    const res = await axiosInstance.post<SessionState>('/sessions/start', { assignmentId });
    return res.data;
  },

  /**
   * @brief Queries the currently active session for the authenticated student.
   * @return Promise resolving to SessionState or null if no session is active.
   */
  getActiveSession: async (): Promise<SessionState | null> => {
    try {
      const res = await axiosInstance.get<SessionState>('/sessions/active');
      return res.data;
    } catch {
      return null;
    }
  },

  /**
   * @brief Retrieves next difficulty-matched question for the session.
   * @param sessionId Active session identifier.
   * @return Promise resolving to QuestionResponse.
   */
  getNextQuestion: async (sessionId: string): Promise<QuestionResponse> => {
    const res = await axiosInstance.get<QuestionResponse>(`/sessions/${sessionId}/next-question`);
    return res.data;
  },

  /**
   * @brief Submits student answer to question, adjusting CAT difficulty level.
   * @param sessionId Active session identifier.
   * @param data Answer submission payload.
   * @return Promise resolving to AnswerFeedback.
   */
  submitAnswer: async (sessionId: string, data: AnswerRequest): Promise<AnswerFeedback> => {
    const res = await axiosInstance.post<AnswerFeedback>(`/sessions/${sessionId}/answer`, data);
    return res.data;
  },

  /**
   * @brief Concludes testing session and generates final graded submission.
   * @param sessionId Active session identifier.
   * @return Promise resolving to SubmissionResult.
   */
  completeSession: async (sessionId: string): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>(`/sessions/${sessionId}/complete`);
    return res.data;
  },
};
