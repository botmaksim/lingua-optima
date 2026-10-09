/**
 * @file UsageCounterRepository.java
 * @brief Spring Data JPA repository for weekly AI evaluation and OCR quota counters.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.UsageCounter;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for weekly AI evaluation and OCR quota counters.
 */
@Repository
public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {
    /**
     * @brief Queries repository via findByUser.
     * @param user Filter parameter user.
     * @return Query result (Optional&lt;UsageCounter&gt;).
     */
    Optional<UsageCounter> findByUser(User user);
    /**
     * @brief Queries repository via findByUserId.
     * @param userId Filter parameter userId.
     * @return Query result (Optional&lt;UsageCounter&gt;).
     */
    Optional<UsageCounter> findByUserId(UUID userId);
}
