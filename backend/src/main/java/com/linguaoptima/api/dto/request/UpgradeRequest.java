package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file UpgradeRequest.java
 * @brief Request payload for upgrading subscription tier with payment token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeRequest {

    @NotNull(message = "Target tier is required")
    private SubscriptionTier targetTier;

    private String paymentToken;
}
