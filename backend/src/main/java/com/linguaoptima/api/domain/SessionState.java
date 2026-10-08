package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TaskAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private int currentQuestionIndex = 0;

    @Column(name = "current_difficulty", nullable = false)
    @Builder.Default
    private int currentDifficulty = 2; // Starts at MEDIUM = 2

    @Column(name = "answers_json", columnDefinition = "TEXT")
    @Builder.Default
    private String answersJson = "[]";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "last_active_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (lastActiveAt == null) {
            lastActiveAt = LocalDateTime.now();
        }
    }
}
