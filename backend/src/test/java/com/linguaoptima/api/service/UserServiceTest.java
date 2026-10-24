/**
 * @file UserServiceTest.java
 * @brief Unit and slice test suite for UserService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
import com.linguaoptima.api.dto.request.UpdateStudentNameRequest;
import com.linguaoptima.api.dto.request.UpdateUserRequest;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private ProgressRecordRepository progressRecordRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UsageCounterRepository usageCounterRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private SessionStateRepository sessionStateRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TeacherStudentAliasRepository teacherStudentAliasRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
            .id(UUID.randomUUID())
            .email("test@lingua.com")
            .fullName("Original Name")
            .passwordHash("hashedPass")
            .role(Role.STUDENT)
            .cefrLevel(CefrLevel.B1)
            .displayAlias("Linguist #1234")
            .build();
    }

    /**
     * @brief Verifies unit test scenario: get current user.
     */
    @Test
    void testGetCurrentUser() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        UserResponse response = userService.getCurrentUser(sampleUser);
        assertNotNull(response);
        assertEquals("test@lingua.com", response.getEmail());

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser(sampleUser));
    }

    /**
     * @brief Verifies unit test scenario: update user.
     */
    @Test
    void testUpdateUser() {
        UpdateUserRequest req = UpdateUserRequest.builder()
            .fullName("New Name")
            .displayAlias("Polyglot")
            .cefrLevel(CefrLevel.B2)
            .role(Role.TEACHER)
            .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.updateUser(sampleUser, req);
        assertEquals("New Name", res.getFullName());
        assertEquals("Polyglot", res.getDisplayAlias());
        assertEquals(CefrLevel.B2, res.getCefrLevel());
        assertEquals(Role.TEACHER, res.getRole());

        UpdateUserRequest blankAndNullReq = UpdateUserRequest.builder()
            .fullName("   ")
            .displayAlias("   ")
            .cefrLevel(null)
            .role(null)
            .build();
        UserResponse preserved1 = userService.updateUser(sampleUser, blankAndNullReq);
        assertEquals("New Name", preserved1.getFullName());
        assertEquals("Polyglot", preserved1.getDisplayAlias());
        assertEquals(CefrLevel.B2, preserved1.getCefrLevel());
        assertEquals(Role.TEACHER, preserved1.getRole());

        UpdateUserRequest allNullReq = UpdateUserRequest.builder()
            .fullName(null)
            .displayAlias(null)
            .cefrLevel(null)
            .role(null)
            .build();
        UserResponse preserved2 = userService.updateUser(sampleUser, allNullReq);
        assertEquals("New Name", preserved2.getFullName());
        assertEquals("Polyglot", preserved2.getDisplayAlias());
        assertEquals(Role.TEACHER, preserved2.getRole());
    }

    /**
     * @brief Verifies unit test scenario: change password success.
     */
    @Test
    void testChangePasswordSuccess() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
            .oldPassword("oldSecret")
            .newPassword("newSecret123")
            .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldSecret", "hashedPass")).thenReturn(true);
        when(passwordEncoder.encode("newSecret123")).thenReturn("newHashedPass");

        assertDoesNotThrow(() -> userService.changePassword(sampleUser, req));
        verify(userRepository).save(argThat(u -> "newHashedPass".equals(u.getPasswordHash())));
    }

    /**
     * @brief Verifies unit test scenario: change password wrong old password throws.
     */
    @Test
    void testChangePasswordWrongOldPasswordThrows() {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
            .oldPassword("wrongSecret")
            .newPassword("newSecret123")
            .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongSecret", "hashedPass")).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> userService.changePassword(sampleUser, req));
    }

    /**
     * @brief Verifies unit test scenario: delete account gdpr cascade.
     */
    @Test
    void testDeleteAccountGdprCascade() {
        UUID uid = sampleUser.getId();
        when(apiKeyRepository.findAllByUserId(uid)).thenReturn(List.of(ApiKey.builder().id(UUID.randomUUID()).build()));
        when(progressRecordRepository.findByStudentId(uid)).thenReturn(List.of(ProgressRecord.builder().id(UUID.randomUUID()).build()));
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(uid)).thenReturn(List.of(Submission.builder().id(UUID.randomUUID()).build()));
        when(taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(uid)).thenReturn(List.of(TaskAssignment.builder().id(UUID.randomUUID()).build()));

        assertDoesNotThrow(() -> userService.deleteAccount(sampleUser));

        verify(apiKeyRepository).delete(any(ApiKey.class));
        verify(progressRecordRepository).delete(any(ProgressRecord.class));
        verify(submissionRepository).delete(any(Submission.class));
        verify(taskAssignmentRepository).delete(any(TaskAssignment.class));
        verify(userRepository).deleteById(uid);

        when(userRepository.findById(uid)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(sampleUser, UpdateUserRequest.builder().build()));
        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword(sampleUser, ChangePasswordRequest.builder().oldPassword("a").newPassword("b").build()));
    }

    /**
     * @brief Verifies educator can successfully rename an enrolled student.
     */
    @Test
    void testUpdateStudentNameSuccess() {
        User teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Old Name").role(Role.STUDENT).build();
        Group group = Group.builder().id(UUID.randomUUID()).teacher(teacher).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(teacherStudentAliasRepository.findByTeacherIdAndStudentId(teacher.getId(), student.getId())).thenReturn(Optional.empty());

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("New Name").build();
        UserResponse response = userService.updateStudentName(student.getId(), req, teacher);

        assertNotNull(response);
        assertEquals("New Name", response.getFullName());
        verify(userRepository, never()).save(any(User.class));
        verify(teacherStudentAliasRepository).save(argThat(a -> "New Name".equals(a.getCustomName())));
    }

    /**
     * @brief Verifies administrator can rename any student without group membership check.
     */
    @Test
    void testUpdateStudentNameAdminSuccess() {
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Student Old").role(Role.STUDENT).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(teacherStudentAliasRepository.findByTeacherIdAndStudentId(admin.getId(), student.getId())).thenReturn(Optional.empty());

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Student Renamed").build();
        UserResponse response = userService.updateStudentName(student.getId(), req, admin);

        assertNotNull(response);
        assertEquals("Student Renamed", response.getFullName());
        verify(userRepository, never()).save(any(User.class));
        verify(teacherStudentAliasRepository).save(argThat(a -> "Student Renamed".equals(a.getCustomName())));
    }

    /**
     * @brief Verifies non-educator user cannot rename students.
     */
    @Test
    void testUpdateStudentNameNonTeacherThrows() {
        User otherStudent = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        User targetStudent = User.builder().id(UUID.randomUUID()).fullName("Target").role(Role.STUDENT).build();

        when(userRepository.findById(targetStudent.getId())).thenReturn(Optional.of(targetStudent));
        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Hacked Name").build();

        assertThrows(ForbiddenException.class, () -> userService.updateStudentName(targetStudent.getId(), req, otherStudent));
    }

    /**
     * @brief Verifies educator cannot rename a student not enrolled in their groups.
     */
    @Test
    void testUpdateStudentNameNotTeacherOfStudentThrows() {
        User teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Independent Student").role(Role.STUDENT).build();
        Group group = Group.builder().id(UUID.randomUUID()).teacher(teacher).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(false);

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Renamed").build();
        assertThrows(ForbiddenException.class, () -> userService.updateStudentName(student.getId(), req, teacher));
    }

    /**
     * @brief Verifies student not found throws ResourceNotFoundException.
     */
    @Test
    void testUpdateStudentNameNotFoundThrows() {
        User teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        UUID nonExistent = UUID.randomUUID();

        when(userRepository.findById(nonExistent)).thenReturn(Optional.empty());
        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Renamed").build();

        assertThrows(ResourceNotFoundException.class, () -> userService.updateStudentName(nonExistent, req, teacher));
    }

    /**
     * @brief Verifies educator updating a student name creates or updates a custom alias without mutating student entity.
     */
    @Test
    void testUpdateStudentNameByTeacherSavesAliasAndDoesNotMutateStudentFullName() {
        User teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Private Real Name").role(Role.STUDENT).build();
        Group group = Group.builder().id(UUID.randomUUID()).teacher(teacher).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(teacherStudentAliasRepository.findByTeacherIdAndStudentId(teacher.getId(), student.getId())).thenReturn(Optional.empty());

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Teacher's Nickname").build();
        UserResponse response = userService.updateStudentName(student.getId(), req, teacher);

        assertEquals("Teacher's Nickname", response.getFullName());
        assertEquals("Private Real Name", student.getFullName());
        verify(teacherStudentAliasRepository).save(any());
        verify(userRepository, never()).save(student);
    }

    @Test
    void testUpdateStudentNameByTeacher_ExistingAliasUpdated() {
        User teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Private Real Name").role(Role.STUDENT).build();
        Group group = Group.builder().id(UUID.randomUUID()).teacher(teacher).build();
        com.linguaoptima.api.domain.TeacherStudentAlias existingAlias = com.linguaoptima.api.domain.TeacherStudentAlias.builder()
            .id(UUID.randomUUID())
            .teacher(teacher)
            .student(student)
            .customName("Old Nickname")
            .build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(teacherStudentAliasRepository.findByTeacherIdAndStudentId(teacher.getId(), student.getId()))
            .thenReturn(Optional.of(existingAlias));

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("New Nickname").build();
        UserResponse response = userService.updateStudentName(student.getId(), req, teacher);

        assertEquals("New Nickname", response.getFullName());
        assertEquals("New Nickname", existingAlias.getCustomName());
        verify(teacherStudentAliasRepository).save(existingAlias);
    }

    @Test
    void testUpdateStudentNameByAdmin_BypassesCohortCheck() {
        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        User student = User.builder().id(UUID.randomUUID()).fullName("Private Real Name").role(Role.STUDENT).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(teacherStudentAliasRepository.findByTeacherIdAndStudentId(admin.getId(), student.getId()))
            .thenReturn(Optional.empty());

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Admin Note").build();
        UserResponse response = userService.updateStudentName(student.getId(), req, admin);

        assertEquals("Admin Note", response.getFullName());
        verify(teacherStudentAliasRepository).save(any());
        verify(groupRepository, never()).findByTeacher(any());
    }

    @Test
    void testUpdateStudentNameByStudent_ThrowsForbidden() {
        User studentCaller = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        User student = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        UpdateStudentNameRequest req = UpdateStudentNameRequest.builder().fullName("Hacker").build();
        assertThrows(ForbiddenException.class, () ->
            userService.updateStudentName(student.getId(), req, studentCaller)
        );
    }
}

