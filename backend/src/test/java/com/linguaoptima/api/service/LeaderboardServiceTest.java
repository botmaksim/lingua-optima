/**
 * @file LeaderboardServiceTest.java
 * @brief Unit and slice test suite for LeaderboardService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
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
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

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
        student1 = User.builder().id(UUID.randomUUID()).displayAlias("EagleEye").fullName("John Doe").cefrLevel(CefrLevel.B2).role(Role.STUDENT).build();
        student2 = User.builder().id(UUID.randomUUID()).displayAlias(null).fullName("Jane Smith").cefrLevel(CefrLevel.B1).role(Role.STUDENT).build();
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

        TaskAssignment a1 = TaskAssignment.builder().assignedBy(teacher).status(AssignmentStatus.SUBMITTED).build();
        TaskAssignment a2 = TaskAssignment.builder().assignedBy(teacher).status(AssignmentStatus.PENDING).build();
        TaskAssignment aOther = TaskAssignment.builder().assignedBy(stranger).status(AssignmentStatus.SUBMITTED).build();
        TaskAssignment aNoTeacher = TaskAssignment.builder().assignedBy(null).status(AssignmentStatus.SUBMITTED).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs1, gs2));
        when(submissionRepository.findActiveGroupSubmissionsSince(eq(group.getId()), any())).thenReturn(List.of(sub1, sub2));
        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of(sub1, sub2));

        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(student1.getId())).thenReturn(List.of(a1, a2, aOther, aNoTeacher));
        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(student2.getId())).thenReturn(List.of());

        List<LeaderboardEntryResponse> board = leaderboardService.getGroupLeaderboard(group.getId(), teacher);

        assertEquals(2, board.size());
        assertEquals(1, board.get(0).getRank());
        assertEquals("EagleEye", board.get(0).getDisplayAlias());
        assertEquals("John Doe", board.get(0).getFullName());
        assertEquals(90.0, board.get(0).getWeeklyScore());
        assertEquals(1, board.get(0).getCompletedTasks());
        assertEquals(2, board.get(0).getTotalTasks());
        assertEquals(50, board.get(0).getCompletionRate());
        assertEquals(90.0, board.get(0).getAverageScore());

        assertEquals(2, board.get(1).getRank());
        assertEquals("Jane Smith", board.get(1).getDisplayAlias());
        assertEquals("Jane Smith", board.get(1).getFullName());
        assertEquals(70.0, board.get(1).getWeeklyScore());
        assertEquals(0, board.get(1).getTotalTasks());
        assertEquals(0, board.get(1).getCompletionRate());
        assertEquals(70.0, board.get(1).getAverageScore());
    }

    /**
     * @brief Verifies unit test scenario: fallback aliases and edge case scores.
     */
    @Test
    void testGetGroupLeaderboardAliasFallbacks() {
        User userLegacyAlias = User.builder().id(UUID.randomUUID()).displayAlias("Linguist #12345678").fullName("Real Name").cefrLevel(CefrLevel.B1).role(Role.STUDENT).build();
        User userNoName = User.builder().id(UUID.randomUUID()).displayAlias("").fullName("   ").cefrLevel(CefrLevel.A2).role(Role.STUDENT).build();
        User userNoId = User.builder().id(null).displayAlias(null).fullName(null).cefrLevel(CefrLevel.A1).role(Role.STUDENT).build();

        GroupStudent gsLegacy = GroupStudent.builder().group(group).student(userLegacyAlias).isActive(true).build();
        GroupStudent gsNoName = GroupStudent.builder().group(group).student(userNoName).isActive(true).build();
        GroupStudent gsNoId = GroupStudent.builder().group(group).student(userNoId).isActive(true).build();

        Submission subLegacy = Submission.builder().student(userLegacyAlias).aiScore(30.0).submittedAt(LocalDateTime.now()).build();
        Submission subNoName = Submission.builder().student(userNoName).aiScore(20.0).submittedAt(LocalDateTime.now()).build();
        Submission subNoId = Submission.builder().student(userNoId).aiScore(10.0).submittedAt(LocalDateTime.now()).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gsLegacy, gsNoName, gsNoId));
        when(submissionRepository.findActiveGroupSubmissionsSince(eq(group.getId()), any())).thenReturn(List.of(subLegacy, subNoName, subNoId));
        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of());

        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(userLegacyAlias.getId())).thenReturn(List.of());
        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(userNoName.getId())).thenReturn(List.of());
        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(null)).thenReturn(List.of());

        List<LeaderboardEntryResponse> board = leaderboardService.getGroupLeaderboard(group.getId(), teacher);

        assertEquals(3, board.size());
        assertEquals(30.0, board.get(0).getWeeklyScore());
        assertEquals("Real Name", board.get(0).getDisplayAlias());
        assertEquals(20.0, board.get(1).getWeeklyScore());
        assertTrue(board.get(1).getDisplayAlias().startsWith("Linguist #"));
        assertEquals(10.0, board.get(2).getWeeklyScore());
        assertEquals("Linguist #Learner", board.get(2).getDisplayAlias());
        assertEquals(0.0, board.get(0).getAverageScore());
    }

    /**
     * @brief Verifies unit test scenario: get group leaderboard stranger forbidden.
     */
    @Test
    void testGetGroupLeaderboardStrangerForbidden() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), stranger.getId())).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> leaderboardService.getGroupLeaderboard(group.getId(), stranger));

        UUID missingGroup = UUID.randomUUID();
        when(groupRepository.findById(missingGroup)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> leaderboardService.getGroupLeaderboard(missingGroup, teacher));

        User blankAliasStudent = User.builder().id(UUID.randomUUID()).displayAlias("   ").cefrLevel(CefrLevel.B1).role(Role.STUDENT).build();
        User softDeletedStudent = User.builder().id(UUID.randomUUID()).displayAlias("Ghost").cefrLevel(CefrLevel.C1).role(Role.STUDENT).build();
        GroupStudent activeGs = GroupStudent.builder().group(group).student(blankAliasStudent).isActive(true).build();
        Submission activeSub = Submission.builder().student(blankAliasStudent).aiScore(80.0).submittedAt(LocalDateTime.now()).build();
        Submission ghostSub = Submission.builder().student(softDeletedStudent).aiScore(100.0).submittedAt(LocalDateTime.now()).build();

        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), blankAliasStudent.getId())).thenReturn(true);
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(activeGs));
        when(submissionRepository.findActiveGroupSubmissionsSince(eq(group.getId()), any())).thenReturn(List.of(activeSub, ghostSub));
        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of(activeSub));
        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(blankAliasStudent.getId())).thenReturn(List.of());

        List<LeaderboardEntryResponse> memberBoard = leaderboardService.getGroupLeaderboard(group.getId(), blankAliasStudent);
        assertEquals(1, memberBoard.size(), "Soft-deleted student's submissions must be excluded from leaderboard rankings");
        assertTrue(memberBoard.get(0).getDisplayAlias().startsWith("Linguist #"));
    }
}

