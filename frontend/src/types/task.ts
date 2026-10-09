/**
 * @file task.ts
 * @brief Task domain model, question definitions, parameters, and assignment contracts.
 */

import { CefrLevel, User } from './user';

/**
 * @brief Types of pedagogical tasks available.
 */
export type TaskType = 'MCQ' | 'GAP_FILL' | 'REWRITE' | 'ESSAY';

/**
 * @brief Task difficulty categorization.
 */
export type DifficultyLevel = 'EASY' | 'MEDIUM' | 'HARD';

/**
 * @brief Assignment workflow status.
 */
export type AssignmentStatus = 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED';

/**
 * @brief Individual question within a multi-item task.
 */
export interface TaskQuestion {
  id: string;
  questionOrder: number;
  questionText: string;
  correctAnswer: string;
  options: string[];
  difficulty: number;
  grammarRule: string;
}

/**
 * @brief Language learning task or exercise template.
 */
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

/**
 * @brief Parameters for generating or configuring a task.
 */
export interface TaskParams {
  cefrLevel: CefrLevel;
  grammarTopic?: string;
  domain?: string;
  taskType: TaskType;
  difficulty: DifficultyLevel;
  numberOfQuestions?: number;
}

/**
 * @brief Task assignment linking a student to an assigned exercise.
 */
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
