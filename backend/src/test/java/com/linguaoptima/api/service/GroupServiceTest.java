/**
 * @file GroupServiceTest.java
 * @brief Unit and slice test suite for GroupService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.CreateGroupRequest;
import com.linguaoptima.api.dto.response.GroupResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
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
     * @brief Verifies unit test scenario: add new student.
     */
    @Test
    void testAddNewStudent() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());
        when(groupStudentRepository.countByGroupIdAndIsActiveTrue(group.getId())).thenReturn(10);

        groupService.addStudent(group.getId(), "student@lingua.com", teacher);

        verify(groupStudentRepository).save(argThat(gs -> gs.isActive() && gs.getStudent().equals(student)));
        verify(notificationService).send(eq(student), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: add student reactivates soft deleted.
     */
    @Test
    void testAddStudentReactivatesSoftDeleted() {
        GroupStudent softDeleted = GroupStudent.builder()
            .group(group)
            .student(student)
            .isActive(false)
            .removedAt(LocalDateTime.now().minusDays(3))
            .build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.of(softDeleted));

        groupService.addStudent(group.getId(), "student@lingua.com", teacher);

        assertTrue(softDeleted.isActive());
        assertNull(softDeleted.getRemovedAt());
        verify(groupStudentRepository).save(softDeleted);
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
}
