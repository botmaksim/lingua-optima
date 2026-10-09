package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @file SubscriptionRepository.java
 * @brief Spring Data JPA repository for user subscription licenses.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    /**
     * @brief Queries repository via findByUser.
     * @param user Filter parameter user.
     * @return Query result (Optional&lt;Subscription&gt;).
     */
    Optional<Subscription> findByUser(User user);
    /**
     * @brief Queries repository via findByUserId.
     * @param userId Filter parameter userId.
     * @return Query result (Optional&lt;Subscription&gt;).
     */
    Optional<Subscription> findByUserId(UUID userId);
}
