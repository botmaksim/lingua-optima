/**
 * @file TaskParamsRequest.java
 * @brief Request DTO specifying CEFR level, topic, domain, and type for AI task generation.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO specifying CEFR level, topic, domain, and type for AI task generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskParamsRequest {

    /** @brief Field representing cefr level in TaskParamsRequest. */
    @NotNull(message = "CEFR level is required")
    private CefrLevel cefrLevel;

    /** @brief Field representing grammar topic in TaskParamsRequest. */
    private String grammarTopic;

    /** @brief Field representing domain in TaskParamsRequest. */
    private String domain;

    /** @brief Field representing task type in TaskParamsRequest. */
    @NotNull(message = "Task type is required")
    private TaskType taskType;

    /** @brief Field representing difficulty in TaskParamsRequest. */
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    /** @brief Field representing number of questions in TaskParamsRequest. */
    @Builder.Default
    private int numberOfQuestions = 5;

    /** @brief Field representing provider in TaskParamsRequest. */
    private AIProvider provider;

    /** @brief Field representing model name in TaskParamsRequest. */
    private String modelName;
}
