export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';
export type CefrLevel = 'B1' | 'B2' | 'C1';

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

export interface AuthResponse {
  accessToken: string;
  user: User;
}
