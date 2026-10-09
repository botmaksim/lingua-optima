/**
 * @file SubmissionItemResponse.java
 * @brief Response DTO representing an individual evaluated sentence or exercise question.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO representing an individual evaluated sentence or exercise question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionItemResponse {

    /** @brief 1-based question or sentence index. */
    private int questionNumber;

    /** @brief Full prompt or sentence text being solved. */
    private String sentence;

    /** @brief Student's submitted response. */
    private String studentAnswer;

    /** @brief Expected correct answer. */
    private String correctAnswer;

    /** @brief True if student's answer was evaluated as correct. */
    private boolean isCorrect;

    /** @brief Pedagogical explanation of why the answer is correct or incorrect. */
    private String explanation;

    /** @brief Targeted grammar rule or linguistic concept tested. */
    private String grammarRule;
}
