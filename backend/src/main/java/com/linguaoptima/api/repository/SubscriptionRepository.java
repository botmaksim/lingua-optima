package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUser(User user);
    Optional<Subscription> findByUserId(UUID userId);
}
