package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @file LeaderboardEntryResponse.java
 * @brief Response DTO representing a ranked student entry in an intra-group leaderboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {

    private int rank;
    private UUID studentId;
    private String displayAlias;
    private double weeklyScore;
    private CefrLevel cefrLevel;
}
