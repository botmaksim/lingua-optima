/**
 * @file GroupTaskRepository.java
 * @brief Spring Data JPA repository for cohort-level task assignments.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.GroupTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for cohort-level task assignments.
 */
@Repository
public interface GroupTaskRepository extends JpaRepository<GroupTask, UUID> {

    /**
     * @brief Finds all group tasks for a cohort with eagerly loaded task metadata ordered by creation date descending.
     * @param groupId Cohort UUID.
     * @return List of GroupTask entities.
     */
    @Query("SELECT gt FROM GroupTask gt JOIN FETCH gt.task WHERE gt.group.id = :groupId ORDER BY gt.createdAt DESC")
    List<GroupTask> findByGroupIdWithTask(@Param("groupId") UUID groupId);

    Optional<GroupTask> findByGroupIdAndTaskId(UUID groupId, UUID taskId);

    List<GroupTask> findByGroupId(UUID groupId);

    void deleteByGroupIdAndTaskId(UUID groupId, UUID taskId);
}
