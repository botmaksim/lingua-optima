package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.UsageCounter;
import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {
    Optional<UsageCounter> findByUser(User user);
    Optional<UsageCounter> findByUserId(UUID userId);
}
