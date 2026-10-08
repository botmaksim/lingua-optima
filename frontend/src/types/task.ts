import { CefrLevel, User } from './user';

export type TaskType = 'MCQ' | 'GAP_FILL' | 'REWRITE' | 'ESSAY';
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';
export type AssignmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED';

export interface TaskQuestion {
  id: string;
  questionOrder: number;
  questionText: string;
  correctAnswer: string;
  options: string[];
  difficulty: number;
  grammarRule: string;
}

export interface Task {
  id: string;
  type: TaskType;
  cefrLevel: CefrLevel;
  grammarTopic: string;
  domain: string;
  difficulty: DifficultyLevel;
  content: string;
  answerKey: string;
  questions?: TaskQuestion[];
  isTemplate?: boolean;
  createdBy?: User;
  createdAt: string;
}

export interface TaskParams {
  cefrLevel: CefrLevel;
  grammarTopic?: string;
  domain?: string;
  taskType: TaskType;
  difficulty: DifficultyLevel;
  numberOfQuestions?: number;
}

export interface TaskAssignment {
  id: string;
  taskId: string;
  studentId: string;
  assignedById: string;
  dueDate?: string;
  status: AssignmentStatus;
  task?: Task;
  createdAt: string;
}
