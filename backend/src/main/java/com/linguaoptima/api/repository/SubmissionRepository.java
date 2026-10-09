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

/**
 * @file SubmissionRepository.java
 * @brief Spring Data JPA repository for student text and OCR submissions.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    /**
     * @brief Queries repository via findByStudent.
     * @param student Filter parameter student.
     * @return Query result (List&lt;Submission&gt;).
     */
    List<Submission> findByStudent(User student);
    /**
     * @brief Queries repository via findByStudentIdOrderBySubmittedAtDesc.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;Submission&gt;).
     */
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);
    /**
     * @brief Queries repository via findByAssignmentId.
     * @param assignmentId Filter parameter assignmentId.
     * @return Query result (List&lt;Submission&gt;).
     */
    List<Submission> findByAssignmentId(UUID assignmentId);

    /**
     * @brief Queries repository via findByStudentIdInOrderBySubmittedAtDesc.
     * @param studentIds Filter parameter studentIds.
     * @return Query result (List&lt;Submission&gt;).
     */
    @Query("SELECT s FROM Submission s WHERE s.student.id IN :studentIds ORDER BY s.submittedAt DESC")
    List<Submission> findByStudentIdInOrderBySubmittedAtDesc(@Param("studentIds") List<UUID> studentIds);

    @Query("SELECT s FROM Submission s JOIN GroupStudent gs ON s.student.id = gs.student.id " +
           "WHERE gs.group.id = :groupId AND gs.isActive = true AND s.submittedAt >= :sinceDate")
    /**
     * @brief Queries repository via findActiveGroupSubmissionsSince.
     * @param groupId Filter parameter groupId.
     * @param sinceDate Filter parameter sinceDate.
     * @return Query result (List&lt;Submission&gt;).
     */
    List<Submission> findActiveGroupSubmissionsSince(@Param("groupId") UUID groupId, @Param("sinceDate") LocalDateTime sinceDate);

    @Query("SELECT s FROM Submission s JOIN GroupStudent gs ON s.student.id = gs.student.id " +
           "WHERE gs.group.id = :groupId AND gs.isActive = true ORDER BY s.submittedAt DESC")
    /**
     * @brief Queries repository via findActiveGroupSubmissions.
     * @param groupId Filter parameter groupId.
     * @return Query result (List&lt;Submission&gt;).
     */
    List<Submission> findActiveGroupSubmissions(@Param("groupId") UUID groupId);
}
