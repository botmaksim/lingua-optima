package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByUserAndProvider(User user, AIProvider provider);
    Optional<ApiKey> findByUserIdAndProvider(UUID userId, AIProvider provider);
    List<ApiKey> findAllByUser(User user);
    List<ApiKey> findAllByUserId(UUID userId);
}
