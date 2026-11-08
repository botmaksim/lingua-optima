/**
 * @file GroupTask.java
 * @brief JPA entity representing an educational task explicitly assigned to a study cohort.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity representing an educational task explicitly assigned to a study cohort.
 *
 * Ensures tasks assigned to cohorts are persisted and visible even when the cohort has zero
 * active students, and enables automatic backfill of assignments when new students enroll.
 */
@Entity
@Table(name = "group_tasks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_group_task", columnNames = {"group_id", "task_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (maxAttempts == null) {
            maxAttempts = 1;
        }
    }
}
