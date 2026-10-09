/**
 * @file AiAnalysisResponse.java
 * @brief Response DTO representing synthesized AI analysis of student strengths, weaknesses, and recommended study gaps.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @brief Response DTO representing synthesized AI analysis of student strengths, weaknesses, and recommended study gaps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    /** @brief High-level summary of performance on this exercise. */
    private String summary;

    /** @brief Specific grammar topics or linguistic structures where errors occurred. */
    @Builder.Default
    private List<String> weaknesses = new ArrayList<>();

    /** @brief Concepts successfully demonstrated with high accuracy. */
    @Builder.Default
    private List<String> strengths = new ArrayList<>();

    /** @brief Actionable personalized pedagogical recommendations from the AI coach. */
    private String recommendations;

    /** @brief Suggested next topics or practice areas to close detected gaps. */
    @Builder.Default
    private List<String> suggestedTopics = new ArrayList<>();
}
