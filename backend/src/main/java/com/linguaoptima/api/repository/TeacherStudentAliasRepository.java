/**
 * @file TeacherStudentAliasRepository.java
 * @brief Spring Data JPA repository for educator-student display aliases.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.TeacherStudentAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for educator-student display aliases.
 */
@Repository
public interface TeacherStudentAliasRepository extends JpaRepository<TeacherStudentAlias, UUID> {

    /**
     * @brief Finds custom alias set by a specific teacher for a given student.
     * @param teacherId Educator identifier.
     * @param studentId Student identifier.
     * @return Optional containing the alias entity if configured.
     */
    Optional<TeacherStudentAlias> findByTeacherIdAndStudentId(UUID teacherId, UUID studentId);

    /**
     * @brief Queries all student aliases configured by a specific educator.
     * @param teacherId Educator identifier.
     * @return List of student aliases.
     */
    List<TeacherStudentAlias> findByTeacherId(UUID teacherId);

    /**
     * @brief Queries student aliases configured by an educator for a specific set of student IDs.
     * @param teacherId Educator identifier.
     * @param studentIds Set of target student IDs.
     * @return List of matching aliases.
     */
    List<TeacherStudentAlias> findByTeacherIdAndStudentIdIn(UUID teacherId, Collection<UUID> studentIds);

    /**
     * @brief Deletes custom alias set by an educator for a student.
     * @param teacherId Educator identifier.
     * @param studentId Student identifier.
     */
    void deleteByTeacherIdAndStudentId(UUID teacherId, UUID studentId);
}
