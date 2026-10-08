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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final GroupStudentRepository groupStudentRepository;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createSseEmitter(UUID userId) {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1 hour timeout
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to notifications"));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    @Transactional
    public Notification send(User user, String message, NotificationType type) {
        Notification notification = notificationRepository.save(Notification.builder()
            .user(user)
            .message(message)
            .type(type)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build());

        // Emit SSE to active client connections
        List<SseEmitter> userEmitters = emitters.get(user.getId());
        if (userEmitters != null) {
            NotificationResponse response = NotificationResponse.fromEntity(notification);
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().name("NOTIFICATION").data(response));
                } catch (Exception e) {
                    removeEmitter(user.getId(), emitter);
                }
            }
        }

        return notification;
    }

    @Transactional
    public void sendToGroup(Group group, String message, NotificationType type) {
        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId());
        for (GroupStudent gs : activeStudents) {
            send(gs.getStudent(), message, type);
        }
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(NotificationResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification does not belong to user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
