package com.linguaoptima.api.exception;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import lombok.Getter;

/**
 * @file PaymentException.java
 * @brief Exception thrown during payment authorization, settlement, or balance errors.
 */
@Getter
public class PaymentException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    /**
     * @brief Constructs a PaymentException with generic PAYMENT_FAILED code and message.
     * @param message Detailed error explanation.
     */
    public PaymentException(String message) {
        super(message);
        this.errorCode = PaymentErrorCode.PAYMENT_FAILED;
    }

    /**
     * @brief Constructs a PaymentException with a specific PaymentErrorCode and message.
     * @param errorCode Machine-readable error code.
     * @param message Detailed error explanation.
     */
    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
