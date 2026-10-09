/**
 * @file SentenceCorrectionResponse.java
 * @brief Response DTO representing an individual sentence correction with pedagogical feedback.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO representing an individual sentence correction with pedagogical feedback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentenceCorrectionResponse {

    /** @brief Erroneous or unpolished original sentence written by the student. */
    private String original;

    /** @brief Suggested grammatically accurate and natural rewrite. */
    private String corrected;

    /** @brief Explanation of the grammatical or stylistic error. */
    private String explanation;

    /** @brief Grammatical rule or category of the mistake. */
    private String grammarRule;
}
