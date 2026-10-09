/**
 * @file PendingAiTask.java
 * @brief JPA entity queuing failed AI requests for background retry processing.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity queuing failed AI requests for background retry processing.
 */
@Entity
@Table(name = "pending_ai_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingAiTask {

    /** @brief Field representing id in PendingAiTask. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing user id in PendingAiTask. */
    @Column(name = "user_id")
    private UUID userId;

    /** @brief Field representing task type in PendingAiTask. */
    @Column(name = "task_type", nullable = false)
    private String taskType;

    /** @brief Field representing prompt in PendingAiTask. */
    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt;

    /** @brief Field representing status in PendingAiTask. */
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "QUEUED";

    /** @brief Field representing created at in PendingAiTask. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * @brief JPA lifecycle callback invoked via prePersist to update entity state.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
