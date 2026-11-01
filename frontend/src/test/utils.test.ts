/**
 * @file utils.test.ts
 * @brief Unit tests for frontend utility functions (wordCount, cefrColors, formatDate).
 */

import { describe, it, expect } from 'vitest';
import { getWordCount } from '../utils/wordCount';
import { getCefrBadgeClasses } from '../utils/cefrColors';
import { formatDate } from '../utils/formatDate';
import { CefrLevel } from '../types/user';
import {
  splitTextForSpeechWithPauses,
  parseSituationContent,
  evaluateSituationalSpeech
} from '../utils/phonetics';

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

describe('splitTextForSpeechWithPauses utility', () => {
  it('returns empty array for empty or whitespace text', () => {
    expect(splitTextForSpeechWithPauses('')).toEqual([]);
    expect(splitTextForSpeechWithPauses('   ')).toEqual([]);
  });

  it('preserves plain sentences without blanks', () => {
    expect(splitTextForSpeechWithPauses('The weather is nice today.')).toEqual([
      'The weather is nice today.'
    ]);
  });

  it('splits at underscores with blanks', () => {
    const result = splitTextForSpeechWithPauses('I _____ to the supermarket yesterday.');
    expect(result).toEqual(['I', 'to the supermarket yesterday.']);
  });

  it('splits at bracketed verb prompts and strips leading question index', () => {
    const result = splitTextForSpeechWithPauses('1. If heat increases, ice (melt) into liquid.');
    expect(result).toEqual(['If heat increases, ice', 'into liquid.']);
  });

  it('handles multiple blanks and expansion cues with slashes', () => {
    const result = splitTextForSpeechWithPauses('She (not / see) him because he (already / leave) _____ for work.');
    expect(result).toEqual(['She', 'him because he', 'for work.']);
  });

  it('attaches trailing punctuation to the previous segment if there is no succeeding text', () => {
    const result = splitTextForSpeechWithPauses('Where have you _____?');
    expect(result).toEqual(['Where have you?']);
  });
});

describe('parseSituationContent & evaluateSituationalSpeech utilities', () => {
  it('correctly parses structured situation text into fields', () => {
    const rawText = `Role: Hotel Guest
Setting: Central London hotel reception
Goal: Request a room upgrade due to noisy street traffic

Talking Points:
• Explain politely that the current room is facing a noisy avenue
• Inquire whether an executive room with courtyard view is available
• Offer to pay any rate difference if needed

Useful Expressions:
• I was wondering if it might be possible to...
• Would you happen to have any rooms facing...?
• I would really appreciate your assistance.`;

    const parsed = parseSituationContent(rawText);
    expect(parsed.role).toBe('Hotel Guest');
    expect(parsed.setting).toBe('Central London hotel reception');
    expect(parsed.goal).toBe('Request a room upgrade due to noisy street traffic');
    expect(parsed.talkingPoints.length).toBe(3);
    expect(parsed.talkingPoints[0]).toContain('Explain politely');
    expect(parsed.usefulExpressions.length).toBe(3);
    expect(parsed.usefulExpressions[0]).toContain('I was wondering if');
  });

  it('evaluates situational speech delivery, pacing, and talking points coverage', () => {
    const context = {
      role: 'Hotel Guest',
      talkingPoints: [
        'Explain that your room is facing noisy traffic',
        'Inquire about a quieter courtyard room',
      ],
      usefulExpressions: ['I was wondering if it might be possible to change rooms'],
    };

    const transcript =
      'Good morning. I was wondering if it might be possible to change rooms because my current room is facing noisy street traffic and I cannot sleep. Do you have a quieter courtyard room available?';

    const result = evaluateSituationalSpeech(context, transcript, 15);
    expect(result.wpm).toBeGreaterThan(60);
    expect(result.accuracyScore).toBeGreaterThanOrEqual(80);
    expect(result.overallScore).toBeGreaterThanOrEqual(80);
    expect(result.words.length).toBeGreaterThan(20);
    expect(result.generalFeedback.length).toBeGreaterThan(1);
  });

  it('handles recorded audio fallback gracefully', () => {
    const context = {
      talkingPoints: ['Describe your project'],
    };

    const result = evaluateSituationalSpeech(context, '[Recorded Audio Submission]', 20);
    expect(result.accuracyScore).toBe(85);
    expect(result.overallScore).toBe(85);
    expect(result.generalFeedback[0]).toContain('Audio recording captured successfully');
  });
});

