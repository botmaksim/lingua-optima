/**
 * @file CustomCurriculumRepository.java
 * @brief Spring Data JPA repository for managing educator custom rules and vocabulary sets.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.CustomCurriculumEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for managing educator custom rules and vocabulary sets.
 */
@Repository
public interface CustomCurriculumRepository extends JpaRepository<CustomCurriculumEntry, UUID> {

    /**
     * @brief Finds all curriculum entries owned by user filtered by type.
     */
    List<CustomCurriculumEntry> findByUserIdAndCurriculumTypeOrderByCreatedAtDesc(UUID userId, String curriculumType);

    /**
     * @brief Finds all curriculum entries owned by user.
     */
    List<CustomCurriculumEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * @brief Counts entries of specific type owned by user.
     */
    long countByUserIdAndCurriculumType(UUID userId, String curriculumType);
}
