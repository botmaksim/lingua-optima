/**
 * @file session.ts
 * @brief Types for adaptive practice sessions, questions, and answering feedback.
 */

/**
 * @brief Current runtime state of an adaptive exercise session.
 */
export interface SessionState {
  id: string;
  assignmentId: string;
  currentDifficulty: number;
  currentQuestionIndex: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  startedAt: string;
  lastActiveAt: string;
}

/**
 * @brief Individual question presented during an adaptive session.
 */
export interface QuestionResponse {
  id: string;
  questionOrder: number;
  questionText: string;
  options: string[];
  difficulty: number;
  grammarRule: string;
}

/**
 * @brief Payload for submitting an answer in an adaptive session.
 */
export interface AnswerRequest {
  questionId: string;
  answer: string;
}

/**
 * @brief Evaluation result and updated state following an answered question.
 */
export interface AnswerFeedback {
  correct: boolean;
  correctAnswer: string;
  explanation: string;
  newDifficulty: number;
  currentQuestionIndex: number;
  completed: boolean;
  masteryScore?: number;
}
