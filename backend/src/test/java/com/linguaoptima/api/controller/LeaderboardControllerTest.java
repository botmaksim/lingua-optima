package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock
    private LeaderboardService leaderboardService;

    @InjectMocks
    private LeaderboardController leaderboardController;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testGetGroupLeaderboard() {
        UUID groupId = UUID.randomUUID();
        LeaderboardEntryResponse entry = LeaderboardEntryResponse.builder().rank(1).displayAlias("Linguist #1").weeklyScore(88.0).build();

        when(leaderboardService.getGroupLeaderboard(groupId, user)).thenReturn(List.of(entry));

        ResponseEntity<List<LeaderboardEntryResponse>> res = leaderboardController.getGroupLeaderboard(groupId, user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
        assertEquals(1, res.getBody().get(0).getRank());
    }
}
