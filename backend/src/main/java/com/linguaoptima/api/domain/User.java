/**
 * @file User.java
 * @brief JPA entity representing a platform user (Student, Teacher, or Admin).
 */
package com.linguaoptima.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity representing a platform user (Student, Teacher, or Admin).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * @brief Unique identifier of the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * @brief Unique email address used for login and notifications.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * @brief BCrypt password hash (excluded from JSON serialization).
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * @brief User's full display name.
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * @brief Role assigned to the user (STUDENT, TEACHER, ADMIN).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * @brief Current CEFR language proficiency level of the user.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false)
    @Builder.Default
    private CefrLevel cefrLevel = CefrLevel.A1;

    /**
     * @brief Privacy-preserving alias displayed on intra-group leaderboards.
     */
    @Column(name = "display_alias")
    private String displayAlias;

    /**
     * @brief Consecutive daily practice streak counter.
     */
    @Column(name = "streak_count", nullable = false)
    @Builder.Default
    private int streakCount = 0;

    /**
     * @brief Date of the user's most recent completed learning activity.
     */
    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    /**
     * @brief Available streak freeze tokens protecting against missed days.
     */
    @Column(name = "freeze_tokens", nullable = false)
    @Builder.Default
    private int freezeTokens = 0;

    /**
     * @brief Timestamp when a CEFR level promotion was last suggested.
     */
    @Column(name = "level_up_suggested_at")
    private LocalDateTime levelUpSuggestedAt;

    /**
     * @brief Account registration timestamp.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * @brief Transient per-request AI provider override selected during task generation.
     */
    @Transient
    @JsonIgnore
    private AIProvider preferredProvider;

    /**
     * @brief Transient per-request AI model identifier override selected during task generation.
     */
    @Transient
    @JsonIgnore
    private String preferredModel;

    /**
     * @brief Pre-persist JPA callback populating default creation date, CEFR level, and anonymized alias.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (cefrLevel == null) {
            cefrLevel = CefrLevel.A1;
        }
        if (displayAlias == null || displayAlias.isBlank()) {
            displayAlias = (fullName != null && !fullName.isBlank())
                ? fullName.trim()
                : "Linguist #" + (id != null ? id.toString().substring(0, 8) : "Learner");
        }
    }
}
