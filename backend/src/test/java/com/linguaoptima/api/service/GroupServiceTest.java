/**
 * @file GroupServiceTest.java
 * @brief Unit and slice test suite for GroupService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.enums.EnrollmentStatus;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupInvitationResponse;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.NotificationRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UserRepository;
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
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for GroupService.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    /** @brief Test fixture or mock dependency for group repository. */
    @Mock
    private GroupRepository groupRepository;
    /** @brief Test fixture or mock dependency for group student repository. */
    @Mock
    private GroupStudentRepository groupStudentRepository;
    /** @brief Test fixture or mock dependency for user repository. */
    @Mock
    private UserRepository userRepository;
    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Test fixture or mock dependency for notification service. */
    @Mock
    private NotificationService notificationService;
    /** @brief Test fixture or mock dependency for notification repository. */
    @Mock
    private NotificationRepository notificationRepository;
    /** @brief Pricing properties mock for tiered quota testing. */
    @Mock
    private PricingProperties pricingProperties;
    /** @brief Subscription repository mock for educator tier testing. */
    @Mock
    private SubscriptionRepository subscriptionRepository;

    /** @brief Test fixture or mock dependency for group service. */
    @InjectMocks
    private GroupService groupService;

    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;
    /** @brief Test fixture or mock dependency for other teacher. */
    private User otherTeacher;
    /** @brief Test fixture or mock dependency for student. */
    private User student;
    /** @brief Test fixture or mock dependency for group. */
    private Group group;

    /**
     * @brief Initializes test fixtures and mock state before each test in GroupServiceTest.
     */
    @BeforeEach
    void setUp() {
        teacher = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@lingua.com")
            .fullName("Teacher Alice")
            .role(Role.TEACHER)
            .build();

        otherTeacher = User.builder()
            .id(UUID.randomUUID())
            .email("other@lingua.com")
            .role(Role.TEACHER)
            .build();

        student = User.builder()
            .id(UUID.randomUUID())
            .email("student@lingua.com")
            .fullName("Bob Student")
            .role(Role.STUDENT)
            .build();

        group = Group.builder()
            .id(UUID.randomUUID())
            .name("Advanced B2")
            .teacher(teacher)
            .build();

        lenient().when(pricingProperties.getTierConfig(any())).thenReturn(
            PricingProperties.TierConfig.builder().maxGroups(50).build()
        );
    }

    /**
     * @brief Verifies unit test scenario: create group.
     */
    @Test
    void testCreateGroup() {
        CreateGroupRequest req = CreateGroupRequest.builder().name("Advanced B2").build();
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupResponse res = groupService.createGroup(req, teacher);
        assertNotNull(res);
        assertEquals("Advanced B2", res.getName());
    }

    /**
     * @brief Verifies unit test scenario: get groups and details.
     */
    @Test
    void testGetGroupsAndDetails() {
        GroupStudent gs = GroupStudent.builder().group(group).student(student).isActive(true).build();
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.findByStudentIdAndIsActiveTrue(student.getId())).thenReturn(List.of(gs));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertEquals(1, groupService.getGroupsForTeacher(teacher).size());
        assertEquals(1, groupService.getGroupsForTeacher(student).size());
        assertEquals("Advanced B2", groupService.getGroupDetails(group.getId(), teacher).getName());

        assertThrows(ForbiddenException.class, () -> groupService.getGroupDetails(group.getId(), otherTeacher));
    }

    /**
     * @brief Verifies unit test scenario: add new student sends invitation.
     */
    @Test
    void testAddNewStudent() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());
        when(groupStudentRepository.countByGroupIdAndIsActiveTrue(group.getId())).thenReturn(10);

        groupService.addStudent(group.getId(), "student@lingua.com", teacher);

        verify(groupStudentRepository).save(argThat(gs -> !gs.isActive() && gs.getStatus() == EnrollmentStatus.PENDING && gs.getStudent().equals(student)));
        verify(notificationService).send(eq(student), anyString(), eq(NotificationType.GROUP_INVITATION), eq(group.getId()));
    }

    /**
     * @brief Verifies unit test scenario: add student re-invites previously declined or removed student.
     */
    @Test
    void testAddStudentReactivatesSoftDeleted() {
        GroupStudent softDeleted = GroupStudent.builder()
            .group(group)
            .student(student)
            .isActive(false)
            .status(EnrollmentStatus.DECLINED)
            .removedAt(LocalDateTime.now().minusDays(3))
            .build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.of(softDeleted));

        groupService.addStudent(group.getId(), "student@lingua.com", teacher);

        assertFalse(softDeleted.isActive());
        assertEquals(EnrollmentStatus.PENDING, softDeleted.getStatus());
        assertNull(softDeleted.getRemovedAt());
        verify(groupStudentRepository).save(softDeleted);
    }

    /**
     * @brief Verifies unit test scenario: add student already active throws.
     */
    @Test
    void testAddStudentAlreadyActiveThrows() {
        GroupStudent activeMember = GroupStudent.builder()
            .group(group)
            .student(student)
            .isActive(true)
            .status(EnrollmentStatus.ACCEPTED)
            .build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.of(activeMember));

        assertThrows(IllegalStateException.class, () -> groupService.addStudent(group.getId(), "student@lingua.com", teacher));
    }

    /**
     * @brief Verifies unit test scenario: get pending invitations.
     */
    @Test
    void testGetPendingInvitations() {
        GroupStudent gs = GroupStudent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .student(student)
            .status(EnrollmentStatus.PENDING)
            .isActive(false)
            .joinedAt(LocalDateTime.now())
            .build();

        when(groupStudentRepository.findByStudentIdAndStatus(student.getId(), EnrollmentStatus.PENDING))
            .thenReturn(List.of(gs));

        List<GroupInvitationResponse> invites = groupService.getPendingInvitations(student);
        assertEquals(1, invites.size());
        assertEquals(group.getName(), invites.get(0).getGroupName());
        assertEquals("Teacher Alice", invites.get(0).getTeacherName());
    }

    /**
     * @brief Verifies unit test scenario: accept invitation.
     */
    @Test
    void testAcceptInvitation() {
        GroupStudent gs = GroupStudent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .student(student)
            .status(EnrollmentStatus.PENDING)
            .isActive(false)
            .build();

        Notification notif = Notification.builder()
            .id(UUID.randomUUID())
            .user(student)
            .type(NotificationType.GROUP_INVITATION)
            .referenceId(group.getId())
            .isRead(false)
            .build();

        when(groupStudentRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), student.getId(), EnrollmentStatus.PENDING))
            .thenReturn(Optional.of(gs));
        when(notificationRepository.findByUserIdAndTypeAndReferenceId(student.getId(), NotificationType.GROUP_INVITATION, group.getId()))
            .thenReturn(List.of(notif));

        GroupResponse res = groupService.acceptInvitation(group.getId(), student);
        assertNotNull(res);
        assertTrue(gs.isActive());
        assertEquals(EnrollmentStatus.ACCEPTED, gs.getStatus());
        assertTrue(notif.isRead());
        verify(groupStudentRepository).save(gs);
        verify(notificationRepository).save(notif);
        verify(notificationService).send(eq(teacher), anyString(), eq(NotificationType.SYSTEM));
    }

    /**
     * @brief Verifies unit test scenario: accept invitation not found throws.
     */
    @Test
    void testAcceptInvitationNotFoundThrows() {
        when(groupStudentRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), student.getId(), EnrollmentStatus.PENDING))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.acceptInvitation(group.getId(), student));
    }

    /**
     * @brief Verifies unit test scenario: decline invitation.
     */
    @Test
    void testDeclineInvitation() {
        GroupStudent gs = GroupStudent.builder()
            .id(UUID.randomUUID())
            .group(group)
            .student(student)
            .status(EnrollmentStatus.PENDING)
            .isActive(false)
            .build();

        Notification notif = Notification.builder()
            .id(UUID.randomUUID())
            .user(student)
            .type(NotificationType.GROUP_INVITATION)
            .referenceId(group.getId())
            .isRead(false)
            .build();

        when(groupStudentRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), student.getId(), EnrollmentStatus.PENDING))
            .thenReturn(Optional.of(gs));
        when(notificationRepository.findByUserIdAndTypeAndReferenceId(student.getId(), NotificationType.GROUP_INVITATION, group.getId()))
            .thenReturn(List.of(notif));

        groupService.declineInvitation(group.getId(), student);
        assertFalse(gs.isActive());
        assertEquals(EnrollmentStatus.DECLINED, gs.getStatus());
        assertNotNull(gs.getRemovedAt());
        assertTrue(notif.isRead());
        verify(groupStudentRepository).save(gs);
        verify(notificationRepository).save(notif);
        verify(notificationService).send(eq(teacher), anyString(), eq(NotificationType.SYSTEM));
    }

    /**
     * @brief Verifies unit test scenario: decline invitation not found throws.
     */
    @Test
    void testDeclineInvitationNotFoundThrows() {
        when(groupStudentRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), student.getId(), EnrollmentStatus.PENDING))
            .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.declineInvitation(group.getId(), student));
    }

    /**
     * @brief Verifies unit test scenario: add student max capacity throws.
     */
    @Test
    void testAddStudentMaxCapacityThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());
        when(groupStudentRepository.countByGroupIdAndIsActiveTrue(group.getId())).thenReturn(200);

        assertThrows(ForbiddenException.class, () -> groupService.addStudent(group.getId(), "student@lingua.com", teacher));
    }

    /**
     * @brief Verifies unit test scenario: remove student soft delete.
     */
    @Test
    void testRemoveStudentSoftDelete() {
        GroupStudent activeMember = GroupStudent.builder()
            .group(group)
            .student(student)
            .isActive(true)
            .build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.of(activeMember));

        groupService.removeStudent(group.getId(), student.getId(), teacher);

        assertFalse(activeMember.isActive());
        assertNotNull(activeMember.getRemovedAt());
        verify(groupStudentRepository).save(activeMember);
    }

    /**
     * @brief Verifies unit test scenario: get group details with submissions and students.
     */
    @Test
    void testGetGroupDetailsWithSubmissionsAndStudents() {
        GroupStudent gs = GroupStudent.builder().group(group).student(student).isActive(true).build();
        Submission sub = Submission.builder().id(UUID.randomUUID()).student(student).aiScore(88.0).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs));
        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of(sub));

        GroupResponse res = groupService.getGroupDetails(group.getId(), teacher);
        assertNotNull(res);
        assertEquals(1, res.getStudentCount());
        assertEquals(88.0, res.getAvgScore());
        assertEquals(1, res.getStudents().size());
    }

    /**
     * @brief Verifies unit test scenario: get group details with pending students.
     */
    @Test
    void testGetGroupDetailsWithPendingStudents() {
        GroupStudent activeGs = GroupStudent.builder().group(group).student(student).isActive(true).build();
        User pendingStudent = User.builder().id(UUID.randomUUID()).email("pending@lingua.com").fullName("Pending Student").build();
        GroupStudent pendingGs = GroupStudent.builder().group(group).student(pendingStudent).status(EnrollmentStatus.PENDING).isActive(false).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(activeGs));
        when(groupStudentRepository.findByGroupIdAndStatus(group.getId(), EnrollmentStatus.PENDING)).thenReturn(List.of(pendingGs));
        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of());

        GroupResponse res = groupService.getGroupDetails(group.getId(), teacher);
        assertNotNull(res);
        assertEquals(1, res.getStudentCount());
        assertEquals(1, res.getPendingStudents().size());
        assertEquals("Pending Student", res.getPendingStudents().get(0).getFullName());
    }

    /**
     * @brief Verifies unit test scenario: group not found throws.
     */
    @Test
    void testGroupNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        when(groupRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.getGroupDetails(randomId, teacher));
    }

    /**
     * @brief Verifies unit test scenario: add student user not found throws.
     */
    @Test
    void testAddStudentUserNotFoundThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("nonexistent@lingua.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.addStudent(group.getId(), "nonexistent@lingua.com", teacher));
    }

    /**
     * @brief Verifies unit test scenario: remove student not found throws.
     */
    @Test
    void testRemoveStudentNotFoundThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.removeStudent(group.getId(), student.getId(), teacher));
    }

    /**
     * @brief Verifies unit test scenario: delete group.
     */
    @Test
    void testDeleteGroup() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.deleteGroup(group.getId(), teacher);
        verify(groupRepository).delete(group);
    }

    @Test
    void testCreateGroup_QuotaExceeded() {
        when(pricingProperties.getTierConfig(any())).thenReturn(
            PricingProperties.TierConfig.builder().maxGroups(1).build()
        );
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));

        CreateGroupRequest req = CreateGroupRequest.builder().name("Second Cohort").build();
        assertThrows(QuotaExceededException.class, () -> groupService.createGroup(req, teacher));
    }

    @Test
    void testGetGroupsForTeacher_MarksLockedWhenOverQuota() {
        Group group2 = Group.builder().id(UUID.randomUUID()).name("Locked Group").teacher(teacher).build();
        when(pricingProperties.getTierConfig(any())).thenReturn(
            PricingProperties.TierConfig.builder().maxGroups(1).build()
        );
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group, group2));

        List<GroupResponse> responses = groupService.getGroupsForTeacher(teacher);
        assertEquals(2, responses.size());
        assertFalse(responses.get(0).isLocked());
        assertTrue(responses.get(1).isLocked());
    }

    @Test
    void testValidateGroupIsActive() {
        Group group2 = Group.builder().id(UUID.randomUUID()).name("Locked Group").teacher(teacher).build();
        when(pricingProperties.getTierConfig(any())).thenReturn(
            PricingProperties.TierConfig.builder().maxGroups(1).build()
        );
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group, group2));

        // Group 1 (index 0) is within quota limit 1
        assertDoesNotThrow(() -> groupService.validateGroupIsActive(group.getId(), teacher));

        // Group 2 (index 1) exceeds quota limit 1 -> locked
        assertThrows(QuotaExceededException.class, () ->
            groupService.validateGroupIsActive(group2.getId(), teacher)
        );
    }
}
