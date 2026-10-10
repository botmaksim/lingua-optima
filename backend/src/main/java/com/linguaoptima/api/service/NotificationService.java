/**
 * @file NotificationService.java
 * @brief Real-time Server-Sent Events (SSE) notification delivery and persistence service.
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

/**
 * @brief Real-time Server-Sent Events (SSE) notification delivery and persistence service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** @brief Field representing notification repository in NotificationService. */
    private final NotificationRepository notificationRepository;
    /** @brief Field representing group student repository in NotificationService. */
    private final GroupStudentRepository groupStudentRepository;

    /** @brief Field representing emitters in NotificationService. */
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * @brief Instantiates a new Server-Sent Events emitter with a 1-hour timeout.
     * @return Newly created SseEmitter instance.
     */
    protected SseEmitter newSseEmitter() {
        return new SseEmitter(60 * 60 * 1000L);
    }

    /**
     * @brief Creates and registers a new Server-Sent Events emitter connection for real-time delivery.
     * @param userId Unique identifier of the subscribing user.
     * @return Configured SseEmitter with 1-hour connection lifetime.
     */
    public SseEmitter createSseEmitter(UUID userId) {
        SseEmitter emitter = newSseEmitter();
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

    /**
     * @brief Cleans up and detaches an inactive or terminated SSE emitter instance.
     * @param userId Unique identifier of the user.
     * @param emitter The emitter instance to remove.
     */
    private void removeEmitter(UUID userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * @brief Persists a notification to the database and broadcasts it immediately to active SSE connections.
     * @param user Recipient user entity.
     * @param message Text payload of the notification.
     * @param type Notification category type (SYSTEM, TASK, GRADE, CONTEXTUAL, GROUP_INVITATION).
     * @return Saved Notification entity.
     */
    @Transactional
    public Notification send(User user, String message, NotificationType type) {
        return send(user, message, type, null);
    }

    /**
     * @brief Persists a notification referencing an entity ID and broadcasts it via SSE.
     * @param user Recipient user entity.
     * @param message Text payload of the notification.
     * @param type Notification category type.
     * @param referenceId Referenced entity identifier (e.g. group or task UUID).
     * @return Saved Notification entity.
     */
    @Transactional
    public Notification send(User user, String message, NotificationType type, UUID referenceId) {
        Notification notification = notificationRepository.save(Notification.builder()
            .user(user)
            .message(message)
            .type(type)
            .referenceId(referenceId)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build());

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

    /**
     * @brief Broadcasts a notification to all active enrolled students within a specified group.
     * @param group Target student group.
     * @param message Notification message string.
     * @param type Notification category type.
     */
    @Transactional
    public void sendToGroup(Group group, String message, NotificationType type) {
        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId());
        for (GroupStudent gs : activeStudents) {
            send(gs.getStudent(), message, type);
        }
    }

    /**
     * @brief Returns count of unread notifications for the specified user.
     * @param userId Unique identifier of the user.
     * @return Number of unread notifications.
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * @brief Retrieves all notifications for the specified user ordered by timestamp descending.
     * @param userId Unique identifier of the user.
     * @return List of NotificationResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(NotificationResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @brief Marks a specific notification as read after verifying ownership.
     * @param notificationId Unique identifier of the notification.
     * @param user Authenticated user marking the notification.
     * @throws ResourceNotFoundException if notification does not exist or does not belong to user.
     */
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

    /**
     * @brief Marks all unread notifications for a user as read in bulk.
     * @param user Authenticated user marking all notifications as read.
     */
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        if (!unread.isEmpty()) {
            for (Notification n : unread) {
                n.setRead(true);
            }
            notificationRepository.saveAll(unread);
        }
    }
}
