/**
 * @file export.ts
 * @brief Types for academic report exports and cohort homework performance previews.
 */

import { CefrLevel } from './user';
import { TaskType } from './task';

/**
 * @brief Summary metrics for an enrolled student in cohort report.
 */
export interface StudentSummaryReport {
  studentId: string;
  studentName: string;
  email: string;
  cefrLevel: string;
  completedTasks: number;
  totalTasks: number;
  averageScore?: number;
  submissionCount: number;
  joinedDate: string;
}

/**
 * @brief Individual student completion record for a specific homework assignment.
 */
export interface StudentHomeworkResult {
  studentId: string;
  studentName: string;
  email: string;
  status: string;
  score?: number;
  totalPoints: number;
  percentage?: number;
  attemptsUsed: number;
  maxAttempts: number;
  submittedAt?: string;
  teacherComment?: string;
  aiFeedback?: string;
}

/**
 * @brief Homework assignment record with student completion breakdown.
 */
export interface HomeworkReport {
  taskId: string;
  grammarTopic: string;
  taskType: TaskType;
  cefrLevel: CefrLevel;
  totalPoints: number;
  dueDate?: string;
  assignedAt: string;
  studentResults: StudentHomeworkResult[];
}

/**
 * @brief Comprehensive cohort performance report data DTO.
 */
export interface GroupReportResponse {
  groupId: string;
  groupName: string;
  teacherName: string;
  generatedAt: string;
  activeStudentCount: number;
  assignedHomeworkCount: number;
  totalSubmissionsCount: number;
  groupAverageScore?: number;
  studentSummaries: StudentSummaryReport[];
  homeworkReports: HomeworkReport[];
}
