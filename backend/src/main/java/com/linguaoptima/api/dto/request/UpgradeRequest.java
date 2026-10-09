/**
 * @file UpgradeRequest.java
 * @brief Request payload for upgrading subscription tier with payment token.
 */
package com.linguaoptima.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Request payload for upgrading subscription tier with payment token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeRequest {

    /** @brief Target subscription tier to upgrade to (accepts targetTier or tier). */
    @JsonAlias("tier")
    @NotNull(message = "Target tier is required")
    private SubscriptionTier targetTier;

    /** @brief Optional payment provider verification token. */
    private String paymentToken;
}
