package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByCreatedBy(User createdBy);
    List<Task> findByCreatedById(UUID createdById);

    @Query("SELECT t FROM Task t WHERE t.isTemplate = true")
    List<Task> findTemplates();

    @Query("SELECT t FROM Task t WHERE t.isTemplate = true OR t.createdBy.id = :userId")
    List<Task> findAllAccessibleForUser(UUID userId);
}
