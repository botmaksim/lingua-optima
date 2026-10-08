export interface SessionState {
  id: string;
  assignmentId: string;
  currentDifficulty: number;
  currentQuestionIndex: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';
  startedAt: string;
  lastActiveAt: string;
}

export interface QuestionResponse {
  id: string;
  questionOrder: number;
  questionText: string;
  options: string[];
  difficulty: number;
  grammarRule: string;
}

export interface AnswerRequest {
  questionId: string;
  answer: string;
}

export interface AnswerFeedback {
  correct: boolean;
  correctAnswer: string;
  explanation: string;
  newDifficulty: number;
  currentQuestionIndex: number;
  completed: boolean;
  masteryScore?: number;
}
