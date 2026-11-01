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

    private UUID id;
    private TaskType type;
    private CefrLevel cefrLevel;
    private String grammarTopic;
    private String domain;
    private String content;
    /** @brief Difficulty level of the task. */
    private DifficultyLevel difficulty;
    /** @brief Total points / max score for the task. */
    private Integer totalPoints;
    private boolean isTemplate;
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
    /** @brief Flag indicating whether this task was formally assigned by an educator. */
    @Builder.Default
    private Boolean isTeacherTask = false;
    /** @brief Optional speaking mode: TEXT (oral reading passage) or SITUATION (communicative role-play scenario). */
    private String speakingMode;
    /** @brief Target phonemes or contrastive sound pairs for oral speaking practice. */
    private String targetSounds;
    /** @brief Custom title or drill topic for speaking tasks. */
    private String speakingTitle;
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

        String resolvedSpeakingMode = null;
        String resolvedTargetSounds = null;
        String resolvedSpeakingTitle = null;

        if (task.getType() == TaskType.SPEAKING) {
            resolvedSpeakingTitle = task.getGrammarTopic();
            if (task.getAnswerKey() != null && !task.getAnswerKey().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(task.getAnswerKey());
                    if (node.has("speakingMode")) {
                        resolvedSpeakingMode = node.get("speakingMode").asText(null);
                    }
                    if (node.has("targetSounds")) {
                        resolvedTargetSounds = node.get("targetSounds").asText(null);
                    }
                    if (node.has("speakingTitle")) {
                        resolvedSpeakingTitle = node.get("speakingTitle").asText(resolvedSpeakingTitle);
                    }
                } catch (Exception ignored) {
                    // Answer key is not a metadata JSON, fallback to content analysis
                }
            }
            if (resolvedSpeakingMode == null) {
                if (task.getContent() != null && (
                    task.getContent().contains("Role:") ||
                    task.getContent().contains("Setting:") ||
                    task.getContent().contains("Goal:") ||
                    task.getContent().contains("Talking Points:") ||
                    task.getContent().contains("Communicative Situation")
                )) {
                    resolvedSpeakingMode = "SITUATION";
                } else {
                    resolvedSpeakingMode = "TEXT";
                }
            }
        }

        return TaskResponse.builder()
            .id(task.getId())
            .type(task.getType())
            .cefrLevel(task.getCefrLevel())
            .grammarTopic(task.getGrammarTopic())
            .domain(task.getDomain())
            .content(task.getContent())
            .difficulty(task.getDifficulty())
            .totalPoints(task.getTotalPoints())
            .isTemplate(task.isTemplate())
            .createdAt(task.getCreatedAt())
            .speakingMode(resolvedSpeakingMode)
            .targetSounds(resolvedTargetSounds)
            .speakingTitle(resolvedSpeakingTitle)
            .questions(questionResponses)
            .build();
    }
}
