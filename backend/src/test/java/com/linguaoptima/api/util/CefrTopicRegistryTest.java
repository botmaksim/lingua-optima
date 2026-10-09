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
        for (CefrLevel level : CefrLevel.values()) {
            List<String> topics = CefrTopicRegistry.getTopicsForLevel(level);
            assertNotNull(topics);
            assertFalse(topics.isEmpty());
        }

        List<String> nullTopics = CefrTopicRegistry.getTopicsForLevel(null);
        assertNotNull(nullTopics);
        assertTrue(nullTopics.isEmpty());

        List<String> domains = CefrTopicRegistry.getCommonDomains();
        assertNotNull(domains);
        assertFalse(domains.isEmpty());
    }
}
