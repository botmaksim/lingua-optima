/**
 * @file SessionState.java
 * @brief JPA entity maintaining state for interactive Computerized Adaptive Testing (CAT) sessions.
 */
package com.linguaoptima.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.linguaoptima.api.domain.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity maintaining state for interactive Computerized Adaptive Testing (CAT) sessions.
 */
@Entity
@Table(name = "session_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionState {

    /**
     * @brief Unique identifier of the adaptive session.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * @brief Associated task assignment being evaluated in this session.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private TaskAssignment assignment;

    /**
     * @brief Student participating in this adaptive session.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * @brief Zero-based index of the current question in the adaptive sequence.
     */
    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private int currentQuestionIndex = 0;

    /**
     * @brief Current adaptive difficulty level (1 to 4).
     */
    @Column(name = "current_difficulty", nullable = false)
    @Builder.Default
    private int currentDifficulty = 2;

    /**
     * @brief Serialized JSON array recording historical question answers.
     */
    @Column(name = "answers_json", columnDefinition = "TEXT")
    @Builder.Default
    private String answersJson = "[]";

    /**
     * @brief Lifecycle status of the adaptive session (IN_PROGRESS, COMPLETED, EXPIRED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    /**
     * @brief Timestamp when the session was started.
     */
    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    /**
     * @brief Timestamp of the latest student interaction in the session.
     */
    @Column(name = "last_active_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    /**
     * @brief Exposes the associated task assignment UUID for JSON serialization.
     * @return UUID of the task assignment or null.
     */
    @JsonProperty("assignmentId")
    public UUID getAssignmentId() {
        return assignment != null ? assignment.getId() : null;
    }

    /**
     * @brief Pre-persist lifecycle callback setting default timestamps.
     */
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
