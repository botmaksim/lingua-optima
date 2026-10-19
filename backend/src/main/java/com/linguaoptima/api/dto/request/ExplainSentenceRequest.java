/**
 * @file ExplainSentenceRequest.java
 * @brief Payload for requesting AI pedagogical explanation of a sentence or excerpt.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Payload for requesting AI pedagogical explanation of a sentence or excerpt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplainSentenceRequest {

    /**
     * @brief The English sentence or phrase to explain.
     */
    @NotBlank(message = "Sentence must not be blank")
    private String sentence;

    /**
     * @brief Optional context surrounding the sentence (e.g., full paragraph or passage).
     */
    private String context;

    /**
     * @brief Optional student CEFR proficiency level for calibrating explanation depth.
     */
    private CefrLevel cefrLevel;
}
