/**
 * @file curriculum.ts
 * @brief Types for teacher custom grammar rules and vocabulary sets.
 */

import { CefrLevel } from './user';

export interface CustomCurriculumEntry {
  id: string;
  title: string;
  cefrLevel?: CefrLevel;
  curriculumType: 'RULE' | 'VOCABULARY';
  content: string;
  createdAt: string;
}

export interface CreateCustomCurriculumRequest {
  title: string;
  cefrLevel?: CefrLevel;
  curriculumType: 'RULE' | 'VOCABULARY';
  content: string;
}
