package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
