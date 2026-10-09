/**
 * @file submission.ts
 * @brief Types for essay/assignment submissions, AI evaluations, and teacher grading overrides.
 */

/**
 * @brief Medium format used for task submission.
 */
export type SubmissionType = 'TEXT' | 'IMAGE';

/**
 * @brief Completed evaluation record for a student submission.
 */
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

/**
 * @brief Teacher payload to override AI score and provide qualitative feedback.
 */
export interface OverrideRequest {
  overrideScore: number;
  teacherComment?: string;
}
