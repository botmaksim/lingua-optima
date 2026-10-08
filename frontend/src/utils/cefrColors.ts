import { CefrLevel } from '../types/user';

export const getCefrBadgeClasses = (level: CefrLevel): string => {
  switch (level) {
    case 'B1':
      return 'bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-900/40 dark:text-emerald-300';
    case 'B2':
      return 'bg-sky-100 text-sky-800 border-sky-300 dark:bg-sky-900/40 dark:text-sky-300';
    case 'C1':
      return 'bg-purple-100 text-purple-800 border-purple-300 dark:bg-purple-900/40 dark:text-purple-300';
    default:
      return 'bg-slate-100 text-slate-800 border-slate-300';
  }
};
