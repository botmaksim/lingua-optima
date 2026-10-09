package com.linguaoptima.api.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * @file TaskQuestion.java
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "correct_answer", columnDefinition = "TEXT", nullable = false)
    private String correctAnswer;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    @Column(nullable = false)
    @Builder.Default
    private int difficulty = 2;

    @Column(name = "grammar_rule")
    private String grammarRule;
}
