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

    @NotNull(message = "CEFR level is required")
    private CefrLevel cefrLevel;

    private String grammarTopic;

    private String domain;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    private String content;

    @Valid
    @NotEmpty(message = "At least one question is required")
    @Builder.Default
    private List<CustomQuestionRequest> questions = new ArrayList<>();

    private Integer totalPoints;

    private boolean isTemplate;

    /** @brief Optional target cohort group IDs for direct assignment. */
    private List<UUID> groupIds;

    /** @brief Optional submission due date for direct assignment. */
    private LocalDateTime dueDate;

    /** @brief Optional maximum allowed attempts for direct assignment. */
    private Integer maxAttempts;

    /** @brief Optional target grammar rule for essay or grammar tasks. */
    private String customRule;

    /** @brief Optional target vocabulary for essay or reading tasks. */
    private String customVocabulary;
}
