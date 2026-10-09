/**
 * @file CefrTopicRegistry.java
 * @brief Static registry mapping CEFR proficiency levels (B1, B2, C1) to syllabus grammar topics and contexts.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;

import java.util.List;
import java.util.Map;

/**
 * @brief Static registry mapping CEFR proficiency levels (B1, B2, C1) to syllabus grammar topics and contexts.
 */
public final class CefrTopicRegistry {

    private CefrTopicRegistry() {}

    /** @brief Constant or enum value representing cefr grammar topics in CefrTopicRegistry. */
    private static final Map<CefrLevel, List<String>> CEFR_GRAMMAR_TOPICS = Map.of(
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
        )
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
     * @brief Returns standard thematic domains for contextualized exercise generation.
     * @return List of thematic domains.
     */
    public static List<String> getCommonDomains() {
        return COMMON_DOMAINS;
    }
}
