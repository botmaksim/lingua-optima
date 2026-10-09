/**
 * @file NotificationController.java
 * @brief REST controller managing real-time Server-Sent Events (SSE) and notification status.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.NotificationResponse;
import com.linguaoptima.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * @brief REST controller managing real-time Server-Sent Events (SSE) and notification status.
 *
 * Dispatches real-time assignment notifications, evaluation feedback alerts, and streak reminders.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    /** @brief Field representing notification service in NotificationController. */
    private final NotificationService notificationService;

    /**
     * @brief Establishes an SSE stream for real-time notification dispatch.
     *
     * @param user Authenticated user principal.
     * @return SseEmitter streaming live notification events.
     */
    @GetMapping(value = {"/stream", "/subscribe"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        return notificationService.createSseEmitter(user.getId());
    }

    /**
     * @brief Fetches current count of unread notifications for navbar badges.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with unread notification count.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
    }

    /**
     * @brief Retrieves historical notifications list for user.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with list of user notifications.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getId()));
    }

    /**
     * @brief Marks a specified notification as read.
     *
     * @param notificationId Identifier of target notification.
     * @param user Authenticated user principal.
     * @return HTTP 200 OK.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable("id") UUID notificationId,
        @AuthenticationPrincipal User user
    ) {
        notificationService.markAsRead(notificationId, user);
        return ResponseEntity.ok().build();
    }
}
