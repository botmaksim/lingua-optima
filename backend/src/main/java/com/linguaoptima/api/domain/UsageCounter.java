/**
 * @file UsageCounter.java
 * @brief JPA entity recording weekly evaluation and OCR counts for rate-limited free users.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity recording weekly evaluation and OCR counts for rate-limited free users.
 */
@Entity
@Table(name = "usage_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCounter {

    /** @brief Field representing id in UsageCounter. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing user in UsageCounter. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** @brief Field representing week evaluations in UsageCounter. */
    @Column(name = "week_evaluations", nullable = false)
    @Builder.Default
    private int weekEvaluations = 0;

    /** @brief Field representing week ocr uploads in UsageCounter. */
    @Column(name = "week_ocr_uploads", nullable = false)
    @Builder.Default
    private int weekOcrUploads = 0;

    /** @brief Field representing accumulated token consumption during the current week. */
    @Column(name = "week_tokens_used", nullable = false)
    @Builder.Default
    private long weekTokensUsed = 0L;

    /** @brief Field representing week reset at in UsageCounter. */
    @Column(name = "week_reset_at", nullable = false)
    @Builder.Default
    private LocalDateTime weekResetAt = LocalDateTime.now();

    /**
     * @brief JPA lifecycle callback invoked via prePersist to update entity state.
     */
    @PrePersist
    public void prePersist() {
        if (weekResetAt == null) {
            weekResetAt = LocalDateTime.now();
        }
    }
}
