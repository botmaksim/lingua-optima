package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.dto.response.NotificationResponse;
import com.linguaoptima.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @file NotificationControllerTest.java
 * @brief Unit and slice test suite for NotificationController.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testStreamNotifications() {
        when(notificationService.createSseEmitter(user.getId())).thenReturn(new SseEmitter());
        SseEmitter emitter = notificationController.streamNotifications(user);
        assertNotNull(emitter);
    }

    @Test
    void testGetUnreadCountAndNotifications() {
        when(notificationService.getUnreadCount(user.getId())).thenReturn(5);
        assertEquals(5, notificationController.getUnreadCount(user).getBody());

        NotificationResponse nr = NotificationResponse.builder().id(UUID.randomUUID()).message("Hello").type(NotificationType.SYSTEM).build();
        when(notificationService.getNotifications(user.getId())).thenReturn(List.of(nr));
        assertEquals(1, notificationController.getNotifications(user).getBody().size());
    }

    @Test
    void testMarkAsRead() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> res = notificationController.markAsRead(id, user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(notificationService).markAsRead(id, user);
    }
}
