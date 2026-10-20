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
    'Conditionals 0, 1, 2',
    'Passive Voice (Present & Past Simple)',
    'Modal Verbs (obligation, permission, advice)',
    'Comparatives and Superlatives',
    'Used to / Would',
    'Relative Clauses (defining)',
  ],
  B2: [
    'Third Conditional & Mixed Conditionals',
    'Passive Voice (Continuous & Perfect)',
    'Reported Speech',
    'Wish / If Only',
    'Modal Verbs of Deduction (Past)',
    'Relative Clauses (non-defining)',
    'Gerunds vs Infinitives',
    'Inversion with Negative Adverbials',
  ],
  C1: [
    'Advanced Inversion & Fronting',
    'Subjunctive Mood',
    'Cleft Sentences',
    'Participle Clauses',
    'Ellipsis and Substitution',
    'Advanced Discourse Markers',
    'Nuanced Modal Idioms',
    'Hypothetical Meaning & Unreal Past',
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

/**
 * @brief Curated pool of essay topics and discursive prompts by CEFR level.
 */
export const DEFAULT_ESSAY_TOPICS: Record<CefrLevel, string[]> = {
  A1: [
    'My Typical Day and Daily Routine',
    'My Hometown and Favorite Places',
    'My Best Friend or Family Member',
    'My Favorite Hobby and Free Time',
  ],
  A2: [
    'An Unforgettable Holiday or Journey',
    'Living in a Big City vs Living in a Small Village',
    'How I Use My Smartphone and Social Media',
    'Healthy Eating Habits and Favorite Foods',
  ],
  B1: [
    'Advantages and Disadvantages of Working or Studying Remotely',
    'Should School Uniforms Be Mandatory for All Students?',
    'The Impact of Social Media on Modern Friendships',
    'Why Protecting the Environment Is Every Citizen’s Duty',
    'Is Public Transport Better Than Owning a Private Car?',
  ],
  B2: [
    'Discuss the Impacts of Artificial Intelligence on Future Employment',
    'Should Governments Invest More in Space Exploration or Climate Mitigation?',
    'To What Extent Does Tourism Benefit or Harm Local Communities?',
    'The Role of Higher Education: Career Preparation vs Intellectual Growth',
    'Consumerism and the Influence of Targeted Digital Advertising',
  ],
  C1: [
    'Ethical Dilemmas in Genetic Engineering and Biotechnology',
    'The Balance Between National Security and Personal Digital Privacy',
    'Linguistic Imperialism: The Global Dominance of English and Indigenous Language Loss',
    'Economic Growth vs Environmental Sustainability: A False Dichotomy?',
    'The Impact of Algorithmic Curation on Democratic Discourse and Polarization',
  ],
  C2: [
    'The Epistemic Responsibilities of Academia in an Age of Post-Truth Populism',
    'Surveillance Capitalism, Autonomous Systems, and the Erosion of Human Agency',
    'Cultural Relativism vs Universal Human Rights: Negotiating Global Norms',
    'Aesthetic Value in the Anthropocene: Art, Commodification, and Digital Ephemera',
    'Technological Determinism vs Societal Sovereignty in Shaping Future Governance',
  ],
};

/**
 * @brief Standard curated vocabulary sets available for quick one-click inclusion.
 */
export interface StandardVocabularyPreset {
  id: string;
  title: string;
  category: string;
  words: string[];
}

export const STANDARD_VOCABULARY_PRESETS: StandardVocabularyPreset[] = [
  {
    id: 'academic_transitions',
    title: 'Academic Transitions',
    category: 'Academic',
    words: ['furthermore', 'consequently', 'notwithstanding', 'in light of', 'substantiate', 'exemplify'],
  },
  {
    id: 'formal_argumentation',
    title: 'Formal Argumentation',
    category: 'Argumentation',
    words: ['paramount', 'contentious', 'advocate', 'corroborate', 'refute', 'implication', 'premise'],
  },
  {
    id: 'business_strategy',
    title: 'Business & Strategy',
    category: 'Business',
    words: ['stakeholder', 'feasibility', 'sustainable', 'leverage', 'paradigm', 'benchmarking', 'mitigate'],
  },
  {
    id: 'technology_innovation',
    title: 'Tech & Innovation',
    category: 'Technology',
    words: ['breakthrough', 'automation', 'algorithmic', 'disruptive', 'scalability', 'interoperability'],
  },
  {
    id: 'environment_climate',
    title: 'Environment & Climate',
    category: 'Society',
    words: ['biodiversity', 'renewable', 'carbon footprint', 'resilience', 'mitigation', 'conservation'],
  },
  {
    id: 'daily_wellbeing',
    title: 'Daily Life & Well-being',
    category: 'Daily Life',
    words: ['convenient', 'lifestyle', 'commute', 'work-life balance', 'community', 'mindfulness', 'leisure'],
  },
];
