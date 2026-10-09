/**
 * @file PendingAiTaskRepository.java
 * @brief Spring Data JPA repository for queued asynchronous AI task items.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.PendingAiTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for queued asynchronous AI task items.
 */
@Repository
public interface PendingAiTaskRepository extends JpaRepository<PendingAiTask, UUID> {
    /**
     * @brief Queries repository via findByStatusOrderByCreatedAtAsc.
     * @param status Filter parameter status.
     * @return Query result (List&lt;PendingAiTask&gt;).
     */
    List<PendingAiTask> findByStatusOrderByCreatedAtAsc(String status);
}
