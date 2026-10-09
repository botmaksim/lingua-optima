/**
 * @file CefrLevel.java
 * @brief CEFR language proficiency levels (B1 Intermediate, B2 Upper-Intermediate, C1 Advanced).
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief CEFR language proficiency levels (B1 Intermediate, B2 Upper-Intermediate, C1 Advanced).
 */
public enum CefrLevel {
    /** @brief Constant or enum value representing b1 in CefrLevel. */
    B1,
    /** @brief Constant or enum value representing b2 in CefrLevel. */
    B2,
    /** @brief Constant or enum value representing c1 in CefrLevel. */
    C1;

    /**
     * @brief Returns the next higher CEFR level in the progression ladder.
     * @return Next CEFR proficiency level (or C1 if already at maximum).
     */
    public CefrLevel getNextLevel() {
        return switch (this) {
            case B1 -> B2;
            case B2 -> C1;
            case C1 -> C1;
        };
    }
}
