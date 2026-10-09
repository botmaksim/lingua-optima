/**
 * @file CreateApiKeyRequest.java
 * @brief Request DTO for registering a user BYOK API key.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.AIProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request DTO for registering a user BYOK API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    /** @brief Field representing provider in CreateApiKeyRequest. */
    @NotNull(message = "AI provider is required")
    private AIProvider provider;

    /** @brief Field representing raw key in CreateApiKeyRequest. */
    @NotBlank(message = "API key is required")
    private String rawKey;
}
