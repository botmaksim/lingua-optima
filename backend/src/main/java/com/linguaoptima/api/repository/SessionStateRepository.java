/**
 * @file SessionStateRepository.java
 * @brief Spring Data JPA repository for Computerized Adaptive Testing (CAT) session states.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for Computerized Adaptive Testing (CAT) session states.
 */
@Repository
public interface SessionStateRepository extends JpaRepository<SessionState, UUID> {
    /**
     * @brief Queries repository via findFirstByStudentAndStatusOrderByStartedAtDesc.
     * @param student Filter parameter student.
     * @param status Filter parameter status.
     * @return Query result (Optional&lt;SessionState&gt;).
     */
    Optional<SessionState> findFirstByStudentAndStatusOrderByStartedAtDesc(User student, SessionStatus status);
    /**
     * @brief Queries repository via findFirstByStudentIdAndStatusOrderByStartedAtDesc.
     * @param studentId Filter parameter studentId.
     * @param status Filter parameter status.
     * @return Query result (Optional&lt;SessionState&gt;).
     */
    Optional<SessionState> findFirstByStudentIdAndStatusOrderByStartedAtDesc(UUID studentId, SessionStatus status);
    /**
     * @brief Queries repository via findByAssignmentId.
     * @param assignmentId Filter parameter assignmentId.
     * @return Query result (Optional&lt;SessionState&gt;).
     */
    Optional<SessionState> findByAssignmentId(UUID assignmentId);
    /**
     * @brief Queries repository via findAllByStudentIdAndStatus.
     * @param studentId Filter parameter studentId.
     * @param status Filter parameter status.
     * @return Query result (List&lt;SessionState&gt;).
     */
    List<SessionState> findAllByStudentIdAndStatus(UUID studentId, SessionStatus status);

    /**
     * @brief Queries all adaptive sessions across all statuses for a student.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;SessionState&gt;).
     */
    List<SessionState> findAllByStudentId(UUID studentId);
}
