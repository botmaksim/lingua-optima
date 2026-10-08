package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private TaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false)
    private SubmissionType submissionType;

    /**
     * ZERO-RETENTION OCR:
     * Only extracted text is stored. Image bytes are processed in RAM and never stored on disk or DB.
     */
    @Column(name = "student_text", columnDefinition = "TEXT", nullable = false)
    private String studentText;

    @Column(name = "ai_score")
    private Double aiScore;

    @Column(name = "ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "override_score")
    private Double overrideScore;

    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

    @Column(name = "provider_used")
    private String providerUsed;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public Double getEffectiveScore() {
        return overrideScore != null ? overrideScore : aiScore;
    }
}
