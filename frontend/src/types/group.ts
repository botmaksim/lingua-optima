/**
 * @file group.ts
 * @brief Types for educational study groups and group membership.
 */

import { User } from './user';

/**
 * @brief Student enrollment within a study group.
 */
export interface GroupStudent {
  /** @brief Property representing id in GroupStudent. */
  id: string;
  /** @brief Property representing student in GroupStudent. */
  student: User;
  /** @brief Property representing is active in GroupStudent. */
  isActive: boolean;
  /** @brief Property representing joined at in GroupStudent. */
  joinedAt: string;
}

/**
 * @brief Study group managed by an educator.
 */
export interface Group {
  /** @brief Property representing id in Group. */
  id: string;
  /** @brief Property representing name in Group. */
  name: string;
  /** @brief Property representing student count in Group. */
  studentCount: number;
  /** @brief Property representing avg score in Group. */
  avgScore?: number;
  /** @brief Property representing students in Group. */
  students?: User[];
  /** @brief Property representing students with pending invitations in Group. */
  pendingStudents?: User[];
  /** @brief Property representing created at in Group. */
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
  /** @brief Property representing name in CreateGroupRequest. */
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

