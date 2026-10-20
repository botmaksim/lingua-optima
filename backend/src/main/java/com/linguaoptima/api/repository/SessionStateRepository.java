/**
 * @file SessionStateRepository.java
 * @brief Spring Data JPA repository for Computerized Adaptive Testing (CAT) session states.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for Computerized Adaptive Testing (CAT) session states.
 */
@Repository
public interface SessionStateRepository extends JpaRepository<SessionState, UUID> {
    Optional<SessionState> findFirstByStudentAndStatusOrderByStartedAtDesc(User student, SessionStatus status);
    Optional<SessionState> findFirstByStudentIdAndStatusOrderByStartedAtDesc(UUID studentId, SessionStatus status);
    @Query("SELECT s FROM SessionState s WHERE s.assignment.id = :assignmentId")
    Optional<SessionState> findByAssignmentId(@Param("assignmentId") UUID assignmentId);
    List<SessionState> findAllByStudentIdAndStatus(UUID studentId, SessionStatus status);

    /**
     * @brief Queries all adaptive sessions across all statuses for a student.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;SessionState&gt;).
     */
    List<SessionState> findAllByStudentId(UUID studentId);
}
