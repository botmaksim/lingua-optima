/**
 * @file PricingProperties.java
 * @brief Centralized configuration properties for subscription tiers, token limits, and pricing.
 */
package com.linguaoptima.api.config;

import com.linguaoptima.api.domain.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @brief Configuration properties mapped from application.yml under `app.pricing`.
 *
 * Externalizes all pricing, token limits, and quota parameters so they are configurable
 * without modifying Java code.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    /** @brief Configuration settings for FREE tier accounts. */
    private TierConfig free = new TierConfig(50_000L, 10, 3, 0.0, 1, 1, 1);

    /** @brief Configuration settings for PREMIUM tier accounts. */
    private TierConfig premium = new TierConfig(1_000_000L, 500, 100, 9.99, 3, 5, 5);

    /** @brief Configuration settings for EDUCATOR tier accounts. */
    private TierConfig educator = new TierConfig(10_000_000L, 10_000, 2_000, 49.99, 50, 50, 50);

    /**
     * @brief Detailed quota and pricing limits per subscription tier.
     */
    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierConfig {
        /** @brief Weekly token allowance across AI evaluation and generation. */
        private long weeklyTokenLimit = 50_000L;
        /** @brief Maximum task evaluations allowed per week. */
        private int weeklyEvaluationLimit = 10;
        /** @brief Maximum OCR uploads allowed per week. */
        private int weeklyOcrLimit = 3;
        /** @brief Monthly subscription cost in USD. */
        private double monthlyPriceUsd = 0.0;
        /** @brief Maximum active student groups/cohorts allowed. */
        private int maxGroups = 1;
        /** @brief Maximum saved custom grammar rules. */
        private int maxCustomRules = 1;
        /** @brief Maximum saved custom vocabulary dictionaries. */
        private int maxCustomVocabularySets = 1;

        public TierConfig(long weeklyTokenLimit, int weeklyEvaluationLimit, int weeklyOcrLimit, double monthlyPriceUsd) {
            this(weeklyTokenLimit, weeklyEvaluationLimit, weeklyOcrLimit, monthlyPriceUsd, 1, 1, 1);
        }
    }

    /**
     * @brief Resolves configuration settings for a given subscription tier.
     * @param tier Subscription tier enum value.
     * @return TierConfig matching the requested tier.
     */
    public TierConfig getTierConfig(SubscriptionTier tier) {
        if (tier == null) return free;
        return switch (tier) {
            case FREE -> free;
            case PREMIUM -> premium;
            case EDUCATOR -> educator;
        };
    }
}
