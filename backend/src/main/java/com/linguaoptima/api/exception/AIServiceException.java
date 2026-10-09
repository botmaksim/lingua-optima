package com.linguaoptima.api.exception;

/**
 * @file AIServiceException.java
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
}
