/**
 * @file CefrLevelTest.java
 * @brief Unit tests for CefrLevel enum progression ladder.
 */
package com.linguaoptima.api.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @brief Unit tests verifying CEFR progression ladder transitions.
 */
class CefrLevelTest {

    /**
     * @brief Verifies that getNextLevel correctly advances through the CEFR scale and caps at C2.
     */
    @Test
    void testGetNextLevelLadder() {
        assertEquals(CefrLevel.A2, CefrLevel.A1.getNextLevel());
        assertEquals(CefrLevel.B1, CefrLevel.A2.getNextLevel());
        assertEquals(CefrLevel.B2, CefrLevel.B1.getNextLevel());
        assertEquals(CefrLevel.C1, CefrLevel.B2.getNextLevel());
        assertEquals(CefrLevel.C2, CefrLevel.C1.getNextLevel());
        assertEquals(CefrLevel.C2, CefrLevel.C2.getNextLevel());
    }

    /**
     * @brief Verifies valueOf and values contract for the 6 CEFR levels.
     */
    @Test
    void testCefrLevelValues() {
        assertEquals(6, CefrLevel.values().length);
        assertEquals(CefrLevel.A1, CefrLevel.valueOf("A1"));
        assertEquals(CefrLevel.A2, CefrLevel.valueOf("A2"));
        assertEquals(CefrLevel.B1, CefrLevel.valueOf("B1"));
        assertEquals(CefrLevel.B2, CefrLevel.valueOf("B2"));
        assertEquals(CefrLevel.C1, CefrLevel.valueOf("C1"));
        assertEquals(CefrLevel.C2, CefrLevel.valueOf("C2"));
    }
}
