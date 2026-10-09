package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @file GroupStudentRepository.java
 * @brief Spring Data JPA repository for managing student group enrollments and soft deletions.
 */
@Repository
public interface GroupStudentRepository extends JpaRepository<GroupStudent, UUID> {
    /**
     * @brief Queries repository via findByGroupAndStudent.
     * @param group Filter parameter group.
     * @param student Filter parameter student.
     * @return Query result (Optional&lt;GroupStudent&gt;).
     */
    Optional<GroupStudent> findByGroupAndStudent(Group group, User student);
    /**
     * @brief Queries repository via findByGroupIdAndStudentId.
     * @param groupId Filter parameter groupId.
     * @param studentId Filter parameter studentId.
     * @return Query result (Optional&lt;GroupStudent&gt;).
     */
    Optional<GroupStudent> findByGroupIdAndStudentId(UUID groupId, UUID studentId);
    /**
     * @brief Queries repository via findByGroupIdAndIsActiveTrue.
     * @param groupId Filter parameter groupId.
     * @return Query result (List&lt;GroupStudent&gt;).
     */
    List<GroupStudent> findByGroupIdAndIsActiveTrue(UUID groupId);
    /**
     * @brief Queries repository via findByStudentIdAndIsActiveTrue.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;GroupStudent&gt;).
     */
    List<GroupStudent> findByStudentIdAndIsActiveTrue(UUID studentId);
    /**
     * @brief Queries repository via existsByGroupIdAndStudentIdAndIsActiveTrue.
     * @param groupId Filter parameter groupId.
     * @param studentId Filter parameter studentId.
     * @return Query result (boolean).
     */
    boolean existsByGroupIdAndStudentIdAndIsActiveTrue(UUID groupId, UUID studentId);
    /**
     * @brief Queries repository via countByGroupIdAndIsActiveTrue.
     * @param groupId Filter parameter groupId.
     * @return Query result (int).
     */
    int countByGroupIdAndIsActiveTrue(UUID groupId);
}
