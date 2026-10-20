/**
 * @file group.ts
 * @brief Types for educational study groups and group membership.
 */

import { User } from './user';

/**
 * @brief Student enrollment within a study group.
 */
export interface GroupStudent {
  id: string;
  student: User;
  isActive: boolean;
  joinedAt: string;
}

/**
 * @brief Study group managed by an educator.
 */
export interface Group {
  id: string;
  name: string;
  studentCount: number;
  avgScore?: number;
  students?: User[];
  pendingStudents?: User[];
  createdAt: string;
  /** @brief Indicates if cohort is locked into read-only mode due to tier limits. */
  isLocked?: boolean;
}

/**
 * @brief Pending group invitation for a student.
 */
export interface GroupInvitation {
  id: string;
  groupId: string;
  groupName: string;
  teacherName: string;
  teacherEmail: string;
  createdAt: string;
}

/**
 * @brief Request payload to create a new study group.
 */
export interface CreateGroupRequest {
  name: string;
}

/**
 * @brief Representation of an educational task assigned to a study cohort.
 */
export interface GroupTask {
  taskId: string;
  type: string;
  cefrLevel: string;
  grammarTopic: string;
  domain: string;
  totalPoints: number;
  dueDate?: string;
  maxAttempts?: number;
  assignedStudentCount: number;
  completedStudentCount: number;
  avgScore?: number;
  createdAt: string;
}

/**
 * @brief Request payload for transferring or copying an assigned task between cohorts.
 */
export interface TransferTaskRequest {
  targetGroupId: string;
  keepInSource?: boolean;
  dueDate?: string;
  maxAttempts?: number;
}

