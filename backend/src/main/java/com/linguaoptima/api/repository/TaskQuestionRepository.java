/**
 * @file TaskQuestionRepository.java
 * @brief Spring Data JPA repository for individual task questions.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.TaskQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for individual task questions.
 */
@Repository
public interface TaskQuestionRepository extends JpaRepository<TaskQuestion, UUID> {
    /**
     * @brief Queries repository via findByTaskIdOrderByQuestionOrder.
     * @param taskId Filter parameter taskId.
     * @return Query result (List&lt;TaskQuestion&gt;).
     */
    List<TaskQuestion> findByTaskIdOrderByQuestionOrder(UUID taskId);
    /**
     * @brief Queries repository via findByTaskIdAndDifficulty.
     * @param taskId Filter parameter taskId.
     * @param difficulty Filter parameter difficulty.
     * @return Query result (List&lt;TaskQuestion&gt;).
     */
    List<TaskQuestion> findByTaskIdAndDifficulty(UUID taskId, int difficulty);
}
