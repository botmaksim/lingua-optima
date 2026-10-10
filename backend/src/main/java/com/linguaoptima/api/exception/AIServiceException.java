/**
 * @file AIServiceException.java
 * @brief Exception thrown when all upstream AI providers fail or when inference cannot proceed.
 */
package com.linguaoptima.api.exception;

/**
 * @brief Exception thrown when all upstream AI providers fail or when inference cannot proceed.
 */
public class AIServiceException extends RuntimeException {

    /**
     * @brief Constructs an AIServiceException with an explanatory error message.
     * @param message Detailed reason for the AI service failure.
     */
    public AIServiceException(String message) {
        super(message);
    }

    /**
     * @brief Constructs an AIServiceException with an explanatory error message and root cause.
     * @param message Detailed reason for the AI service failure.
     * @param cause Underlying root exception cause.
     */
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
