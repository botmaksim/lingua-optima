/**
 * @file SubmissionResultResponse.java
 * @brief Response DTO representing an evaluated student submission with scores and feedback.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.enums.SubmissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @brief Response DTO representing an evaluated student submission with scores and feedback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultResponse {

    /** @brief Unique identifier of the submission. */
    private UUID id;

    /** @brief Optional identifier of the linked task assignment. */
    private UUID assignmentId;

    /** @brief Identifier of the student who submitted the work. */
    private UUID studentId;

    /** @brief Submission modality (TEXT or IMAGE). */
    private SubmissionType submissionType;

    /** @brief Submitted or OCR-extracted student text. */
    private String originalText;

    /** @brief Primary score (0-100 scale). */
    private Double score;

    /** @brief Effective score resolving teacher override over AI score. */
    private Double effectiveScore;

    /** @brief Narrative AI evaluation feedback. */
    private String feedback;

    /** @brief Structured rubric breakdown map. */
    private Map<String, Object> rubric;

    /** @brief Educator manual override score, if applied. */
    private Double overrideScore;

    /** @brief Educator commentary accompanying a score override. */
    private String teacherComment;

    /** @brief Identifier of the AI provider that evaluated the submission. */
    private String providerUsed;

    /** @brief Timestamp when the submission was recorded. */
    private LocalDateTime submittedAt;

    /**
     * @brief Maps a domain Submission entity to a SubmissionResultResponse DTO.
     * @param submission Domain entity instance.
     * @return Mapped SubmissionResultResponse DTO or null if input is null.
     */
    public static SubmissionResultResponse fromEntity(Submission submission) {
        if (submission == null) return null;
        return SubmissionResultResponse.builder()
            .id(submission.getId())
            .assignmentId(submission.getAssignment() != null ? submission.getAssignment().getId() : null)
            .studentId(submission.getStudent().getId())
            .submissionType(submission.getSubmissionType())
            .originalText(submission.getStudentText())
            .score(submission.getEffectiveScore())
            .effectiveScore(submission.getEffectiveScore())
            .feedback(submission.getAiFeedback())
            .overrideScore(submission.getOverrideScore())
            .teacherComment(submission.getTeacherComment())
            .providerUsed(submission.getProviderUsed())
            .submittedAt(submission.getSubmittedAt())
            .build();
    }
}
