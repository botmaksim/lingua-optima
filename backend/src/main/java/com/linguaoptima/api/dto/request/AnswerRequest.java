/**
 * @file AnswerRequest.java
 * @brief Request DTO for submitting an answer during an adaptive CAT session.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @brief Request DTO for submitting an answer during an adaptive CAT session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {

    /** @brief Field representing question id in AnswerRequest. */
    @NotNull(message = "Question ID is required")
    private UUID questionId;

    /** @brief Field representing answer in AnswerRequest. */
    @NotBlank(message = "Answer is required")
    private String answer;
}
