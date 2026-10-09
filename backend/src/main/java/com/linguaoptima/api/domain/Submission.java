/**
 * @file Submission.java
 * @brief JPA entity representing student homework submission, AI scores, and teacher overrides.
 */
package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity representing student homework submission, AI scores, and teacher overrides.
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    /** @brief Field representing id in Submission. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing assignment in Submission. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private TaskAssignment assignment;

    /** @brief Field representing student in Submission. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** @brief Field representing submission type in Submission. */
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false)
    private SubmissionType submissionType;

    /**
     * ZERO-RETENTION OCR:
     * Only extracted text is stored. Image bytes are processed in RAM and never stored on disk or DB.
     */
    @Column(name = "student_text", columnDefinition = "TEXT", nullable = false)
    private String studentText;

    /** @brief Field representing ai score in Submission. */
    @Column(name = "ai_score")
    private Double aiScore;

    /** @brief Field representing ai feedback in Submission. */
    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    /** @brief Field representing override score in Submission. */
    @Column(name = "override_score")
    private Double overrideScore;

    /** @brief Field representing teacher comment in Submission. */
    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    /** @brief Field representing provider used in Submission. */
    @Column(name = "provider_used")
    private String providerUsed;

    /** @brief Field representing submitted at in Submission. */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    /**
     * @brief JPA lifecycle callback invoked via prePersist to update entity state.
     */
    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    /**
     * @brief Executes get effective score operation on Submission.
     */
    public Double getEffectiveScore() {
        return overrideScore != null ? overrideScore : aiScore;
    }
}
