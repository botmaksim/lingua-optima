/**
 * @file GroupStudent.java
 * @brief JPA join entity managing student membership in groups with soft-delete support.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA join entity managing student membership in groups with soft-delete support.
 */
@Entity
@Table(name = "group_students", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupStudent {

    /** @brief Field representing id in GroupStudent. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing group in GroupStudent. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** @brief Field representing student in GroupStudent. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** @brief Field representing is active in GroupStudent. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** @brief Field representing enrollment status in GroupStudent. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private com.linguaoptima.api.domain.enums.EnrollmentStatus status = com.linguaoptima.api.domain.enums.EnrollmentStatus.ACCEPTED;

    /** @brief Field representing removed at in GroupStudent. */
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    /** @brief Field representing joined at in GroupStudent. */
    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /**
     * @brief JPA lifecycle callback invoked via prePersist to update entity state.
     */
    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
