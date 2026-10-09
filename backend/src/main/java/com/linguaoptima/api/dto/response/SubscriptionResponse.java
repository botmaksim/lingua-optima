/**
 * @file SubscriptionResponse.java
 * @brief Response DTO representing active user subscription tier and remaining quotas.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @brief Response DTO representing active user subscription tier and remaining quotas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    /** @brief Field representing tier in SubscriptionResponse. */
    private SubscriptionTier tier;
    /** @brief Field representing expires at in SubscriptionResponse. */
    private LocalDateTime expiresAt;
    /** @brief Field representing evaluations remaining in SubscriptionResponse. */
    private Integer evaluationsRemaining;
    /** @brief Field representing ocr remaining in SubscriptionResponse. */
    private Integer ocrRemaining;
    /** @brief Field representing unlimited in SubscriptionResponse. */
    private boolean unlimited;
}
