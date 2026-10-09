/**
 * @file textSanitizer.ts
 * @brief Utility for stripping LLM prompt leaks, meta-instructions, and system parameters from user-facing text.
 */

const PROMPT_LEAK_PATTERNS = [
  /generate an english/i,
  /cefr level:/i,
  /grammar topic:/i,
  /domain\/context:/i,
  /task type:/i,
  /return only a valid json/i,
  /critical: do not echo/i,
  /number of questions:/i,
  /system parameters/i,
  /overall instructions or context passage/i,
  /evaluate the following english essay/i,
  /compare the student's text/i,
  /official answer key/i,
  /cambridge\/ielts english examiner/i,
  /0-10 scale/i,
];

/**
 * @brief Checks if a string contains leaked AI system prompt text or instructions.
 * @param text The text to evaluate.
 * @return True if prompt leak patterns are detected.
 */
export function containsPromptLeak(text?: string | null): boolean {
  if (!text) return false;
  return PROMPT_LEAK_PATTERNS.some((pattern) => pattern.test(text));
}

/**
 * @brief Sanitizes educational task or essay content by filtering out internal prompt leaks.
 * @param content The raw content string from task or AI.
 * @param fallback Contextual student-facing fallback text.
 * @return Clean human-readable text.
 */
export function sanitizeTaskContent(content?: string | null, fallback?: string): string {
  if (!content || !content.trim() || containsPromptLeak(content)) {
    return fallback || 'Read the instructions carefully and complete the exercises below.';
  }
  return content.trim();
}

/**
 * @brief Sanitizes AI feedback by filtering out prompt leak preambles.
 * @param feedback The raw feedback string.
 * @param fallback Student-facing fallback evaluation text.
 * @return Clean feedback text.
 */
export function sanitizeFeedback(feedback?: string | null, fallback?: string): string {
  if (!feedback || !feedback.trim() || containsPromptLeak(feedback)) {
    return (
      fallback ||
      'Your submission has been evaluated according to CEFR standards. Review the scoring criteria breakdown above.'
    );
  }
  return feedback.trim();
}
