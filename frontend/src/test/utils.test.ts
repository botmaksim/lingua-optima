/**
 * @file utils.test.ts
 * @brief Unit tests for frontend utility functions (wordCount, cefrColors, formatDate).
 */

import { describe, it, expect } from 'vitest';
import { getWordCount } from '../utils/wordCount';
import { getCefrBadgeClasses } from '../utils/cefrColors';
import { formatDate } from '../utils/formatDate';
import { CefrLevel } from '../types/user';

describe('wordCount utility', () => {
  it('returns 0 for empty or whitespace string', () => {
    expect(getWordCount('')).toBe(0);
    expect(getWordCount('   ')).toBe(0);
  });

  it('correctly counts words separated by spaces', () => {
    expect(getWordCount('Hello world')).toBe(2);
    expect(getWordCount('   The quick  brown   fox jumps   ')).toBe(5);
  });

  it('correctly counts words separated by newlines and tabs', () => {
    expect(getWordCount("One\ntwo\tthree\r\nfour")).toBe(4);
  });
});

describe('cefrColors utility', () => {
  it('returns appropriate teal classes for A1', () => {
    const classes = getCefrBadgeClasses('A1');
    expect(classes).toContain('teal');
  });

  it('returns appropriate cyan classes for A2', () => {
    const classes = getCefrBadgeClasses('A2');
    expect(classes).toContain('cyan');
  });

  it('returns appropriate emerald classes for B1', () => {
    const classes = getCefrBadgeClasses('B1');
    expect(classes).toContain('emerald');
  });

  it('returns appropriate sky classes for B2', () => {
    const classes = getCefrBadgeClasses('B2');
    expect(classes).toContain('sky');
  });

  it('returns appropriate purple classes for C1', () => {
    const classes = getCefrBadgeClasses('C1');
    expect(classes).toContain('purple');
  });

  it('returns appropriate amber classes for C2', () => {
    const classes = getCefrBadgeClasses('C2');
    expect(classes).toContain('amber');
  });

  it('returns fallback slate classes for unknown levels', () => {
    const classes = getCefrBadgeClasses('UNKNOWN' as unknown as CefrLevel);
    expect(classes).toContain('slate');
  });
});

describe('formatDate utility', () => {
  it('returns empty string for null or undefined input', () => {
    expect(formatDate(undefined)).toBe('');
    expect(formatDate('')).toBe('');
  });

  it('formats valid ISO date string properly', () => {
    const formatted = formatDate('2026-05-15T12:00:00Z');
    expect(formatted).toContain('2026');
    expect(formatted).toContain('May');
  });
});
