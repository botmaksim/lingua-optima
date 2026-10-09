package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file AnswerFeedbackResponse.java
 * @brief Response DTO containing immediate feedback and updated CAT difficulty after an answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedbackResponse {

    private boolean correct;
    private String correctAnswer;
    private String explanation;
    private int newDifficulty;
    private int currentQuestionIndex;
    private boolean completed;
    private Double masteryScore;
}
