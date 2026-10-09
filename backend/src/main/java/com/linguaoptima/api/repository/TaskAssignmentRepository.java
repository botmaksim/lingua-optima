package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @file TaskAssignmentRepository.java
 * @brief Spring Data JPA repository for assigned student exercises.
 */
@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    /**
     * @brief Queries repository via findByStudentAndStatus.
     * @param student Filter parameter student.
     * @param status Filter parameter status.
     * @return Query result (List&lt;TaskAssignment&gt;).
     */
    List<TaskAssignment> findByStudentAndStatus(User student, AssignmentStatus status);
    /**
     * @brief Queries repository via findByStudentIdAndStatus.
     * @param studentId Filter parameter studentId.
     * @param status Filter parameter status.
     * @return Query result (List&lt;TaskAssignment&gt;).
     */
    List<TaskAssignment> findByStudentIdAndStatus(UUID studentId, AssignmentStatus status);
    /**
     * @brief Queries repository via findByTaskId.
     * @param taskId Filter parameter taskId.
     * @return Query result (List&lt;TaskAssignment&gt;).
     */
    List<TaskAssignment> findByTaskId(UUID taskId);
    /**
     * @brief Queries repository via findByStudentAndTask.
     * @param student Filter parameter student.
     * @param task Filter parameter task.
     * @return Query result (Optional&lt;TaskAssignment&gt;).
     */
    Optional<TaskAssignment> findByStudentAndTask(User student, Task task);
    /**
     * @brief Queries repository via findByStudentIdAndTaskId.
     * @param studentId Filter parameter studentId.
     * @param taskId Filter parameter taskId.
     * @return Query result (Optional&lt;TaskAssignment&gt;).
     */
    Optional<TaskAssignment> findByStudentIdAndTaskId(UUID studentId, UUID taskId);
    /**
     * @brief Queries repository via findByStudentIdOrderByCreatedAtDesc.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;TaskAssignment&gt;).
     */
    List<TaskAssignment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
