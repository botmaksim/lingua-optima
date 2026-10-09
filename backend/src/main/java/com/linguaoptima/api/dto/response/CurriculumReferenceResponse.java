/**
 * @file CurriculumReferenceResponse.java
 * @brief DTO providing pedagogical reference grammar rules and target vocabulary for a given CEFR level and topic.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @brief Response DTO containing reference grammar rules and vocabulary collocations for task generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumReferenceResponse {

    /** @brief Target CEFR language proficiency benchmark. */
    private String cefrLevel;

    /** @brief Grammar topic or custom practice subject. */
    private String grammarTopic;

    /** @brief Pedagogical grammar rule explanation and structural formulas. */
    private String referenceRule;

    /** @brief Target vocabulary words, expressions, and collocations. */
    private List<String> referenceVocabulary;

    /** @brief Source of the reference curriculum (CANONICAL, CUSTOM, or SYNTHESIZED). */
    private String source;
}
