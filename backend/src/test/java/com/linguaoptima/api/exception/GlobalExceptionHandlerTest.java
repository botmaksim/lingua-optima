/**
 * @file GlobalExceptionHandlerTest.java
 * @brief Unit and slice test suite for GlobalExceptionHandler.
 */
package com.linguaoptima.api.exception;

import com.linguaoptima.api.domain.enums.PaymentErrorCode;
import com.linguaoptima.api.dto.response.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    /** @brief Test fixture or mock dependency for handler. */
    private GlobalExceptionHandler handler;

    /**
     * @brief Initializes test fixtures and mock state before each test in GlobalExceptionHandlerTest.
     */
    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    /**
     * @brief Verifies unit test scenario: handle ocr exception.
     */
    @Test
    void testHandleOcrException() {
        ResponseEntity<ErrorResponse> res = handler.handleOcrException(new OcrException("Image blurred"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, res.getStatusCode());
        assertEquals(422, res.getBody().getStatus());
        assertEquals("Image blurred", res.getBody().getMessage());
    }

    /**
     * @brief Verifies unit test scenario: handle quota exceeded.
     */
    @Test
    void testHandleQuotaExceeded() {
        ResponseEntity<ErrorResponse> res = handler.handleQuotaExceeded(new QuotaExceededException("Limit reached"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, res.getStatusCode());
        assertEquals(429, res.getBody().getStatus());
    }

    /**
     * @brief Verifies unit test scenario: handle aiservice exception.
     */
    @Test
    void testHandleAIServiceException() {
        ResponseEntity<ErrorResponse> res = handler.handleAIServiceException(new AIServiceException("AI down"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
        assertEquals(503, res.getBody().getStatus());

        AIServiceException withCause = new AIServiceException("AI down", new RuntimeException("Root cause"));
        assertEquals("AI down", withCause.getMessage());
        assertNotNull(withCause.getCause());
    }

    /**
     * @brief Verifies unit test scenario: handle payment exception.
     */
    @Test
    void testHandlePaymentException() {
        ResponseEntity<ErrorResponse> res = handler.handlePaymentException(
            new PaymentException(PaymentErrorCode.CARD_DECLINED, "Card declined"));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, res.getStatusCode());
        assertEquals(402, res.getBody().getStatus());
        assertEquals("CARD_DECLINED", res.getBody().getErrorCode());
    }

    /**
     * @brief Verifies unit test scenario: handle resource not found.
     */
    @Test
    void testHandleResourceNotFound() {
        ResponseEntity<ErrorResponse> res = handler.handleResourceNotFound(new ResourceNotFoundException("Not found"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertEquals(404, res.getBody().getStatus());
    }

    /**
     * @brief Verifies unit test scenario: handle unauthorized and bad credentials.
     */
    @Test
    void testHandleUnauthorizedAndBadCredentials() {
        ResponseEntity<ErrorResponse> r1 = handler.handleUnauthorized(new UnauthorizedException("Unauthorized"));
        assertEquals(HttpStatus.UNAUTHORIZED, r1.getStatusCode());

        ResponseEntity<ErrorResponse> r2 = handler.handleBadCredentials(new BadCredentialsException("Bad creds"));
        assertEquals(HttpStatus.UNAUTHORIZED, r2.getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: handle forbidden.
     */
    @Test
    void testHandleForbidden() {
        ResponseEntity<ErrorResponse> res = handler.handleForbidden(new ForbiddenException("Denied"));
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertEquals(403, res.getBody().getStatus());
    }

    /**
     * @brief Verifies unit test scenario: handle validation.
     */
    @Test
    void testHandleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "field", "is required")));

        ResponseEntity<ErrorResponse> res = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals(1, res.getBody().getErrors().size());
    }

    /**
     * @brief Verifies unit test scenario: handle max size.
     */
    @Test
    void testHandleMaxSize() {
        ResponseEntity<ErrorResponse> res = handler.handleMaxSize(new MaxUploadSizeExceededException(1000));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, res.getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: handle generic and null message fallbacks.
     */
    @Test
    void testHandleGeneric() {
        ResponseEntity<ErrorResponse> res = handler.handleGeneric(new RuntimeException("Crash"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());

        assertEquals("Image is unclear, please try again.",
            handler.handleOcrException(new OcrException(null)).getBody().getMessage());
        assertEquals("Weekly evaluation limit reached.",
            handler.handleQuotaExceeded(new QuotaExceededException(null)).getBody().getMessage());
        assertEquals("AI service temporarily unavailable.",
            handler.handleAIServiceException(new AIServiceException(null)).getBody().getMessage());
        assertEquals("PAYMENT_FAILED",
            handler.handlePaymentException(new PaymentException(null, "err")).getBody().getErrorCode());
        assertEquals("Access denied",
            handler.handleForbidden(new ForbiddenException(null)).getBody().getMessage());
        assertEquals("An unexpected internal error occurred",
            handler.handleGeneric(new RuntimeException((String) null)).getBody().getMessage());
    }
}
