/**
 * @file PaymentErrorCode.java
 * @brief Standardized payment error codes returned by the billing stub infrastructure.
 */
package com.linguaoptima.api.domain.enums;

/**
 * @brief Standardized payment error codes returned by the billing stub infrastructure.
 */
public enum PaymentErrorCode {
    /** @brief Constant or enum value representing payment failed in PaymentErrorCode. */
    PAYMENT_FAILED,
    /** @brief Constant or enum value representing card declined in PaymentErrorCode. */
    CARD_DECLINED,
    /** @brief Constant or enum value representing insufficient funds in PaymentErrorCode. */
    INSUFFICIENT_FUNDS,
    /** @brief Constant or enum value representing expired card in PaymentErrorCode. */
    EXPIRED_CARD,
    /** @brief Constant or enum value representing network error in PaymentErrorCode. */
    NETWORK_ERROR,
    PROVIDER_ERROR
/** @brief Constant or enum value representing provider error in PaymentErrorCode. */
}
