/**
 * @file CefrTopicRegistryTest.java
 * @brief Unit and slice test suite for CefrTopicRegistry.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit and slice test suite for CefrTopicRegistry.
 */
class CefrTopicRegistryTest {

    /**
     * @brief Verifies unit test scenario: cefr topics.
     */
    @Test
    void testCefrTopics() {
        List<String> b1 = CefrTopicRegistry.getTopicsForLevel(CefrLevel.B1);
        assertNotNull(b1);
        assertFalse(b1.isEmpty());

        List<String> b2 = CefrTopicRegistry.getTopicsForLevel(CefrLevel.B2);
        assertNotNull(b2);
        assertFalse(b2.isEmpty());

        List<String> c1 = CefrTopicRegistry.getTopicsForLevel(CefrLevel.C1);
        assertNotNull(c1);
        assertFalse(c1.isEmpty());

        List<String> domains = CefrTopicRegistry.getCommonDomains();
        assertNotNull(domains);
        assertFalse(domains.isEmpty());
    }
}
