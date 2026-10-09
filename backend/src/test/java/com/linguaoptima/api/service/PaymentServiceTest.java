/**
 * @file PaymentServiceTest.java
 * @brief Unit and slice test suite for PaymentService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit and slice test suite for PaymentService.
 */
class PaymentServiceTest {

    /** @brief Test fixture or mock dependency for payment service. */
    private PaymentService paymentService;

    /**
     * @brief Initializes test fixtures and mock state before each test in PaymentServiceTest.
     */
    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
    }

    /**
     * @brief Verifies unit test scenario: process payment success stub.
     */
    @Test
    void testProcessPaymentSuccessStub() {
        PaymentResultResponse res = paymentService.processPayment(19.99, "valid_token");
        assertNotNull(res);
        assertTrue(res.isSuccess());
        assertNotNull(res.getTransactionId());
        assertTrue(res.getTransactionId().startsWith("STUB-"));
        assertNull(res.getErrorCode());
    }

    /**
     * @brief Verifies unit test scenario: process payment card declined error trigger.
     */
    @Test
    void testProcessPaymentCardDeclinedErrorTrigger() {
        PaymentException ex = assertThrows(PaymentException.class,
            () -> paymentService.processPayment(10.0, "DECLINE_TOKEN"));
        assertEquals(PaymentErrorCode.CARD_DECLINED, ex.getErrorCode());
    }

    /**
     * @brief Verifies unit test scenario: process payment insufficient funds trigger.
     */
    @Test
    void testProcessPaymentInsufficientFundsTrigger() {
        PaymentException ex = assertThrows(PaymentException.class,
            () -> paymentService.processPayment(10.0, "INSUFFICIENT_FUNDS_TOKEN"));
        assertEquals(PaymentErrorCode.INSUFFICIENT_FUNDS, ex.getErrorCode());
    }
}
