/**
 * @file formatDate.ts
 * @brief Utility function formatting ISO timestamp strings into localized human-readable dates.
 */

/**
 * @brief Formats an ISO date string into standard US localized representation.
 * @param dateString ISO timestamp string.
 * @return Formatted date string (e.g. "Oct 9, 2026, 03:22 AM").
 */
export const formatDate = (dateString?: string): string => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};
