export type SubmissionType = 'TEXT' | 'IMAGE';

export interface SubmissionResult {
  id: string;
  assignmentId?: string;
  studentId: string;
  submissionType: SubmissionType;
  originalText: string;
  score: number;
  effectiveScore: number;
  feedback: string;
  rubric?: Record<string, any>;
  overrideScore?: number;
  teacherComment?: string;
  providerUsed?: string;
  submittedAt: string;
}

export interface OverrideRequest {
  overrideScore: number;
  teacherComment?: string;
}
