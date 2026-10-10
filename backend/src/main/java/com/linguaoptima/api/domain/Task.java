/**
 * @file Task.java
 * @brief JPA entity representing an educational assignment task or lesson template.
 */
package com.linguaoptima.api.domain;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief JPA entity representing an educational assignment task or lesson template.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    /** @brief Field representing id in Task. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing type in Task. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType type;

    /** @brief Field representing cefr level in Task. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false)
    private CefrLevel cefrLevel;

    /** @brief Field representing grammar topic in Task. */
    @Column(name = "grammar_topic")
    private String grammarTopic;

    /** @brief Field representing domain in Task. */
    @Column
    private String domain;

    /** @brief Field representing content in Task. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** @brief Field representing answer key in Task. */
    @Column(name = "answer_key", columnDefinition = "TEXT")
    private String answerKey;

    /** @brief Field representing difficulty in Task. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    /** @brief Field representing total points / max score in Task. */
    @Column(name = "total_points")
    @Builder.Default
    private Integer totalPoints = 100;

    /** @brief Field representing created by in Task. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** @brief Field representing is template in Task. */
    @Column(name = "is_template", nullable = false)
    @Builder.Default
    private boolean isTemplate = false;

    /** @brief Field representing created at in Task. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** @brief Field representing questions in Task. */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TaskQuestion> questions = new ArrayList<>();

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
