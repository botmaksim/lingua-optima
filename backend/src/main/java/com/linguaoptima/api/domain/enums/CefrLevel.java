package com.linguaoptima.api.domain.enums;

/**
 * @file CefrLevel.java
 * @brief CEFR language proficiency levels (B1 Intermediate, B2 Upper-Intermediate, C1 Advanced).
 */
public enum CefrLevel {
    B1,
    B2,
    C1;

    public CefrLevel getNextLevel() {
        return switch (this) {
            case B1 -> B2;
            case B2 -> C1;
            case C1 -> C1;
        };
    }
}
