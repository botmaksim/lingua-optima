/**
 * @file TaskAssignmentRepository.java
 * @brief Spring Data JPA repository for assigned student exercises.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for assigned student exercises.
 */
@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    List<TaskAssignment> findByStudentAndStatus(User student, AssignmentStatus status);
    List<TaskAssignment> findByStudentIdAndStatus(UUID studentId, AssignmentStatus status);
    List<TaskAssignment> findByTaskId(UUID taskId);
    Optional<TaskAssignment> findByStudentAndTask(User student, Task task);
    Optional<TaskAssignment> findByStudentIdAndTaskId(UUID studentId, UUID taskId);
    List<TaskAssignment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    /**
     * @brief Finds task assignments for students with eagerly loaded tasks and student user profiles.
     * @param studentIds List of student UUIDs.
     * @return List of TaskAssignment entities ordered by creation date descending.
     */
    @Query("SELECT DISTINCT ta FROM TaskAssignment ta " +
           "JOIN FETCH ta.task t " +
           "JOIN FETCH ta.student s " +
           "WHERE s.id IN :studentIds " +
           "ORDER BY ta.createdAt DESC")
    List<TaskAssignment> findByStudentIdsWithTaskAndStudent(@Param("studentIds") List<UUID> studentIds);
}
