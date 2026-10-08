package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false)
    @Builder.Default
    private CefrLevel cefrLevel = CefrLevel.B1;

    @Column(name = "display_alias")
    private String displayAlias;

    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private int streakCount = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(name = "freeze_tokens", nullable = false)
    @Builder.Default
    private int freezeTokens = 0;

    @Column(name = "level_up_suggested_at")
    private LocalDateTime levelUpSuggestedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (cefrLevel == null) {
            cefrLevel = CefrLevel.B1;
        }
        if (displayAlias == null || displayAlias.isBlank()) {
            displayAlias = "Linguist #" + (id != null ? id.toString().substring(0, 8) : "Learner");
        }
    }
}
