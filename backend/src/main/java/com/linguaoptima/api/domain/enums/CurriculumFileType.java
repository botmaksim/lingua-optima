/**
 * @file CurriculumFileType.java
 * @brief Discriminator enum for uploaded curriculum materials (grammar rules or vocabulary lists).
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Discriminator enum for uploaded curriculum materials (grammar rules or vocabulary lists).
 */
public enum CurriculumFileType {
    /** @brief Grammar rule reference or pedagogical syntax explanation. */
    RULE,
    /** @brief Target vocabulary, idiom, or collocation reference word list. */
    VOCABULARY
}
