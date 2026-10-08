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
    private int difficulty;
    private String grammarRule;

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            .id(question.getId())
            .questionOrder(question.getQuestionOrder())
            .questionText(question.getQuestionText())
            .options(parsedOptions)
            .difficulty(question.getDifficulty())
            .grammarRule(question.getGrammarRule())
            .build();
    }
}
