/**
 * @file TaskRepository.java
 * @brief Spring Data JPA repository for curriculum tasks and templates.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for curriculum tasks and templates.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    /**
     * @brief Queries repository via findByCreatedBy.
     * @param createdBy Filter parameter createdBy.
     * @return Query result (List&lt;Task&gt;).
     */
    List<Task> findByCreatedBy(User createdBy);
    /**
     * @brief Queries repository via findByCreatedById.
     * @param createdById Filter parameter createdById.
     * @return Query result (List&lt;Task&gt;).
     */
    List<Task> findByCreatedById(UUID createdById);

    /**
     * @brief Queries repository via findTemplates.
     * @return Query result (List&lt;Task&gt;).
     */
    @Query("SELECT t FROM Task t WHERE t.isTemplate = true")
    List<Task> findTemplates();

    /**
     * @brief Queries repository via findAllAccessibleForUser including templates, created tasks, and assigned tasks.
     * @param userId Filter parameter userId.
     * @return Query result (List&lt;Task&gt;).
     */
    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN TaskAssignment ta ON ta.task = t WHERE t.isTemplate = true OR t.createdBy.id = :userId OR ta.student.id = :userId")
    List<Task> findAllAccessibleForUser(UUID userId);
}
