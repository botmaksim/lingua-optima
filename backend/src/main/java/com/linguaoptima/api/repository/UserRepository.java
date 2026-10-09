package com.linguaoptima.api.repository;

import com.linguaoptima.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @file UserRepository.java
 * @brief Spring Data JPA repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * @brief Queries repository via findByEmail.
     * @param email Filter parameter email.
     * @return Query result (Optional&lt;User&gt;).
     */
    Optional<User> findByEmail(String email);
    /**
     * @brief Queries repository via existsByEmail.
     * @param email Filter parameter email.
     * @return Query result (boolean).
     */
    boolean existsByEmail(String email);
}
