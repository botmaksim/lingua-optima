/**
 * @file CefrLevel.java
 * @brief Complete CEFR language proficiency scale from Beginner (A1) to Mastery (C2).
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Complete CEFR language proficiency scale (A1, A2, B1, B2, C1, C2).
 */
public enum CefrLevel {
    /** @brief Constant or enum value representing beginner A1 level. */
    A1,
    /** @brief Constant or enum value representing elementary A2 level. */
    A2,
    /** @brief Constant or enum value representing intermediate B1 level. */
    B1,
    /** @brief Constant or enum value representing upper-intermediate B2 level. */
    B2,
    /** @brief Constant or enum value representing advanced C1 level. */
    C1,
    /** @brief Constant or enum value representing mastery C2 level. */
    C2;

    /**
     * @brief Returns the next higher CEFR level in the progression ladder.
     * @return Next CEFR proficiency level (or C2 if already at maximum).
     */
    public CefrLevel getNextLevel() {
        return switch (this) {
            case A1 -> A2;
            case A2 -> B1;
            case B1 -> B2;
            case B2 -> C1;
            case C1 -> C2;
            case C2 -> C2;
        };
    }
}
