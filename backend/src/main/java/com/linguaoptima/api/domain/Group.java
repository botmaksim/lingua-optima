/**
 * @file Group.java
 * @brief JPA entity representing an educator study group or classroom.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief JPA entity representing an educator study group or classroom.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    /** @brief Field representing id in Group. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing name in Group. */
    @Column(nullable = false)
    private String name;

    /** @brief Field representing teacher in Group. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /** @brief Field representing created at in Group. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** @brief Field representing group students in Group. */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GroupStudent> groupStudents = new ArrayList<>();

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
