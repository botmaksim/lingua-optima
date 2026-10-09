/**
 * @file AnswerFeedbackResponse.java
 * @brief Response DTO containing immediate feedback and updated CAT difficulty after an answer.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO containing immediate feedback and updated CAT difficulty after an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedbackResponse {

    /** @brief Field representing correct in AnswerFeedbackResponse. */
    private boolean correct;
    /** @brief Field representing correct answer in AnswerFeedbackResponse. */
    private String correctAnswer;
    /** @brief Field representing explanation in AnswerFeedbackResponse. */
    private String explanation;
    /** @brief Field representing new difficulty in AnswerFeedbackResponse. */
    private int newDifficulty;
    /** @brief Field representing current question index in AnswerFeedbackResponse. */
    private int currentQuestionIndex;
    /** @brief Field representing completed in AnswerFeedbackResponse. */
    private boolean completed;
    /** @brief Field representing mastery score in AnswerFeedbackResponse. */
    private Double masteryScore;
}
