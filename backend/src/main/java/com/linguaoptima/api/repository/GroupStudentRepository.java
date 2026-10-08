package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupStudentRepository extends JpaRepository<GroupStudent, UUID> {
    Optional<GroupStudent> findByGroupAndStudent(Group group, User student);
    Optional<GroupStudent> findByGroupIdAndStudentId(UUID groupId, UUID studentId);
    List<GroupStudent> findByGroupIdAndIsActiveTrue(UUID groupId);
    List<GroupStudent> findByStudentIdAndIsActiveTrue(UUID studentId);
    boolean existsByGroupIdAndStudentIdAndIsActiveTrue(UUID groupId, UUID studentId);
    int countByGroupIdAndIsActiveTrue(UUID groupId);
}
