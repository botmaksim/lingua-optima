package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @file UsageResponse.java
 * @brief Response DTO detailing weekly AI evaluation and OCR quota consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageResponse {

    private int weekEvaluations;
    private int weekOcrUploads;
    private Integer evaluationLimit;
    private Integer ocrLimit;
    private Integer evaluationsRemaining;
    private Integer ocrRemaining;
    private LocalDateTime weekResetAt;
}
