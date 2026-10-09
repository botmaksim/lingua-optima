/**
 * @file LeaderboardServiceTest.java
 * @brief Unit and slice test suite for LeaderboardService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for LeaderboardService.
 */
@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    /** @brief Test fixture or mock dependency for group repository. */
    @Mock
    private GroupRepository groupRepository;
    /** @brief Test fixture or mock dependency for group student repository. */
    @Mock
    private GroupStudentRepository groupStudentRepository;
    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;

    /** @brief Test fixture or mock dependency for leaderboard service. */
    @InjectMocks
    private LeaderboardService leaderboardService;

    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;
    /** @brief Test fixture or mock dependency for student1. */
    private User student1;
    /** @brief Test fixture or mock dependency for student2. */
    private User student2;
    /** @brief Test fixture or mock dependency for stranger. */
    private User stranger;
    /** @brief Test fixture or mock dependency for group. */
    private Group group;

    /**
     * @brief Initializes test fixtures and mock state before each test in LeaderboardServiceTest.
     */
    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        student1 = User.builder().id(UUID.randomUUID()).displayAlias("EagleEye").cefrLevel(CefrLevel.B2).role(Role.STUDENT).build();
        student2 = User.builder().id(UUID.randomUUID()).displayAlias(null).cefrLevel(CefrLevel.B1).role(Role.STUDENT).build();
        stranger = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();

        group = Group.builder().id(UUID.randomUUID()).name("Group B1").teacher(teacher).build();
    }

    /**
     * @brief Verifies unit test scenario: get group leaderboard success.
     */
    @Test
    void testGetGroupLeaderboardSuccess() {
        GroupStudent gs1 = GroupStudent.builder().group(group).student(student1).isActive(true).build();
        GroupStudent gs2 = GroupStudent.builder().group(group).student(student2).isActive(true).build();

        Submission sub1 = Submission.builder().student(student1).aiScore(90.0).submittedAt(LocalDateTime.now()).build();
        Submission sub2 = Submission.builder().student(student2).aiScore(70.0).submittedAt(LocalDateTime.now()).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs1, gs2));
        when(submissionRepository.findActiveGroupSubmissionsSince(eq(group.getId()), any())).thenReturn(List.of(sub1, sub2));

        List<LeaderboardEntryResponse> board = leaderboardService.getGroupLeaderboard(group.getId(), teacher);

        assertEquals(2, board.size());
        assertEquals(1, board.get(0).getRank());
        assertEquals("EagleEye", board.get(0).getDisplayAlias());
        assertEquals(90.0, board.get(0).getWeeklyScore());

        assertEquals(2, board.get(1).getRank());
        assertTrue(board.get(1).getDisplayAlias().startsWith("Linguist #"));
        assertEquals(70.0, board.get(1).getWeeklyScore());
    }

    /**
     * @brief Verifies unit test scenario: get group leaderboard stranger forbidden.
     */
    @Test
    void testGetGroupLeaderboardStrangerForbidden() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), stranger.getId())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> leaderboardService.getGroupLeaderboard(group.getId(), stranger));
    }
}
