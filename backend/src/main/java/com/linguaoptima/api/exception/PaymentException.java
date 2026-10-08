package com.linguaoptima.api.exception;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import lombok.Getter;

@Getter
public class PaymentException extends RuntimeException {
    private final PaymentErrorCode errorCode;

    public PaymentException(String message) {
        super(message);
        this.errorCode = PaymentErrorCode.PAYMENT_FAILED;
    }

    public PaymentException(PaymentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
