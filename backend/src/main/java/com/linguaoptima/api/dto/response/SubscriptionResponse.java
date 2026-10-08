package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
