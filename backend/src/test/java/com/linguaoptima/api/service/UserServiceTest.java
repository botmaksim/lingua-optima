package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.ChangePasswordRequest;
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
 * @file UserServiceTest.java
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
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private SessionStateRepository sessionStateRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

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

    @Test
    void testGetCurrentUser() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        UserResponse response = userService.getCurrentUser(sampleUser);
        assertNotNull(response);
        assertEquals("test@lingua.com", response.getEmail());

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser(sampleUser));
    }

    @Test
    void testUpdateUser() {
        UpdateUserRequest req = UpdateUserRequest.builder()
            .fullName("New Name")
            .displayAlias("Polyglot")
            .cefrLevel(CefrLevel.B2)
            .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = userService.updateUser(sampleUser, req);
        assertEquals("New Name", res.getFullName());
        assertEquals("Polyglot", res.getDisplayAlias());
        assertEquals(CefrLevel.B2, res.getCefrLevel());
    }

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

    @Test
    void testDeleteAccountGdprCascade() {
        UUID uid = sampleUser.getId();
        when(apiKeyRepository.findAllByUserId(uid)).thenReturn(List.of(ApiKey.builder().id(UUID.randomUUID()).build()));
        when(progressRecordRepository.findByStudentId(uid)).thenReturn(List.of(ProgressRecord.builder().id(UUID.randomUUID()).build()));

        assertDoesNotThrow(() -> userService.deleteAccount(sampleUser));

        verify(apiKeyRepository).delete(any(ApiKey.class));
        verify(progressRecordRepository).delete(any(ProgressRecord.class));
        verify(userRepository).deleteById(uid);
    }
}
