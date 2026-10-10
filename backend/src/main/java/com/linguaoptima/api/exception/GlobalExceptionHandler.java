/**
 * @file GlobalExceptionHandler.java
 * @brief Centralized exception handler and HTTP error response mapper.
 */
package com.linguaoptima.api.exception;

import com.linguaoptima.api.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @brief Centralized exception handler and HTTP error response mapper.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @brief Translates OcrException into HTTP 422 Unprocessable Entity error response.
     * @param ex Caught OcrException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(OcrException.class)
    public ResponseEntity<ErrorResponse> handleOcrException(OcrException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "Image is unclear, please try again.")
                .errorCode("OCR_ERROR")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates QuotaExceededException into HTTP 429 Too Many Requests response.
     * @param ex Caught QuotaExceededException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "Weekly evaluation limit reached.")
                .errorCode("QUOTA_EXCEEDED")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates ConcurrentGenerationException into HTTP 409 Conflict response with takeover prompt.
     * @param ex Caught ConcurrentGenerationException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(ConcurrentGenerationException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentGeneration(ConcurrentGenerationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "AI task generation is currently running on another device.")
                .errorCode("CONCURRENT_GENERATION")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates AIServiceException into HTTP 503 Service Unavailable response.
     * @param ex Caught AIServiceException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<ErrorResponse> handleAIServiceException(AIServiceException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
            ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "AI service temporarily unavailable.")
                .errorCode("AI_SERVICE_UNAVAILABLE")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates PaymentException into HTTP 402 Payment Required response.
     * @param ex Caught PaymentException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            ErrorResponse.builder()
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode().name() : "PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates ResourceNotFoundException into HTTP 404 Not Found response.
     * @param ex Caught ResourceNotFoundException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .errorCode("RESOURCE_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates UnauthorizedException into HTTP 401 Unauthorized response.
     * @param ex Caught UnauthorizedException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message(ex.getMessage())
                .errorCode("UNAUTHORIZED")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates BadCredentialsException into HTTP 401 Unauthorized response.
     * @param ex Caught BadCredentialsException.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid email or password")
                .errorCode("BAD_CREDENTIALS")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates ForbiddenException and AccessDeniedException into HTTP 403 Forbidden response.
     * @param ex Caught security exception.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "Access denied")
                .errorCode("FORBIDDEN")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Translates MethodArgumentNotValidException into HTTP 400 Bad Request with field validation errors.
     * @param ex Caught validation exception.
     * @return ErrorResponse ResponseEntity with error item list.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build()
        );
    }

    /**
     * @brief Translates MaxUploadSizeExceededException into HTTP 413 Payload Too Large response.
     * @param ex Caught upload size exception.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ErrorResponse.builder()
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .message("Uploaded file exceeds 10MB limit")
                .errorCode("FILE_TOO_LARGE")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }

    /**
     * @brief Handles unhandled general exceptions, returning HTTP 500 Internal Server Error.
     * @param ex Caught generic Exception.
     * @return ErrorResponse ResponseEntity.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected internal error occurred")
                .errorCode("INTERNAL_SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .build()
        );
    }
}
