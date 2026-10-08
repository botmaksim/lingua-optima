package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * PaymentService STUB:
 * Always returns successful payment results for testing.
 * Provides complete error infrastructure as specified in design documentation.
 */
@Slf4j
@Service
public class PaymentService {

    public PaymentResultResponse processPayment(double amount, String paymentToken) {
        log.info("Processing payment stub for amount: {}, token: {}", amount, paymentToken);

        // Optional test token triggering simulated error for testing error handling pipeline
        if ("DECLINE_TOKEN".equalsIgnoreCase(paymentToken)) {
            throw new PaymentException(PaymentErrorCode.CARD_DECLINED, "Card was declined by issuing bank");
        }
        if ("INSUFFICIENT_FUNDS_TOKEN".equalsIgnoreCase(paymentToken)) {
            throw new PaymentException(PaymentErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds in account");
        }

        // Default stub behavior: Always succeeds
        String transactionId = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResultResponse.builder()
            .success(true)
            .transactionId(transactionId)
            .errorCode(null)
            .errorMessage(null)
            .build();
    }
}
