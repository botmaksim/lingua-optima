/**
 * @file DifficultyLevel.java
 * @brief Question and task difficulty levels used in Computerized Adaptive Testing (CAT).
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Question and task difficulty levels used in Computerized Adaptive Testing (CAT).
 */
public enum DifficultyLevel {
    /** @brief Constant or enum value representing easy in DifficultyLevel. */
    EASY(1),
    /** @brief Constant or enum value representing medium in DifficultyLevel. */
    MEDIUM(2),
    /** @brief Constant or enum value representing hard in DifficultyLevel. */
    HARD(3),
    /** @brief Constant or enum value representing expert in DifficultyLevel. */
    EXPERT(4);

    /** @brief Field representing level in DifficultyLevel. */
    private final int level;

    /**
     * @brief Constructs a new DifficultyLevel instance.
     */
    DifficultyLevel(int level) {
        this.level = level;
    }

    /**
     * @brief Retrieves the numeric integer value of the difficulty level.
     * @return Numeric difficulty (1 to 4).
     */
    public int getLevel() {
        return level;
    }

    /**
     * @brief Resolves a DifficultyLevel enum constant from its numeric level.
     * @param level Numeric difficulty level (1 to 4).
     * @return Corresponding DifficultyLevel constant.
     */
    public static DifficultyLevel fromLevel(int level) {
        return switch (level) {
            case 1 -> EASY;
            case 2 -> MEDIUM;
            case 3 -> HARD;
            default -> EXPERT;
        };
    }
}
