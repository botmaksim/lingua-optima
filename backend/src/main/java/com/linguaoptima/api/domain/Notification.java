/**
 * @file Notification.java
 * @brief JPA entity representing system alerts, task assignments, and level-up notifications.
 */
package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity representing system alerts, task assignments, and level-up notifications.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /** @brief Field representing id in Notification. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing user in Notification. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** @brief Field representing message in Notification. */
    @Column(nullable = false, length = 1024)
    private String message;

    /** @brief Field representing type in Notification. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** @brief Field representing is read in Notification. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /** @brief Field representing created at in Notification. */
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
