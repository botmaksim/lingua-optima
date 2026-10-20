/**
 * @file QuestionResponse.java
 * @brief Response DTO representing an individual question within a task or CAT session.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.TaskQuestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief Response DTO representing an individual question within a task or CAT session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private UUID id;
    private int questionOrder;
    private String questionText;
    @Builder.Default
    private List<String> options = new ArrayList<>();
    private String correctAnswer;
    private int difficulty;
    private int points;
    private String grammarRule;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @brief Maps a domain TaskQuestion entity to a QuestionResponse DTO including correct answer.
     * @param question Domain entity instance.
     * @return Mapped QuestionResponse DTO or null if input is null.
     */
    public static QuestionResponse fromEntity(TaskQuestion question) {
        if (question == null) return null;
        List<String> parsedOptions = new ArrayList<>();
        if (question.getOptionsJson() != null && !question.getOptionsJson().isBlank()) {
            try {
                parsedOptions = MAPPER.readValue(question.getOptionsJson(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
            }
        }
        return QuestionResponse.builder()
            .id(question.getId() != null ? question.getId() : UUID.randomUUID())
            .questionOrder(question.getQuestionOrder())
            .questionText(question.getQuestionText())
            .correctAnswer(question.getCorrectAnswer())
            .options(parsedOptions)
            .difficulty(question.getDifficulty())
            .points(question.getPoints())
            .grammarRule(question.getGrammarRule())
            .build();
    }

    /**
     * @brief Maps a domain TaskQuestion entity to a QuestionResponse DTO omitting correct answer for tests.
     * @param question Domain entity instance.
     * @return Mapped QuestionResponse DTO without correct answer or null if input is null.
     */
    public static QuestionResponse fromEntityWithoutAnswer(TaskQuestion question) {
        if (question == null) return null;
        List<String> parsedOptions = new ArrayList<>();
        if (question.getOptionsJson() != null && !question.getOptionsJson().isBlank()) {
            try {
                parsedOptions = MAPPER.readValue(question.getOptionsJson(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
            }
        }
        return QuestionResponse.builder()
            .id(question.getId() != null ? question.getId() : UUID.randomUUID())
            .questionOrder(question.getQuestionOrder())
            .questionText(question.getQuestionText())
            .correctAnswer(null)
            .options(parsedOptions)
            .difficulty(question.getDifficulty())
            .points(question.getPoints())
            .grammarRule(null)
            .build();
    }
}
