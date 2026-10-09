/**
 * @file CefrTopicRegistry.java
 * @brief Static registry mapping full CEFR proficiency levels (A1, A2, B1, B2, C1, C2) to syllabus grammar topics and contexts.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;

import java.util.*;

/**
 * @brief Static registry mapping CEFR proficiency levels (A1 to C2) to syllabus grammar topics and contexts.
 */
public final class CefrTopicRegistry {

    private CefrTopicRegistry() {}

    /** @brief Constant or enum value representing cefr grammar topics in CefrTopicRegistry. */
    private static final Map<CefrLevel, List<String>> CEFR_GRAMMAR_TOPICS = Map.of(
        CefrLevel.A1, List.of(
            "Present Simple (to be & common verbs)",
            "Articles (a, an, the) & Demonstratives",
            "Basic Prepositions of Place & Time (in, at, on)",
            "Can / Can't for Ability & Permission",
            "Possessive Adjectives & Possessive 's",
            "Imperatives & Basic Question Formation"
        ),
        CefrLevel.A2, List.of(
            "Past Simple (Regular & Irregular Verbs)",
            "Future with 'Going to' vs 'Will'",
            "Comparative and Superlative Adjectives",
            "Countable vs Uncountable Nouns (some, any, much, many)",
            "Have to & Must (Basic Rules)",
            "Present Continuous for Future Arrangements"
        ),
        CefrLevel.B1, List.of(
            "Present Perfect vs Past Simple",
            "Past Continuous",
            "Conditionals 0, 1, 2",
            "Passive Voice (Present & Past Simple)",
            "Modal Verbs (obligation, permission, advice)",
            "Comparatives and Superlatives",
            "Used to / Would",
            "Relative Clauses (defining)"
        ),
        CefrLevel.B2, List.of(
            "Third Conditional & Mixed Conditionals",
            "Passive Voice (Continuous & Perfect)",
            "Reported Speech",
            "Wish / If Only",
            "Modal Verbs of Deduction (Past)",
            "Relative Clauses (non-defining)",
            "Gerunds vs Infinitives",
            "Inversion with Negative Adverbials"
        ),
        CefrLevel.C1, List.of(
            "Advanced Inversion & Fronting",
            "Subjunctive Mood",
            "Cleft Sentences",
            "Participle Clauses",
            "Ellipsis and Substitution",
            "Advanced Discourse Markers",
            "Nuanced Modal Idioms",
            "Hypothetical Meaning & Unreal Past"
        ),
        CefrLevel.C2, List.of(
            "Stylistic Inversion & Rhetorical Fronting",
            "Subtle Modal Nuances & Speculative Stance",
            "Complex Cleft Constructions & Focalization",
            "Idiomatic Phrasal Collocations & Register Shifts",
            "Advanced Ellipsis, Substitution & Cohesive Ties",
            "Figurative Language & Lexical Precision"
        )
    );

    /** @brief Dedicated mixed grammar challenges per CEFR proficiency level. */
    private static final Map<CefrLevel, List<String>> CEFR_MIXED_TOPICS = Map.of(
        CefrLevel.A1, List.of(
            "Mixed A1 Review: Present Simple, Pronouns & Basic Prepositions",
            "Questions & Negatives Mixed (Be, Do, Can)"
        ),
        CefrLevel.A2, List.of(
            "Mixed Tenses: Present Simple, Continuous & Past Simple",
            "Mixed Modals & Semi-Modals (Can, Must, Have to, Should)",
            "Mixed Quantifiers & Articles (Some, Any, Much, Many)"
        ),
        CefrLevel.B1, List.of(
            "Mixed Narrative Tenses: Past Simple, Continuous & Perfect",
            "Mixed Conditionals (Zero, First & Second)",
            "Mixed Active vs Passive Voice Transformations",
            "Phrasal Verbs & Dependent Prepositions Mixed"
        ),
        CefrLevel.B2, List.of(
            "Comprehensive Mixed Conditionals (Type 1, 2, 3 & Mixed)",
            "Mixed Narrative & Perfect Tenses with Time Clauses",
            "Mixed Reported Speech with Advanced Reporting Verbs",
            "Gerunds vs Infinitives in Complex Sentences",
            "Mixed Modals of Deduction & Speculation (Present & Past)"
        ),
        CefrLevel.C1, List.of(
            "Integrated Inversion, Fronting & Cleft Sentences",
            "Mixed Subjunctive, Inverted Conditionals & Unreal Past",
            "Participle Clauses & Reduced Adverbial Clauses Mixed",
            "Advanced Discourse Markers, Ellipsis & Cohesive Ties"
        ),
        CefrLevel.C2, List.of(
            "Stylistic Mastery: Integrated Inversion, Clefts & Rhetorical Syntax",
            "Nuanced Register Shifts: Colloquial, Academic & Diplomatic",
            "Idiomatic Mastery, Lexical Collocations & Phrasal Precision",
            "Cambridge C2 Proficiency (CPE) Key Word Transformations"
        )
    );

    /** @brief Cross-level thematic mixed challenges for advanced real-world contexts. */
    private static final List<String> THEMATIC_MIXED_TOPICS = List.of(
        "Mixed Comprehensive Grammar Review",
        "Business English: Mixed Polite Modals, Passive & Indirect Style",
        "Academic Writing: Mixed Syntax, Hedging & Cohesive Devices",
        "IELTS / TOEFL: Advanced Mixed Grammar & Cohesion Challenge"
    );

    /** @brief Constant or enum value representing common domains in CefrTopicRegistry. */
    private static final List<String> COMMON_DOMAINS = List.of(
        "Business & Work",
        "Travel & Tourism",
        "Academic & Science",
        "Culture & Arts",
        "Technology & AI",
        "Daily Life & Relationships"
    );

    /**
     * @brief Retrieves canonical grammar topics for a given CEFR proficiency level.
     * @param level Target CEFR level.
     * @return List of grammar topic titles.
     */
    public static List<String> getTopicsForLevel(CefrLevel level) {
        if (level == null) {
            return List.of();
        }
        return CEFR_GRAMMAR_TOPICS.getOrDefault(level, List.of("General Grammar"));
    }

    /**
     * @brief Retrieves dedicated mixed grammar challenges for a given CEFR proficiency level.
     * @param level Target CEFR level.
     * @return List of mixed challenge topic titles.
     */
    public static List<String> getMixedTopicsForLevel(CefrLevel level) {
        if (level == null) {
            return List.of();
        }
        return CEFR_MIXED_TOPICS.getOrDefault(level, List.of());
    }

    /**
     * @brief Retrieves all syllabus topics (both standard and mixed) for a given CEFR proficiency level.
     * @param level Target CEFR level.
     * @return Combined list of standard and mixed topics.
     */
    public static List<String> getAllTopicsForLevel(CefrLevel level) {
        List<String> core = getTopicsForLevel(level);
        List<String> mixed = getMixedTopicsForLevel(level);
        List<String> all = new ArrayList<>(core);
        all.addAll(mixed);
        return Collections.unmodifiableList(all);
    }

    /**
     * @brief Retrieves thematic cross-level mixed challenge topics.
     * @return List of thematic challenge titles.
     */
    public static List<String> getThematicMixedTopics() {
        return THEMATIC_MIXED_TOPICS;
    }

    /**
     * @brief Returns standard thematic domains for contextualized exercise generation.
     * @return List of thematic domains.
     */
    public static List<String> getCommonDomains() {
        return COMMON_DOMAINS;
    }
}
