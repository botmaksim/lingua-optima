/**
 * @file PaymentService.java
 * @brief Payment gateway integration stub with test simulation capabilities.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @brief Payment gateway integration stub with test simulation capabilities.
 *
 * Implements billing flows and provides test hooks to simulate gateway error states
 * such as card declines and insufficient funds.
 */
@Slf4j
@Service
public class PaymentService {

    /**
     * @brief Processes a subscription payment transaction.
     * @param amount Transaction amount in USD.
     * @param paymentToken Payment gateway token or simulated test token.
     * @return PaymentResultResponse DTO containing transaction ID and status.
     * @throws PaymentException if payment token indicates a simulated card decline or insufficient funds.
     */
    public PaymentResultResponse processPayment(double amount, String paymentToken) {
        log.info("Processing payment stub for amount: {}, token: {}", amount, paymentToken);

        if ("DECLINE_TOKEN".equalsIgnoreCase(paymentToken)) {
            throw new PaymentException(PaymentErrorCode.CARD_DECLINED, "Card was declined by issuing bank");
        }
        if ("INSUFFICIENT_FUNDS_TOKEN".equalsIgnoreCase(paymentToken)) {
            throw new PaymentException(PaymentErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds in account");
        }

        String transactionId = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResultResponse.builder()
            .success(true)
            .transactionId(transactionId)
            .errorCode(null)
            .errorMessage(null)
            .build();
    }
}
