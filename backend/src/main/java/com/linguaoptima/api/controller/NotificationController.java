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

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal User user) {
        return notificationService.createSseEmitter(user.getId());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getNotifications(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable("id") UUID notificationId,
        @AuthenticationPrincipal User user
    ) {
        notificationService.markAsRead(notificationId, user);
        return ResponseEntity.ok().build();
    }
}
