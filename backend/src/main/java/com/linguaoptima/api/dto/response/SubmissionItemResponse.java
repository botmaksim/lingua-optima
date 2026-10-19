/**
 * @file SubmissionItemResponse.java
 * @brief Response DTO representing an individual evaluated sentence or exercise question.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    @JsonProperty("isCorrect")
    private boolean isCorrect;

    /** @brief True if this question was skipped or unattempted by the student. */
    @JsonProperty("isSkipped")
    private boolean isSkipped;

    /** @brief Pedagogical explanation of why the answer is correct or incorrect. */
    private String explanation;

    /** @brief Targeted grammar rule or linguistic concept tested. */
    private String grammarRule;

    /** @brief Point value / weight of this question item. */
    private Integer points;

    /** @brief Multiple-choice options or answer choices, if applicable. */
    @Builder.Default
    private List<String> options = new ArrayList<>();

    @JsonProperty("isCorrect")
    public boolean isCorrect() {
        return isCorrect;
    }

    @JsonProperty("isCorrect")
    public void setCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    @JsonProperty("correct")
    public boolean getCorrect() {
        return isCorrect;
    }

    @JsonProperty("isSkipped")
    public boolean isSkipped() {
        return isSkipped;
    }

    @JsonProperty("isSkipped")
    public void setSkipped(boolean isSkipped) {
        this.isSkipped = isSkipped;
    }
}
