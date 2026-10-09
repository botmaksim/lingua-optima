package com.linguaoptima.api.exception;

/**
 * @file UnauthorizedException.java
 * @brief Exception thrown when authentication tokens are missing, expired, or revoked.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * @brief Constructs an UnauthorizedException with an explanatory message.
     * @param message Detailed reason for the authentication failure.
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
