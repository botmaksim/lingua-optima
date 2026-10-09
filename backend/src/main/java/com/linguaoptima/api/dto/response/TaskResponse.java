/**
 * @file TaskResponse.java
 * @brief Response DTO representing a generated or assigned pedagogical task.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @brief Response DTO representing a generated or assigned pedagogical task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    /** @brief Field representing id in TaskResponse. */
    private UUID id;
    /** @brief Field representing type in TaskResponse. */
    private TaskType type;
    /** @brief Field representing cefr level in TaskResponse. */
    private CefrLevel cefrLevel;
    /** @brief Field representing grammar topic in TaskResponse. */
    private String grammarTopic;
    /** @brief Field representing domain in TaskResponse. */
    private String domain;
    /** @brief Field representing content in TaskResponse. */
    private String content;
    /** @brief Field representing difficulty in TaskResponse. */
    private DifficultyLevel difficulty;
    /** @brief Field representing is template in TaskResponse. */
    private boolean isTemplate;
    /** @brief Field representing created at in TaskResponse. */
    private LocalDateTime createdAt;
    /** @brief Optional task assignment ID for the student. */
    private UUID assignmentId;
    /** @brief Full name of the educator who assigned the task, if applicable. */
    private String assignedByName;
    /** @brief Optional submission due date for an assigned task. */
    private LocalDateTime dueDate;
    /** @brief Current status of the student's assignment (PENDING, SUBMITTED, GRADED). */
    private String assignmentStatus;
    /** @brief Maximum allowed attempts (1 = single attempt, 0 = unlimited). */
    private Integer maxAttempts;
    /** @brief Number of attempts used by the student so far. */
    private Integer attemptsUsed;
    /** @brief Identifier of the student's most recent submission for this task. */
    private UUID latestSubmissionId;
    /** @brief Flag indicating whether the student is allowed to submit another attempt. */
    @Builder.Default
    private Boolean canSubmit = true;
    /** @brief Field representing questions in TaskResponse. */
    @Builder.Default
    private List<QuestionResponse> questions = new ArrayList<>();

    /**
     * @brief Maps a domain Task entity to a TaskResponse DTO.
     * @param task Domain entity instance.
     * @return Mapped TaskResponse DTO or null if input is null.
     */
    public static TaskResponse fromEntity(Task task) {
        if (task == null) return null;
        List<QuestionResponse> questionResponses = task.getQuestions() != null
            ? task.getQuestions().stream().map(QuestionResponse::fromEntity).collect(Collectors.toList())
            : new ArrayList<>();

        return TaskResponse.builder()
            .id(task.getId())
            .type(task.getType())
            .cefrLevel(task.getCefrLevel())
            .grammarTopic(task.getGrammarTopic())
            .domain(task.getDomain())
            .content(task.getContent())
            .difficulty(task.getDifficulty())
            .isTemplate(task.isTemplate())
            .createdAt(task.getCreatedAt())
            .questions(questionResponses)
            .build();
    }
}
