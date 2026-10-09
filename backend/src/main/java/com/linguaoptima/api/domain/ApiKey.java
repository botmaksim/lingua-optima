/**
 * @file ApiKey.java
 * @brief JPA entity storing encrypted user-provided AI provider API keys.
 */
package com.linguaoptima.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.linguaoptima.api.domain.enums.AIProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @brief JPA entity storing encrypted user-provided AI provider API keys.
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    /**
     * @brief Unique identifier of the API key record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * @brief User who owns this API key (excluded from JSON serialization).
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * @brief Target AI provider for this key (GEMINI, GROQ, DEEPSEEK, QWEN, KIMI, OPENAI, ANTHROPIC).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AIProvider provider;

    /**
     * @brief Selected AI model identifier for this key (e.g., deepseek-chat, gemini-2.5-flash, gpt-5-mini).
     */
    @Column(name = "model_name")
    private String modelName;

    /**
     * @brief AES-256-GCM encrypted API key ciphertext (excluded from JSON serialization).
     */
    @JsonIgnore
    @Column(name = "encrypted_key", nullable = false, length = 1024)
    private String encryptedKey;

    /**
     * @brief Timestamp when the API key was registered.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * @brief Pre-persist JPA lifecycle callback ensuring creation timestamp is populated.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
