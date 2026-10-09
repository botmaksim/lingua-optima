/**
 * @file TaskQuestion.java
 * @brief JPA entity representing an individual question or exercise item within a task.
 */
package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * @brief JPA entity representing an individual question or exercise item within a task.
 */
@Entity
@Table(name = "task_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskQuestion {

    /** @brief Field representing id in TaskQuestion. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** @brief Field representing task in TaskQuestion. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /** @brief Field representing question order in TaskQuestion. */
    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    /** @brief Field representing question text in TaskQuestion. */
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /** @brief Field representing correct answer in TaskQuestion. */
    @Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
    private String correctAnswer;

    /** @brief Field representing options json in TaskQuestion. */
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    /** @brief Field representing difficulty in TaskQuestion. */
    @Column(nullable = false)
    @Builder.Default
    private int difficulty = 2;

    /** @brief Field representing grammar rule in TaskQuestion. */
    @Column(name = "grammar_rule")
    private String grammarRule;
}
