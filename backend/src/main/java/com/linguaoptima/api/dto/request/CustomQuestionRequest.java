/**
 * @file CustomQuestionRequest.java
 * @brief Request DTO representing a customized question within a task.
 */
package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @brief Request DTO representing a customized question within a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomQuestionRequest {

    /** @brief Field representing question order in CustomQuestionRequest. */
    private int questionOrder;

    /** @brief Field representing question text in CustomQuestionRequest. */
    @NotBlank(message = "Question text is required")
    private String questionText;

    /** @brief Field representing correct answer in CustomQuestionRequest. */
    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    /** @brief Field representing options in CustomQuestionRequest. */
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /** @brief Field representing difficulty in CustomQuestionRequest. */
    @Builder.Default
    private int difficulty = 2;

    /** @brief Field representing grammar rule in CustomQuestionRequest. */
    private String grammarRule;
}
