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

    private int questionOrder;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Builder.Default
    private int difficulty = 2;

    @Builder.Default
    private int points = 10;

    private String grammarRule;
}
