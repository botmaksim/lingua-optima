package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file TaskParamsRequest.java
 * @brief Request DTO specifying CEFR level, topic, domain, and type for AI task generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskParamsRequest {

    @NotNull(message = "CEFR level is required")
    private CefrLevel cefrLevel;

    private String grammarTopic;

    private String domain;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Builder.Default
    private int numberOfQuestions = 5;
}
