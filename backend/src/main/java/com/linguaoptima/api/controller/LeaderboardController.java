/**
 * @file LeaderboardController.java
 * @brief REST controller providing privacy-first cohort-scoped leaderboards.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller providing privacy-first cohort-scoped leaderboards.
 *
 * Enforces strict design constraint: no global leaderboard exists.
 * Leaderboards are only accessible within designated study groups with masked aliases.
 */
@RestController
@RequestMapping({"/api/leaderboard", "/api/leaderboards"})
@RequiredArgsConstructor
public class LeaderboardController {

    /** @brief Field representing leaderboard service in LeaderboardController. */
    private final LeaderboardService leaderboardService;

    /**
     * @brief Calculates anonymized ranking positions for members of a specific cohort.
     *
     * @param groupId Unique identifier of the study group.
     * @param user Authenticated user requesting the leaderboard.
     * @return HTTP 200 with list of leaderboard entries sorted by score.
     */
    @GetMapping("/group/{id}")
    public ResponseEntity<List<LeaderboardEntryResponse>> getGroupLeaderboard(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(leaderboardService.getGroupLeaderboard(groupId, user));
    }
}
