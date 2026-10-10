/**
 * @file UsageResponse.java
 * @brief Response DTO detailing weekly AI evaluation and OCR quota consumption.
 */
package com.linguaoptima.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @brief Response DTO detailing weekly AI evaluation and OCR quota consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageResponse {

    /** @brief Number of AI evaluations consumed during the current billing week. */
    private int weekEvaluations;

    /** @brief Number of OCR image scans consumed during the current billing week. */
    private int weekOcrUploads;

    /** @brief Accumulated tokens consumed during the current billing week. */
    private long weekTokensUsed;

    /** @brief Maximum tokens allowed per week (null if unlimited). */
    private Long tokenLimit;

    /** @brief Remaining tokens for the week (null if unlimited). */
    private Long tokensRemaining;

    /** @brief Maximum AI evaluations allowed per week (null if unlimited). */
    private Integer evaluationLimit;

    /** @brief Maximum OCR uploads allowed per week (null if unlimited). */
    private Integer ocrLimit;

    /** @brief Remaining AI evaluations for the week (null if unlimited). */
    private Integer evaluationsRemaining;

    /** @brief Remaining OCR uploads for the week (null if unlimited). */
    private Integer ocrRemaining;

    /** @brief Timestamp when weekly quota counters reset. */
    private LocalDateTime weekResetAt;

    /**
     * @brief Alias exposing weekEvaluations as dailyAiEvaluations for frontend compatibility.
     * @return Consumed AI evaluations count.
     */
    @JsonProperty("dailyAiEvaluations")
    public int getDailyAiEvaluations() {
        return weekEvaluations;
    }

    /**
     * @brief Alias exposing evaluationLimit as dailyEvaluationsLimit (999999 when unlimited).
     * @return Evaluation quota limit.
     */
    @JsonProperty("dailyEvaluationsLimit")
    public int getDailyEvaluationsLimit() {
        return evaluationLimit != null ? evaluationLimit : 999999;
    }

    /**
     * @brief Alias exposing weekOcrUploads as dailyOcrScans for frontend compatibility.
     * @return Consumed OCR scans count.
     */
    @JsonProperty("dailyOcrScans")
    public int getDailyOcrScans() {
        return weekOcrUploads;
    }

    /**
     * @brief Alias exposing ocrLimit as dailyOcrLimit (999999 when unlimited).
     * @return OCR quota limit.
     */
    @JsonProperty("dailyOcrLimit")
    public int getDailyOcrLimit() {
        return ocrLimit != null ? ocrLimit : 999999;
    }

    /**
     * @brief Alias exposing weekResetAt as resetDate for frontend compatibility.
     * @return Quota reset timestamp.
     */
    @JsonProperty("resetDate")
    public LocalDateTime getResetDate() {
        return weekResetAt;
    }
}
