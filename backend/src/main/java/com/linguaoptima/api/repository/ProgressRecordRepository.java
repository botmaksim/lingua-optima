package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, UUID> {
    List<ProgressRecord> findByStudent(User student);
    List<ProgressRecord> findByStudentId(UUID studentId);
    Optional<ProgressRecord> findByStudentAndGrammarTopic(User student, String grammarTopic);
    Optional<ProgressRecord> findByStudentIdAndGrammarTopic(UUID studentId, String grammarTopic);
    List<ProgressRecord> findByStudentIdAndMasteryScoreLessThan(UUID studentId, double maxMasteryScore);
}
