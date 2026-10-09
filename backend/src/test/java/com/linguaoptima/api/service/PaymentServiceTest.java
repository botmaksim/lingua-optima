package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @file PaymentServiceTest.java
 * @brief Unit and slice test suite for PaymentService.
 */
class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    @Test
    void testProcessPaymentSuccessStub() {
        PaymentResultResponse res = paymentService.processPayment(19.99, "valid_token");
        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertNotNull(res.getTransactionId());
        assertTrue(res.getTransactionId().startsWith("STUB-"));
        assertNull(res.getErrorCode());
    }

    @Test
    void testProcessPaymentCardDeclinedErrorTrigger() {
        PaymentException ex = assertThrows(PaymentException.class,
            () -> paymentService.processPayment(10.0, "DECLINE_TOKEN"));
        assertEquals(PaymentErrorCode.CARD_DECLINED, ex.getErrorCode());
    }

    @Test
    void testProcessPaymentInsufficientFundsTrigger() {
        PaymentException ex = assertThrows(PaymentException.class,
            () -> paymentService.processPayment(10.0, "INSUFFICIENT_FUNDS_TOKEN"));
        assertEquals(PaymentErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
    }
}
