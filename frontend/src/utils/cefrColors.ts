import { CefrLevel } from '../types/user';

/**
 * @file cefrColors.ts
 * @brief Visual design helper providing color classes for CEFR level badges.
 */

/**
 * @brief Resolves Tailwind CSS color badge classes for a given CEFR proficiency level.
 * @param level CEFR level ('B1', 'B2', 'C1').
 * @return String of CSS class names.
 */
export const getCefrBadgeClasses = (level: CefrLevel): string => {
  switch (level) {
    case 'A1':
      return 'bg-teal-100 text-teal-800 border-teal-300 dark:bg-teal-900/40 dark:text-teal-300';
    case 'A2':
      return 'bg-cyan-100 text-cyan-800 border-cyan-300 dark:bg-cyan-900/40 dark:text-cyan-300';
    case 'B1':
      return 'bg-emerald-100 text-emerald-800 border-emerald-300 dark:bg-emerald-900/40 dark:text-emerald-300';
    case 'B2':
      return 'bg-sky-100 text-sky-800 border-sky-300 dark:bg-sky-900/40 dark:text-sky-300';
    case 'C1':
      return 'bg-purple-100 text-purple-800 border-purple-300 dark:bg-purple-900/40 dark:text-purple-300';
    case 'C2':
      return 'bg-amber-100 text-amber-800 border-amber-300 dark:bg-amber-900/40 dark:text-amber-300';
    default:
      return 'bg-slate-100 text-slate-800 border-slate-300';
  }
};
