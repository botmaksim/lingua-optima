/**
 * @file taskStore.ts
 * @brief Zustand store holding currently selected task, active CAT session, and active question.
 */

import { create } from 'zustand';
import { Task } from '../types/task';
import { SessionState, QuestionResponse } from '../types/session';

/**
 * @interface TaskState
 * @brief State structure for student tasks and interactive sessions.
 */
interface TaskState {
  currentTask: Task | null;
  activeSession: SessionState | null;
  currentQuestion: QuestionResponse | null;
  setCurrentTask: (task: Task | null) => void;
  setActiveSession: (session: SessionState | null) => void;
  setCurrentQuestion: (question: QuestionResponse | null) => void;
}

/**
 * @brief Global task and testing session store hook.
 */
export const useTaskStore = create<TaskState>((set) => ({
  currentTask: null,
  activeSession: null,
  currentQuestion: null,

  setCurrentTask: (currentTask) => set({ currentTask }),
  setActiveSession: (activeSession) => set({ activeSession }),
  setCurrentQuestion: (currentQuestion) => set({ currentQuestion }),
}));
