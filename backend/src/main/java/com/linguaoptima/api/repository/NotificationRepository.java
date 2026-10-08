package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Notification;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserAndIsReadFalse(User user);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    int countByUserIdAndIsReadFalse(UUID userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
