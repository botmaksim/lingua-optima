package com.linguaoptima.api.domain.enums;

public enum DifficultyLevel {
    EASY(1),
    MEDIUM(2),
    HARD(3),
    EXPERT(4);

    private final int level;

    DifficultyLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static DifficultyLevel fromLevel(int level) {
        return switch (level) {
            case 1 -> EASY;
            case 2 -> MEDIUM;
            case 3 -> HARD;
            default -> EXPERT;
        };
    }
}
