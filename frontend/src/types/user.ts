/**
 * @file user.ts
 * @brief User domain model, roles, CEFR levels, and authentication response types.
 */

/**
 * @brief User roles in the LinguaOptima platform.
 */
export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';

/**
 * @brief CEFR language proficiency levels supported by the platform.
 */
export type CefrLevel = 'B1' | 'B2' | 'C1';

/**
 * @brief Represents a registered user in the application.
 */
export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  cefrLevel: CefrLevel;
  streakCount: number;
  lastActiveAt?: string;
  createdAt: string;
  levelUpSuggestedAt?: string;
}

/**
 * @brief Authentication payload returned on successful login or registration.
 */
export interface AuthResponse {
  accessToken: string;
  user: User;
}
