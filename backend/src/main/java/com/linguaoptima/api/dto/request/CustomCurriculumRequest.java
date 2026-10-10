/**
 * @file CustomCurriculumRequest.java
 * @brief Request DTO for creating or saving a custom grammar rule or vocabulary set.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for creating or saving a custom grammar rule or vocabulary set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomCurriculumRequest {

    /** @brief Display title or topic identifier. */
    @NotBlank(message = "Title is required")
    private String title;

    /** @brief Targeted CEFR level benchmark. */
    private CefrLevel cefrLevel;

    /** @brief Entry classification: RULE or VOCABULARY. */
    @NotBlank(message = "Curriculum type is required (RULE or VOCABULARY)")
    private String curriculumType;

    /** @brief Rule explanation or vocabulary words list. */
    @NotBlank(message = "Content is required")
    private String content;
}
