package com.linguaoptima.api.domain.enums;

/**
 * @file PaymentErrorCode.java
 * @brief Standardized payment error codes returned by the billing stub infrastructure.
 */
public enum PaymentErrorCode {
    PAYMENT_FAILED,
    CARD_DECLINED,
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD,
    NETWORK_ERROR,
    PROVIDER_ERROR
}
