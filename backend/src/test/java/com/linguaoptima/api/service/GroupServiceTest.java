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
 * @file GroupServiceTest.java
 * @brief Unit and slice test suite for GroupService.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GroupService groupService;

    private User teacher;
    private User otherTeacher;
    private User student;
    private Group group;

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

    @Test
    void testCreateGroup() {
        CreateGroupRequest req = CreateGroupRequest.builder().name("Advanced B2").build();
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupResponse res = groupService.createGroup(req, teacher);
        assertNotNull(res);
        assertEquals("Advanced B2", res.getName());
    }

    @Test
    void testGetGroupsAndDetails() {
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        assertEquals(1, groupService.getGroupsForTeacher(teacher).size());
        assertEquals("Advanced B2", groupService.getGroupDetails(group.getId(), teacher).getName());

        assertThrows(ForbiddenException.class, () -> groupService.getGroupDetails(group.getId(), otherTeacher));
    }

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

    @Test
    void testAddStudentMaxCapacityThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(student));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());
        when(groupStudentRepository.countByGroupIdAndIsActiveTrue(group.getId())).thenReturn(200);

        assertThrows(ForbiddenException.class, () -> groupService.addStudent(group.getId(), "student@lingua.com", teacher));
    }

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

    @Test
    void testGroupNotFoundThrows() {
        UUID randomId = UUID.randomUUID();
        when(groupRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.getGroupDetails(randomId, teacher));
    }

    @Test
    void testAddStudentUserNotFoundThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findByEmail("nonexistent@lingua.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.addStudent(group.getId(), "nonexistent@lingua.com", teacher));
    }

    @Test
    void testRemoveStudentNotFoundThrows() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndStudentId(group.getId(), student.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> groupService.removeStudent(group.getId(), student.getId(), teacher));
    }

    @Test
    void testDeleteGroup() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));

        groupService.deleteGroup(group.getId(), teacher);
        verify(groupRepository).delete(group);
    }
}
