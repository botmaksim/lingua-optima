package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.PendingAiTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PendingAiTaskRepository extends JpaRepository<PendingAiTask, UUID> {
    List<PendingAiTask> findByStatusOrderByCreatedAtAsc(String status);
}
