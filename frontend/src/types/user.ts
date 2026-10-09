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
  /** @brief Property representing id in User. */
  id: string;
  /** @brief Property representing email in User. */
  email: string;
  /** @brief Property representing full name in User. */
  fullName: string;
  /** @brief Property representing role in User. */
  role: Role;
  /** @brief Property representing cefr level in User. */
  cefrLevel: CefrLevel;
  /** @brief Property representing streak count in User. */
  streakCount: number;
  /** @brief Property representing last active at in User. */
  lastActiveAt?: string;
  /** @brief Property representing created at in User. */
  createdAt: string;
  /** @brief Property representing level up suggested at in User. */
  levelUpSuggestedAt?: string;
}

/**
 * @brief Authentication payload returned on successful login or registration.
 */
export interface AuthResponse {
  /** @brief Property representing access token in AuthResponse. */
  accessToken: string;
  /** @brief Property representing user in AuthResponse. */
  user: User;
}
