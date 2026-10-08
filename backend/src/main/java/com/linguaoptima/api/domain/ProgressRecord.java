package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "grammar_topic", nullable = false)
    private String grammarTopic;

    @Column(name = "total_attempts", nullable = false)
    @Builder.Default
    private int totalAttempts = 0;

    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private int errorCount = 0;

    @Column(name = "mastery_score", nullable = false)
    @Builder.Default
    private double masteryScore = 0.0;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
