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
 * @file SubmissionResultResponse.java
 * @brief Response DTO representing an evaluated student submission with scores and feedback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultResponse {

    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private SubmissionType submissionType;
    private String originalText;
    private Double score;
    private String feedback;
    private Map<String, Object> rubric;
    private Double overrideScore;
    private String teacherComment;
    private String providerUsed;
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
            .feedback(submission.getAiFeedback())
            .overrideScore(submission.getOverrideScore())
            .teacherComment(submission.getTeacherComment())
            .providerUsed(submission.getProviderUsed())
            .submittedAt(submission.getSubmittedAt())
            .build();
    }
}
