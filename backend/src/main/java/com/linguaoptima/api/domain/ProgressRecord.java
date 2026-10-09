/**
 * @file ProgressRecord.java
 * @brief JPA entity tracking student attempts, errors, and mastery score per grammar topic.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity tracking student attempts, errors, and mastery score per grammar topic.
 */
@Entity
@Table(name = "progress_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_id", "grammar_topic"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressRecord {

    /** @brief Field representing id in ProgressRecord. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing student in ProgressRecord. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** @brief Field representing grammar topic in ProgressRecord. */
    @Column(name = "grammar_topic", nullable = false)
    private String grammarTopic;

    /** @brief Field representing total attempts in ProgressRecord. */
    @Column(name = "total_attempts", nullable = false)
    @Builder.Default
    private int totalAttempts = 0;

    /** @brief Field representing error count in ProgressRecord. */
    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private int errorCount = 0;

    /** @brief Field representing mastery score in ProgressRecord. */
    @Column(name = "mastery_score", nullable = false)
    @Builder.Default
    private double masteryScore = 0.0;

    /** @brief Field representing updated at in ProgressRecord. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * @brief JPA lifecycle callback invoked via onUpdate to update entity state.
     */
    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
