/**
 * @file LeaderboardEntryResponse.java
 * @brief Response DTO representing a ranked student entry in an intra-group leaderboard.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @brief Response DTO representing a ranked student entry in an intra-group leaderboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {

    /** @brief Numerical rank position within the class cohort (1-indexed). */
    private int rank;

    /** @brief Unique identifier of the ranked student. */
    private UUID studentId;

    /** @brief Privacy-preserving display alias of the student. */
    private String displayAlias;

    /** @brief Cumulative weekly score earned by the student. */
    private double weeklyScore;

    /** @brief Current CEFR proficiency level of the student. */
    private CefrLevel cefrLevel;

    /** @brief Consecutive daily practice streak count of the student. */
    private int streakCount;

    /**
     * @brief Exposes studentId as userId for frontend compatibility.
     * @return Student UUID.
     */
    @JsonProperty("userId")
    public UUID getUserId() {
        return studentId;
    }

    /**
     * @brief Exposes displayAlias as alias for frontend compatibility.
     * @return Anonymized student display alias.
     */
    @JsonProperty("alias")
    public String getAlias() {
        return displayAlias;
    }
}
