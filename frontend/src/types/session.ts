/**
 * @file session.ts
 * @brief Types for adaptive practice sessions, questions, and answering feedback.
 */

/**
 * @brief Current runtime state of an adaptive exercise session.
 */
export interface SessionState {
  /** @brief Property representing id in SessionState. */
  id: string;
  /** @brief Property representing assignment id in SessionState. */
  assignmentId: string;
  /** @brief Property representing current difficulty in SessionState. */
  currentDifficulty: number;
  /** @brief Property representing current question index in SessionState. */
  currentQuestionIndex: number;
  /** @brief Property representing status in SessionState. */
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  /** @brief Property representing started at in SessionState. */
  startedAt: string;
  /** @brief Property representing last active at in SessionState. */
  lastActiveAt: string;
}

/**
 * @brief Individual question presented during an adaptive session.
 */
export interface QuestionResponse {
  /** @brief Property representing id in QuestionResponse. */
  id: string;
  /** @brief Property representing question order in QuestionResponse. */
  questionOrder: number;
  /** @brief Property representing question text in QuestionResponse. */
  questionText: string;
  /** @brief Property representing options in QuestionResponse. */
  options: string[];
  /** @brief Property representing difficulty in QuestionResponse. */
  difficulty: number;
  /** @brief Property representing grammar rule in QuestionResponse. */
  grammarRule: string;
}

/**
 * @brief Payload for submitting an answer in an adaptive session.
 */
export interface AnswerRequest {
  /** @brief Property representing question id in AnswerRequest. */
  questionId: string;
  /** @brief Property representing answer in AnswerRequest. */
  answer: string;
}

/**
 * @brief Evaluation result and updated state following an answered question.
 */
export interface AnswerFeedback {
  /** @brief Property representing correct in AnswerFeedback. */
  correct: boolean;
  /** @brief Property representing correct answer in AnswerFeedback. */
  correctAnswer: string;
  /** @brief Property representing explanation in AnswerFeedback. */
  explanation: string;
  /** @brief Property representing new difficulty in AnswerFeedback. */
  newDifficulty: number;
  /** @brief Property representing current question index in AnswerFeedback. */
  currentQuestionIndex: number;
  /** @brief Property representing completed in AnswerFeedback. */
  completed: boolean;
  /** @brief Property representing mastery score in AnswerFeedback. */
  masteryScore?: number;
}
