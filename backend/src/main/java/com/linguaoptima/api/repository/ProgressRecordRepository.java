package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @file ProgressRecordRepository.java
 * @brief Spring Data JPA repository for student grammar topic mastery records.
 */
@Repository
public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, UUID> {
    /**
     * @brief Queries repository via findByStudent.
     * @param student Filter parameter student.
     * @return Query result (List&lt;ProgressRecord&gt;).
     */
    List<ProgressRecord> findByStudent(User student);
    /**
     * @brief Queries repository via findByStudentId.
     * @param studentId Filter parameter studentId.
     * @return Query result (List&lt;ProgressRecord&gt;).
     */
    List<ProgressRecord> findByStudentId(UUID studentId);
    /**
     * @brief Queries repository via findByStudentAndGrammarTopic.
     * @param student Filter parameter student.
     * @param grammarTopic Filter parameter grammarTopic.
     * @return Query result (Optional&lt;ProgressRecord&gt;).
     */
    Optional<ProgressRecord> findByStudentAndGrammarTopic(User student, String grammarTopic);
    /**
     * @brief Queries repository via findByStudentIdAndGrammarTopic.
     * @param studentId Filter parameter studentId.
     * @param grammarTopic Filter parameter grammarTopic.
     * @return Query result (Optional&lt;ProgressRecord&gt;).
     */
    Optional<ProgressRecord> findByStudentIdAndGrammarTopic(UUID studentId, String grammarTopic);
    /**
     * @brief Queries repository via findByStudentIdAndMasteryScoreLessThan.
     * @param studentId Filter parameter studentId.
     * @param maxMasteryScore Filter parameter maxMasteryScore.
     * @return Query result (List&lt;ProgressRecord&gt;).
     */
    List<ProgressRecord> findByStudentIdAndMasteryScoreLessThan(UUID studentId, double maxMasteryScore);
}
