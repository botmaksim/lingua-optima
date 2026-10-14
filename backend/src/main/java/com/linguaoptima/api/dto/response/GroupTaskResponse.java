/**
 * @file GroupTaskResponse.java
 * @brief Response DTO representing an educational task assigned to a study cohort.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief Response DTO representing an educational task assigned to a study cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTaskResponse {

    /** @brief Unique identifier of the task. */
    private UUID taskId;

    /** @brief Pedagogical exercise type (e.g., MCQ, GAP_FILL, ESSAY). */
    private TaskType type;

    /** @brief CEFR proficiency tier (A1 through C2). */
    private CefrLevel cefrLevel;

    /** @brief Target grammar structure or topic. */
    private String grammarTopic;

    /** @brief Topical domain or contextual theme. */
    private String domain;

    /** @brief Total points / maximum score for the exercise. */
    private Integer totalPoints;

    /** @brief Submission due date for this cohort. */
    private LocalDateTime dueDate;

    /** @brief Maximum allowed attempts (1 = single attempt, 0 = unlimited). */
    private Integer maxAttempts;

    /** @brief Number of students in this cohort assigned the task. */
    private int assignedStudentCount;

    /** @brief Number of students who have completed the assignment. */
    private int completedStudentCount;

    /** @brief Average percentage score across completed student submissions. */
    private Double avgScore;

    /** @brief Timestamp when the task was initially created. */
    private LocalDateTime createdAt;
}
