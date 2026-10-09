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
  /** @brief Property representing id in TaskQuestion. */
  id: string;
  /** @brief Property representing question order in TaskQuestion. */
  questionOrder: number;
  /** @brief Property representing question text in TaskQuestion. */
  questionText: string;
  /** @brief Property representing correct answer in TaskQuestion. */
  correctAnswer: string;
  /** @brief Property representing options in TaskQuestion. */
  options: string[];
  /** @brief Property representing difficulty in TaskQuestion. */
  difficulty: number;
  /** @brief Property representing grammar rule in TaskQuestion. */
  grammarRule: string;
}

/**
 * @brief Language learning task or exercise template.
 */
export interface Task {
  /** @brief Property representing id in Task. */
  id: string;
  /** @brief Property representing type in Task. */
  type: TaskType;
  /** @brief Property representing cefr level in Task. */
  cefrLevel: CefrLevel;
  /** @brief Property representing grammar topic in Task. */
  grammarTopic: string;
  /** @brief Property representing domain in Task. */
  domain: string;
  /** @brief Property representing difficulty in Task. */
  difficulty: DifficultyLevel;
  /** @brief Property representing content in Task. */
  content: string;
  /** @brief Property representing answer key in Task. */
  answerKey: string;
  /** @brief Property representing questions in Task. */
  questions?: TaskQuestion[];
  /** @brief Property representing is template in Task. */
  isTemplate?: boolean;
  /** @brief Property representing created by in Task. */
  createdBy?: User;
  /** @brief Property representing created at in Task. */
  createdAt: string;
}

/**
 * @brief Parameters for generating or configuring a task.
 */
export interface TaskParams {
  /** @brief Property representing cefr level in TaskParams. */
  cefrLevel: CefrLevel;
  /** @brief Property representing grammar topic in TaskParams. */
  grammarTopic?: string;
  /** @brief Property representing domain in TaskParams. */
  domain?: string;
  /** @brief Property representing task type in TaskParams. */
  taskType: TaskType;
  /** @brief Property representing difficulty in TaskParams. */
  difficulty: DifficultyLevel;
  /** @brief Property representing number of questions in TaskParams. */
  numberOfQuestions?: number;
  /** @brief Property representing preferred AI provider in TaskParams. */
  provider?: string;
  /** @brief Property representing preferred AI model identifier in TaskParams. */
  modelName?: string;
  /** @brief Optional custom grammar rule text to guide AI generation. */
  customRule?: string;
  /** @brief Optional target vocabulary words or collocations to enforce. */
  customVocabulary?: string;
  /** @brief Server relative path of uploaded custom rule file. */
  ruleFilePath?: string;
  /** @brief Server relative path of uploaded custom vocabulary file. */
  vocabularyFilePath?: string;
  /** @brief Optional Eco Mode toggle to skip verbose rule and vocabulary prompts and save tokens. */
  ecoMode?: boolean;
}

/**
 * @brief Discriminator for uploaded curriculum files.
 */
export type CurriculumFileType = 'RULE' | 'VOCABULARY';

/**
 * @brief Response payload returned after uploading a curriculum file.
 */
export interface CurriculumUploadResponse {
  fileName: string;
  fileType: CurriculumFileType;
  fileSize: number;
  contentSnippet: string;
  fullContent: string;
  serverPath: string;
  topic?: string;
}

/**
 * @brief Reference grammar rule and vocabulary response for a level/topic.
 */
export interface CurriculumReferenceResponse {
  cefrLevel: string;
  grammarTopic: string;
  referenceRule: string;
  referenceVocabulary: string[];
  source: 'CANONICAL' | 'SYNTHESIZED';
}

/**
 * @brief Topics catalog response containing standard, mixed, and cross-level topics.
 */
export interface TopicsCatalogResponse {
  topicsByLevel: Record<string, string[]>;
  mixedTopicsByLevel: Record<string, string[]>;
  crossLevelTopics: string[];
  domains: string[];
}

/**
 * @brief Task assignment linking a student to an assigned exercise.
 */
export interface TaskAssignment {
  /** @brief Property representing id in TaskAssignment. */
  id: string;
  /** @brief Property representing task id in TaskAssignment. */
  taskId: string;
  /** @brief Property representing student id in TaskAssignment. */
  studentId: string;
  /** @brief Property representing assigned by id in TaskAssignment. */
  assignedById: string;
  /** @brief Property representing due date in TaskAssignment. */
  dueDate?: string;
  /** @brief Property representing status in TaskAssignment. */
  status: AssignmentStatus;
  /** @brief Property representing task in TaskAssignment. */
  task?: Task;
  /** @brief Property representing created at in TaskAssignment. */
  createdAt: string;
}
