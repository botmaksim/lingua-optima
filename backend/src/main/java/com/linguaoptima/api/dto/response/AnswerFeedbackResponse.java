package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
