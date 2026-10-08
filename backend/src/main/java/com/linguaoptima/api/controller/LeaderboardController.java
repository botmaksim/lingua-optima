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
 * Leaderboard Controller:
 * STRICT ARCHITECTURAL CONSTRAINT: NO GLOBAL LEADERBOARD.
 * Only group-scoped leaderboards exist in the system.
 */
@RestController
@RequestMapping({"/api/leaderboard", "/api/leaderboards"})
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/group/{id}")
    public ResponseEntity<List<LeaderboardEntryResponse>> getGroupLeaderboard(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(leaderboardService.getGroupLeaderboard(groupId, user));
    }
}
