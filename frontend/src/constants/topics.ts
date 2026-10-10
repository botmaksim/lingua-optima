/**
 * @file topics.ts
 * @brief Canonical CEFR grammar topics, mixed challenges, and vocabulary domain presets.
 */

import { CefrLevel } from '../types/user';

/**
 * @brief Default core grammar topics mapped by CEFR proficiency level.
 */
export const DEFAULT_CEFR_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple (to be & common verbs)',
    'Articles (a, an, the) & Demonstratives',
    'Basic Prepositions of Place & Time (in, at, on)',
    "Can / Can't for Ability & Permission",
    "Possessive Adjectives & Possessive 's",
    'Imperatives & Basic Question Formation',
  ],
  A2: [
    'Past Simple (Regular & Irregular Verbs)',
    "Future with 'Going to' vs 'Will'",
    'Comparative and Superlative Adjectives',
    'Countable vs Uncountable Nouns (some, any, much, many)',
    'Have to & Must (Basic Rules)',
    'Present Continuous for Future Arrangements',
  ],
  B1: [
    'Present Perfect vs Past Simple',
    'Past Continuous',
    'Conditionals (First & Second)',
    'Modal Verbs of Obligation',
    'Passive Voice (Basic)',
    'Relative Clauses (Defining)',
    'Used to & Would',
  ],
  B2: [
    'Third & Mixed Conditionals',
    'Passive Voice (Advanced & Causative)',
    'Reported Speech',
    'Wish & If Only Structures',
    'Modal Verbs of Deduction',
    'Inversion for Emphasis',
    'Participle Clauses',
  ],
  C1: [
    'Advanced Inversion & Fronting',
    'Subjunctive Mood',
    'Cleft Sentences',
    'Complex Gerunds & Infinitives',
    'Discourse Markers & Nuance',
    'Ellipsis & Substitution',
  ],
  C2: [
    'Stylistic Inversion & Rhetorical Fronting',
    'Subtle Modal Nuances & Speculative Stance',
    'Complex Cleft Constructions & Focalization',
    'Idiomatic Phrasal Collocations & Register Shifts',
    'Advanced Ellipsis, Substitution & Cohesive Ties',
    'Figurative Language & Lexical Precision',
  ],
};

/**
 * @brief Pre-configured mixed multi-topic grammar challenges.
 */
export const DEFAULT_MIXED_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'Present Simple vs Present Continuous in Daily Routines',
    'Articles, Plurals, and Demonstrative Pronouns',
    'Question Formation with To Be, Do/Does, and Can',
  ],
  A2: [
    'Past Simple vs Past Continuous Narrative Interruption',
    'Future Plans: Going to vs Present Continuous vs Will',
    'Comparatives, Superlatives, and As...As Equality',
  ],
  B1: [
    'Narrative Tenses: Past Simple, Continuous, and Perfect',
    'Mixed Modal Verbs: Obligation, Permission, and Advice',
    'Zero, First, and Second Conditionals with Unless',
  ],
  B2: [
    'Mixed Conditionals (Past Cause with Present Result)',
    'Advanced Passive and Causative Structures (Have/Get something done)',
    'Reported Speech Shifts with Reporting Verbs & Modals',
  ],
  C1: [
    'Negative Inversion and Cleft Sentences Combined',
    'Participle Clauses with Reduced Relatives & Adverbials',
    'Subjunctive Mood and Formulaic Mandative Expressions',
  ],
  C2: [
    'Stylistic Inversion, Clefting, and Focal Fronting',
    'Epistemic Stance, Subtle Modal Nuances, and Hedging',
    'Advanced Ellipsis, Substitution, and Cohesive Chaining',
  ],
};

/**
 * @brief Standard thematic domains for contextual vocabulary generation.
 */
export const DOMAINS = ['Daily Life', 'Business', 'Academic', 'Technology', 'Travel & Culture'];
