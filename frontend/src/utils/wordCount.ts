/**
 * @file wordCount.ts
 * @brief Utility calculating word counts for student essay editor constraint validation.
 */

/**
 * @brief Computes word count of a string delimited by whitespace.
 * @param text The input text string.
 * @return Number of words.
 */
export const getWordCount = (text: string): number => {
  if (!text) return 0;
  const trimmed = text.trim();
  if (!trimmed) return 0;
  return trimmed.split(/\s+/).length;
};
