package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeRequest {

    @NotNull(message = "Target tier is required")
    private SubscriptionTier targetTier;

    private String paymentToken; // Token for payment stub
}
