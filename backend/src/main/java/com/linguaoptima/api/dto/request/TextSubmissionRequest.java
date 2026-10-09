/**
 * @file TextSubmissionRequest.java
 * @brief Request DTO for submitting written essay or grammar exercise text.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @brief Request DTO for submitting written essay or grammar exercise text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextSubmissionRequest {

    /** @brief Field representing assignment id in TextSubmissionRequest. */
    private UUID assignmentId;

    /** @brief Field representing text in TextSubmissionRequest. */
    @NotBlank(message = "Text cannot be blank")
    private String text;

    /**
     * GRAMMAR, ESSAY, REWRITE, etc.
     */
    private String type;
}
