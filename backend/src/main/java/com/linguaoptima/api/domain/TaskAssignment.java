/**
 * @file TaskAssignment.java
 * @brief JPA entity linking a task to a student with status and due date.
 */
package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity linking a task to a student with status and due date.
 */
@Entity
@Table(name = "task_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignment {

    /** @brief Field representing id in TaskAssignment. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing task in TaskAssignment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** @brief Field representing student in TaskAssignment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** @brief Field representing assigned by in TaskAssignment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    /** @brief Field representing due date in TaskAssignment. */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /** @brief Field representing status in TaskAssignment. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.PENDING;

    /** @brief Field representing created at in TaskAssignment. */
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
