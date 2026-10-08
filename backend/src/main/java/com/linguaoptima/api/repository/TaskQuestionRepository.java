package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.TaskQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskQuestionRepository extends JpaRepository<TaskQuestion, UUID> {
    List<TaskQuestion> findByTaskIdOrderByQuestionOrder(UUID taskId);
    List<TaskQuestion> findByTaskIdAndDifficulty(UUID taskId, int difficulty);
}
