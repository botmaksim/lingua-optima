package com.linguaoptima.api.domain.enums;

/**
 * @file DifficultyLevel.java
 * @brief Question and task difficulty levels used in Computerized Adaptive Testing (CAT).
 */
public enum DifficultyLevel {
    EASY(1),
    MEDIUM(2),
    HARD(3),
    EXPERT(4);

    private final int level;

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
