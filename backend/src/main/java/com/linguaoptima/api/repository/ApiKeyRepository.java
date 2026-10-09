/**
 * @file ApiKeyRepository.java
 * @brief Spring Data JPA repository for managing encrypted BYOK API keys.
 */
package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Spring Data JPA repository for managing encrypted BYOK API keys.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    /**
     * @brief Queries repository via findByUserAndProvider.
     * @param user Filter parameter user.
     * @param provider Filter parameter provider.
     * @return Query result (Optional&lt;ApiKey&gt;).
     */
    Optional<ApiKey> findByUserAndProvider(User user, AIProvider provider);
    /**
     * @brief Queries repository via findByUserIdAndProvider.
     * @param userId Filter parameter userId.
     * @param provider Filter parameter provider.
     * @return Query result (Optional&lt;ApiKey&gt;).
     */
    Optional<ApiKey> findByUserIdAndProvider(UUID userId, AIProvider provider);
    /**
     * @brief Queries repository via findAllByUser.
     * @param user Filter parameter user.
     * @return Query result (List&lt;ApiKey&gt;).
     */
    List<ApiKey> findAllByUser(User user);
    /**
     * @brief Queries repository via findAllByUserId.
     * @param userId Filter parameter userId.
     * @return Query result (List&lt;ApiKey&gt;).
     */
    List<ApiKey> findAllByUserId(UUID userId);
}
