package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    List<Group> findByTeacher(User teacher);
    List<Group> findByTeacherId(UUID teacherId);
    Optional<Group> findByIdAndTeacherId(UUID id, UUID teacherId);
}
