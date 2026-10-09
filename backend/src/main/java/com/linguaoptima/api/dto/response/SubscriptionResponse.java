package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @file SubscriptionResponse.java
 * @brief Response DTO representing active user subscription tier and remaining quotas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private SubscriptionTier tier;
    private LocalDateTime expiresAt;
    private Integer evaluationsRemaining;
    private Integer ocrRemaining;
    private boolean unlimited;
}
