/**
 * @file TopicsCatalogResponse.java
 * @brief DTO providing comprehensive syllabus topics, mixed grammar challenges, and situational domains.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @brief Response DTO delivering categorized syllabus topics and domain options for task generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicsCatalogResponse {

    /** @brief Core CEFR syllabus grammar topics keyed by proficiency level. */
    private Map<CefrLevel, List<String>> topicsByLevel;

    /** @brief Dedicated mixed grammar challenges keyed by proficiency level. */
    private Map<CefrLevel, List<String>> mixedTopicsByLevel;

    /** @brief Cross-level thematic mixed challenges (e.g. Business, Academic, IELTS). */
    private List<String> crossLevelTopics;

    /** @brief Common situational vocabulary domains. */
    private List<String> domains;
}
