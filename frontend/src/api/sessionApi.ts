import { axiosInstance } from './axiosInstance';
import { SessionState, QuestionResponse, AnswerRequest, AnswerFeedback } from '../types/session';
import { SubmissionResult } from '../types/submission';

export const sessionApi = {
  startSession: async (assignmentId: string): Promise<SessionState> => {
    const res = await axiosInstance.post<SessionState>('/sessions/start', { assignmentId });
    return res.data;
  },

  getActiveSession: async (): Promise<SessionState | null> => {
    try {
      const res = await axiosInstance.get<SessionState>('/sessions/active');
      return res.data;
    } catch {
      return null;
    }
  },

  getNextQuestion: async (sessionId: string): Promise<QuestionResponse> => {
    const res = await axiosInstance.get<QuestionResponse>(`/sessions/${sessionId}/next-question`);
    return res.data;
  },

  submitAnswer: async (sessionId: string, data: AnswerRequest): Promise<AnswerFeedback> => {
    const res = await axiosInstance.post<AnswerFeedback>(`/sessions/${sessionId}/answer`, data);
    return res.data;
  },

  completeSession: async (sessionId: string): Promise<SubmissionResult> => {
    const res = await axiosInstance.post<SubmissionResult>(`/sessions/${sessionId}/complete`);
    return res.data;
  },
};
