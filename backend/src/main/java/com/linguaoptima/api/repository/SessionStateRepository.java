package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionStateRepository extends JpaRepository<SessionState, UUID> {
    Optional<SessionState> findFirstByStudentAndStatusOrderByStartedAtDesc(User student, SessionStatus status);
    Optional<SessionState> findFirstByStudentIdAndStatusOrderByStartedAtDesc(UUID studentId, SessionStatus status);
    Optional<SessionState> findByAssignmentId(UUID assignmentId);
    List<SessionState> findAllByStudentIdAndStatus(UUID studentId, SessionStatus status);
}
