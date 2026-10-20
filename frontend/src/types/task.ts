/**
 * @file task.ts
 * @brief Task domain model, question definitions, parameters, and assignment contracts.
 */

import { CefrLevel, User } from './user';
export type { CefrLevel };

/**
 * @brief Types of pedagogical tasks available.
 */
export type TaskType = 'MCQ' | 'GAP_FILL' | 'OPEN_BRACKETS' | 'REWRITE' | 'ESSAY' | 'SHORT_ANSWER' | 'READING' | 'SPEAKING';

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
  points?: number;
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
  totalPoints?: number;
  content: string;
  answerKey: string;
  questions?: TaskQuestion[];
  isTemplate?: boolean;
  createdBy?: User;
  createdAt: string;
  /** @brief Optional task assignment ID for the student. */
  assignmentId?: string;
  /** @brief Educator full name if assigned by a teacher. */
  assignedByName?: string;
  /** @brief Optional assignment due date. */
  dueDate?: string;
  /** @brief Current status of the student's assignment. */
  assignmentStatus?: AssignmentStatus;
  /** @brief Maximum allowed attempts (1 = single attempt default, 0 = unlimited). */
  maxAttempts?: number;
  /** @brief Number of submission attempts already used by the student. */
  attemptsUsed?: number;
  /** @brief Latest submission ID for direct review navigation. */
  latestSubmissionId?: string;
  /** @brief Flag indicating whether the student can still submit an attempt. */
  canSubmit?: boolean;
  /** @brief Target phonemes or contrastive sound pairs for oral speaking practice. */
  targetSounds?: string;
  /** @brief Passage length preset for speaking exercises. */
  speakingLength?: string;
  /** @brief Flag indicating whether this task was assigned by an educator. */
  isTeacherTask?: boolean;
  /** @brief Custom title or drill topic for speaking tasks. */
  speakingTitle?: string;
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
  totalPoints?: number;
  provider?: string;
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
  /** @brief Optional target cohort group IDs for direct assignment upon generation. */
  groupIds?: string[];
  /** @brief Optional submission due date when deploying directly to cohorts. */
  dueDate?: string;
  /** @brief Optional maximum allowed attempts when deploying directly to cohorts. */
  maxAttempts?: number;
  /** @brief Optional reading assignment title. */
  readingTitle?: string;
  /** @brief Optional reading passage length or volume preset (e.g. SHORT, MEDIUM, LONG). */
  readingLength?: string;
  /** @brief Optional description of paragraph count or structure for the reading passage. */
  readingParagraphs?: string;
  /** @brief Optional count of multiple choice questions to generate for reading tasks. */
  mcqQuestionsCount?: number;
  /** @brief Optional points allocated per multiple choice question in reading tasks. */
  mcqPoints?: number;
  /** @brief Optional count of True/False questions to generate for reading tasks. */
  trueFalseQuestionsCount?: number;
  /** @brief Optional points allocated per True/False question in reading tasks. */
  trueFalsePoints?: number;
  /** @brief Optional count of sentence completion / open questions to generate for reading tasks. */
  openQuestionsCount?: number;
  /** @brief Optional points allocated per sentence completion / open question in reading tasks. */
  openPoints?: number;
  /** @brief Optional speaking and pronunciation assignment title. */
  speakingTitle?: string;
  /** @brief Optional target sounds or phonemes (e.g. /θ/ vs /ð/, /r/ vs /l/). */
  targetSounds?: string;
  /** @brief Optional speaking passage length preset (SHORT, MEDIUM, LONG, CUSTOM). */
  speakingLength?: string;
  /** @brief Optional ID of pre-calibrated pronunciation pack from phonetics/pronunciation_packs.json. */
  pronunciationPackId?: string;
}

/**
 * @brief Phonetic minimal pair contrast.
 */
export interface MinimalPair {
  word1: string;
  word2: string;
  phoneme1: string;
  phoneme2: string;
}

/**
 * @brief Pre-calibrated CEFR pronunciation drill pack loaded from phonetics JSON resource.
 */
export interface PronunciationPack {
  id: string;
  cefrLevel: CefrLevel;
  category: string;
  title: string;
  phonemes: string[];
  drillWords: string[];
  minimalPairs: MinimalPair[];
  contrastFocus: string;
  tongueTwister: string;
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
  id: string;
  taskId: string;
  studentId: string;
  assignedById: string;
  dueDate?: string;
  status: AssignmentStatus;
  task?: Task;
  createdAt: string;
}

/**
 * @brief Question payload for custom task creation or preview deployment.
 */
export interface CustomQuestionRequest {
  questionOrder?: number;
  questionText: string;
  correctAnswer: string;
  options?: string[];
  difficulty?: number;
  points?: number;
  grammarRule?: string;
}

/**
 * @brief Request payload to persist a customized or reviewed task.
 */
export interface CreateCustomTaskRequest {
  cefrLevel: CefrLevel;
  grammarTopic: string;
  domain?: string;
  taskType: TaskType;
  difficulty?: DifficultyLevel;
  totalPoints?: number;
  content: string;
  questions: CustomQuestionRequest[];
  isTemplate?: boolean;
  groupIds?: string[];
  dueDate?: string;
  maxAttempts?: number;
}

/**
 * @brief Request payload to explain a sentence or phrase with AI.
 */
export interface ExplainSentenceRequest {
  sentence: string;
  context?: string;
  cefrLevel?: CefrLevel;
}

/**
 * @brief Structured response containing pedagogical sentence breakdown and translation.
 */
export interface ExplainSentenceResponse {
  originalSentence: string;
  translation: string;
  grammarBreakdown: string;
  keyVocabulary: string[];
  explanation: string;
}
