/**
 * @file SubscriptionRepository.java
 * @brief Spring Data JPA repository for user subscription licenses.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for user subscription licenses.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUser(User user);
    Optional<Subscription> findByUserId(UUID userId);
}
