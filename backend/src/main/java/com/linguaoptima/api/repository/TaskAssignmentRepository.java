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

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {
    List<TaskAssignment> findByStudentAndStatus(User student, AssignmentStatus status);
    List<TaskAssignment> findByStudentIdAndStatus(UUID studentId, AssignmentStatus status);
    List<TaskAssignment> findByTaskId(UUID taskId);
    Optional<TaskAssignment> findByStudentAndTask(User student, Task task);
    Optional<TaskAssignment> findByStudentIdAndTaskId(UUID studentId, UUID taskId);
    List<TaskAssignment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
}
