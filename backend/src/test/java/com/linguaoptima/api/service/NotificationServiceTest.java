/**
 * @file NotificationServiceTest.java
 * @brief Unit and slice test suite for NotificationService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.dto.response.NotificationResponse;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    /** @brief Test fixture or mock dependency for notification repository. */
    @Mock
    private NotificationRepository notificationRepository;

    /** @brief Test fixture or mock dependency for group student repository. */
    @Mock
    private GroupStudentRepository groupStudentRepository;

    /** @brief Test fixture or mock dependency for notification service. */
    @InjectMocks
    private NotificationService notificationService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in NotificationServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("note@lingua.com").build();
    }

    /**
     * @brief Verifies unit test scenario: create sse emitter.
     */
    @Test
    void testCreateSseEmitter() {
        SseEmitter emitter = notificationService.createSseEmitter(user.getId());
        assertNotNull(emitter);
    }

    /**
     * @brief Verifies unit test scenario: send notification with sse emitter success and failure.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testSendNotificationWithSseEmitterSuccessAndFailure() throws Exception {
        SseEmitter mockEmitterBad = mock(SseEmitter.class);
        doThrow(new IOException("Broken pipe")).when(mockEmitterBad).send(any(SseEmitter.SseEventBuilder.class));

        Map<UUID, List<SseEmitter>> emittersMap =
            (Map<UUID, List<SseEmitter>>) ReflectionTestUtils.getField(notificationService, "emitters");
        if (emittersMap != null) {
            emittersMap.computeIfAbsent(user.getId(), k -> new CopyOnWriteArrayList<>()).add(mockEmitterBad);
        }

        Notification n = Notification.builder()
            .id(UUID.randomUUID())
            .user(user)
            .message("SSE test message")
            .type(NotificationType.SYSTEM)
            .isRead(false)
            .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(n);

        Notification result = notificationService.send(user, "SSE test message", NotificationType.SYSTEM);
        assertNotNull(result);
    }

    /**
     * @brief Verifies unit test scenario: send notification.
     */
    @Test
    void testSendNotification() {
        Notification n = Notification.builder()
            .id(UUID.randomUUID())
            .user(user)
            .message("Test note")
            .type(NotificationType.SYSTEM)
            .isRead(false)
            .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(n);

        Notification result = notificationService.send(user, "Test note", NotificationType.SYSTEM);
        assertNotNull(result);
        assertEquals("Test note", result.getMessage());
    }

    /**
     * @brief Verifies unit test scenario: send to group.
     */
    @Test
    void testSendToGroup() {
        Group g = Group.builder().id(UUID.randomUUID()).build();
        GroupStudent gs = GroupStudent.builder().student(user).isActive(true).build();

        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(g.getId())).thenReturn(List.of(gs));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.sendToGroup(g, "Group announcement", NotificationType.SYSTEM);
        verify(notificationRepository).save(any(Notification.class));
    }

    /**
     * @brief Verifies unit test scenario: get unread count and notifications.
     */
    @Test
    void testGetUnreadCountAndNotifications() {
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(3);
        assertEquals(3, notificationService.getUnreadCount(user.getId()));

        Notification n = Notification.builder().id(UUID.randomUUID()).user(user).message("msg").type(NotificationType.SYSTEM).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(n));

        List<NotificationResponse> list = notificationService.getNotifications(user.getId());
        assertEquals(1, list.size());
    }

    /**
     * @brief Verifies unit test scenario: mark as read.
     */
    @Test
    void testMarkAsRead() {
        Notification n = Notification.builder().id(UUID.randomUUID()).user(user).isRead(false).build();
        when(notificationRepository.findById(n.getId())).thenReturn(Optional.of(n));

        notificationService.markAsRead(n.getId(), user);
        assertTrue(n.isRead());
        verify(notificationRepository).save(n);

        User other = User.builder().id(UUID.randomUUID()).build();
        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(n.getId(), other));

        UUID missing = UUID.randomUUID();
        when(notificationRepository.findById(missing)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(missing, user));
    }
}
