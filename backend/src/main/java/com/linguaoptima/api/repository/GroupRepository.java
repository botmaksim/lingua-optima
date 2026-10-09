/**
 * @file GroupRepository.java
 * @brief Spring Data JPA repository for managing educator cohort study groups.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for managing educator cohort study groups.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    /**
     * @brief Queries repository via findByTeacher.
     * @param teacher Filter parameter teacher.
     * @return Query result (List&lt;Group&gt;).
     */
    List<Group> findByTeacher(User teacher);
    /**
     * @brief Queries repository via findByTeacherId.
     * @param teacherId Filter parameter teacherId.
     * @return Query result (List&lt;Group&gt;).
     */
    List<Group> findByTeacherId(UUID teacherId);
    /**
     * @brief Queries repository via findByIdAndTeacherId.
     * @param id Filter parameter id.
     * @param teacherId Filter parameter teacherId.
     * @return Query result (Optional&lt;Group&gt;).
     */
    Optional<Group> findByIdAndTeacherId(UUID id, UUID teacherId);
}
