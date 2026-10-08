package com.linguaoptima.api.domain.enums;

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
