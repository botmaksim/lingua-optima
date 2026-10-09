package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.AIProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file CreateApiKeyRequest.java
 * @brief Request DTO for registering a user BYOK API key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    @NotNull(message = "AI provider is required")
    private AIProvider provider;

    @NotBlank(message = "API key is required")
    private String rawKey;
}
