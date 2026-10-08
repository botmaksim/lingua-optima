import { create } from 'zustand';
import { Task } from '../types/task';
import { SessionState, QuestionResponse } from '../types/session';

interface TaskState {
  currentTask: Task | null;
  activeSession: SessionState | null;
  currentQuestion: QuestionResponse | null;
  setCurrentTask: (task: Task | null) => void;
  setActiveSession: (session: SessionState | null) => void;
  setCurrentQuestion: (question: QuestionResponse | null) => void;
}

export const useTaskStore = create<TaskState>((set) => ({
  currentTask: null,
  activeSession: null,
  currentQuestion: null,

  setCurrentTask: (currentTask) => set({ currentTask }),
  setActiveSession: (activeSession) => set({ activeSession }),
  setCurrentQuestion: (currentQuestion) => set({ currentQuestion }),
}));
