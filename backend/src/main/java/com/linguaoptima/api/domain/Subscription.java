/**
 * @file Subscription.java
 * @brief JPA entity maintaining user subscription tier, expiration dates, and billing state.
 */
package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity maintaining user subscription tier, expiration dates, and billing state.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    /** @brief Field representing id in Subscription. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing user in Subscription. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** @brief Field representing tier in Subscription. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionTier tier = SubscriptionTier.FREE;

    /** @brief Field representing expires at in Subscription. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** @brief Field representing created at in Subscription. */
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
