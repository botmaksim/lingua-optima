/**
 * @file NotificationRepository.java
 * @brief Spring Data JPA repository for persisted user notifications.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for persisted user notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserAndIsReadFalse(User user);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    int countByUserIdAndIsReadFalse(UUID userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * @brief Queries notifications by user ID, notification type, and reference entity ID.
     * @param userId Recipient user identifier.
     * @param type Notification category.
     * @param referenceId Referenced entity identifier.
     * @return Matching notification list.
     */
    List<Notification> findByUserIdAndTypeAndReferenceId(UUID userId, com.linguaoptima.api.domain.enums.NotificationType type, UUID referenceId);
}
