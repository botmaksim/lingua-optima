/**
 * @file CustomCurriculumEntry.java
 * @brief JPA entity storing teacher custom grammar rules and custom vocabulary sets.
 */
package com.linguaoptima.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.linguaoptima.api.domain.enums.CefrLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity storing teacher custom grammar rules and custom vocabulary sets.
 */
@Entity
@Table(name = "custom_curriculum_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomCurriculumEntry {

    /** @brief Unique identifier of the custom curriculum entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Educator who owns this custom rule or vocabulary set. */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** @brief Display title or topic name for the entry. */
    @Column(nullable = false)
    private String title;

    /** @brief Targeted CEFR level benchmark. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level")
    private CefrLevel cefrLevel;

    /** @brief Entry classification: RULE or VOCABULARY. */
    @Column(name = "curriculum_type", nullable = false)
    private String curriculumType;

    /** @brief Rule markdown text or vocabulary word list content. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** @brief Registration timestamp. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** @brief Pre-persist callback ensuring creation timestamp. */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
