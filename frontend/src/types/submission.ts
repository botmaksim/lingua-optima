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
  /** @brief Property representing id in SubmissionResult. */
  id: string;
  /** @brief Property representing assignment id in SubmissionResult. */
  assignmentId?: string;
  /** @brief Property representing student id in SubmissionResult. */
  studentId: string;
  /** @brief Property representing submission type in SubmissionResult. */
  submissionType: SubmissionType;
  /** @brief Property representing original text in SubmissionResult. */
  originalText: string;
  /** @brief Property representing score in SubmissionResult. */
  score: number;
  /** @brief Property representing effective score in SubmissionResult. */
  effectiveScore: number;
  /** @brief Property representing feedback in SubmissionResult. */
  feedback: string;
  /** @brief Property representing rubric in SubmissionResult. */
  rubric?: Record<string, any>;
  /** @brief Property representing override score in SubmissionResult. */
  overrideScore?: number;
  /** @brief Property representing teacher comment in SubmissionResult. */
  teacherComment?: string;
  /** @brief Property representing provider used in SubmissionResult. */
  providerUsed?: string;
  /** @brief Property representing submitted at in SubmissionResult. */
  submittedAt: string;
}

/**
 * @brief Teacher payload to override AI score and provide qualitative feedback.
 */
export interface OverrideRequest {
  /** @brief Property representing override score in OverrideRequest. */
  overrideScore: number;
  /** @brief Property representing teacher comment in OverrideRequest. */
  teacherComment?: string;
}
