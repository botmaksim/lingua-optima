package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_counters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "week_evaluations", nullable = false)
    @Builder.Default
    private int weekEvaluations = 0;

    @Column(name = "week_ocr_uploads", nullable = false)
    @Builder.Default
    private int weekOcrUploads = 0;

    @Column(name = "week_reset_at", nullable = false)
    @Builder.Default
    private LocalDateTime weekResetAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (weekResetAt == null) {
            weekResetAt = LocalDateTime.now();
        }
    }
}
