/**
 * @file TaskType.java
 * @brief Supported pedagogical task formats for AI generation and evaluation.
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Supported pedagogical task formats for AI generation and evaluation.
 */
public enum TaskType {
    /** @brief Constant or enum value representing mcq in TaskType. */
    MCQ,
    /** @brief Constant or enum value representing gap fill in TaskType. */
    GAP_FILL,
    /** @brief Constant or enum value representing essay in TaskType. */
    ESSAY,
    /** @brief Constant or enum value representing rewrite in TaskType. */
    REWRITE,
    /** @brief Constant or enum value representing open brackets (раскрытие скобок) in TaskType. */
    OPEN_BRACKETS,
    /** @brief Constant or enum value representing short answer in TaskType. */
    SHORT_ANSWER
}
