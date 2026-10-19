/**
 * @file ExplainSentenceResponse.java
 * @brief Response DTO containing pedagogical breakdown of an English sentence.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @brief Response DTO containing pedagogical breakdown of an English sentence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainSentenceResponse {

    /**
     * @brief The original English sentence.
     */
    private String originalSentence;

    /**
     * @brief Natural Russian translation of the sentence in context.
     */
    private String translation;

    /**
     * @brief Core grammatical breakdown (tense, clauses, active/passive voice, syntax).
     */
    private String grammarBreakdown;

    /**
     * @brief Key vocabulary and idioms with definitions.
     */
    private List<String> keyVocabulary;

    /**
     * @brief Clear pedagogical explanation and practical usage tip.
     */
    private String explanation;
}
