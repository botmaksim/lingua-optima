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
  createdAt: string;
}

/**
 * @brief Request payload to create a new study group.
 */
export interface CreateGroupRequest {
  name: string;
}
