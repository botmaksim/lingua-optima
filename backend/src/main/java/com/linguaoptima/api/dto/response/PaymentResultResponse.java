package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @file PaymentResultResponse.java
 * @brief Response DTO representing the outcome of a subscription payment transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {

    private boolean success;
    private String transactionId;
    private String errorCode;
    private String errorMessage;
}
