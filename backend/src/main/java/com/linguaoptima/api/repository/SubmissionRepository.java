package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findByStudent(User student);
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);
    List<Submission> findByAssignmentId(UUID assignmentId);

    @Query("SELECT s FROM Submission s WHERE s.student.id IN :studentIds ORDER BY s.submittedAt DESC")
    List<Submission> findByStudentIdInOrderBySubmittedAtDesc(@Param("studentIds") List<UUID> studentIds);

    @Query("SELECT s FROM Submission s JOIN GroupStudent gs ON s.student.id = gs.student.id " +
           "WHERE gs.group.id = :groupId AND gs.isActive = true AND s.submittedAt >= :sinceDate")
    List<Submission> findActiveGroupSubmissionsSince(@Param("groupId") UUID groupId, @Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT s FROM Submission s JOIN GroupStudent gs ON s.student.id = gs.student.id " +
           "WHERE gs.group.id = :groupId AND gs.isActive = true ORDER BY s.submittedAt DESC")
    List<Submission> findActiveGroupSubmissions(@Param("groupId") UUID groupId);
}
