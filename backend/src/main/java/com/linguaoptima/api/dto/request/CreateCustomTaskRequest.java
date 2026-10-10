/**
 * @file CreateCustomTaskRequest.java
 * @brief Request DTO for creating, customizing, and optionally deploying a task.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief Request DTO for creating, customizing, and optionally deploying a task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomTaskRequest {

    /** @brief Field representing cefr level in CreateCustomTaskRequest. */
    @NotNull(message = "CEFR level is required")
    private CefrLevel cefrLevel;

    /** @brief Field representing grammar topic in CreateCustomTaskRequest. */
    private String grammarTopic;

    /** @brief Field representing domain in CreateCustomTaskRequest. */
    private String domain;

    /** @brief Field representing task type in CreateCustomTaskRequest. */
    @NotNull(message = "Task type is required")
    private TaskType taskType;

    /** @brief Field representing difficulty in CreateCustomTaskRequest. */
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    /** @brief Field representing content in CreateCustomTaskRequest. */
    private String content;

    /** @brief Field representing questions in CreateCustomTaskRequest. */
    @Valid
    @NotEmpty(message = "At least one question is required")
    @Builder.Default
    private List<CustomQuestionRequest> questions = new ArrayList<>();

    /** @brief Field representing is template in CreateCustomTaskRequest. */
    private boolean isTemplate;

    /** @brief Optional target cohort group IDs for direct assignment. */
    private List<UUID> groupIds;

    /** @brief Optional submission due date for direct assignment. */
    private LocalDateTime dueDate;

    /** @brief Optional maximum allowed attempts for direct assignment. */
    private Integer maxAttempts;
}
