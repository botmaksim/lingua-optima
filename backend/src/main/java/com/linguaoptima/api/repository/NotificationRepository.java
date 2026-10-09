package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @file NotificationRepository.java
 * @brief Spring Data JPA repository for persisted user notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * @brief Queries repository via findByUserAndIsReadFalse.
     * @param user Filter parameter user.
     * @return Query result (List&lt;Notification&gt;).
     */
    List<Notification> findByUserAndIsReadFalse(User user);
    /**
     * @brief Queries repository via findByUserIdAndIsReadFalse.
     * @param userId Filter parameter userId.
     * @return Query result (List&lt;Notification&gt;).
     */
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    /**
     * @brief Queries repository via countByUserIdAndIsReadFalse.
     * @param userId Filter parameter userId.
     * @return Query result (int).
     */
    int countByUserIdAndIsReadFalse(UUID userId);
    /**
     * @brief Queries repository via findByUserIdOrderByCreatedAtDesc.
     * @param userId Filter parameter userId.
     * @return Query result (List&lt;Notification&gt;).
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
