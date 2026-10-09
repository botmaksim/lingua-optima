/**
 * @file NotificationType.java
 * @brief Categories of real-time and persisted user notifications.
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Categories of real-time and persisted user notifications.
 */
public enum NotificationType {
    /** @brief Notification for a newly assigned curriculum task. */
    TASK,
    /** @brief Notification for an evaluated or teacher-overridden grade. */
    GRADE,
    /** @brief System-wide or account lifecycle notification. */
    SYSTEM,
    /** @brief Contextual learning recommendation or level-up prompt. */
    CONTEXTUAL,
    /** @brief Daily practice streak milestone or freeze alert. */
    STREAK,
    /** @brief CEFR proficiency level promotion alert. */
    LEVEL_UP
}
