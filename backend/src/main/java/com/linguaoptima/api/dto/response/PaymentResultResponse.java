/**
 * @file PaymentResultResponse.java
 * @brief Response DTO representing the outcome of a subscription payment transaction.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO representing the outcome of a subscription payment transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultResponse {

    /** @brief Field representing success in PaymentResultResponse. */
    private boolean success;
    /** @brief Field representing transaction id in PaymentResultResponse. */
    private String transactionId;
    /** @brief Field representing error code in PaymentResultResponse. */
    private String errorCode;
    /** @brief Field representing error message in PaymentResultResponse. */
    private String errorMessage;
}
