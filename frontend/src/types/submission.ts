/**
 * @file submission.ts
 * @brief Types for essay/assignment submissions, AI evaluations, itemized sentence breakdowns, and teacher grading overrides.
 */

/**
 * @brief Medium format used for task submission.
 */
export type SubmissionType = 'TEXT' | 'IMAGE';

/**
 * @brief Itemized evaluated question or sentence from a task.
 */
export interface SubmissionItem {
  /** @brief 1-based question or sentence order number. */
  questionNumber: number;
  /** @brief Full prompt or sentence text being evaluated. */
  sentence: string;
  /** @brief Student's submitted answer text. */
  studentAnswer: string;
  /** @brief Official expected correct answer. */
  correctAnswer: string;
  /** @brief Flag indicating if the student answer was evaluated as correct. */
  isCorrect: boolean;
  /** @brief Pedagogical explanation of why the answer is correct or incorrect. */
  explanation: string;
  /** @brief Tested grammar rule or linguistic syllabus topic. */
  grammarRule?: string;
}

/**
 * @brief Sentence-level correction with polished model suggestion.
 */
export interface SentenceCorrection {
  /** @brief Erroneous or awkward original student sentence. */
  original: string;
  /** @brief Polished, grammatically accurate suggested rewrite. */
  corrected: string;
  /** @brief Clear explanation of the grammatical or stylistic error. */
  explanation: string;
  /** @brief Grammatical rule category tested. */
  grammarRule?: string;
}

/**
 * @brief Synthesized AI gap analysis highlighting strengths, weaknesses, and next practice steps.
 */
export interface AiAnalysis {
  /** @brief Summary evaluation statement. */
  summary: string;
  /** @brief Specific grammar topics or structures where mistakes occurred. */
  weaknesses: string[];
  /** @brief Concepts successfully demonstrated. */
  strengths: string[];
  /** @brief Actionable personalized pedagogical recommendations. */
  recommendations: string;
  /** @brief Suggested next topics or practice areas to close detected gaps. */
  suggestedTopics: string[];
}

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
  /** @brief Associated task ID if linked. */
  taskId?: string;
  /** @brief Associated task type format (MCQ, GAP_FILL, OPEN_BRACKETS, REWRITE, ESSAY). */
  taskType?: string;
  /** @brief Student-facing reading context passage or assignment topic. */
  taskContent?: string;
  /** @brief Targeted syllabus grammar topic. */
  grammarTopic?: string;
  /** @brief Target CEFR benchmark level. */
  cefrLevel?: string;
  /** @brief Sentence-by-sentence or question-by-question evaluation items. */
  items?: SubmissionItem[];
  /** @brief Sentence-level grammatical and stylistic corrections. */
  corrections?: SentenceCorrection[];
  /** @brief Synthesized AI gap analysis. */
  aiAnalysis?: AiAnalysis;
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
